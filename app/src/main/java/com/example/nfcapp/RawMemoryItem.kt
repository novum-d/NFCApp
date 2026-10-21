package com.example.nfcapp

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

private const val BYTES_PER_ITEM = 2
internal const val BYTES_PER_BLOCK = 4
internal val targetBlockNumbers = 1..4

data class RawMemoryItem(
    val blockNumber: Int,
    val byteOffset: Int,
    val hex: String,
)

fun ByteArray.parseBlockItems(blockNumber: Int): ImmutableList<RawMemoryItem> =
    toList()
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
fun ByteArray.parseMemoryItems(): ImmutableList<RawMemoryItem> =
    toList()
        .chunked(BYTES_PER_BLOCK)
        .flatMapIndexed { index, blockBytes ->
            blockBytes.toByteArray().parseBlockItems(
                blockNumber = targetBlockNumbers.first + index,
            )
        }
        .toPersistentList()
