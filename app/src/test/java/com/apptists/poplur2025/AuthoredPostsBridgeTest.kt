package com.apptists.poplur2025

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class AuthoredPostsBridgeTest {
    @Test fun preservesPopMessageClassificationWithOrWithoutAttachments() {
        for (card in listOf(true, false)) {
            val row = AuthoredPostsBridge.publicRow(mapOf("contentType" to "message", "messageCard" to card,
                "caption" to "Hello #Toronto", "mediaType" to if (card) "photo" else "video"))!!
            assertEquals("message", row.getString("contentType"))
            assertEquals(card, row.getBoolean("messageCard"))
            assertEquals("Hello #Toronto", row.getString("caption"))
        }
    }

    @Test fun excludesNonpublicAndDeletedPostsBeforeSendingToWebView() {
        for (status in listOf("draft", "expired", "deleted")) {
            assertNull(AuthoredPostsBridge.publicRow(mapOf("visibilityStatus" to status, "mediaURL" to "https://example.test/photo.jpg")))
        }
        assertNull(AuthoredPostsBridge.publicRow(mapOf("audience" to "friends")))
        assertNull(AuthoredPostsBridge.publicRow(mapOf("deleted" to true)))
    }

    @Test fun keepsReachButNeverLeaksHiddenOriginsOrUnrelatedFields() {
        for (privacy in listOf(mapOf("locationVisibility" to "hidden"), mapOf("privateAccount" to true), mapOf("ghostMode" to "true"))) {
            val row = AuthoredPostsBridge.publicRow(mapOf("locationVisibility" to "approximate", "radiusMeters" to 1250,
                "latitude" to 43.0, "longitude" to -79.0, "privateNote" to "not public") + privacy)!!
            assertEquals(1250, row.getInt("radiusMeters"))
            assertEquals("hidden", row.getString("locationVisibility"))
            assertFalse(row.has("latitude"))
            assertFalse(row.has("longitude"))
            assertFalse(row.has("privateNote"))
        }
    }

    @Test fun allowsOnlyExplicitPublicOriginAndFiniteValues() {
        val row = AuthoredPostsBridge.publicRow(mapOf("locationVisibility" to "approximate", "latitude" to 43.0,
            "longitude" to -79.0, "viewCount" to Double.NaN))!!
        assertEquals(43.0, row.getDouble("latitude"), .0001)
        assertEquals(-79.0, row.getDouble("longitude"), .0001)
        assertFalse(row.has("viewCount"))
    }
}
