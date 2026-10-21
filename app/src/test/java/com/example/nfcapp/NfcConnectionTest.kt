package com.example.nfcapp

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrapError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.CancellationException

class NfcConnectionTest {
    @Test
    fun closesAfterAReadSucceeds() {
        val events = mutableListOf<String>()

        val result = useNfcConnection(Closeable { events.add("close") }) {
            events.add("read")
            Ok("data")
        }

        assertEquals(Ok("data"), result)
        assertEquals(listOf("read", "close"), events)
    }

    @Test
    fun closesAfterAnErrAndPropagatesTheSameError() {
        val error = NfcReadError.Io(IOException("read failed"))
        var closeCount = 0

        val result = useNfcConnection(Closeable { closeCount++ }) { Err(error) }

        assertSame(error, result.unwrapError())
        assertEquals(1, closeCount)
    }

    @Test
    fun preservesTheReadErrorWhenClosingAlsoFails() {
        val error = NfcReadError.Io(IOException("read failed"))
        val closeFailure = IOException("close failed")

        val result = useNfcConnection(Closeable { throw closeFailure }) { Err(error) }

        assertSame(error, result.unwrapError())
        assertEquals(listOf(closeFailure), error.cause.suppressed.toList())
    }

    @Test
    fun returnsTheCloseErrorWhenTheReadSucceeded() {
        val closeFailure = IOException("close failed")

        val result = useNfcConnection(Closeable { throw closeFailure }) { Ok("data") }

        assertEquals(NfcReadError.Io(closeFailure), result.unwrapError())
    }

    @Test
    fun closesAndRethrowsCancellation() {
        val cancellation = CancellationException("reader stopped")
        var closeCount = 0

        val thrown = assertThrows(CancellationException::class.java) {
            useNfcConnection<Unit>(Closeable { closeCount++ }) { throw cancellation }
        }

        assertSame(cancellation, thrown)
        assertEquals(1, closeCount)
    }
}
