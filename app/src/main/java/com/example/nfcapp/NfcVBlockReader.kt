package com.example.nfcapp

import com.github.michaelbull.result.Result
import java.io.IOException

private const val HIGH_DATA_RATE: Byte = 0x02
private const val READ_SINGLE_BLOCK: Byte = 0x20
private const val RESPONSE_FLAGS_BYTES = 1
private const val RESPONSE_ERROR_FLAG = 0x01

/** 接続済みのNFC-Vタグから1ブロックを読む。接続・切断は呼び出し元が担当する。 */
fun readNfcVBlock(
    blockNumber: Int,
    transceive: (ByteArray) -> ByteArray,
): Result<ByteArray, NfcReadError> = nfcResult {
    require(blockNumber in 0..0xFF) { "Read Single Block requires a one-byte block number" }

    // 例: ブロック1なら [02 20 01]。
    // Flags=02: 高速・非アドレス指定・Optionなし、Command=20: Read Single Block。
    // UIDやCRCは付けない。CRCの付加はAndroidのNfcV.transceiveが担当する。
    // https://developer.android.com/reference/android/nfc/tech/NfcV#transceive(byte[])
    val request = byteArrayOf(HIGH_DATA_RATE, READ_SINGLE_BLOCK, blockNumber.toByte())
    val response = transceive(request)

    // Optionなしの正常応答は [Response Flags 1バイト][ブロックのデータ4バイト]。
    // エラー応答をデータとして扱わないため、既存Activityと同じ最小限の確認を行う。
    require(response.isNotEmpty()) { "NFC-V response is empty" }
    val responseFlags = response[0].toInt() and 0xFF
    if (responseFlags and RESPONSE_ERROR_FLAG != 0) {
        throw IOException("NFC-V Read Single Block failed for block $blockNumber")
    }
    require(response.size == RESPONSE_FLAGS_BYTES + BYTES_PER_BLOCK) {
        "Expected response flags and $BYTES_PER_BLOCK data bytes, received ${response.size} bytes"
    }

    // Flagsを除いた4バイトだけを返す。2バイト値の数値化やバイト順の変更は行わない。
    response.copyOfRange(RESPONSE_FLAGS_BYTES, response.size)
}
