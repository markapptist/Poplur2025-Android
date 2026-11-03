package com.apptists.poplur2025

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.LocationManager
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.apptists.poplur2025.auth.AuthViewModel
import com.apptists.poplur2025.data.AccountType
import com.apptists.poplur2025.performance.PoplurWebViewPerformanceController
import com.apptists.poplur2025.prelogin.NativePreloginDraft
import com.apptists.poplur2025.ui.components.PoplurVolumeBars
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONStringer
import java.io.File
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
 * This IS the signed-out flow, as on iOS (PoplurPreloginWeb.isEnabled): every route to pre-login
 * goes through [intent]. NativePreloginActivity stays behind it only as the fallback — a device
 * with no usable WebView, or a sign-up whose profile save is waiting on the native Retry.
 *
 * Bridge: window.PoplurNative.onAction(action, payload), kept in step with the iOS
 * PreloginWebView coordinator — signIn / signUp (real Firebase auth; failures come back through
 * window.poplurAuthResult), resetPassword (the approved Forgot control, see passwordResetScript),
 * requestPermission (records the clay card's answer; the OS sheet waits for the feature),
 * poplurGenderSelected, radioStations, pop and complete. The shell scripts below are the iOS
 * ones, injected rather than written into the generated bundle.
 *
 * Volume: this standalone Activity does not get PoplurRoot's Compose volume overlay, so it handles
 * hardware volume itself — adjusting STREAM_MUSIC with flag 0 (suppresses the SYSTEM HUD) and
 * showing the app's own PoplurVolumeBars in a ComposeView above the WebView, matching the rest of
 * the app instead of the OS volume bar.
 */
class PreloginWebActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var performanceController: PoplurWebViewPerformanceController
    private lateinit var authViewModel: AuthViewModel
    private lateinit var audioManager: AudioManager

    private val origin = "$ASSET_ORIGIN/assets/prelogin"

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
    private var passwordResetPending = false

    /** The approved personal sign-up asks for a profile photo through an <input type=file>,
     *  which a WebView only answers when the host opens a picker for it. */
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        fileChooserCallback?.onReceiveValue(uri?.let { arrayOf(it) })
        fileChooserCallback = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startAtWelcome = intent.getBooleanExtra(EXTRA_START_AT_WELCOME, false)

        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        window.statusBarColor = splashBg
        window.navigationBarColor = splashBg

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        // A missing or broken WebView package throws here. Fall back to the native flow rather
        // than leave a signed-out person on a crashed launch.
        webView = try { WebView(this) } catch (e: Exception) {
            Log.e(TAG, "WebView unavailable — falling back to the native pre-login", e)
            startActivity(Intent(this, NativePreloginActivity::class.java))
            finish()
            return
        }
        webView.apply {
            // Debug builds only: enable Chrome DevTools inspection
            if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            setBackgroundColor(splashBg)

            addJavascriptInterface(Bridge(), "PoplurNative")
            if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                WebViewCompat.addDocumentStartJavaScript(this, SKIP_LOOKALIKE_PERMISSION_CARDS, setOf(ASSET_ORIGIN))
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    view: WebView,
                    callback: ValueCallback<Array<Uri>>,
                    params: FileChooserParams
                ): Boolean {
                    fileChooserCallback?.onReceiveValue(null)
                    fileChooserCallback = callback
                    val type = params.acceptTypes.firstOrNull { it.isNotBlank() } ?: "image/*"
                    return runCatching { pickImage.launch(type) }.fold(
                        onSuccess = { true },
                        onFailure = {
                            fileChooserCallback = null
                            callback.onReceiveValue(null)
                            true
                        }
                    )
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                    // Older WebViews without document-start scripts: early enough for Welcome,
                    // which reads the flag when its permission cards would first open.
                    if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                        view.evaluateJavascript(SKIP_LOOKALIKE_PERMISSION_CARDS, null)
                    }
                }

                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? = interceptAssetRequest(assetLoader, request)

                override fun onPageFinished(view: WebView, url: String?) {
                    // The iOS shell's document-end scripts (PreloginWebView): hide the unwired
                    // Face ID button and give the approved Forgot control its Firebase action.
                    view.evaluateJavascript(HIDE_UNWIRED_FACE_ID_BUTTON, null)
                    view.evaluateJavascript(PASSWORD_RESET_SCRIPT, null)
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
            //
            // Except right after MainActivity's cold-launch splash, which has just played
            // the same approved clip: open on Welcome so it is not shown twice (iOS
            // startAtWelcome).
            if (startAtWelcome) {
                setBackgroundColor(welcomeBg)
                loadUrl("$origin/welcome.html")
            } else {
                loadUrl("$origin/splash.html")
            }
        }
        performanceController = PoplurWebViewPerformanceController(this, "PoplurPreloginWeb")
        performanceController.attach(webView)
        welcomeHandoffRunnable = Runnable {
            if (!isFinishing && !isDestroyed) {
                webView.setBackgroundColor(welcomeBg)
                webView.loadUrl("$origin/welcome.html")
            }
        }
        if (!startAtWelcome) webView.postDelayed(welcomeHandoffRunnable!!, SPLASH_DELAY_MS)

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

    /** Answer the Forgot request the injected passwordResetScript is waiting on. */
    private fun postPasswordResetResult(requestId: String, ok: Boolean, error: String?) {
        val json = JSONStringer().`object`()
            .key("requestId").value(requestId)
            .key("ok").value(ok)
            .key("error").value(error ?: "")
            .endObject()
            .toString()
        webView.post {
            webView.evaluateJavascript(
                "window.poplurPasswordResetResult && window.poplurPasswordResetResult($json)",
                null
            )
        }
    }

    inner class Bridge {
        @JavascriptInterface
        fun onAction(action: String, payload: String?) {
            val json = runCatching { JSONObject(payload ?: "") }.getOrNull()
            // Keys only: the payload carries the password and, on sign-up, the photo.
            Log.i(TAG, "bridge action=$action keys=${json?.keys()?.asSequence()?.toList()}")
            val email = json?.optString("email")?.trim()?.takeIf { it.isNotBlank() }
            val password = json?.optString("password")?.takeIf { it.isNotBlank() }
            val permission = json?.optString("permission")?.takeIf { it.isNotBlank() }
            val choice = json?.optString("choice")?.takeIf { it.isNotBlank() }

            when (action) {
                "pop" -> runOnUiThread {
                    if (::webView.isInitialized) webView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
                "signIn" -> {
                    val method = json?.optString("method")
                    if (method == "faceid") {
                        // Face ID is not wired to the web pre-login (the shell hides the button,
                        // as on iOS). Never fake a success.
                        postAuthResult(false, "Face ID sign-in isn’t available here yet — use your email and password.")
                    } else if (email != null && password != null) {
                        runOnUiThread {
                            authViewModel.signIn(email, password) { ok, err ->
                                Log.i(TAG, "signIn result ok=$ok err=$err")
                                if (ok) {
                                    postAuthResult(true, null)
                                    showLoadingThenFinish()
                                } else {
                                    postAuthResult(false, err)
                                }
                            }
                        }
                    } else {
                        Log.w(TAG, "signIn without credentials or unknown method=$method")
                        postAuthResult(false, "Enter your email and password.")
                    }
                }
                "resetPassword" -> {
                    val requestId = json?.optString("requestId").orEmpty()
                    if (requestId.isEmpty() || requestId.length > 128) return
                    runOnUiThread { resetPassword(email, requestId) }
                }
                "signUp" -> {
                    if (email == null || password == null) {
                        Log.w(TAG, "signUp without credentials — ignoring, no faked success")
                        postAuthResult(false, "Enter your email and password to create your account.")
                        return
                    }
                    val accountType = if (json?.optString("accountType") == "business") {
                        AccountType.Business
                    } else {
                        AccountType.User
                    }
                    val photo = if (accountType == AccountType.User) {
                        decodeProfilePhoto(json?.optString("profilePhotoDataURI")) ?: run {
                            Log.w(TAG, "rejected invalid personal profile photo")
                            postAuthResult(false, "Choose your profile photo again, then create your account.")
                            return
                        }
                    } else {
                        null
                    }
                    runOnUiThread {
                        authViewModel.signUp(email, password, accountType) { ok, err ->
                            Log.i(TAG, "signUp result ok=$ok type=${accountType.wireValue} err=$err")
                            if (!ok) {
                                postAuthResult(false, err)
                                return@signUp
                            }
                            photo?.let(::saveProfilePhoto)
                            postAuthResult(true, null)
                            showLoadingThenFinish()
                        }
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
                "poplurGenderSelected" -> {
                    // The same persistence as the native flow (NativePreloginModel):
                    // PostLoginWebActivity injects it into the clay map as localStorage,
                    // and users/{uid}.profileGender carries it to every device.
                    val gender = json?.optString("gender") ?: json?.optString("type")
                    if (gender != "male" && gender != "female") {
                        Log.i(TAG, "ignored unknown gender=$gender")
                        return
                    }
                    val prefs = getSharedPreferences("poplur_native_onboarding", MODE_PRIVATE).edit()
                        .putString("gender", gender)
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid == null) {
                        prefs.apply()
                        Log.i(TAG, "gender $gender stored without uid — no signed-in user")
                        return
                    }
                    prefs.putString("uid", uid).apply()
                    FirebaseFirestore.getInstance().collection("users").document(uid)
                        .set(mapOf("profileGender" to gender), SetOptions.merge())
                        .addOnFailureListener { Log.w(TAG, "profileGender write FAILED", it) }
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
                    Log.i(TAG, "unhandled bridge action=$action")
                }
            }
        }
    }

    /** The approved Forgot control → Firebase's password reset email (iOS resetPassword). */
    private fun resetPassword(email: String?, requestId: String) {
        if (!::webView.isInitialized || webView.url?.startsWith(origin) != true) return
        if (email == null || email.length > 254) {
            postPasswordResetResult(requestId, false, "Enter your email address first.")
            return
        }
        if (passwordResetPending) {
            postPasswordResetResult(requestId, false, "A reset email is still being requested. Check your inbox before trying again.")
            return
        }
        passwordResetPending = true
        FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener { task ->
            passwordResetPending = false
            // Give the same answer for an unknown account.
            val unknownAccount = task.exception is FirebaseAuthInvalidUserException
            postPasswordResetResult(
                requestId,
                task.isSuccessful || unknownAccount,
                if (task.isSuccessful || unknownAccount) null else task.exception?.localizedMessage
            )
        }
    }

    /**
     * Accept only the bounded JPEG the bundled sign-up page produces (iOS
     * decodeProfilePhotoDataURI): the envelope is checked before decoding, so a crafted
     * message cannot make the shell allocate an arbitrary amount of memory.
     */
    private fun decodeProfilePhoto(dataUri: String?): ByteArray? {
        val prefix = "data:image/jpeg;base64,"
        if (dataUri == null || dataUri.length > MAX_PHOTO_URI_CHARS || !dataUri.startsWith(prefix)) return null
        val bytes = runCatching { Base64.decode(dataUri.substring(prefix.length), Base64.DEFAULT) }.getOrNull()
            ?: return null
        if (bytes.size < 3 || bytes.size > MAX_PHOTO_BYTES ||
            bytes[0] != 0xFF.toByte() || bytes[1] != 0xD8.toByte() || bytes[2] != 0xFF.toByte()
        ) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
            maxOf(bounds.outWidth, bounds.outHeight) > MAX_PHOTO_EDGE
        ) return null
        return bytes
    }

    /**
     * After Firebase confirms the account: the same profile.jpg + profile_pictures/{uid}.jpg +
     * users/{uid}.profilePictureURL the native flow writes. The upload runs on Firebase's own
     * tasks so it carries on after this activity hands off to Home.
     */
    private fun saveProfilePhoto(bytes: ByteArray) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        runCatching { File(filesDir, "profile.jpg").writeBytes(bytes) }
            .onFailure { Log.w(TAG, "profile photo persistence FAILED", it) }
        val reference = FirebaseStorage.getInstance().reference.child("profile_pictures/$uid.jpg")
        reference.putBytes(bytes, StorageMetadata.Builder().setContentType("image/jpeg").build())
            .continueWithTask { upload ->
                upload.exception?.let { throw it }
                reference.downloadUrl
            }
            .continueWithTask { url ->
                url.exception?.let { throw it }
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .set(mapOf("profilePictureURL" to url.result.toString()), SetOptions.merge())
            }
            .addOnFailureListener { Log.w(TAG, "profile photo upload FAILED", it) }
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
     *
     * THESE TWO MUST STAY PAIRED. `WebView.onPause()` halts rendering, so an
     * `onStop` that pauses without an `onStart` that resumes is a permanent black
     * screen from the first time the app is backgrounded — it has already happened
     * once, when the resume was dropped during a merge and the pause survived. If
     * you touch either half, check the other.
     */
    override fun onStart() {
        super.onStart()
        com.apptists.poplur2025.util.PoplurEQSync.noteActivityStarted()
        if (::performanceController.isInitialized) performanceController.setVisible(true)
        if (::webView.isInitialized) {
            webView.onResume()
            // Undo a whole-app pauseTimers(); see PostLoginWebActivity.
            webView.resumeTimers()
        }
    }

    override fun onStop() {
        if (::performanceController.isInitialized) performanceController.setVisible(false)
        if (com.apptists.poplur2025.util.PoplurEQSync.noteActivityStopped() &&
            ::webView.isInitialized
        ) {
            webView.pauseTimers()
        }
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

    // The "Account deleted" confirmation is queued by PoplurAccountDeletion and
    // shown on the signed-out screen the deleted session lands on.
    override fun onResume() {
        super.onResume()
        PoplurAccountDeletion.bind(this)
    }

    override fun onPause() {
        PoplurAccountDeletion.unbind(this)
        super.onPause()
    }

    override fun onDestroy() {
        fileChooserCallback?.onReceiveValue(null)
        fileChooserCallback = null
        if (::performanceController.isInitialized) performanceController.close()
        if (::webView.isInitialized) {
            hideVolumeRunnable?.let { webView.removeCallbacks(it) }
            welcomeHandoffRunnable?.let { webView.removeCallbacks(it) }
            webView.destroy()
        }
        super.onDestroy()
    }

    companion object {
        /** The approved web flow IS the pre-login, as on iOS. The native flow stays in the
         *  tree behind this switch only. */
        private const val IS_ENABLED = true
        private const val EXTRA_START_AT_WELCOME = "startAtWelcome"

        /**
         * Every route to the signed-out flow. A sign-up whose profile save failed after the
         * account was created is a native draft only NativePreloginActivity can resume, so it
         * keeps that one.
         */
        fun intent(context: Context, startAtWelcome: Boolean = false): Intent =
            if (IS_ENABLED && !NativePreloginDraft.isPending(context)) {
                Intent(context, PreloginWebActivity::class.java)
                    .putExtra(EXTRA_START_AT_WELCOME, startAtWelcome)
            } else {
                Intent(context, NativePreloginActivity::class.java)
            }

        private const val TAG = "PreloginWeb"
        private const val ASSET_ORIGIN = "https://appassets.androidplatform.net"
        private const val SPLASH_DELAY_MS = 3200L
        private const val VOLUME_HIDE_MS = 1500L
        // The same coordinates the approved Welcome source hardcodes.
        private const val FALLBACK_LAT = 43.6532
        private const val FALLBACK_LON = -79.3832
        // The bounded JPEG the bundled sign-up page sends (iOS PreloginWebView limits).
        private const val MAX_PHOTO_BYTES = 900_000
        private const val MAX_PHOTO_URI_CHARS = 1_250_000
        private const val MAX_PHOTO_EDGE = 1024

        /** Same as iOS skipLookalikePermissionCards: the clay location / camera-roll cards
         *  are skipped at startup; the OS asks when a feature that needs it is opened. */
        private const val SKIP_LOOKALIKE_PERMISSION_CARDS =
            "try { sessionStorage.setItem('poplurWelcomePerms', '1') } catch (e) {}"

        /** Same as iOS hideUnwiredFaceIDButton: Face ID is not wired on the web pre-login. */
        private const val HIDE_UNWIRED_FACE_ID_BUTTON = """
            (function () {
              if (document.getElementById('poplurHideFaceId')) return
              var style = document.createElement('style')
              style.id = 'poplurHideFaceId'
              style.textContent = '#faceIdBtn, #faceIdBtn + .stackSub { display: none !important; }'
              ;(document.head || document.documentElement).appendChild(style)
            })();
        """

        /** iOS passwordResetScript, posting through PoplurNative instead of webkit. */
        private const val PASSWORD_RESET_SCRIPT = """
            (function () {
              var button = document.querySelector('#paneSignIn .forgotBtn')
              var emailField = document.querySelector('#paneSignIn .fieldMail')
              if (!button || !emailField || button.dataset.passwordResetBound) return
              button.dataset.passwordResetBound = '1'
              var pending = null, timeout = null
              var label = button.textContent
              function notice(message, title) {
                if (window.__poplurShowAlert) window.__poplurShowAlert(message, title)
              }
              function finish(message, title) {
                clearTimeout(timeout)
                pending = null
                button.disabled = false
                button.textContent = label
                notice(message, title)
              }
              window.poplurPasswordResetResult = function (result) {
                if (!pending || !result || result.requestId !== pending) return
                finish(result.ok
                  ? 'If this email has an account, a password reset link will arrive shortly.'
                  : result.error || 'Could not send the reset email. Please try again.',
                  result.ok ? 'Check your email' : 'Hold on!')
              }
              button.addEventListener('click', function () {
                if (pending) return
                var email = emailField.value.trim()
                if (!email || !emailField.checkValidity()) {
                  notice('Enter your email address first.', 'Hold on!')
                  emailField.focus()
                  return
                }
                var bridge = window.PoplurNative
                if (!bridge || typeof bridge.onAction !== 'function') { notice('Could not send the reset email. Please try again.', 'Hold on!'); return }
                pending = String(Date.now()) + '-' + Math.random().toString(36).slice(2)
                button.disabled = true
                button.textContent = 'SENDING…'
                timeout = setTimeout(function () {
                  finish('Could not confirm the reset email. Check your inbox before trying again.', 'Hold on!')
                }, 30000)
                try { bridge.onAction('resetPassword', JSON.stringify({ email: email, requestId: pending })) }
                catch (error) { finish('Could not send the reset email. Please try again.', 'Hold on!') }
              })
            })();
        """
    }
}
