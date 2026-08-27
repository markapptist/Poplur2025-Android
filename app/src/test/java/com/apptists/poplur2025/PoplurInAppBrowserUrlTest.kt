package com.apptists.poplur2025

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PoplurInAppBrowserUrlTest {
    @Test
    fun acceptsOnlyCredentialFreeHttpsDestinations() {
        assertEquals(
            "https://shop.example/offer?q=1",
            normalizePoplurInAppBrowserUrl(" https://shop.example/offer?q=1 "),
        )
        assertNull(normalizePoplurInAppBrowserUrl("http://shop.example/offer"))
        assertNull(normalizePoplurInAppBrowserUrl("https://person:secret@shop.example/offer"))
        assertNull(normalizePoplurInAppBrowserUrl("javascript:alert(1)"))
        assertNull(normalizePoplurInAppBrowserUrl("not a url"))
    }

    @Test
    fun selectsAnIsolatedProfileOrTheStatelessCompatibilityPath() {
        assertEquals(
            PoplurBrowserPrivacyMode.IsolatedProfile,
            poplurBrowserPrivacyMode(multiProfileSupported = true),
        )
        assertEquals(
            PoplurBrowserPrivacyMode.StatelessFallback,
            poplurBrowserPrivacyMode(multiProfileSupported = false),
        )
    }
}
