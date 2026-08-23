package com.apptists.poplur2025.ads

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import java.lang.ref.WeakReference
import com.apptists.poplur2025.BuildConfig
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions

object AdManager {
    private val interstitialUnitId = BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID
    private val adsAllowed = !isEmulator() && isProductionUnitId(interstitialUnitId)

    val isAvailable: Boolean
        get() = adsAllowed

    private var interstitial: InterstitialAd? = null
    private var isLoadingInterstitial = false
    private var pendingActivity: WeakReference<Activity>? = null
    private var pendingVisibilityChanged: ((Boolean) -> Unit)? = null

    fun initialize(context: Context) {
        if (!adsAllowed) {
            Log.i("Ads", "Ads disabled on emulator or because the production unit is invalid")
            return
        }
        MobileAds.initialize(context)
        loadInterstitial(context)
    }

    /** Preloaded like iOS: the ad is fetched ahead of the moment it is shown,
     *  and a fresh one is requested as soon as the current one is consumed. */
    fun loadInterstitial(context: Context) {
        if (!adsAllowed || isLoadingInterstitial || interstitial != null) return
        isLoadingInterstitial = true
        InterstitialAd.load(
            context.applicationContext,
            interstitialUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoadingInterstitial = false
                    interstitial = ad
                    presentPendingInterstitialIfPossible()
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoadingInterstitial = false
                    interstitial = null
                    Log.w("Ads", "Interstitial failed to load: ${error.message}")
                    clearPendingPresentation(visible = false)
                }
            },
        )
    }

    /** Feed interstitial at a natural Feed transition. If the preloaded ad has
     *  not arrived yet, keep this real-ad opportunity pending and present as
     *  soon as it does instead of silently dropping the first eligible break. */
    fun presentInterstitial(
        activity: Activity,
        onVisibilityChanged: (Boolean) -> Unit = {},
    ) {
        if (!adsAllowed) {
            onVisibilityChanged(false)
            return
        }
        val ad = interstitial ?: run {
            pendingActivity = WeakReference(activity)
            pendingVisibilityChanged = onVisibilityChanged
            loadInterstitial(activity)
            return
        }
        interstitial = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() = onVisibilityChanged(true)

            override fun onAdDismissedFullScreenContent() {
                onVisibilityChanged(false)
                loadInterstitial(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                onVisibilityChanged(false)
                loadInterstitial(activity)
            }
        }
        ad.show(activity)
    }

    private fun presentPendingInterstitialIfPossible() {
        val activity = pendingActivity?.get()
        val onVisibilityChanged = pendingVisibilityChanged
        pendingActivity = null
        pendingVisibilityChanged = null
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            onVisibilityChanged?.invoke(false)
            return
        }
        presentInterstitial(activity, onVisibilityChanged ?: {})
    }

    private fun clearPendingPresentation(visible: Boolean) {
        val onVisibilityChanged = pendingVisibilityChanged
        pendingActivity = null
        pendingVisibilityChanged = null
        onVisibilityChanged?.invoke(visible)
    }

    fun loadBanner(adView: AdView) {
        if (!adsAllowed) return
        adView.loadAd(AdRequest.Builder().build())
    }

    fun loadNativeAd(
        context: Context,
        adUnitId: String,
        onLoaded: (NativeAd) -> Unit,
        onFailed: (LoadAdError) -> Unit = {}
    ) {
        if (!adsAllowed || !isProductionUnitId(adUnitId)) return
        AdLoader.Builder(context, adUnitId)
            .forNativeAd(onLoaded)
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_ANY)
                    .build()
            )
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) = onFailed(error)
            })
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    private fun isProductionUnitId(value: String): Boolean =
        Regex("ca-app-pub-[0-9]+/[0-9]+").matches(value) &&
            !value.contains("3940256099942544")

    private fun isEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        return fingerprint.startsWith("generic") ||
            fingerprint.contains("emulator") ||
            fingerprint.contains("robolectric") ||
            model.contains("google_sdk") ||
            model.contains("emulator") ||
            model.contains("android sdk built for") ||
            Build.MANUFACTURER.equals("Genymotion", ignoreCase = true) ||
            (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
            Build.PRODUCT == "google_sdk"
    }
}
