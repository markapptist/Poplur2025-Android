package com.apptists.poplur2025

/** Client proposal only. Firestore rules check the transition against server
 * time and require the post in the same atomic commit. */
internal data class PostAdmissionPlan(val windowStartMillis: Long?, val count: Int)

internal fun nextPostAdmission(nowMillis: Long, lastMillis: Long, startMillis: Long?, count: Int): PostAdmissionPlan {
    require(lastMillis <= 0 || nowMillis - lastMillis >= 60_000) {
        "Wait a minute between posts. Your draft is saved."
    }
    val active = startMillis != null && startMillis > 0 && nowMillis - startMillis < 86_400_000
    require(!active || count < 30) {
        "You've reached 30 posts in this 24-hour window. Your draft is saved."
    }
    return PostAdmissionPlan(if (active) startMillis else null, if (active) count.coerceAtLeast(0) + 1 else 1)
}
