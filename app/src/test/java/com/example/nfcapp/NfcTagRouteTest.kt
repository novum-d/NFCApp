package com.example.nfcapp

import org.junit.Assert.assertEquals
import org.junit.Test

class NfcTagRouteTest {
    @Test fun nfcVRemainsTheFirstChoice() {
        assertEquals(
            NfcTagRoute.NFC_V,
            routeForTechnologies(
                listOf("android.nfc.tech.NfcV", "android.nfc.tech.NfcA", "android.nfc.tech.IsoDep")
            )
        )
    }

    @Test fun isoDepCardIsUnsupportedEvenWhenItAlsoExposesNfcA() {
        assertEquals(
            NfcTagRoute.UNSUPPORTED,
            routeForTechnologies(listOf("android.nfc.tech.NfcA", "android.nfc.tech.IsoDep"))
        )
    }

    @Test fun nfcAWithoutIsoDepStillUsesTheNtag213Reader() {
        assertEquals(
            NfcTagRoute.NTAG213,
            routeForTechnologies(listOf("android.nfc.tech.NfcA", "android.nfc.tech.Ndef"))
        )
    }

    @Test fun otherTechnologiesAreUnsupported() {
        assertEquals(
            NfcTagRoute.UNSUPPORTED,
            routeForTechnologies(listOf("android.nfc.tech.NfcB"))
        )
        assertEquals(NfcTagRoute.UNSUPPORTED, routeForTechnologies(emptyList()))
    }
}
