package com.example.nfcapp.ui.main

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Transient screen status for the current Reader Mode session; no tag data is retained here. */
class NfcReadStatusStore {
    private val mutableStatus = MutableStateFlow<NfcReadStatus>(NfcReadStatus.Ready)
    val status: StateFlow<NfcReadStatus> = mutableStatus.asStateFlow()

    fun onReady() {
        mutableStatus.value = NfcReadStatus.Ready
    }

    fun onTagDiscovered() {
        mutableStatus.value = NfcReadStatus.Reading
    }

    fun onReadCompleted() {
        mutableStatus.value = NfcReadStatus.Completed
    }

    fun onReadFailed(reason: NfcReadFailure) {
        mutableStatus.value = NfcReadStatus.Failed(reason)
    }
}

sealed interface NfcReadStatus {
    data object Ready : NfcReadStatus
    data object Reading : NfcReadStatus
    data object Completed : NfcReadStatus
    data class Failed(val reason: NfcReadFailure) : NfcReadStatus
}

enum class NfcReadFailure {
    TAG_LOST,
    IO,
    INVALID_DATA,
    UNSUPPORTED_TAG,
    NFC_UNAVAILABLE,
}
