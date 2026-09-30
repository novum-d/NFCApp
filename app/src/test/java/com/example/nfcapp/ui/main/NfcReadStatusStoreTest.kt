package com.example.nfcapp.ui.main

import org.junit.Assert.assertEquals
import org.junit.Test

class NfcReadStatusStoreTest {
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
        assertEquals(NfcReadStatus.Completed, store.status.value)
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
