package com.apptists.poplur2025

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PostAdmissionTest {
    private val now = 2_000_000_000L

    @Test fun firstPostStartsAWindow() {
        assertEquals(PostAdmissionPlan(null, 1), nextPostAdmission(now, 0, null, 0))
    }
    @Test fun minuteBoundaryIsInclusive() {
        assertThrows(IllegalArgumentException::class.java) { nextPostAdmission(now, now - 59_999, now - 100_000, 1) }
        assertEquals(PostAdmissionPlan(now - 100_000, 2), nextPostAdmission(now, now - 60_000, now - 100_000, 1))
    }
    @Test fun thirtiethPostIsAllowedButThirtyFirstIsNot() {
        assertEquals(30, nextPostAdmission(now, now - 60_000, now - 3_600_000, 29).count)
        assertThrows(IllegalArgumentException::class.java) { nextPostAdmission(now, now - 60_000, now - 3_600_000, 30) }
    }
    @Test fun expiredWindowResetsWithoutSkippingCooldown() {
        assertEquals(PostAdmissionPlan(null, 1), nextPostAdmission(now, now - 60_000, now - 86_400_000, 30))
        assertThrows(IllegalArgumentException::class.java) { nextPostAdmission(now, now - 1, now - 86_400_000, 30) }
    }
}
