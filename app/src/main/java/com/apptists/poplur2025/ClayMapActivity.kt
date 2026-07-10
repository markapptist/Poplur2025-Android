package com.apptists.poplur2025

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.activity.ComponentActivity
import org.json.JSONObject

/**
 * Phase 1 embed of the Poplur clay map (Mapbox GL JS) — the same bundle the
 * iOS app ships, served from assets/claymap (synced by
 * poplur-clay-map/scripts/sync-mobile.sh).
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

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            // file:///android_asset pages must fetch their sibling assets
            settings.allowFileAccess = true
            setBackgroundColor(0xFF050407.toInt())
            addJavascriptInterface(BridgeInterface(), "PoplurNative")
            loadUrl("file:///android_asset/claymap/index.html")
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
