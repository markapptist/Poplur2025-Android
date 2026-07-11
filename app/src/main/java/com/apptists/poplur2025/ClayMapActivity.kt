package com.apptists.poplur2025

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.webkit.WebViewAssetLoader
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
 *   JS → Android: window.PoplurNative.onEvent('{"type":"pop"}') → haptic
 *   Android → JS: webView.evaluateJavascript("window.__poplurNative.flyTo(...)")
 */
class ClayMapActivity : ComponentActivity() {

    private lateinit var webView: WebView

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
            loadUrl("https://appassets.androidplatform.net/assets/claymap/index.html")
        }
        setContentView(webView)
    }

    inner class BridgeInterface {
        @JavascriptInterface
        fun onEvent(json: String) {
            val type = runCatching { JSONObject(json).optString("type") }.getOrNull()
            when (type) {
                "pop" -> runOnUiThread {
                    webView.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                }
                "poiTap" -> Unit // Phase 1.5: route to channel/post
            }
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
