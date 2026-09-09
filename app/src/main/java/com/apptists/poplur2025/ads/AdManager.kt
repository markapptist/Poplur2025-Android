package com.apptists.poplur2025.ads

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
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
    private var initialized = false
    private var pendingActivity: WeakReference<Activity>? = null
    private var pendingVisibilityChanged: ((Boolean) -> Unit)? = null
    /** An interstitial that is already on screen. Without this, a second
     *  eligible break could stack another AdActivity on top of the first, so
     *  dismissing one revealed another instead of the app. iOS has carried this
     *  guard since it shipped (AdManager.swift `isPresenting`). */
    private var isPresenting = false
    /** The app is foregrounded. iOS refuses to present unless
     *  `UIApplication.shared.applicationState == .active`; Android has no
     *  equivalent global, so the hosting Activity reports it. */
    private var appActive = true
    private val pendingHandler = Handler(Looper.getMainLooper())
    private var pendingExpiry: Runnable? = null
    /** When the cached interstitial arrived. AdMob expires an unshown ad after
     *  about an hour; without this, a session that reaches no eligible break
     *  for that long calls show() on a dead ad and spends the one break that
     *  DID qualify on a failure callback. iOS discards at the same threshold. */
    private var loadedAtMs: Long = 0L
    private const val INTERSTITIAL_MAX_AGE_MS = 3_500_000L
    /** How long an unfilled break may wait for inventory. Past this the
     *  opportunity is GONE, not deferred: an ad that arrives thirty seconds
     *  later lands on whatever screen the user has since moved to - which on
     *  this app meant a full-screen ad over the map. Matches iOS's 2 seconds. */
    private const val PENDING_MAX_WAIT_MS = 2_000L

    fun initialize(context: Context) {
        if (initialized) return
        if (!adsAllowed) {
            Log.i("Ads", "Ads disabled on emulator or because the production unit is invalid")
            return
        }
        initialized = true
        MobileAds.initialize(context.applicationContext)
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
                    loadedAtMs = android.os.SystemClock.elapsedRealtime()
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
        // Backgrounded, or an ad is already up: this break is spent, not saved.
        if (!appActive || isPresenting || activity.isFinishing || activity.isDestroyed) {
            onVisibilityChanged(false)
            cancelPendingPresentation()
            return
        }
        // A stale cached ad must not consume the break: drop it and let the
        // pending path serve this one from the fresh load.
        if (interstitial != null &&
            android.os.SystemClock.elapsedRealtime() - loadedAtMs >= INTERSTITIAL_MAX_AGE_MS
        ) {
            interstitial = null
        }
        val ad = interstitial ?: run {
            // Only the FIRST unfilled break starts the clock; a later one must
            // not keep pushing the deadline out.
            if (pendingActivity?.get() == null) {
                pendingActivity = WeakReference(activity)
                pendingVisibilityChanged = onVisibilityChanged
                armPendingExpiry()
            } else {
                onVisibilityChanged(false)
            }
            loadInterstitial(activity)
            return
        }
        interstitial = null
        disarmPendingExpiry()
        pendingActivity = null
        pendingVisibilityChanged = null
        isPresenting = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() = onVisibilityChanged(true)

            override fun onAdDismissedFullScreenContent() {
                isPresenting = false
                onVisibilityChanged(false)
                loadInterstitial(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                isPresenting = false
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
        disarmPendingExpiry()
        val onVisibilityChanged = pendingVisibilityChanged
        pendingActivity = null
        pendingVisibilityChanged = null
        onVisibilityChanged?.invoke(visible)
    }

    private fun armPendingExpiry() {
        disarmPendingExpiry()
        val expiry = Runnable {
            pendingExpiry = null
            Log.i("Ads", "feed break waited ${PENDING_MAX_WAIT_MS}ms for inventory - dropped")
            clearPendingPresentation(visible = false)
        }
        pendingExpiry = expiry
        pendingHandler.postDelayed(expiry, PENDING_MAX_WAIT_MS)
    }

    private fun disarmPendingExpiry() {
        pendingExpiry?.let { pendingHandler.removeCallbacks(it) }
        pendingExpiry = null
    }

    /**
     * Abandon a break that is still waiting for inventory.
     *
     * The host calls this the moment the user leaves the Feed, and when the app
     * stops being active. Without it a pending ad waited indefinitely and then
     * presented over whichever surface the user had reached - which is how a
     * full-screen interstitial ended up covering the map, taking the native map
     * and its tile server down with it (the map's mount gate is document
     * visibility). iOS has cancelled on both edges since it shipped.
     */
    fun cancelPendingPresentation() {
        if (pendingActivity == null && pendingExpiry == null) return
        clearPendingPresentation(visible = false)
    }

    /**
     * The host's foreground state. Going inactive abandons a pending break for
     * the same reason as leaving the Feed: by the time the app returns, the
     * moment that earned the ad is gone.
     */
    fun setAppActive(active: Boolean) {
        appActive = active
        if (!active) {
            cancelPendingPresentation()
            return
        }
        // The host is in front again, so no interstitial is over it. Normally
        // onAdDismissedFullScreenContent has already said so; if that callback
        // is ever lost - a process death behind the ad, an AdActivity torn down
        // without notifying - a stuck `isPresenting` would refuse every future
        // ad for the life of the process. Resuming is proof, so trust it.
        isPresenting = false
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
