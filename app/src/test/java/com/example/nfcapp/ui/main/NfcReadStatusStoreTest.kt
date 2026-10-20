package com.example.nfcapp.ui.main

import com.example.nfcapp.formatRawMemory
import org.junit.Assert.assertEquals
import org.junit.Test

class NfcReadStatusStoreTest {
    @Test fun completedItemsAreClearedWhenAnotherTagIsDiscoveredOrReadingFails() {
        val store = NfcReadStatusStore()
        val items = formatRawMemory(byteArrayOf(1, 2, 3, 4))
        store.onReadCompleted(items)
        assertEquals(NfcReadStatus.Completed(items), store.status.value)

        store.onTagDiscovered()
        assertEquals(NfcReadStatus.Reading, store.status.value)
        store.onReadFailed(NfcReadFailure.TAG_LOST)
        assertEquals(NfcReadStatus.Failed(NfcReadFailure.TAG_LOST), store.status.value)
    }

    @Test fun returningToReadyDropsPreviousTagData() {
        val store = NfcReadStatusStore()
        store.onReadCompleted(formatRawMemory(byteArrayOf(1, 2, 3, 4)))
        store.onReady()
        assertEquals(NfcReadStatus.Ready, store.status.value)
    }

    @Test fun publishingItemsTakesACopyOfTheMutableList() {
        val store = NfcReadStatusStore()
        val items = formatRawMemory(byteArrayOf(1, 2, 3, 4)).toMutableList()
        store.onReadCompleted(items)
        items.clear()
        assertEquals(1, (store.status.value as NfcReadStatus.Completed).items.size)
    }

    @Test fun tagLostReplacesReadingStateAndNextDetectionCanComplete() {
        val store = NfcReadStatusStore()
        assertEquals(NfcReadStatus.Ready, store.status.value)

        store.onTagDiscovered()
        assertEquals(NfcReadStatus.Reading, store.status.value)

        store.onReadFailed(NfcReadFailure.TAG_LOST)
        assertEquals(NfcReadStatus.Failed(NfcReadFailure.TAG_LOST), store.status.value)

        store.onTagDiscovered()
        assertEquals(NfcReadStatus.Reading, store.status.value)
        store.onReadCompleted()
        assertEquals(NfcReadStatus.Completed(), store.status.value)
    }

    @Test fun ioFailureDoesNotReportCompletion() {
        val store = NfcReadStatusStore()
        store.onTagDiscovered()
        store.onReadFailed(NfcReadFailure.IO)
        assertEquals(NfcReadStatus.Failed(NfcReadFailure.IO), store.status.value)
    }

    @Test fun malformedDataHasDifferentStatusFromIoFailure() {
        val store = NfcReadStatusStore()
        store.onTagDiscovered()
        store.onReadFailed(NfcReadFailure.INVALID_DATA)
        assertEquals(NfcReadStatus.Failed(NfcReadFailure.INVALID_DATA), store.status.value)
    }

    @Test fun unavailableNfcCanReturnToReadyWhenReaderModeIsEnabled() {
        val store = NfcReadStatusStore()
        store.onReadFailed(NfcReadFailure.NFC_UNAVAILABLE)
        store.onReady()
        assertEquals(NfcReadStatus.Ready, store.status.value)
    }
}
