package com.example.nfcapp

import android.nfc.Tag
import android.nfc.tech.NfcV
import com.github.michaelbull.result.Result
import kotlinx.collections.immutable.ImmutableList
import java.io.ByteArrayOutputStream

/**
 * ICODE SLIの実ブロック1〜4を読み、2バイト単位の8件へ変換するData層の入口。
 *
 * ReaderCallbackなどで得たTagを渡す。connect/transceiveはブロッキングI/Oなので、
 * 呼び出し元でDispatchers.IOなどのメインスレッド以外から実行する。
 * https://developer.android.com/reference/android/nfc/tech/NfcV
 */
class IcodeSliReader {
    fun read(tag: Tag): Result<ImmutableList<RawMemoryItem>, NfcReadError> = nfcResult {
        val nfcV = NfcV.get(tag) ?: throw UnsupportedTagException("NFC-V is unavailable")

        // 一度だけ接続し、4回の読取が終わったら切断する。例外時もuseがcloseを呼ぶ。
        // use/connectの公式サンプル:
        // https://developer.android.com/develop/connectivity/nfc/advanced-nfc#read-write
        nfcV.use { connection ->
            connection.connect()
            readIcodeSliItemsBytes(connection::transceive)
        }
    }
}

/** 接続済みタグの送受信を受け取る。テストでは模擬応答を渡し、通信手順を確認する。 */
fun readIcodeSliItems(
    transceive: (ByteArray) -> ByteArray,
): Result<ImmutableList<RawMemoryItem>, NfcReadError> = nfcResult {
    readIcodeSliItemsBytes(transceive)
}

private fun readIcodeSliItemsBytes(
    transceive: (ByteArray) -> ByteArray,
): ImmutableList<RawMemoryItem> {
    val memory = ByteArrayOutputStream(targetBlockNumbers.count() * BYTES_PER_BLOCK)

    // 実ブロック1, 2, 3, 4へ順番にRead Single Blockを送る。ブロック0は読まない。
    for (blockNumber in targetBlockNumbers) {
        memory.write(readNfcVBlockBytes(blockNumber, transceive))
    }

    // 取得した16バイトは既にテストした変換処理へ渡し、上位には不変リストを返す。
    return parseMemoryItems(memory.toByteArray())
}
