package com.apptists.poplur2025

import android.annotation.SuppressLint
import android.media.AudioManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.webkit.WebViewAssetLoader
import com.apptists.poplur2025.ui.components.PoplurVolumeBars
import org.json.JSONObject

/**
 * Post-login host for the approved web flow.
 *
 * iOS keeps the approved Loading surface over the approved clay-map Home while the map
 * warms up. Android does the same here: both pages are loaded from the checked-in bundles,
 * then Loading fades only after its minimum beat and Home reports that its map is ready.
 * This replaces the old native Compose handoff so a successful sign-in lands on the same
 * GCL prototype on both platforms.
 */
class PostLoginWebActivity : ComponentActivity() {

    private lateinit var homeWebView: WebView
    private lateinit var loadingWebView: WebView
    private lateinit var audioManager: AudioManager

    private val homeReady = mutableStateOf(false)
    private val minimumElapsed = mutableStateOf(false)
    private val volumeVisible = mutableStateOf(false)
    private val volumeFraction = mutableStateOf(0f)
    private var hideVolumeRunnable: Runnable? = null
    private var revealRunnable: Runnable? = null

    private val darkHome = 0xFF050407.toInt()
    private val darkLoading = 0xFF0A0709.toInt()
    private val origin = "https://appassets.androidplatform.net/assets"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // With MainActivity routing straight to the web surfaces (USE_WEB_SURFACES),
        // PoplurRoot no longer runs to start this — keep incoming calls working.
        com.apptists.poplur2025.call.CallSessionManager.startListeningForIncomingCalls()

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        window.statusBarColor = darkLoading
        window.navigationBarColor = darkLoading

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        homeWebView = createWebView(assetLoader, darkHome, receivesNativeEvents = true)
        loadingWebView = createWebView(assetLoader, darkLoading, receivesNativeEvents = false)

        val root = FrameLayout(this)
        root.addView(homeWebView, matchParentParams())
        root.addView(loadingWebView, matchParentParams())

        // Keep the app-owned volume bars consistent with the pre-login and native surfaces.
        val volumeOverlay = ComposeView(this).apply {
            setContent {
                val visible by volumeVisible
                val fraction by volumeFraction
                if (visible) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                        PoplurVolumeBars(
                            modifier = Modifier.padding(end = 28.dp),
                            volumeFraction = fraction
                        )
                    }
                }
            }
        }
        root.addView(volumeOverlay, matchParentParams())
        setContentView(root)

        // Both URLs are the exact bundles used by the iOS post-login path.
        homeWebView.loadUrl("$origin/claymap/index.html?view=home&tabs=1")
        // User rule: the approved globe Loading only after explicit sign-in/up;
        // an already-signed-in launch loops the splash (its clay bar refills)
        // while the home warms up behind it.
        val fromAuth = intent.getBooleanExtra("fromAuth", false)
        loadingWebView.loadUrl(
            if (fromAuth) "$origin/prelogin/loading.html?embed=Loading"
            else "$origin/prelogin/splash.html"
        )

        // Current-location start: the map falls back to Toronto unless native
        // pushes a fix. Request the OS permission on first entry; push once
        // granted (also re-pushed whenever the web asks via requestLocation).
        if (PoplurWebLocation.hasPermission(this)) {
            PoplurWebLocation.pushLastKnown(homeWebView)
        } else {
            PoplurWebLocation.requestPermission(this)
        }

        // Match the approved iOS loading minimum. A bounded fallback avoids trapping a user
        // on Loading forever if Mapbox is unavailable and therefore cannot emit homeReady.
        revealRunnable = Runnable {
            minimumElapsed.value = true
            revealIfReady()
        }
        loadingWebView.postDelayed(revealRunnable!!, LOADING_HOLD_MS)
        loadingWebView.postDelayed({
            if (!isFinishing && !homeReady.value) {
                homeReady.value = true
                revealIfReady()
            }
        }, HOME_READY_TIMEOUT_MS)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(
        assetLoader: WebViewAssetLoader,
        backgroundColor: Int,
        receivesNativeEvents: Boolean
    ): WebView = WebView(this).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.mediaPlaybackRequiresUserGesture = false
        overScrollMode = WebView.OVER_SCROLL_NEVER
        setBackgroundColor(backgroundColor)
        if (receivesNativeEvents) addJavascriptInterface(Bridge(), "PoplurNative")
        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? = interceptAssetRequest(assetLoader, request)

            override fun onPageFinished(view: WebView, url: String?) {
                // 3GB-class devices: flatten expensive effects (see PoplurLiteTier).
                // The home bundle also self-detects via deviceMemory; injecting here
                // covers the loading page too and is idempotent.
                if (PoplurLiteTier.isLowTier(view.context)) PoplurLiteTier.injectLiteCss(view)
            }
        }
    }

    /**
     * The shared HomeTwin intentionally uses the same root-relative map URL as iOS. Android's
     * WebViewAssetLoader mounts checked-in web assets under /assets/, so map-root requests from
     * HomeTwin's iframe are mapped back to assets/claymap here without changing the approved web
     * source or its iOS URL scheme.
     */
    private fun interceptAssetRequest(
        assetLoader: WebViewAssetLoader,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val url = request.url
        if (url.host != "appassets.androidplatform.net") return null
        val path = url.encodedPath ?: return null
        if (path.startsWith("/assets/")) return assetLoader.shouldInterceptRequest(url)

        val relativePath = path.removePrefix("/")
        // Only the clay map's OWN root-relative requests belong under assets/claymap/.
        // Shared bundles live at assets/<path> — notably the approved Loading screen's
        // /poplur-clay-map/... three.js + DRACO + globe. Blanket-mapping everything to
        // claymap/ made those 404 (FileNotFoundException on DRACOLoader.js), leaving the
        // web home erroring and thrashing at ~13fps — the Android/iPad "lag".
        val mappedPath = when {
            relativePath.isEmpty() -> "/assets/claymap/index.html"
            relativePath.startsWith("poplur-clay-map/") -> "/assets/$relativePath"
            relativePath.startsWith("prelogin/") -> "/assets/$relativePath"
            relativePath.startsWith("Poplur2025-iOS/") -> "/assets/$relativePath"
            else -> "/assets/claymap/$relativePath"
        }
        return assetLoader.shouldInterceptRequest(url.buildUpon().encodedPath(mappedPath).build())
    }

    private fun revealIfReady() {
        if (!minimumElapsed.value || !homeReady.value || isFinishing) return
        window.statusBarColor = darkHome
        window.navigationBarColor = darkHome
        loadingWebView.animate()
            .alpha(0f)
            .setDuration(REVEAL_DURATION_MS)
            .withEndAction {
                // DESTROY the loading WebView, don't just hide it: a whole second
                // WebView (loading page + three.js globe) kept resident is real
                // memory on a 3GB device that is already swapping — freeing it
                // gives the home WebView room to run smoothly.
                (loadingWebView.parent as? android.view.ViewGroup)?.removeView(loadingWebView)
                loadingWebView.destroy()
            }
            .start()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val code = event.keyCode
        if (code == KeyEvent.KEYCODE_VOLUME_UP || code == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                val direction = if (code == KeyEvent.KEYCODE_VOLUME_UP) {
                    AudioManager.ADJUST_RAISE
                } else {
                    AudioManager.ADJUST_LOWER
                }
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
                showCustomVolumeBars()
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun showCustomVolumeBars() {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        volumeFraction.value = if (max > 0) current.toFloat() / max else 0f
        volumeVisible.value = true
        hideVolumeRunnable?.let { homeWebView.removeCallbacks(it) }
        hideVolumeRunnable = Runnable { volumeVisible.value = false }
        homeWebView.postDelayed(hideVolumeRunnable!!, VOLUME_HIDE_MS)
    }

    inner class Bridge {
        @JavascriptInterface
        fun onEvent(json: String) {
            val type = runCatching { JSONObject(json).optString("type") }.getOrNull()
            if (type == "homeReady" || type == "mapReady") {
                runOnUiThread {
                    homeReady.value = true
                    revealIfReady()
                }
            }
            if (type == "requestCameraRoll") {
                runOnUiThread {
                    if (PoplurWebCameraRoll.hasPermission(this@PostLoginWebActivity)) {
                        PoplurWebCameraRoll.push(homeWebView)
                    } else {
                        PoplurWebCameraRoll.requestPermission(this@PostLoginWebActivity)
                    }
                }
            }
            if (type == "requestLocation") {
                runOnUiThread {
                    if (PoplurWebLocation.hasPermission(this@PostLoginWebActivity)) {
                        PoplurWebLocation.pushLastKnown(homeWebView)
                    } else {
                        PoplurWebLocation.requestPermission(this@PostLoginWebActivity)
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PoplurWebLocation.REQUEST_CODE &&
            grantResults.any { it == android.content.pm.PackageManager.PERMISSION_GRANTED }
        ) {
            PoplurWebLocation.pushLastKnown(homeWebView)
        }
        if (requestCode == PoplurWebCameraRoll.REQUEST_CODE) {
            if (grantResults.any { it == android.content.pm.PackageManager.PERMISSION_GRANTED }) {
                PoplurWebCameraRoll.push(homeWebView)
            } else {
                PoplurWebCameraRoll.deliverDenied(homeWebView)
            }
        }
    }

    override fun onDestroy() {
        revealRunnable?.let { loadingWebView.removeCallbacks(it) }
        hideVolumeRunnable?.let { homeWebView.removeCallbacks(it) }
        homeWebView.destroy()
        // The loading WebView is destroyed at reveal time; only destroy here if
        // the activity dies before the reveal (destroy() twice throws).
        if (loadingWebView.parent != null) loadingWebView.destroy()
        super.onDestroy()
    }

    private companion object {
        const val LOADING_HOLD_MS = 3500L
        const val HOME_READY_TIMEOUT_MS = 12000L
        const val REVEAL_DURATION_MS = 450L
        const val VOLUME_HIDE_MS = 1500L
    }
}

private fun matchParentParams(): FrameLayout.LayoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
    )
