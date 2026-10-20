package com.example.nfcapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RawMemoryItemTest {
    @Test fun formatsOffsetsUnsignedHexAndPrintableAsciiWithoutGuessingEncoding() {
        val items = formatRawMemory(
            byteArrayOf(0x41, 0x20, 0x7E, 0x7F, 0x00, 0x80.toByte(), 0xFF.toByte(), 0x30)
        )
        assertEquals(
            listOf(
                RawMemoryItem(0, "41 20 7E 7F", "A ~."),
                RawMemoryItem(4, "00 80 FF 30", "...0"),
            ),
            items,
        )
    }

    @Test fun rejectsIncompleteUnitsInsteadOfDisplayingPartialMemory() {
        assertThrows(IllegalArgumentException::class.java) { formatRawMemory(byteArrayOf(1, 2, 3)) }
    }

    @Test fun formattedRowsDoNotChangeWhenSourceBufferChanges() {
        val memory = byteArrayOf(0x41, 0x42, 0x43, 0x44)
        val items = formatRawMemory(memory)
        memory.fill(0)
        assertEquals(RawMemoryItem(0, "41 42 43 44", "ABCD"), items.single())
    }

    @Test fun emptyMemoryProducesNoItems() {
        assertEquals(emptyList<RawMemoryItem>(), formatRawMemory(byteArrayOf()))
    }
}
