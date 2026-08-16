package com.apptists.poplur2025

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppIconCatalogTest {
    @Test
    fun catalogHasFourStableUniqueIcons() {
        val icons = AppIconManager.options
        assertEquals(4, icons.size)
        assertEquals(icons.size, icons.map { it.id }.toSet().size)
        assertEquals("default", icons.first().id)
        assertTrue(icons.all { it.displayName.isNotBlank() && it.previewRes != 0 })
    }

    @Test
    fun launcherAliasesAreUnique() {
        val aliases = AppIconManager.options.map { it.aliasSuffix }
        assertEquals(aliases.size, aliases.toSet().size)
    }
}
