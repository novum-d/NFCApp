package com.example.nfcapp

import java.io.IOException

/**
 * SLI用のユーザー領域を単一ブロックREADで取得する。接続・切断は呼び出し側が管理する。
 * 既存の0x02 / 0x20コマンドを維持し、読取範囲だけをブロック0〜27へ広げる。
 * 全ブロックの検証が成功してから返し、途中のデータを成功結果として公開しない。
 */
internal fun readIcodeSliUserMemory(transceive: (ByteArray) -> ByteArray): ByteArray {
    val memory = ByteArray(SLI_BLOCK_COUNT * SLI_BLOCK_SIZE)
    for (block in 0 until SLI_BLOCK_COUNT) {
        // High data rate、Read Single Block、ブロック番号。CRCはAndroidが付加する。
        val response = transceive(byteArrayOf(0x02, 0x20, block.toByte()))
        val flags = response.firstOrNull()?.toInt()?.and(0xFF)
            ?: throw InvalidNfcVResponseException("Empty response for block $block")
        if (flags and 0x01 != 0) {
            if (response.size != 2) {
                throw InvalidNfcVResponseException("Malformed error response for block $block")
            }
            val error = response[1].toInt() and 0xFF
            throw IOException("Read block $block failed with error 0x${error.toString(16)}")
        }
        // Option flagなし。応答Flags 1バイトとデータ4バイトを区別する。
        if (response.size != 1 + SLI_BLOCK_SIZE) {
            throw InvalidNfcVResponseException(
                "Read block $block returned ${response.size} bytes; expected ${1 + SLI_BLOCK_SIZE}"
            )
        }
        response.copyInto(memory, block * SLI_BLOCK_SIZE, startIndex = 1)
    }
    return memory
}

internal class InvalidNfcVResponseException(message: String) : IOException(message)

private const val SLI_BLOCK_COUNT = 28
private const val SLI_BLOCK_SIZE = 4
