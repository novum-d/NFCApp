package com.example.nfcapp

import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.runCatching
import java.io.ByteArrayOutputStream
import java.io.IOException

/** NTAG213からバイト列を読む層。取得した値の業務上の意味付けは[BusinessIdParser]に任せる。 */
class Ntag213Reader {
    fun read(tag: Tag): Ntag213Read {
        // Rawコマンドを送るためNfcAへ接続し、GET_VERSIONで対象タグを確認する。
        val nfcA = NfcA.get(tag) ?: throw UnsupportedTagException("NFC-A is unavailable")
        val userMemory = runCatching {
            nfcA.connect()
            nfcA.timeout = 1_000

            val version = nfcA.transceive(byteArrayOf(0x60))
            if (!Ntag213Format.isNtag213(version)) {
                throw UnsupportedTagException("NFC-A tag is not NTAG213")
            }

            // READ(0x30)は指定ページから4ページ＝16バイトを返す。
            // 0x04から0x24まで4ページずつ進め、ユーザーメモリ144バイトをつなぐ。
            val output = ByteArrayOutputStream(Ntag213Format.USER_MEMORY_SIZE)
            for (page in 0x04..0x24 step 4) {
                val response = nfcA.transceive(byteArrayOf(0x30, page.toByte()))
                if (response.size != 16) {
                    throw IOException("READ page 0x%02X returned %d bytes".format(page, response.size))
                }
                output.write(response)
            }
            output.toByteArray()
        }.also {
            // 途中で失敗した場合もRaw接続を閉じ、後続のNdef接続と競合させない。
            runCatching { nfcA.close() }
        }.getOrThrow()

        // NfcAを閉じてからNdefへ接続し、Android標準APIが読むメッセージを取得する。
        // Raw側のTLV抽出結果との比較はNtag213Readにまとめる。
        val ndef = Ndef.get(tag)
        val ndefMessage = ndef?.let {
            runCatching {
                it.connect()
                it.ndefMessage?.toByteArray()
            }.also { _ ->
                runCatching { it.close() }
            }.getOrThrow()
        }

        return Ntag213Read(
            uid = tag.id.copyOf(),
            userMemory = userMemory,
            platformNdefAvailable = ndef != null,
            platformNdefMessage = ndefMessage,
        )
    }
}

class UnsupportedTagException(message: String) : Exception(message)

data class Ntag213Read(
    val uid: ByteArray,
    val userMemory: ByteArray,
    val platformNdefAvailable: Boolean,
    val platformNdefMessage: ByteArray?,
) {
    // Raw READで得たユーザーメモリから、NDEF Message TLVの値だけを取り出す。
    val rawNdefMessage: ByteArray? get() = Ntag213Format.findNdefMessage(userMemory)

    // 標準APIが使えない場合、メッセージがない場合、一致／不一致を区別する。
    val comparison: NdefComparison get() = when {
        !platformNdefAvailable -> NdefComparison.PLATFORM_NDEF_UNAVAILABLE
        platformNdefMessage == null && (rawNdefMessage == null || rawNdefMessage?.isEmpty() == true) ->
            NdefComparison.NO_MESSAGE
        platformNdefMessage?.contentEquals(rawNdefMessage) == true -> NdefComparison.MATCH
        else -> NdefComparison.MISMATCH
    }
}

enum class NdefComparison {
    MATCH,
    MISMATCH,
    NO_MESSAGE,
    PLATFORM_NDEF_UNAVAILABLE,
}

/** タグから得たバイト列を業務IDへ変換する境界。タグI/Oの処理には変換規則を置かない。 */
fun interface BusinessIdParser {
    fun parse(read: Ntag213Read): String?
}

/** 業務IDの仕様が決まるまで、UIDや固定オフセットをIDとみなさない。 */
object UnspecifiedBusinessIdParser : BusinessIdParser {
    override fun parse(read: Ntag213Read): String? = null
}

/** NTAG213の読取結果を解釈するための判定・Type 2 TLV抽出。タグには書き込まない。 */
object Ntag213Format {
    const val USER_MEMORY_SIZE = 144

    fun isNtag213(version: ByteArray): Boolean =
        version.size == 8 &&
            version[0] == 0x00.toByte() &&
            version[1] == 0x04.toByte() &&
            version[2] == 0x04.toByte() &&
            version[6] == 0x0F.toByte() &&
            version[7] == 0x03.toByte()

    fun findNdefMessage(userMemory: ByteArray): ByteArray? {
        // ユーザーメモリを先頭からTLV単位で走査し、NDEF Message TLV (0x03)を探す。
        var offset = 0
        while (offset < userMemory.size) {
            val type = userMemory[offset++].toInt() and 0xFF
            when (type) {
                0x00 -> continue // NULL TLV
                0xFE -> return null // Terminator TLV
            }

            // NULL/終端以外は長さを読み、拡張長(0xFF)なら続く2バイトを使う。
            require(offset < userMemory.size) { "Truncated Type 2 TLV length" }
            var length = userMemory[offset++].toInt() and 0xFF
            if (length == 0xFF) {
                require(offset + 2 <= userMemory.size) { "Truncated extended TLV length" }
                length = ((userMemory[offset].toInt() and 0xFF) shl 8) or
                    (userMemory[offset + 1].toInt() and 0xFF)
                offset += 2
            }
            require(length <= userMemory.size - offset) { "TLV extends beyond NTAG213 user memory" }
            if (type == 0x03) return userMemory.copyOfRange(offset, offset + length)
            offset += length // NDEF以外のTLVは値を飛ばし、次のTLVへ進む。
        }
        return null
    }
}
