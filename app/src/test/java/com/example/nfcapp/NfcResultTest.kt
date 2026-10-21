package com.example.nfcapp

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrapError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CancellationException

class NfcResultTest {
    @Test
    fun returnsTheBlockValueAsOkAndCallsItOnce() {
        var calls = 0

        val result: Result<String, NfcReadError> = nfcResult {
            calls++
            "read completed"
        }

        assertEquals(Ok("read completed"), result)
        assertEquals(1, calls)
    }

    @Test
    fun convertsIoExceptionsAndKeepsTheOriginalCause() {
        val cause = IOException("connection failed")

        val result = nfcResult<Unit> { throw cause }

        assertEquals(NfcReadError.Io(cause), result.unwrapError())
    }

    @Test
    fun convertsInvalidDataExceptions() {
        val cause = IllegalArgumentException("invalid data")

        val result = nfcResult<Unit> { throw cause }

        assertEquals(NfcReadError.InvalidData(cause), result.unwrapError())
    }

    @Test
    fun convertsSecurityExceptions() {
        val cause = SecurityException("tag handle is invalid")

        val result = nfcResult<Unit> { throw cause }

        assertEquals(NfcReadError.AccessDenied(cause), result.unwrapError())
    }

    @Test
    fun rethrowsCancellationInsteadOfReturningAnError() {
        val cause = CancellationException("reader stopped")

        val thrown = assertThrows(CancellationException::class.java) {
            nfcResult<Unit> { throw cause }
        }

        assertSame(cause, thrown)
    }
}
