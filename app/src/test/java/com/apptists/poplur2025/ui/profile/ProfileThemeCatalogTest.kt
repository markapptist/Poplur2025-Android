package com.apptists.poplur2025.ui.profile

import com.apptists.poplur2025.billing.PoplurProducts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileThemeCatalogTest {
    @Test
    fun manifestThemeIdsResolveWithoutOrdinals() {
        val expectedIds = listOf(
            "default",
            "chocolate",
            "rainbowNeon",
            "camouflage",
            "toy",
            "rubberPink",
            "icyCrystal",
            "luxeGold",
            "glamPink"
        )

        assertEquals(expectedIds, ProfileThemes.All.map(ProfileTheme::id))
        expectedIds.forEach { id -> assertEquals(id, ProfileThemes.fromId(id).id) }
        assertEquals(ProfileThemes.Default, ProfileThemes.fromId("removed-theme"))
        assertNull(ProfileThemes.Default.backgroundResId)
        ProfileThemes.All.drop(1).forEach { theme ->
            assertNotEquals(0, theme.backgroundResId)
            assertTrue(theme.overlayOpacity in 0f..1f)
        }
    }

    @Test
    fun androidCardSkusArePlaySafeAndStableIdsResolve() {
        val playSkuPattern = Regex("^[a-z0-9._]+$")

        PoplurProducts.cardPackages.forEach { product ->
            assertTrue(product.productId, playSkuPattern.matches(product.productId))
            assertEquals(product.productId, PoplurProducts.resolveCardProductId(product.cardId))
            assertEquals(product.productId, PoplurProducts.resolveCardProductId(product.productId))
        }
        assertEquals(
            "com.apptists.poplur2025.card.splitdiamond",
            PoplurProducts.resolveCardProductId("splitDiamond")
        )
        assertEquals(
            "com.apptists.poplur2025.card.swoosh",
            PoplurProducts.resolveCardProductId("com.apptist.Poplur2025.card.swoosh")
        )
    }
}
