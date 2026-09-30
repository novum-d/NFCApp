package com.example.nfcapp

internal enum class NfcTagRoute { NFC_V, NTAG213, UNSUPPORTED }

internal fun routeForTechnologies(technologies: List<String>): NfcTagRoute = when {
    "android.nfc.tech.NfcV" in technologies -> NfcTagRoute.NFC_V
    "android.nfc.tech.IsoDep" in technologies -> NfcTagRoute.UNSUPPORTED
    "android.nfc.tech.NfcA" in technologies -> NfcTagRoute.NTAG213
    else -> NfcTagRoute.UNSUPPORTED
}
