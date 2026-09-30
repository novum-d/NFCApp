package com.example.nfcapp

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Ntag213FormatTest {
    @Test fun recognizesNtag213Only() {
        assertTrue(Ntag213Format.isNtag213(byteArrayOf(0, 4, 4, 2, 1, 0, 0x0F, 3)))
        assertFalse(Ntag213Format.isNtag213(byteArrayOf(0, 4, 4, 2, 1, 0, 0x11, 3)))
        assertFalse(Ntag213Format.isNtag213(byteArrayOf(0, 4, 4)))
        assertFalse(Ntag213Format.isNtag213(byteArrayOf(1, 4, 4, 2, 1, 0, 0x0F, 3)))
        assertFalse(Ntag213Format.isNtag213(byteArrayOf(0, 4, 4, 2, 1, 0, 0x0F, 4)))
    }

    @Test fun extractsNdefAfterLockControlTlv() {
        val memory = byteArrayOf(0, 1, 3, 0xA0.toByte(), 0x0C, 0x34, 3, 3, 0xD1.toByte(), 1, 0, 0xFE.toByte())
        assertArrayEquals(byteArrayOf(0xD1.toByte(), 1, 0), Ntag213Format.findNdefMessage(memory))
    }

    @Test fun noNdefMessageIsNotAnIoFailure() {
        val memory = byteArrayOf(1, 3, 0xA0.toByte(), 0x0C, 0x34, 0xFE.toByte())
        assertEquals(null, Ntag213Format.findNdefMessage(memory))
        val read = Ntag213Read(byteArrayOf(1), memory, true, null)
        assertEquals(NdefComparison.NO_MESSAGE, read.comparison)
    }

    @Test fun comparesMessageRatherThanWholeRawMemory() {
        val message = byteArrayOf(0xD1.toByte(), 1, 0)
        val read = Ntag213Read(byteArrayOf(1), byteArrayOf(3, 3, *message, 0xFE.toByte()), true, message)
        assertEquals(NdefComparison.MATCH, read.comparison)
    }

    @Test fun initializedEmptyNdefTlvIsNoMessage() {
        val read = Ntag213Read(byteArrayOf(1), byteArrayOf(3, 0, 0xFE.toByte()), true, null)
        assertEquals(NdefComparison.NO_MESSAGE, read.comparison)
    }

    @Test fun nullAndUnknownTlvsAreSkippedBeforeNdef() {
        val memory = byteArrayOf(0, 0xFD.toByte(), 2, 0x11, 0x22, 3, 1, 0x31)
        assertArrayEquals(byteArrayOf(0x31), Ntag213Format.findNdefMessage(memory))
    }

    @Test fun extendedLengthNdefTlvIsReadAsBigEndianLength() {
        val memory = byteArrayOf(3, 0xFF.toByte(), 0, 2, 0x31, 0x32, 0xFE.toByte())
        assertArrayEquals(byteArrayOf(0x31, 0x32), Ntag213Format.findNdefMessage(memory))
    }

    @Test fun terminatorPreventsReadingLaterNdef() {
        assertEquals(null, Ntag213Format.findNdefMessage(byteArrayOf(0xFE.toByte(), 3, 1, 0x31)))
    }

    @Test fun detectsDifferentMessages() {
        val read = Ntag213Read(byteArrayOf(1), byteArrayOf(3, 1, 0x31), true, byteArrayOf(0x32))
        assertEquals(NdefComparison.MISMATCH, read.comparison)
    }

    @Test fun reportsUnavailablePlatformNdef() {
        val read = Ntag213Read(byteArrayOf(1), byteArrayOf(3, 1, 0x31), false, null)
        assertEquals(NdefComparison.PLATFORM_NDEF_UNAVAILABLE, read.comparison)
    }

    @Test fun rawNdefWithoutPlatformMessageIsMismatch() {
        val read = Ntag213Read(byteArrayOf(1), byteArrayOf(3, 1, 0x31), true, null)
        assertEquals(NdefComparison.MISMATCH, read.comparison)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsTruncatedExtendedLength() {
        Ntag213Format.findNdefMessage(byteArrayOf(3, 0xFF.toByte(), 0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsTlvLengthBeyondMemory() {
        Ntag213Format.findNdefMessage(byteArrayOf(3, 2, 0x31))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsTruncatedTlv() {
        Ntag213Format.findNdefMessage(byteArrayOf(3, 4, 1))
    }
}
