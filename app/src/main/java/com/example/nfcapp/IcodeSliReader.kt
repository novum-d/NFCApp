package com.example.nfcapp

import android.nfc.Tag
import android.nfc.tech.NfcV
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.binding
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
    fun read(tag: Tag): Result<ImmutableList<RawMemoryItem>, NfcReadError> =
        nfcResult {
            NfcV.get(tag) ?: throw UnsupportedTagException("NFC-V is unavailable")
        }.andThen { connection ->
            // 接続に成功した場合だけ読取へ進む。Errでもキャンセルでも最後に接続を閉じる。
            useNfcConnection(connection) {
                nfcResult { connection.connect() }
                    .andThen { readIcodeSliItems(connection::transceive) }
            }
        }
}

/** 接続済みタグの送受信を受け取る。テストでは模擬応答を渡し、通信手順を確認する。 */
fun readIcodeSliItems(
    transceive: (ByteArray) -> ByteArray,
): Result<ImmutableList<RawMemoryItem>, NfcReadError> = binding {
    val memory = ByteArrayOutputStream(targetBlockNumbers.count() * BYTES_PER_BLOCK)

    // 実ブロック1, 2, 3, 4へ順番にRead Single Blockを送る。ブロック0は読まない。
    for (blockNumber in targetBlockNumbers) {
        // bindはOkのデータを取り出し、Errなら同じエラーを返してここで打ち切る。
        // 例外の変換はreadNfcVBlockに任せ、ここではResultを連結する。
        val blockBytes = readNfcVBlock(blockNumber, transceive).bind()
        memory.write(blockBytes)
    }

    // 取得した16バイトは既にテストした変換処理へ渡し、上位には不変リストを返す。
    memory.toByteArray().parseMemoryItems()
}
