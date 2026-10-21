package com.example.nfcapp

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class NfcVBlockReaderTest {
    @Test
    fun readsPhysicalBlockOneAndReturnsFourDataBytesAsOk() {
        val requests = mutableListOf<ByteArray>()

        val result: Result<ByteArray, NfcReadError> = readNfcVBlock(
            blockNumber = 1,
            transceive = { request ->
                requests.add(request.copyOf())
                byteArrayOf(0x00, 0x10, 0x11, 0x12, 0x13)
            },
        )

        assertEquals(1, requests.size)
        assertArrayEquals(byteArrayOf(0x02, 0x20, 0x01), requests.single())
        assertArrayEquals(byteArrayOf(0x10, 0x11, 0x12, 0x13), result.unwrap())
    }

    @Test
    fun readsPhysicalBlockFourWithoutChangingHighBitData() {
        val requests = mutableListOf<ByteArray>()

        val result = readNfcVBlock(
            blockNumber = 4,
            transceive = { request ->
                requests.add(request.copyOf())
                byteArrayOf(0x00, 0x80.toByte(), 0xFF.toByte(), 0x00, 0x01)
            },
        )

        assertEquals(1, requests.size)
        assertArrayEquals(byteArrayOf(0x02, 0x20, 0x04), requests.single())
        assertArrayEquals(byteArrayOf(0x80.toByte(), 0xFF.toByte(), 0x00, 0x01), result.unwrap())
    }
}
