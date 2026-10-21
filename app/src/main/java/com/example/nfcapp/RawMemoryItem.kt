package com.example.nfcapp

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

private const val BYTES_PER_ITEM = 2

data class RawMemoryItem(
    val blockNumber: Int,
    val byteOffset: Int,
    val hex: String,
)

fun parseBlockItems(blockNumber: Int, bytes: ByteArray): ImmutableList<RawMemoryItem> =
    bytes.toList()
        .chunked(BYTES_PER_ITEM)
        .mapIndexed { index, itemBytes ->
            RawMemoryItem(
                blockNumber = blockNumber,
                byteOffset = index * BYTES_PER_ITEM,
                hex = itemBytes.toByteArray().toDisplayHex(),
            )
        }
        .toPersistentList()

/** 実ブロック1〜4から順番に読み取った16バイトを受け取る。 */
fun parseMemoryItems(bytes: ByteArray): ImmutableList<RawMemoryItem> = persistentListOf()
