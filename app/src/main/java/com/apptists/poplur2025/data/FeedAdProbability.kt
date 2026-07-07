package com.apptists.poplur2025.data

import android.content.Context
import kotlin.math.max

enum class FeedAdPlacementKind {
    Content,
    PoplurAd,
    GoogleMobileAd
}

data class FeedAdProbabilityConfig(
    val poplurAdPercent: Float = DEFAULT_POPLUR_AD_PERCENT,
    val googleMobileAdPercent: Float = DEFAULT_GOOGLE_MOBILE_AD_PERCENT
) {
    val normalized: FeedAdProbabilityConfig
        get() {
            val poplur = poplurAdPercent.coerceIn(0f, 100f)
            val google = googleMobileAdPercent.coerceIn(0f, 100f)
            val total = poplur + google
            if (total <= 100f) return FeedAdProbabilityConfig(poplur, google)
            val scale = 100f / total
            return FeedAdProbabilityConfig(poplur * scale, google * scale)
        }

    val contentPercent: Float
        get() = max(0f, 100f - normalized.poplurAdPercent - normalized.googleMobileAdPercent)

    companion object {
        const val DEFAULT_POPLUR_AD_PERCENT = 12f
        const val DEFAULT_GOOGLE_MOBILE_AD_PERCENT = 6f
    }
}

object FeedAdProbability {
    const val PREFS_NAME = "poplur_settings"
    const val POPLUR_AD_PERCENT_KEY = "feed_ads_poplur_percent"
    const val GOOGLE_MOBILE_AD_PERCENT_KEY = "feed_ads_google_mobile_percent"

    fun currentConfig(context: Context): FeedAdProbabilityConfig {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        seedDefaultsIfNeeded(context)
        return FeedAdProbabilityConfig(
            poplurAdPercent = prefs.getFloat(
                POPLUR_AD_PERCENT_KEY,
                FeedAdProbabilityConfig.DEFAULT_POPLUR_AD_PERCENT
            ),
            googleMobileAdPercent = prefs.getFloat(
                GOOGLE_MOBILE_AD_PERCENT_KEY,
                FeedAdProbabilityConfig.DEFAULT_GOOGLE_MOBILE_AD_PERCENT
            )
        ).normalized
    }

    fun saveConfig(context: Context, config: FeedAdProbabilityConfig) {
        val normalized = config.normalized
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(POPLUR_AD_PERCENT_KEY, normalized.poplurAdPercent)
            .putFloat(GOOGLE_MOBILE_AD_PERCENT_KEY, normalized.googleMobileAdPercent)
            .apply()
    }

    fun placement(index: Int, namespace: String, config: FeedAdProbabilityConfig): FeedAdPlacementKind {
        val normalized = config.normalized
        val roll = stablePercent(index, namespace)
        return when {
            roll < normalized.poplurAdPercent -> FeedAdPlacementKind.PoplurAd
            roll < normalized.poplurAdPercent + normalized.googleMobileAdPercent -> FeedAdPlacementKind.GoogleMobileAd
            else -> FeedAdPlacementKind.Content
        }
    }

    fun seedDefaultsIfNeeded(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(POPLUR_AD_PERCENT_KEY) || !prefs.contains(GOOGLE_MOBILE_AD_PERCENT_KEY)) {
            prefs.edit()
                .putFloat(
                    POPLUR_AD_PERCENT_KEY,
                    prefs.getFloat(POPLUR_AD_PERCENT_KEY, FeedAdProbabilityConfig.DEFAULT_POPLUR_AD_PERCENT)
                )
                .putFloat(
                    GOOGLE_MOBILE_AD_PERCENT_KEY,
                    prefs.getFloat(GOOGLE_MOBILE_AD_PERCENT_KEY, FeedAdProbabilityConfig.DEFAULT_GOOGLE_MOBILE_AD_PERCENT)
                )
                .apply()
        }
    }

    private fun stablePercent(index: Int, namespace: String): Float {
        var hash = 1125899906842597L
        "$namespace#$index".encodeToByteArray().forEach { byte ->
            hash = (hash * 31) + byte
        }
        return ((kotlin.math.abs(hash) % 10_000L).toFloat() / 100f).coerceIn(0f, 99.99f)
    }
}
