package com.apptists.poplur2025

import org.junit.Assert.*
import org.junit.Test

class WebFeedSnapshotTest {
    private data class Post(val id: String, val uid: String, val radius: Long)
    private fun cache() = WebFeedSnapshot<Post>({ it.id }, { it.uid }).apply { reset("me") }

    @Test fun `owned query finishing first does not announce an empty Feed`() {
        val cache = cache()
        cache.owned = emptyList()
        assertFalse(cache.ready)
        cache.recent = emptyList()
        assertTrue(cache.ready)
    }

    @Test fun `decay replaces stale recent radius and old posts outside the window`() {
        val cache = cache()
        cache.recent = listOf(Post("new", "me", 112316), Post("other", "other", 3000))
        cache.owned = listOf(Post("new", "me", 112316), Post("old", "me", 45532))
        cache.owned = listOf(Post("new", "me", 112214), Post("old", "me", 45437))
        assertEquals(listOf(3000L, 112214L, 45437L), cache.merged().map { it.radius })
    }

    @Test fun `empty owned snapshot removes deleted posts even before recent catches up`() {
        val cache = cache()
        cache.recent = listOf(Post("deleted", "me", 5000), Post("other", "other", 3000))
        cache.owned = emptyList()
        assertEquals(listOf("other"), cache.merged().map { it.id })
        assertEquals(WebAccountRadius(0.0, 0.0), WebAccountRadius.fromPosts(emptyList()))
    }

    @Test fun `account switch clears both snapshots`() {
        val cache = cache()
        cache.owned = listOf(Post("private-account-post", "me", 1000))
        cache.reset("someone-else")
        assertFalse(cache.ready)
        assertTrue(cache.merged().isEmpty())
    }

    @Test fun `account totals decrease independently of map and preserve seed`() {
        fun total(organic: Long) = WebAccountRadius.fromPosts(listOf(
            mapOf("radiusMeters" to organic + 66500, "seedRadiusMeters" to 66500L, "organicRadiusMeters" to organic),
            mapOf("radius" to 45437L),
        ))
        assertEquals(WebAccountRadius(91253.0, 157753.0), total(45816))
        assertEquals(WebAccountRadius(91151.0, 157651.0), total(45714))
    }

    @Test fun `authoritative zero is not replaced by a legacy radius and account sums are not capped`() {
        val total = WebAccountRadius.fromPosts(listOf(
            mapOf("radiusMeters" to 0L, "radius" to 9000L),
            mapOf("radiusMeters" to 6371000L), mapOf("radiusMeters" to 6371000L),
        ))
        assertEquals(12_742_000.0, total.reachMeters, 0.0)
    }
}
