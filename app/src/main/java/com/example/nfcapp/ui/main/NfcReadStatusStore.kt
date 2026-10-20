package com.example.nfcapp.ui.main

import com.example.nfcapp.RawMemoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 読取セッションの状態と一覧。次の検出・再開・失敗時には前回の一覧を破棄する。 */
class NfcReadStatusStore {
    private val mutableStatus = MutableStateFlow<NfcReadStatus>(NfcReadStatus.Ready)
    val status: StateFlow<NfcReadStatus> = mutableStatus.asStateFlow()

    fun onReady() {
        mutableStatus.value = NfcReadStatus.Ready
    }

    fun onTagDiscovered() {
        mutableStatus.value = NfcReadStatus.Reading
    }

    fun onReadCompleted(items: List<RawMemoryItem> = emptyList()) {
        mutableStatus.value = NfcReadStatus.Completed(items.toList())
    }

    fun onReadFailed(reason: NfcReadFailure) {
        mutableStatus.value = NfcReadStatus.Failed(reason)
    }
}

sealed interface NfcReadStatus {
    data object Ready : NfcReadStatus
    data object Reading : NfcReadStatus
    data class Completed(val items: List<RawMemoryItem> = emptyList()) : NfcReadStatus
    data class Failed(val reason: NfcReadFailure) : NfcReadStatus
}

enum class NfcReadFailure {
    TAG_LOST,
    IO,
    INVALID_DATA,
    UNSUPPORTED_TAG,
    NFC_UNAVAILABLE,
}
