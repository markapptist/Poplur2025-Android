package com.apptists.poplur.performance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PoplurAssetCacheTest {
    @Test
    fun insertionEvictsLeastRecentlyUsedToRemainUnderByteBudget() {
        val released = mutableListOf<String>()
        val cache = PoplurAssetCache(maximumBytes = 10, maximumEntries = 3)

        cache.put("a", PoplurAssetCache.AssetType.MODEL, "a", 4, release = released::add)
        cache.put("b", PoplurAssetCache.AssetType.MODEL, "b", 4, release = released::add)
        assertSame("a", cache.get<String>("a")) // b is now the LRU entry.
        assertTrue(cache.put("c", PoplurAssetCache.AssetType.MODEL, "c", 4, release = released::add))

        assertNull(cache.get<String>("b"))
        assertEquals(listOf("b"), released)
        assertTrue(cache.diagnostics().estimatedBytes <= cache.maximumBytes)
    }

    @Test
    fun oversizedOrPinnedBlockedInsertionIsRefusedAndReleased() {
        val released = mutableListOf<String>()
        val cache = PoplurAssetCache(maximumBytes = 5, maximumEntries = 1)

        assertTrue(cache.put("visible", PoplurAssetCache.AssetType.SCENE, "old", 5, pinned = true, release = released::add))
        assertFalse(cache.put("other", PoplurAssetCache.AssetType.SCENE, "new", 1, release = released::add))
        assertFalse(cache.put("huge", PoplurAssetCache.AssetType.MODEL, "huge", 6, release = released::add))

        assertEquals(listOf("new", "huge"), released)
        assertEquals(1, cache.diagnostics().assetCount)
        assertEquals(5, cache.diagnostics().estimatedBytes)
    }

    @Test
    fun criticalTrimReleasesInactiveButPreservesPinnedVisibleAsset() {
        val released = mutableListOf<String>()
        val cache = PoplurAssetCache(maximumBytes = 20, maximumEntries = 4)
        cache.put("visible", PoplurAssetCache.AssetType.SCENE, "visible", 7, pinned = true, release = released::add)
        cache.put("texture", PoplurAssetCache.AssetType.TEXTURE, "texture", 6, release = released::add)
        cache.put("video", PoplurAssetCache.AssetType.VIDEO_RESOURCE, "video", 4, release = released::add)

        assertEquals(10, cache.trimForMemoryPressure(PoplurMemoryPressure.CRITICAL))
        assertEquals(listOf("texture", "video"), released)
        assertEquals(7, cache.diagnostics().estimatedBytes)
        assertSame("visible", cache.get<String>("visible"))
    }
}

