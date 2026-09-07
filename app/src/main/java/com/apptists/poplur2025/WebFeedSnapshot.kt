package com.apptists.poplur2025

/** The owned-post stream is authoritative even while the recent window still has an older row. */
internal class WebFeedSnapshot<T>(private val id: (T) -> String, private val owner: (T) -> String?) {
    var uid: String? = null
        private set
    var recent: List<T>? = null
    var owned: List<T>? = null
    // An empty owned snapshot says nothing about whether the Feed is empty.
    val ready: Boolean get() = recent != null

    fun reset(uid: String?) {
        this.uid = uid
        recent = null
        owned = null
    }

    fun merged(): List<T> {
        val own = owned ?: return recent.orEmpty()
        return (recent.orEmpty().filter { owner(it) != uid } + own).distinctBy(id)
    }
}

/** Match the shared UI/iOS totals: full reach includes seed; earned reach does not. */
internal data class WebAccountRadius(val earnedMeters: Double, val reachMeters: Double) {
    companion object {
        fun fromPosts(posts: List<Map<String, Any>>): WebAccountRadius {
            fun number(value: Any?): Double? = (value as? Number)?.toDouble()?.takeIf { it.isFinite() }
            var earned = 0.0
            var reach = 0.0
            for (post in posts) {
                val radius = (number(post["radiusMeters"]) ?: number(post["radius"]) ?: 0.0)
                    .coerceIn(0.0, 6_371_000.0)
                val seed = (number(post["seedRadiusMeters"]) ?: 0.0).coerceAtLeast(0.0)
                earned += (number(post["organicRadiusMeters"]) ?: (radius - seed)).coerceIn(0.0, 6_371_000.0)
                reach += radius
            }
            return WebAccountRadius(earned, reach)
        }
    }
}
