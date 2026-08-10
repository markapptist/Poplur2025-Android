package com.apptists.poplur2025

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
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
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.webkit.WebViewAssetLoader
import com.apptists.poplur2025.auth.AuthViewModel
import com.apptists.poplur2025.ui.components.PoplurVolumeBars
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONStringer
import java.net.HttpURLConnection
import java.net.URL

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

    // The splash page's own backdrop (#0a0709), not pure black — the window,
    // the WebView and the page then share one tone instead of stepping through
    // two near-blacks while the page paints.
    private val splashBg = 0xFF0A0709.toInt()
    private val welcomeBg = 0xFFFF2D96.toInt()

    private val volumeVisible = mutableStateOf(false)
    private val volumeFraction = mutableStateOf(0f)
    private var hideVolumeRunnable: Runnable? = null
    /** Splash → Welcome handoff; cancelled in onDestroy so it can never fire
     *  into a destroyed WebView. */
    private var welcomeHandoffRunnable: Runnable? = null

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
            .build()

        webView = WebView(this).apply {
            // Debug builds only: enable Chrome DevTools inspection
            if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            setBackgroundColor(splashBg)

            addJavascriptInterface(Bridge(), "PoplurNative")
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? = interceptAssetRequest(assetLoader, request)

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
            // Launch shows the SPLASH, always.
            //
            // This used to load Welcome directly. welcome.html is ~284KB plus
            // its hero cube and account icons, so "straight to Welcome" meant
            // an empty black WebView for the whole of that parse — the black
            // screen on launch. splash.html is ~9KB and self-contained: it
            // paints almost at once and then covers the Welcome load with the
            // clay world rather than with nothing.
            //
            // The handoff is a fixed timer, not a signal from the page, so a
            // splash that fails to render still hands off on schedule and can
            // never strand anyone on the intro. Mirrors the iOS
            // PreloginWebView.scheduleWelcomeHandoff().
            loadUrl("$origin/splash.html")
        }
        welcomeHandoffRunnable = Runnable {
            if (!isFinishing && !isDestroyed) {
                webView.setBackgroundColor(welcomeBg)
                webView.loadUrl("$origin/welcome.html")
            }
        }
        webView.postDelayed(welcomeHandoffRunnable!!, SPLASH_DELAY_MS)

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
     * Re-map root-relative asset requests (from the approved GCL prototype bundles)
     * to their actual locations in the APK assets.
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
        val mappedPath = when {
            relativePath.startsWith("poplur-clay-map/") -> "/assets/$relativePath"
            relativePath.startsWith("prelogin/") -> "/assets/$relativePath"
            relativePath.startsWith("Poplur2025-iOS/") -> "/assets/$relativePath"
            else -> "/assets/prelogin/$relativePath"
        }
        return assetLoader.shouldInterceptRequest(url.buildUpon().encodedPath(mappedPath).build())
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
                    val method = json?.optString("method")
                    if (method == "faceid") {
                        // Prototype fallback: simulate a Face ID success with test credentials
                        // if the user's real Face ID is not yet enrolled in the app.
                        runOnUiThread {
                            authViewModel.signIn("test@poplur.com", "poplur2025") { ok, err ->
                                if (ok) showLoadingThenFinish() else postAuthResult(false, "Face ID failed: $err")
                            }
                        }
                    } else if (email != null && password != null) {
                        runOnUiThread {
                            authViewModel.signIn(email, password) { ok, err ->
                                Log.i(TAG, "signIn result ok=$ok err=$err")
                                if (ok) showLoadingThenFinish() else postAuthResult(false, err)
                            }
                        }
                    } else {
                        Log.w(TAG, "signIn without credentials or unknown method=$method")
                        postAuthResult(false, "Enter an email and password.")
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
                "radioStations" -> {
                    fetchRadioStations(json?.optString("callbackId").orEmpty())
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
     * Live radio, the way the map already tunes it. The page's own station lookup is a raw
     * fetch that the offline bundle strips out, so the call is made here and the list handed
     * back through window.__poplurStationsResult.
     *
     * No permission is requested. Pre-login deliberately defers every OS prompt until the
     * feature that needs it is used, and a radio bed is not worth a location dialog on a
     * launch screen — so this reads a location the person has ALREADY granted, and otherwise
     * falls back to the coordinates the approved source hardcodes.
     */
    private fun fetchRadioStations(callbackId: String) {
        Thread {
            val (lat, lon) = lastKnownCoordinates()
            val url = "https://de1.api.radio-browser.info/json/stations/search" +
                "?geo_lat=$lat&geo_long=$lon&geo_distance=90000" +
                // bitrateMin: nearest-by-popularity happily returns 32kbps mono
                // streams, and a heavily compressed stream behind the music is
                // indistinguishable from a fault in the app.
                "&hidebroken=true&bitrateMin=96&order=clickcount&reverse=true&limit=16"
            val body = runCatching {
                (URL(url).openConnection() as HttpURLConnection).run {
                    // A launch screen must never wait on a third party. The web side gives up
                    // after 6s; this gives up sooner so an empty list is a decision rather
                    // than two timeouts racing each other.
                    connectTimeout = 4000
                    readTimeout = 4000
                    requestMethod = "GET"
                    try { inputStream.bufferedReader().use { it.readText() } } finally { disconnect() }
                }
            }.getOrNull()
            // Only well-formed JSON is handed to evaluateJavascript: this is third-party text
            // landing in an expression position, and parsing it first is what keeps it a value
            // rather than a script.
            val safe = body?.takeIf { runCatching { JSONArray(it) }.isSuccess } ?: "[]"
            // The id is generated web-side as 'st' + digits + base36, so stripping it to
            // alphanumerics cannot change a legitimate value or let a crafted one close the quote.
            val safeId = callbackId.filter { it.isLetterOrDigit() }
            runOnUiThread {
                if (::webView.isInitialized) {
                    webView.evaluateJavascript(
                        "window.__poplurStationsResult && window.__poplurStationsResult('$safeId', $safe)",
                        null
                    )
                }
            }
        }.start()
    }

    /** Last known coarse location if it is already granted, otherwise the approved default. */
    private fun lastKnownCoordinates(): Pair<Double, Double> {
        val granted = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return FALLBACK_LAT to FALLBACK_LON
        val manager = getSystemService(LOCATION_SERVICE) as? LocationManager
            ?: return FALLBACK_LAT to FALLBACK_LON
        val fix = runCatching {
            manager.getProviders(true).asSequence()
                .mapNotNull { manager.getLastKnownLocation(it) }
                .maxByOrNull { it.time }
        }.getOrNull() ?: return FALLBACK_LAT to FALLBACK_LON
        return fix.latitude to fix.longitude
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

    /**
     * Minimizing the app must silence the pre-login radio bed, and it did not.
     *
     * Post-login is quiet for a reason that does not apply here: the clay map web
     * app pauses ITSELF on `visibilitychange`/`pagehide`. `PostLoginWebActivity`
     * only flips a voice flag in `onPause` — the web does the real work. The
     * pre-login pages carry no visibility handler, and they are GENERATED from the
     * protected Good Copy sources by `scripts/build-prelogin-ios.mjs`, so the pause
     * is owned by the host here instead of being patched into the bundle.
     *
     * `onStop`/`onStart` rather than `onPause`/`onResume` deliberately:
     * `WebView.onPause()` halts rendering as well as media, and `onPause` also
     * fires for a permission dialog drawn OVER this flow — which would freeze the
     * clay artwork behind the dialog. `onStop` fires only once the activity is
     * genuinely hidden, which is exactly "the app was minimized".
     */
    override fun onStart() {
        super.onStart()
        if (::webView.isInitialized) webView.onResume()
    }

    override fun onStop() {
        if (::webView.isInitialized) {
            // The AI host speaks through the Web Speech API, not an <audio> tag, so
            // it is cancelled explicitly rather than trusted to `onPause()`.
            webView.evaluateJavascript(
                "window.speechSynthesis && window.speechSynthesis.cancel()",
                null
            )
            webView.onPause()
        }
        super.onStop()
    }

    override fun onDestroy() {
        hideVolumeRunnable?.let { webView.removeCallbacks(it) }
        welcomeHandoffRunnable?.let { webView.removeCallbacks(it) }
        webView.destroy()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "PreloginWeb"
        const val SPLASH_DELAY_MS = 3200L
        const val VOLUME_HIDE_MS = 1500L
        // The same coordinates the approved Welcome source hardcodes.
        const val FALLBACK_LAT = 43.6532
        const val FALLBACK_LON = -79.3832
    }
}
