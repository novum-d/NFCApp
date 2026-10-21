package com.example.nfcapp

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test

class IcodeSliReaderTest {
    @Test
    fun readsOnlyPhysicalBlocksOneThroughFourInOrderAndReturnsEightItems() {
        val requests = mutableListOf<List<Byte>>()
        val responses = listOf(
            byteArrayOf(0x00, 0x00, 0x01, 0x02, 0x03),
            byteArrayOf(0x00, 0x04, 0x05, 0x06, 0x07),
            byteArrayOf(0x00, 0x08, 0x09, 0x0A, 0x0B),
            byteArrayOf(0x00, 0x0C, 0x0D, 0x0E, 0x0F),
        ).iterator()

        val result: Result<ImmutableList<RawMemoryItem>, NfcReadError> = readIcodeSliItems(
            transceive = { request ->
                requests.add(request.toList())
                responses.next()
            },
        )

        assertEquals(
            listOf(
                byteArrayOf(0x02, 0x20, 0x01).toList(),
                byteArrayOf(0x02, 0x20, 0x02).toList(),
                byteArrayOf(0x02, 0x20, 0x03).toList(),
                byteArrayOf(0x02, 0x20, 0x04).toList(),
            ),
            requests,
        )
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
            result.unwrap(),
        )
    }
}
