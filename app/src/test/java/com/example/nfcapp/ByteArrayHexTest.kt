package com.example.nfcapp

import org.junit.Assert.assertEquals
import org.junit.Test

class ByteArrayHexTest {
    @Test
    fun zeroIsDisplayedAsTwoHexDigits() {
        val bytes = byteArrayOf(0x00)

        val result = bytes.toDisplayHex()

        assertEquals("00", result)
    }

    @Test
    fun fourBytesAreDisplayedAsUppercaseHexSeparatedBySpaces() {
        val bytes = byteArrayOf(0x00, 0x01, 0x0A, 0xFF.toByte())

        val result = bytes.toDisplayHex()

        assertEquals("00 01 0A FF", result)
    }
}
