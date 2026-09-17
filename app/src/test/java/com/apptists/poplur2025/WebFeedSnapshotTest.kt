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

    // ── Per-population window (16 Sep 2026) ──────────────────────────────────
    // The simulation posts ~460/day. Under one shared window a student's morning
    // pop was gone from every other phone by the afternoon; this is the fix.

    private data class Row(val id: String, val uid: String)
    private fun pool() = WebFeedSnapshot<Row>(
        { it.id },
        { it.uid },
        synthetic = { WebFeedSnapshot.isSyntheticPost(it.id, it.uid) { _ -> null } },
    ).apply { reset("me") }

    @Test fun `simulation volume never evicts a person's post`() {
        val cache = pool()
        // One person's pop, then far more simulation pops than the window holds.
        val human = Row("human-pop", "student-a")
        val sim = (1..WebFeedSnapshot.POOL_WINDOW * 2).map { Row("post-sim-$it", "sim-daily-$it") }
        cache.recent = listOf(human) + sim
        val merged = cache.merged()
        assertTrue("the person's pop must survive any amount of simulation", merged.contains(human))
        assertEquals(WebFeedSnapshot.POOL_WINDOW, merged.count { it.uid.startsWith("sim-") })
    }

    @Test fun `people keep the window they would have with no simulation at all`() {
        val cache = pool()
        val people = (1..WebFeedSnapshot.POOL_WINDOW + 20).map { Row("human-$it", "student-$it") }
        cache.recent = people
        assertEquals(WebFeedSnapshot.POOL_WINDOW, cache.merged().size)
        // And the ones kept are the NEWEST, order preserved.
        assertEquals("human-1", cache.merged().first().id)
    }

    @Test fun `own posts ride along regardless of population or age`() {
        val cache = pool()
        val sim = (1..WebFeedSnapshot.POOL_WINDOW * 2).map { Row("post-sim-$it", "sim-$it") }
        cache.recent = sim
        cache.owned = listOf(Row("mine", "me"))
        assertTrue(cache.merged().any { it.id == "mine" })
    }

    @Test fun `every marker the simulation announces itself with is recognised`() {
        // uid prefixes
        assertTrue(WebFeedSnapshot.isSyntheticPost("p", "sim-daily-3") { null })
        assertTrue(WebFeedSnapshot.isSyntheticPost("p", "synthetic-conquer-1") { null })
        // document fields
        assertTrue(WebFeedSnapshot.isSyntheticPost("p", "person") { k -> if (k == "syntheticBeta") true else null })
        assertTrue(WebFeedSnapshot.isSyntheticPost("p", "person") { k -> if (k == "syntheticCohort") true else null })
        assertTrue(WebFeedSnapshot.isSyntheticPost("p", "person") { k -> if (k == "syntheticActorId") "a" else null })
        assertTrue(WebFeedSnapshot.isSyntheticPost("p", "person") { k -> if (k == "source") "synthetic-browser" else null })
        // id prefixes
        assertTrue(WebFeedSnapshot.isSyntheticPost("post-sim-9", "person") { null })
        assertTrue(WebFeedSnapshot.isSyntheticPost("post-radius-lab-2", "person") { null })
        // a real student must never be classified as simulation
        assertFalse(WebFeedSnapshot.isSyntheticPost("abc123", "kJ2n4kd9sl") { null })
        assertFalse(WebFeedSnapshot.isSyntheticPost("abc123", "person") { k -> if (k == "syntheticActorId") "" else null })
    }

    @Test fun `a caller that does not classify behaves exactly as before`() {
        // Default synthetic = { false }: everything is a person, so the window
        // only ever trims by age. Protects every other WebFeedSnapshot user.
        val cache = WebFeedSnapshot<Row>({ it.id }, { it.uid }).apply { reset("me") }
        cache.recent = listOf(Row("a", "x"), Row("b", "y"))
        assertEquals(listOf("a", "b"), cache.merged().map { it.id })
    }
}
