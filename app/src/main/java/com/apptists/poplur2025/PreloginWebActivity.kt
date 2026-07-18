package com.apptists.poplur2025

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.webkit.WebViewAssetLoader
import com.apptists.poplur2025.auth.AuthViewModel
import org.json.JSONObject

/**
 * Pre-login web-embed host (Phase 1).
 *
 * Renders the EXACT approved prototype pre-login screens inside a WebView instead of a native
 * Compose remake: the offline-hardened bundles at assets/prelogin/{splash,welcome}.html (cleaned
 * from good-copy-splash.html / good-copy-welcome.html — frame runtime, screenlinks catalog and
 * every network call stripped). Served over WebViewAssetLoader's virtual
 * https://appassets.androidplatform.net origin, the same pattern proven by ClayMapActivity (a
 * file:// page's opaque origin would break module scripts / workers).
 *
 * Flow mirrors the GCL prototype: splash.html for the prototype's 3.2s beat, then welcome.html.
 *
 * Skin: the welcome bundle forces the iphone dark pill skin unconditionally in CSS (the source's
 * `@media (prefers-color-scheme: dark)` rules re-emitted un-gated), so no WebView-side dark hacks
 * are needed — the A03s WebView does not evaluate `prefers-color-scheme: dark` for this content.
 *
 * Bridge: window.PoplurNative.onAction(action, payload) -> AuthViewModel.signIn / signUp. When the
 * web forms do not yet post credentials the call is logged and left unhandled (no faked success);
 * real credential/permission wiring is Phase 3.
 */
class PreloginWebActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var authViewModel: AuthViewModel

    private val origin = "https://appassets.androidplatform.net/assets/prelogin"

    private val splashBg = 0xFF0A0709.toInt()
    private val welcomeBg = 0xFFFF2D96.toInt()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        // Dark status bar for the splash beat; light icons over both dark splash and pink welcome.
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        window.statusBarColor = splashBg
        window.navigationBarColor = splashBg

        // Serve the bundled screens (/assets/prelogin/*.html) plus the mirrored
        // asset tree the screens reference by absolute path — three.js + the Draco
        // decoder under /poplur-clay-map/node_modules/…, the compressed 3D models,
        // the 2D clay-cube fallback, audio, video and logo under
        // /poplur-clay-map/public/… and /Poplur2025-iOS/Poplur2025/…. The mirror
        // lives under assets/ at those exact paths, so a catch-all "/" handler
        // (registered AFTER "/assets/", which is matched first) resolves each
        // request 1:1 with zero network calls. This is why the bundle stays
        // fully offline (grep -c http on welcome.html == 0).
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            // Match the approved splash background (#0a0709) so there is no colour flash before the
            // web splash paints, flowing seamlessly out of the neutralised system splash.
            setBackgroundColor(splashBg)

            addJavascriptInterface(Bridge(), "PoplurNative")
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)
            }
            loadUrl("$origin/splash.html")
        }
        setContentView(webView)

        // Advance splash -> welcome after the prototype's 3.2s splash beat; recolour the status bar
        // to the welcome's pink at the same moment.
        webView.postDelayed({
            if (isFinishing) return@postDelayed
            webView.setBackgroundColor(welcomeBg)
            window.statusBarColor = welcomeBg
            webView.loadUrl("$origin/welcome.html")
        }, SPLASH_DELAY_MS)

        // Pre-login is a gate (the native flow swallowed Back too). Send the task to the background
        // instead of exposing the empty host behind it.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
            }
        })
    }

    inner class Bridge {
        /**
         * The finalized Phase-2 bridge schema. The self-navigating web flow calls
         * window.PoplurNative.onAction(action, payloadJson) at these boundaries:
         *
         *   action              payload                              Phase-2 handling
         *   ------------------  -----------------------------------  ----------------------------------
         *   signIn              {email,password} | {method:"faceId"} real auth if creds → finish; else log
         *   signUp              {email,password,accountType}         real auth if creds → finish; else log
         *   requestPermission   {kind:"camera"|"location", choice}   log only (Phase-3 real OS prompt)
         *   complete            {via}                                hand off to signed-in path (finish
         *                                                            once authenticated)
         *
         * Phase-3 stubs must never fake success: requestPermission does not grant, and signIn/signUp
         * without real credentials only log — they do not sign the user in.
         *
         * @param payload JSON string; may be empty/non-JSON.
         */
        @JavascriptInterface
        fun onAction(action: String, payload: String?) {
            Log.i(TAG, "bridge action=$action payload=${payload?.take(80)}")
            val json = runCatching { JSONObject(payload ?: "") }.getOrNull()
            val email = json?.optString("email")?.takeIf { it.isNotBlank() }
            val password = json?.optString("password")?.takeIf { it.isNotBlank() }
            val kind = json?.optString("kind")?.takeIf { it.isNotBlank() }

            when (action) {
                "signIn" -> {
                    if (email != null && password != null) {
                        runOnUiThread {
                            authViewModel.signIn(email, password) { ok, err ->
                                Log.i(TAG, "signIn result ok=$ok err=$err")
                                if (ok) finish() // hand off to the signed-in app in MainActivity
                            }
                        }
                    } else {
                        // TODO Phase 3: web sign-in form does not post credentials yet. Do not fake
                        // success — leave the user on the web pre-login until real wiring lands.
                        Log.w(TAG, "signIn without credentials — TODO Phase 3 wiring, ignoring")
                    }
                }
                "signUp" -> {
                    if (email != null && password != null) {
                        runOnUiThread {
                            authViewModel.signUp(email, password) { ok, err ->
                                Log.i(TAG, "signUp result ok=$ok err=$err")
                                if (ok) finish()
                            }
                        }
                    } else {
                        // TODO Phase 3: web sign-up flow (account type / profile / verify business)
                        // does not post credentials yet. Do not fake success.
                        Log.w(TAG, "signUp without credentials — TODO Phase 3 wiring, ignoring")
                    }
                }
                "requestPermission" -> {
                    // Phase 3: raise the real Android runtime permission dialog for `kind`
                    // (camera/location) and reflect the OS result back into the web. Phase 2 logs
                    // only — it must NOT fake a grant.
                    Log.i(TAG, "requestPermission kind=$kind — TODO Phase 3 real OS prompt, not granted")
                }
                "complete" -> {
                    // Flow completion → hand off to the signed-in path. The activity only exits once
                    // auth has actually succeeded (isSignedIn == true), so PoplurRoot recomposes into
                    // the signed-in content instead of stranding on the dark placeholder. When the
                    // flow completes before real auth (Phase-2 stubs), stay put — no faked handoff.
                    val signedIn = authViewModel.isSignedIn.value == true
                    if (signedIn) {
                        runOnUiThread { finish() }
                    } else {
                        Log.i(TAG, "complete before auth — staying on pre-login (signIn/signUp finishes on real success)")
                    }
                }
                else -> {
                    // TODO Phase 3: Face ID, in-web navigation boundaries, openSettings.
                    Log.i(TAG, "unhandled bridge action=$action — TODO Phase 3")
                }
            }
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "PreloginWeb"
        const val SPLASH_DELAY_MS = 3200L
    }
}
