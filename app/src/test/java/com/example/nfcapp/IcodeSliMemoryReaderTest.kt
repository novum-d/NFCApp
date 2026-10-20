package com.example.nfcapp

import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class IcodeSliMemoryReaderTest {
    @Test fun readsAllUserBlocksInOrderWithoutIncludingResponseFlags() {
        val commands = mutableListOf<ByteArray>()
        val memory = readIcodeSliUserMemory { request ->
            commands += request.copyOf()
            val block = request[2].toInt() and 0xFF
            byteArrayOf(0x00, block.toByte(), 0x7F, 0x80.toByte(), 0xFF.toByte())
        }

        assertEquals(28, commands.size)
        assertEquals(112, memory.size)
        commands.forEachIndexed { block, command ->
            assertArrayEquals(byteArrayOf(0x02, 0x20, block.toByte()), command)
            assertArrayEquals(
                byteArrayOf(block.toByte(), 0x7F, 0x80.toByte(), 0xFF.toByte()),
                memory.copyOfRange(block * 4, (block + 1) * 4),
            )
        }
    }

    @Test fun malformedResponsesStopReadingBeforeLaterBlocks() {
        for (badResponse in listOf(
            byteArrayOf(),
            byteArrayOf(0x00),
            byteArrayOf(0x00, 0x10, 0x20, 0x30),
            byteArrayOf(0x00, 0x10, 0x20, 0x30, 0x40, 0x50),
            byteArrayOf(0x01),
            byteArrayOf(0x01, 0x10, 0x20),
        )) {
            var calls = 0
            assertThrows(InvalidNfcVResponseException::class.java) {
                readIcodeSliUserMemory {
                    calls++
                    if (calls == 3) badResponse else byteArrayOf(0, 1, 2, 3, 4)
                }
            }
            assertEquals(3, calls)
        }
    }

    @Test fun errorFlagIsNotInterpretedAsMemoryData() {
        var calls = 0
        val error = assertThrows(IOException::class.java) {
            readIcodeSliUserMemory {
                calls++
                byteArrayOf(0x01, 0x10)
            }
        }
        assertEquals(1, calls)
        assertEquals("Read block 0 failed with error 0x10", error.message)
    }

    @Test fun transportFailureIsPropagatedWithoutRetryOrPartialResult() {
        val failure = IOException("Tag connection lost")
        var calls = 0
        val error = assertThrows(IOException::class.java) {
            readIcodeSliUserMemory {
                calls++
                if (calls == 5) throw failure
                byteArrayOf(0, 1, 2, 3, 4)
            }
        }
        assertSame(failure, error)
        assertEquals(5, calls)
    }
}
