package com.apptists.poplur2025

import android.annotation.SuppressLint
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
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
import androidx.lifecycle.ViewModelProvider
import androidx.webkit.WebViewAssetLoader
import com.apptists.poplur2025.auth.AuthViewModel
import com.apptists.poplur2025.ui.components.PoplurVolumeBars
import org.json.JSONObject
import org.json.JSONStringer

/**
 * Pre-login web-embed host (Phase 1/2).
 *
 * Renders the EXACT approved prototype pre-login screens inside a WebView instead of a native
 * Compose remake: the offline-hardened bundles at assets/prelogin/{splash,welcome}.html (cleaned
 * from good-copy-splash.html / good-copy-welcome.html — frame runtime, screenlinks catalog and
 * every network call stripped). Served over WebViewAssetLoader's virtual
 * https://appassets.androidplatform.net origin, the same pattern proven by ClayMapActivity.
 *
 * Flow mirrors the GCL prototype: splash.html for the prototype's 3.2s beat, then welcome.html.
 *
 * Bridge: window.PoplurNative.onAction(action, payload) -> AuthViewModel.signIn / signUp. Real auth
 * runs when the web posts credentials; failures are posted back via window.poplurAuthResult so the
 * form can show them. Real OS permission prompts remain Phase 3.
 *
 * Volume: this standalone Activity does not get PoplurRoot's Compose volume overlay, so it handles
 * hardware volume itself — adjusting STREAM_MUSIC with flag 0 (suppresses the SYSTEM HUD) and
 * showing the app's own PoplurVolumeBars in a ComposeView above the WebView, matching the rest of
 * the app instead of the OS volume bar.
 */
class PreloginWebActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var authViewModel: AuthViewModel
    private lateinit var audioManager: AudioManager

    private val origin = "https://appassets.androidplatform.net/assets/prelogin"

    private val splashBg = 0xFF000000.toInt()
    private val welcomeBg = 0xFFFF2D96.toInt()

    private val volumeVisible = mutableStateOf(false)
    private val volumeFraction = mutableStateOf(0f)
    private var hideVolumeRunnable: Runnable? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        window.statusBarColor = splashBg
        window.navigationBarColor = splashBg

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            setBackgroundColor(splashBg)

            addJavascriptInterface(Bridge(), "PoplurNative")
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

                override fun onPageFinished(view: WebView, url: String?) {
                    // 3GB-class devices: flatten the expensive effects (blur/filter),
                    // keep layout + icons, and throttle the decorative animation
                    // loops to a consistent gentle cadence — see PoplurLiteTier.
                    if (PoplurLiteTier.isLowTier(view.context)) {
                        PoplurLiteTier.injectLiteCss(view)
                        PoplurLiteTier.injectRafThrottle(view)
                    }
                }
            }
            // Launch splash dropped by request: straight to Welcome on black.
            loadUrl("$origin/welcome.html")
        }

        // WebView + a Compose overlay hosting the app's custom volume bars, so a hardware volume
        // press shows PoplurVolumeBars over the web content (the system HUD is suppressed below).
        val root = FrameLayout(this)
        root.addView(webView)
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
        root.addView(volumeOverlay)
        setContentView(root)


        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
            }
        })
    }

    /**
     * Intercept hardware volume keys: adjust STREAM_MUSIC with flag 0 (NO system UI) so the OS
     * volume HUD never appears, and show the app's own PoplurVolumeBars instead.
     */
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
            return true // consume so the system HUD never shows
        }
        return super.dispatchKeyEvent(event)
    }

    private fun showCustomVolumeBars() {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        volumeFraction.value = if (max > 0) current.toFloat() / max else 0f
        volumeVisible.value = true
        hideVolumeRunnable?.let { webView.removeCallbacks(it) }
        hideVolumeRunnable = Runnable { volumeVisible.value = false }
        webView.postDelayed(hideVolumeRunnable!!, VOLUME_HIDE_MS)
    }

    /** Post an auth result back to the web form so it can show a failure (shake + banner). */
    private fun postAuthResult(ok: Boolean, error: String?) {
        val json = JSONStringer().`object`()
            .key("ok").value(ok)
            .key("error").value(error ?: "")
            .endObject()
            .toString()
        webView.post {
            webView.evaluateJavascript(
                "window.poplurAuthResult && window.poplurAuthResult($json)",
                null
            )
        }
    }

    inner class Bridge {
        @JavascriptInterface
        fun onAction(action: String, payload: String?) {
            Log.i(TAG, "bridge action=$action payload=${payload?.take(80)}")
            val json = runCatching { JSONObject(payload ?: "") }.getOrNull()
            val email = json?.optString("email")?.takeIf { it.isNotBlank() }
            val password = json?.optString("password")?.takeIf { it.isNotBlank() }
            val permission = json?.optString("permission")?.takeIf { it.isNotBlank() }
            val choice = json?.optString("choice")?.takeIf { it.isNotBlank() }

            when (action) {
                "signIn" -> {
                    if (email != null && password != null) {
                        runOnUiThread {
                            authViewModel.signIn(email, password) { ok, err ->
                                Log.i(TAG, "signIn result ok=$ok err=$err")
                                if (ok) showLoadingThenFinish() else postAuthResult(false, err)
                            }
                        }
                    } else {
                        Log.w(TAG, "signIn without credentials — ignoring, no faked success")
                    }
                }
                "signUp" -> {
                    if (email != null && password != null) {
                        runOnUiThread {
                            authViewModel.signUp(email, password) { ok, err ->
                                Log.i(TAG, "signUp result ok=$ok err=$err")
                                if (ok) showLoadingThenFinish() else postAuthResult(false, err)
                            }
                        }
                    } else {
                        Log.w(TAG, "signUp without credentials — ignoring, no faked success")
                    }
                }
                "requestPermission" -> {
                    val normalizedChoice = choice?.lowercase()
                    val primerPermission = when (permission) {
                        "location" -> PoplurPermissionPrimer.LOCATION
                        "camera", "photos" -> PoplurPermissionPrimer.PHOTOS
                        else -> null
                    }
                    val denied = normalizedChoice?.endsWith("deny") == true
                    val allowed = normalizedChoice == "allow" ||
                        normalizedChoice?.endsWith("-allow") == true ||
                        normalizedChoice?.endsWith("-once") == true
                    if (primerPermission == null || (!denied && !allowed)) {
                        Log.i(TAG, "requestPermission ignored invalid permission=$permission choice=$choice")
                        return
                    }
                    PoplurPermissionPrimer.markHandled(this@PreloginWebActivity, primerPermission)
                    if (denied) {
                        Log.i(TAG, "requestPermission permission=$permission declined in-app — OS prompt not shown")
                        return
                    }
                    // The custom clay card is the only startup permission UI.
                    // A real Android sheet is deferred until the feature that
                    // needs the capability is opened.
                    Log.i(TAG, "requestPermission permission=$permission accepted in-app; OS prompt deferred")
                }
                "complete" -> {
                    val signedIn = authViewModel.isSignedIn.value == true
                    if (signedIn) {
                        runOnUiThread { finish() }
                    } else {
                        Log.i(TAG, "complete before auth — staying on pre-login (real auth finishes on success)")
                    }
                }
                else -> {
                    Log.i(TAG, "unhandled bridge action=$action — TODO Phase 3")
                }
            }
        }
    }

    /**
     * On real auth success, hand off to the same approved web Loading → Home flow as iOS.
     * The post-login Activity owns both surfaces so the older native Compose home is never
     * exposed during the transition.
     */
    private fun showLoadingThenFinish() {
        // fromAuth: explicit sign-in/up shows the approved globe Loading; the
        // already-signed-in launch path shows the looping splash instead (user rule).
        startActivity(Intent(this, PostLoginWebActivity::class.java).putExtra("fromAuth", true))
        finish()
    }

    override fun onDestroy() {
        hideVolumeRunnable?.let { webView.removeCallbacks(it) }
        webView.destroy()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "PreloginWeb"
        const val SPLASH_DELAY_MS = 3200L
        const val VOLUME_HIDE_MS = 1500L
    }
}
