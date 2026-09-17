package com.apptists.poplur2025

/** The owned-post stream is authoritative even while the recent window still has an older row. */
internal class WebFeedSnapshot<T>(
    private val id: (T) -> String,
    private val owner: (T) -> String?,
    /** Whether a row is simulation output. Defaults to "everything is a person",
     *  which reproduces the single-window behaviour for any caller that does not
     *  classify — the windowing below then only ever trims by age, never by
     *  population. */
    private val synthetic: (T) -> Boolean = { false },
) {
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
        val own = owned ?: return windowByPopulation(recent.orEmpty())
        val others = windowByPopulation(recent.orEmpty().filter { owner(it) != uid })
        return (others + own).distinctBy(id)
    }

    /**
     * The newest [POOL_WINDOW] of EACH population — people and simulation — with
     * order preserved.
     *
     * One shared window let the synthetic cohort (~460 pops/day) turn the whole
     * pool over every few hours, so a student's morning pop was gone from every
     * other phone's Feed and Map by the afternoon. Windowing per population means
     * people keep exactly the window they would have if no synthetic post
     * existed, however loud the simulation gets.
     *
     * Firestore cannot select the ABSENCE of a synthetic marker (`!=` and
     * `not-in` skip documents missing the field, and human writers stamp
     * nothing), so the caller fetches wide — see FEED_POOL_FETCH_LIMIT — and the
     * split happens here.
     *
     * Mirrors windowByPopulation in PostLoginHomeWebView.swift and
     * nativeFeedPosts.ts. Keep the three in step.
     */
    private fun windowByPopulation(documents: List<T>): List<T> {
        var people = 0
        var simulation = 0
        return documents.filter { document ->
            if (synthetic(document)) {
                if (simulation < POOL_WINDOW) { simulation++; true } else false
            } else {
                if (people < POOL_WINDOW) { people++; true } else false
            }
        }
    }

    internal companion object {
        /** Rows kept per population after windowing. */
        const val POOL_WINDOW = 160

        /**
         * Rows the pool listener FETCHES, so there is something to window. At
         * ~460 simulation pops/day this reaches back ≈33 h; raise it to reach
         * further. Matches feedPoolFetchLimit on iOS and LIVE_FEED_FETCH_LIMIT
         * in nativeFeedPosts.ts.
         */
        const val FEED_POOL_FETCH_LIMIT = 640L

        /**
         * Whether a post document is simulation output. Mirrors
         * isSyntheticPostDocument in PostLoginHomeWebView.swift and
         * isSyntheticPostRow in nativeFeedPosts.ts, themselves mirrors of
         * functions/src/syntheticIdentity.ts postClass(). Keep the four in step.
         */
        fun isSyntheticPost(id: String, owner: String?, field: (String) -> Any?): Boolean {
            val writer = owner.orEmpty()
            if (writer.startsWith("synthetic-") || writer.startsWith("sim-")) return true
            if (field("syntheticBeta") == true || field("syntheticCohort") == true) return true
            for (key in listOf("syntheticActorId", "syntheticSource", "syntheticJourney")) {
                val value = field(key) as? String
                if (!value.isNullOrBlank()) return true
            }
            if (field("source") as? String == "synthetic-browser") return true
            return id.startsWith("post-sim-") ||
                id.startsWith("synthetic-") ||
                id.startsWith("post-radius-lab-")
        }
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
