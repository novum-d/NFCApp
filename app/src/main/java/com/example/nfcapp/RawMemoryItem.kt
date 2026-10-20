package com.example.nfcapp

import java.util.Locale

/** 読取領域の先頭からの位置と、バイト列の表示用表現。業務データの解析はしない。 */
data class RawMemoryItem(val offset: Int, val hex: String, val ascii: String)

/** 4バイト単位で整形する。文字コードが未確定のため印字可能ASCII以外は「.」にする。 */
fun formatRawMemory(memory: ByteArray): List<RawMemoryItem> {
    require(memory.size % 4 == 0) { "Memory must contain complete 4-byte units" }
    return (memory.indices step 4).map { offset ->
        val bytes = memory.copyOfRange(offset, offset + 4)
        RawMemoryItem(
            offset = offset,
            hex = bytes.joinToString(" ") { "%02X".format(Locale.ROOT, it.toInt() and 0xFF) },
            ascii = buildString {
                for (byte in bytes) {
                    val value = byte.toInt() and 0xFF
                    append(if (value in 0x20..0x7E) value.toChar() else '.')
                }
            },
        )
    }
}
