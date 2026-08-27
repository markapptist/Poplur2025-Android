package com.apptists.poplur2025

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.webkit.WebViewAssetLoader
import com.apptists.poplur2025.performance.PoplurWebViewPerformanceController
import com.apptists.poplur2025.util.PoplurEQSync
import org.json.JSONObject

/**
 * Phase 1 embed of the Poplur clay map (Mapbox GL JS) — the same bundle the
 * iOS app ships, served from assets/claymap (synced by
 * poplur-clay-map/scripts/sync-mobile.sh).
 *
 * Served over WebViewAssetLoader's virtual https://appassets.androidplatform.net
 * origin rather than file:///android_asset — a file:// page has an opaque
 * origin, so the Vite bundle's <script type="module">, its Web Workers and its
 * dynamic import() calls get blocked and the map renders as the page's
 * #050407 background (a black screen). A real https origin fixes all three.
 * (Mirrors the iOS custom-scheme fix in PoplurClayMapView.swift.)
 *
 * Bridge:
 *   JS → Android: window.PoplurNative.onEvent('{"type":"pop"}') → haptic + pop sound
 *   Android → JS: webView.evaluateJavascript("window.__poplurNative.flyTo(...)")
 */
class ClayMapActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var performanceController: PoplurWebViewPerformanceController
    private var popPlayer: MediaPlayer? = null
    private var isPopPlayerReady = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            setBackgroundColor(0xFF050407.toInt())
            addJavascriptInterface(BridgeInterface(), "PoplurNative")
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)
            }
            val lowTier = PoplurLiteTier.isLowTier(this@ClayMapActivity)
            val performanceQuery = if (lowTier) "?mapLite=1" else ""
            loadUrl(
                "https://appassets.androidplatform.net/assets/claymap/index.html" +
                    performanceQuery
            )
        }
        performanceController = PoplurWebViewPerformanceController(this, "PoplurClayMapWeb")
        performanceController.attach(webView)
        setContentView(webView)
        preparePopPlayer()
    }

    override fun onStart() {
        super.onStart()
        PoplurEQSync.noteActivityStarted()
        performanceController.setVisible(true)
    }

    override fun onStop() {
        performanceController.setVisible(false)
        PoplurEQSync.noteActivityStopped()
        super.onStop()
    }

    private fun preparePopPlayer() {
        val player = MediaPlayer()
        runCatching {
            assets.openFd("pop.mp3").use { asset ->
                player.setDataSource(asset.fileDescriptor, asset.startOffset, asset.length)
            }
            player.setOnPreparedListener { prepared ->
                if (popPlayer === prepared) isPopPlayerReady = true
            }
            player.setOnErrorListener { failed, _, _ ->
                if (popPlayer === failed) isPopPlayerReady = false
                true
            }
            popPlayer = player
            player.prepareAsync()
        }.onFailure {
            if (popPlayer === player) popPlayer = null
            isPopPlayerReady = false
            player.release()
        }
    }

    private fun playPopSound() {
        if (!PoplurEQSync.shouldPlayTransientSound(minimumIntervalMs = 160L)) return
        val player = popPlayer?.takeIf { isPopPlayerReady } ?: return
        runCatching {
            if (player.isPlaying) player.pause()
            player.seekTo(0)
            player.start()
        }
    }

    inner class BridgeInterface {
        @JavascriptInterface
        fun onEvent(json: String) {
            val type = runCatching { JSONObject(json).optString("type") }.getOrNull()
            when (type) {
                "pop" -> runOnUiThread {
                    webView.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    playPopSound()
                }
                "poiTap" -> Unit // Phase 1.5: route to channel/post
            }
        }
    }

    override fun onDestroy() {
        performanceController.close()
        isPopPlayerReady = false
        popPlayer?.release()
        popPlayer = null
        webView.destroy()
        super.onDestroy()
    }
}
