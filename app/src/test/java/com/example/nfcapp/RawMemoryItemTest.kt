package com.example.nfcapp

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test

class RawMemoryItemTest {
    @Test
    fun blockOneIsSplitIntoTwoItemsWithByteOffsets() {
        val bytes = byteArrayOf(0x00, 0x01, 0x02, 0x03)

        val result: ImmutableList<RawMemoryItem> = parseBlockItems(blockNumber = 1, bytes = bytes)

        assertEquals(
            persistentListOf(
                RawMemoryItem(blockNumber = 1, byteOffset = 0, hex = "00 01"),
                RawMemoryItem(blockNumber = 1, byteOffset = 2, hex = "02 03"),
            ),
            result,
        )
    }

    @Test
    fun blocksOneThroughFourAreSplitIntoEightItems() {
        val bytes = byteArrayOf(
            0x00, 0x01, 0x02, 0x03,
            0x04, 0x05, 0x06, 0x07,
            0x08, 0x09, 0x0A, 0x0B,
            0x0C, 0x0D, 0x0E, 0x0F,
        )

        val result: ImmutableList<RawMemoryItem> = parseMemoryItems(bytes)

        assertEquals(
            persistentListOf(
                RawMemoryItem(blockNumber = 1, byteOffset = 0, hex = "00 01"),
                RawMemoryItem(blockNumber = 1, byteOffset = 2, hex = "02 03"),
                RawMemoryItem(blockNumber = 2, byteOffset = 0, hex = "04 05"),
                RawMemoryItem(blockNumber = 2, byteOffset = 2, hex = "06 07"),
                RawMemoryItem(blockNumber = 3, byteOffset = 0, hex = "08 09"),
                RawMemoryItem(blockNumber = 3, byteOffset = 2, hex = "0A 0B"),
                RawMemoryItem(blockNumber = 4, byteOffset = 0, hex = "0C 0D"),
                RawMemoryItem(blockNumber = 4, byteOffset = 2, hex = "0E 0F"),
            ),
            result,
        )
    }
}
