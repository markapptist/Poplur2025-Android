package com.apptists.poplur2025

import android.annotation.SuppressLint
import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.util.Base64
import android.view.KeyEvent
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import com.apptists.poplur2025.ads.AdManager
import com.apptists.poplur2025.data.FeedAdPlacementKind
import com.apptists.poplur2025.data.FeedAdProbability
import com.apptists.poplur2025.ui.screens.bindFullScreenNativeAd
import com.apptists.poplur2025.ui.screens.createFullScreenNativeAdView
import com.google.android.gms.ads.nativead.NativeAd
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewAssetLoader
import com.apptists.poplur2025.ui.components.PoplurVolumeBars
import com.apptists.poplur2025.data.AppTab
import com.apptists.poplur2025.ui.PoplurAICommand
import com.apptists.poplur2025.ui.PoplurAIAssistantOverlay
import com.apptists.poplur2025.ui.PoplurAIParser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.json.JSONArray
import org.json.JSONObject
import java.util.LinkedHashMap
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

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

    private val TAG = "PostLoginWebActivity"

    private lateinit var homeWebView: WebView
    private lateinit var rootLayout: FrameLayout

    // The Google slot is a FEED POST, not an interstitial. It uses the same
    // full-screen native card the retired Compose feed used, laid over the web
    // feed while the web hides its own chrome — so nothing of the home is buried
    // under it and it reads as the next post rather than an interruption.
    private var feedAdContainer: FrameLayout? = null
    private var feedAd: NativeAd? = null
    private lateinit var loadingWebView: WebView
    private lateinit var performanceController: com.apptists.poplur2025.performance.PoplurWebViewPerformanceController
    private lateinit var audioManager: AudioManager

    private val homeReady = mutableStateOf(false)
    private val minimumElapsed = mutableStateOf(false)
    private val volumeVisible = mutableStateOf(false)
    private val volumeFraction = mutableStateOf(0f)
    private val voiceForeground = mutableStateOf(false)
    private val voiceRequestToken = mutableIntStateOf(0)
    private var pendingNativeVoiceCommand: String? = null
    @Volatile private var cameraLiveVisible = false
    private var hideVolumeRunnable: Runnable? = null
    private var revealRunnable: Runnable? = null

    private val darkHome = 0xFF050407.toInt()
    private val darkLoading = 0xFF0A0709.toInt()
    private val origin = "https://appassets.androidplatform.net/assets"
    private data class WebCapture(val uri: Uri, val mimeType: String)
    // Asset requests run on WebView worker threads while camera/picker results
    // arrive on the activity thread.
    private val webCapturesLock = Any()
    private val webCaptures = LinkedHashMap<String, WebCapture>(MAX_WEB_CAPTURE_ROUTES, 0.75f, true)
    private var pendingPhotoUri: Uri? = null
    private var pendingVideoUri: Uri? = null
    private val faceFrameBusy = AtomicBoolean(false)
    private val cameraFaceDetector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setMinFaceSize(0.12f)
                .enableTracking()
                .build()
        )
    }

    private val takeWebPhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingPhotoUri
        pendingPhotoUri = null
        if (saved && uri != null) deliverCapture(uri, "photo", "image/jpeg")
    }
    private val takeWebVideo = registerForActivityResult(ActivityResultContracts.CaptureVideo()) { saved ->
        val uri = pendingVideoUri
        pendingVideoUri = null
        if (saved && uri != null) deliverCapture(uri, "video", "video/mp4")
    }

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
        performanceController = com.apptists.poplur2025.performance.PoplurWebViewPerformanceController(
            this,
            "PoplurPostLoginWeb",
        )
        performanceController.attach(homeWebView, loadingWebView)

        val root = FrameLayout(this)
        rootLayout = root
        root.addView(homeWebView, matchParentParams())
        root.addView(loadingWebView, matchParentParams())

        // Keep the app-owned volume bars consistent with the pre-login and native surfaces.
        val volumeOverlay = ComposeView(this).apply {
            setContent {
                val visible by volumeVisible
                val fraction by volumeFraction
                val voiceEnabled by voiceForeground
                val explicitVoiceRequest by voiceRequestToken
                Box(Modifier.fillMaxSize()) {
                    if (visible) {
                        PoplurVolumeBars(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 28.dp),
                            volumeFraction = fraction
                        )
                    }
                    PoplurAIAssistantOverlay(
                        enabled = voiceEnabled,
                        accent = Color(0xFFFF4FA3),
                        explicitRequestToken = explicitVoiceRequest,
                        modifier = Modifier.fillMaxSize(),
                        onCommand = ::deliverNativeVoiceCommand
                    )
                }
            }
        }
        root.addView(volumeOverlay, matchParentParams())
        setContentView(root)

        // Both URLs are the exact bundles used by the iOS post-login path.
        val mapPrefs = getSharedPreferences("poplur_settings", MODE_PRIVATE)
        val lowTier = PoplurLiteTier.isLowTier(this)
        val use3DMap = if (mapPrefs.contains("poplur_use3DMap")) {
            mapPrefs.getBoolean("poplur_use3DMap", !lowTier)
        } else {
            !lowTier
        }
        val fromAuth = intent.getBooleanExtra("fromAuth", false)
        val permissionQuery = if (fromAuth) {
            ""
        } else {
            buildString {
                if (!PoplurWebLocation.hasPermission(this@PostLoginWebActivity) &&
                    !PoplurPermissionPrimer.wasHandled(
                        this@PostLoginWebActivity,
                        PoplurPermissionPrimer.LOCATION
                    )
                ) {
                    append("&askLocation=1")
                }
                if (!PoplurWebCameraRoll.hasPermission(this@PostLoginWebActivity) &&
                    !PoplurPermissionPrimer.wasHandled(
                        this@PostLoginWebActivity,
                        PoplurPermissionPrimer.PHOTOS
                    )
                ) {
                    append("&askPhotos=1")
                }
            }
        }
        homeWebView.loadUrl(
            "$origin/claymap/index.html?view=home&tabs=1" +
                "&mapMode=${if (use3DMap) "3d" else "2d"}" +
                "&fallback2d=${if (!use3DMap && lowTier) "1" else "0"}" +
                permissionQuery
        )
        // User rule: the approved globe Loading only after explicit sign-in/up;
        // an already-signed-in launch loops the splash (its clay bar refills)
        // while the home warms up behind it.
        // The cover is ALWAYS the splash now: the approved globe Loading needed
        // three.js + WebGL + model decode just to appear, delaying the very wait
        // it was meant to cover. The splash is one hardware-decoded video with the
        // same clay progress bar. The globe screen remains the approved GCL design
        // (and is being kept for the watch/wear build).
        loadingWebView.loadUrl("$origin/prelogin/splash.html")

        // Push an existing fix without prompting. Missing permissions are
        // surfaced by Home's custom clay primer; only its Allow action posts
        // requestLocation/requestCameraRoll back through the bridge below.
        if (PoplurWebLocation.hasPermission(this)) {
            PoplurWebLocation.pushLastKnown(homeWebView)
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
                flushPendingNativeVoiceCommand()
                revealIfReady()
            }
        }, HOME_READY_TIMEOUT_MS)
    }

    // ---- Live cube motion -------------------------------------------------
    //
    // CubeTab3D binds `deviceorientation` only when it is NOT in a native shell
    // and otherwise waits for `poplur-native-motion` — a contract iOS honours
    // (PostLoginHomeWebView.swift:834) and Android never did, so every cube sat
    // dead still here. Web `deviceorientation` is not a fallback on this class
    // of hardware: measured on the A03 (SM-S135DL) it fires ONCE with
    // alpha/beta/gamma all null, because Chrome cannot fuse an orientation
    // without a gyroscope or magnetometer and this phone has neither.
    // `devicemotion` does stream at ~60Hz, so gravity from the accelerometer is
    // the only real source — which is exactly what this reads.
    private val sensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }
    /// Low-passed gravity vector. An accelerometer alone cannot separate gravity
    /// from movement, so without this a shake reads as a tilt.
    private var gravityX = 0f
    private var gravityY = 0f
    private var gravityZ = 0f
    private var hasGravity = false
    private var lastMotionPostUptimeMs = 0L
    private var lastPostedRoll = Float.NaN
    private var lastPostedPitch = Float.NaN

    private val motionListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
            val alpha = 0.8f
            if (hasGravity) {
                gravityX = alpha * gravityX + (1 - alpha) * event.values[0]
                gravityY = alpha * gravityY + (1 - alpha) * event.values[1]
                gravityZ = alpha * gravityZ + (1 - alpha) * event.values[2]
            } else {
                gravityX = event.values[0]
                gravityY = event.values[1]
                gravityZ = event.values[2]
                hasGravity = true
            }

            // Radians, matching Core Motion's attitude.roll/.pitch — the web
            // divides by 0.72 rad (CubeTab3D onNativeMotion), so the unit must
            // match or the cube barely moves. Android's accelerometer frame is
            // x=right, y=top, z=out of screen; at rest face-up that is
            // (0, 0, +9.81). Negating x/y matches Core Motion's sign, so a
            // right-side-down tilt rolls the cube the same way on both phones.
            val roll = kotlin.math.atan2(
                -gravityX,
                kotlin.math.sqrt(gravityY * gravityY + gravityZ * gravityZ)
            )
            val pitch = kotlin.math.atan2(
                -gravityY,
                kotlin.math.sqrt(gravityX * gravityX + gravityZ * gravityZ)
            )

            // Same discipline as iOS: cap at 30Hz and drop samples that have not
            // meaningfully changed, so a phone lying still costs no bridge
            // crossings at all. This runs on an attended foreground screen only.
            val now = android.os.SystemClock.uptimeMillis()
            if (now - lastMotionPostUptimeMs < 33L) return
            // 0.025 rad (~1.4°), NOT iOS's 0.008. Core Motion is a gyro-fused
            // attitude and is quiet at rest; this phone has no gyro, so roll and
            // pitch are derived from raw accelerometer noise whose angular
            // jitter is ~0.01 rad. Measured on the A03: at 0.008 a phone lying
            // untouched still posted 9.7 events/sec — a bridge crossing per
            // 100ms on every post-login screen, for no visible motion. A tilt
            // gesture moves tens of degrees, so this stays well below anything
            // the eye can follow.
            if (!lastPostedRoll.isNaN()
                && kotlin.math.abs(roll - lastPostedRoll) < 0.025f
                && kotlin.math.abs(pitch - lastPostedPitch) < 0.025f
            ) return
            lastMotionPostUptimeMs = now
            lastPostedRoll = roll
            lastPostedPitch = pitch
            if (!::homeWebView.isInitialized) return
            homeWebView.evaluateJavascript(
                "window.dispatchEvent(new CustomEvent('poplur-native-motion'," +
                    "{detail:{roll:$roll,pitch:$pitch}}));true;",
                null
            )
        }
    }

    private fun startCubeMotion() {
        val manager = sensorManager ?: return
        val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        // SENSOR_DELAY_GAME is ~50Hz; the 30Hz post cap above does the real
        // rate limiting. Anything faster only burns battery on a budget phone.
        manager.registerListener(motionListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
    }

    private fun stopCubeMotion() {
        sensorManager?.unregisterListener(motionListener)
        hasGravity = false
        lastPostedRoll = Float.NaN
        lastPostedPitch = Float.NaN
    }

    override fun onResume() {
        super.onResume()
        voiceForeground.value = true
        // Sensor delivery is foreground-only: an unregistered listener is the
        // difference between a cube that moves and a background wake-up budget
        // this app has already been burned by.
        startCubeMotion()
    }

    override fun onStart() {
        super.onStart()
        if (::performanceController.isInitialized) performanceController.setVisible(true)
    }

    override fun onStop() {
        if (::performanceController.isInitialized) performanceController.setVisible(false)
        super.onStop()
    }

    override fun onPause() {
        voiceForeground.value = false
        stopCubeMotion()
        super.onPause()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(
        assetLoader: WebViewAssetLoader,
        backgroundColor: Int,
        receivesNativeEvents: Boolean
    ): WebView = WebView(this).apply {
        // Debug builds only: without this there is no devtools socket on the
        // device, so the web home can only be profiled blind through gfxinfo —
        // frame times with nothing to attribute them to. Never on in release.
        if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.mediaPlaybackRequiresUserGesture = false
        overScrollMode = WebView.OVER_SCROLL_NEVER
        setBackgroundColor(backgroundColor)
        if (receivesNativeEvents) addJavascriptInterface(Bridge(), "PoplurNative")
        if (receivesNativeEvents) {
            webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest) {
                    runOnUiThread {
                        val wantsVideo = request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                        val cameraGranted = ContextCompat.checkSelfPermission(
                            this@PostLoginWebActivity,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (wantsVideo && cameraGranted) {
                            // The shared camera deliberately asks for video only.
                            // Never grant microphone access as a side effect.
                            request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                        } else {
                            request.deny()
                        }
                    }
                }
            }
        }
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
        if (path.startsWith("/assets/captures/")) {
            val key = path.substringAfterLast('/')
            val capture = synchronized(webCapturesLock) { webCaptures[key] } ?: return null
            val stream = try {
                contentResolver.openInputStream(capture.uri)
            } catch (_: Exception) {
                null
            } ?: return null
            return WebResourceResponse(capture.mimeType, null, stream)
        }
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

    private fun launchWebPhotoCapture() {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "Poplur-${System.currentTimeMillis()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Poplur")
            }
        }
        pendingPhotoUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        pendingPhotoUri?.let(takeWebPhoto::launch)
    }

    private fun launchWebVideoCapture() {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "Poplur-${System.currentTimeMillis()}.mp4")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/Poplur")
            }
        }
        pendingVideoUri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        pendingVideoUri?.let(takeWebVideo::launch)
    }

    private fun deliverCapture(uri: Uri, kind: String, mimeType: String) {
        val src = registerWebCapture(uri, mimeType)
        val detail = JSONObject().apply {
            put("kind", kind)
            put("src", src)
        }
        homeWebView.evaluateJavascript(
            "window.dispatchEvent(new CustomEvent('poplur-native-capture', { detail: $detail })); true;", null
        )
    }

    /**
     * The Google placement, rendered as a feed post.
     *
     * It deliberately does NOT go behind the web layer: the home WebView paints
     * every post-login screen opaquely, and making it transparent to show a view
     * underneath would put the whole app's background at risk for one ad slot.
     * Instead the card sits over the feed and the WEB takes its own chrome away
     * for the duration, so the cube and HUD are never buried under an ad.
     */
    private fun showFeedNativeAd() {
        if (feedAdContainer != null) return // one slot at a time
        AdManager.loadNativeAd(
            context = this,
            adUnitId = BuildConfig.ADMOB_FEED_NATIVE_UNIT_ID,
            onLoaded = { ad ->
                runOnUiThread {
                    // A slot that arrives after the screen is gone, or after another
                    // already opened, is dropped rather than leaked.
                    if (isFinishing || isDestroyed || feedAdContainer != null) {
                        ad.destroy()
                        return@runOnUiThread
                    }
                    presentFeedNativeAd(ad)
                }
            },
            onFailed = { /* No fill: the feed simply carries on, with no gap. */ },
        )
    }

    private fun presentFeedNativeAd(ad: NativeAd) {
        val adView = createFullScreenNativeAdView(this)
        bindFullScreenNativeAd(adView, ad)
        val container = FrameLayout(this).apply {
            addView(adView, matchParentParams())
            isClickable = true
            // You leave a feed post by moving on, so the whole card takes the tap.
            setOnClickListener { dismissFeedNativeAd() }
        }
        feedAd = ad
        feedAdContainer = container
        rootLayout.addView(container, matchParentParams())
        setWebFeedAdSlotOpen(true)
    }

    private fun dismissFeedNativeAd() {
        val container = feedAdContainer ?: return
        feedAdContainer = null
        rootLayout.removeView(container)
        // The native ad's MediaView runs on its own clock, outside Poplur's feed
        // audio tracking — without this it keeps talking over the next post.
        runCatching { feedAd?.mediaContent?.videoController?.stop() }
        feedAd?.destroy()
        feedAd = null
        setWebFeedAdSlotOpen(false)
    }

    private fun setWebFeedAdSlotOpen(open: Boolean) {
        homeWebView.evaluateJavascript(
            "window.dispatchEvent(new CustomEvent('poplur-native-feed-ad', " +
                "{ detail: { open: $open } })); true;",
            null,
        )
    }

    private fun registerWebCapture(uri: Uri, mimeType: String): String {
        val key = UUID.randomUUID().toString()
        synchronized(webCapturesLock) {
            webCaptures[key] = WebCapture(uri, mimeType)
            while (webCaptures.size > MAX_WEB_CAPTURE_ROUTES) {
                val eldest = webCaptures.entries.iterator()
                if (eldest.hasNext()) {
                    eldest.next()
                    eldest.remove()
                }
            }
        }
        return "$origin/captures/$key"
    }

    private fun sendUploadResult(requestId: String, success: Boolean, error: String? = null) {
        val result = JSONObject().apply {
            put("requestId", requestId)
            put("ok", success)
            if (!success && !error.isNullOrBlank()) put("error", error)
        }
        homeWebView.post {
            homeWebView.evaluateJavascript(
                "window.dispatchEvent(new CustomEvent('poplur-upload-result', { detail: $result })); true;", null
            )
        }
    }

    private fun uploadClayPost(body: JSONObject) {
        val requestId = body.optString("requestId")
        if (requestId.isBlank()) return
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            sendUploadResult(requestId, false, "You must be signed in before posting.")
            return
        }
        val data = body.optString("data")
        val kind = if (body.optString("kind") == "video") "video" else "photo"
        if (data.isBlank()) {
            sendUploadResult(requestId, false, "That post has no media to upload.")
            return
        }

        val storageRef = FirebaseStorage.getInstance().reference
            .child("posts/${user.uid}/${System.currentTimeMillis()}.${if (kind == "video") "mp4" else "jpg"}")
        val captureKey = Uri.parse(data).lastPathSegment
        val storedCapture = captureKey?.let { key ->
            synchronized(webCapturesLock) { webCaptures[key] }
        }
        val upload = when {
            storedCapture != null -> storageRef.putFile(storedCapture.uri)
            data.startsWith("data:") -> {
                val encoded = data.substringAfter(',', "")
                if (encoded.isBlank()) {
                    sendUploadResult(requestId, false, "That media could not be prepared.")
                    return
                }
                storageRef.putBytes(android.util.Base64.decode(encoded, android.util.Base64.DEFAULT))
            }
            else -> {
                sendUploadResult(requestId, false, "That media is no longer available on this device.")
                return
            }
        }

        upload.addOnFailureListener { error ->
            sendUploadResult(requestId, false, error.localizedMessage ?: "Your post could not be uploaded.")
        }.addOnSuccessListener {
            storageRef.downloadUrl.addOnFailureListener { error ->
                sendUploadResult(requestId, false, error.localizedMessage ?: "Your uploaded media could not be read.")
            }.addOnSuccessListener { mediaUrl ->
                val post = hashMapOf<String, Any>(
                    "userID" to user.uid,
                    "username" to (user.displayName ?: user.email ?: "Poplur user"),
                    "mediaURL" to mediaUrl.toString(),
                    "mediaType" to kind,
                    "viewCount" to 0,
                    "radius" to (body.optDouble("radiusKm", 5.0) * 1000).toInt(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                if (body.has("latitude") && body.has("longitude")) {
                    post["latitude"] = body.optDouble("latitude")
                    post["longitude"] = body.optDouble("longitude")
                }
                body.optString("caption").takeIf { it.isNotBlank() }?.let { post["caption"] = it }
                body.optString("channelId").takeIf { it.isNotBlank() }?.let { post["channelId"] = it }
                body.optString("filterId").takeIf { it.isNotBlank() }?.let { post["filterId"] = it }
                if (body.optBoolean("usedAI")) post["usedAI"] = true

                FirebaseFirestore.getInstance().collection("posts").add(post)
                    .addOnSuccessListener { sendUploadResult(requestId, true) }
                    .addOnFailureListener { error ->
                        sendUploadResult(requestId, false, error.localizedMessage ?: "Your post could not be saved.")
                    }
            }
        }
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
                performanceController.detach(loadingWebView)
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
            val body = runCatching { JSONObject(json) }.getOrNull() ?: return
            val type = body.optString("type")
            if (type == "homeReady" || type == "mapReady") {
                runOnUiThread {
                    homeReady.value = true
                    flushPendingNativeVoiceCommand()
                    revealIfReady()
                }
            }
            if (type == "requestVoiceAssistant") {
                // The shared HEY POPLUR profile button is an explicit action, and
                // therefore the only Home event allowed to launch Android's
                // RECORD_AUDIO permission sheet.
                runOnUiThread { voiceRequestToken.intValue += 1 }
            }
            if (type == "requestProfile") {
                deliverWebProfile()
            }
            if (type == "requestCameraRoll") {
                PoplurPermissionPrimer.markHandled(
                    this@PostLoginWebActivity,
                    PoplurPermissionPrimer.PHOTOS
                )
                runOnUiThread {
                    if (PoplurWebCameraRoll.hasPermission(this@PostLoginWebActivity)) {
                        PoplurWebCameraRoll.push(homeWebView, ::registerWebCapture)
                    } else {
                        // The custom clay primer is the only startup prompt.
                        // Defer the Android system sheet until the user actually
                        // opens Camera Roll.
                        PoplurWebCameraRoll.deliverDenied(homeWebView)
                    }
                }
            }
            if (type == "requestLocation") {
                PoplurPermissionPrimer.markHandled(
                    this@PostLoginWebActivity,
                    PoplurPermissionPrimer.LOCATION
                )
                runOnUiThread {
                    if (PoplurWebLocation.hasPermission(this@PostLoginWebActivity)) {
                        PoplurWebLocation.pushLastKnown(homeWebView)
                    } else {
                        Log.i(TAG, "location primer accepted in-app; system prompt deferred")
                    }
                }
            }
            if (type == "permissionHandled") {
                when (body.optString("permission")) {
                    "location" -> PoplurPermissionPrimer.markHandled(
                        this@PostLoginWebActivity,
                        PoplurPermissionPrimer.LOCATION
                    )
                    "photos", "camera" -> PoplurPermissionPrimer.markHandled(
                        this@PostLoginWebActivity,
                        PoplurPermissionPrimer.PHOTOS
                    )
                }
            }
            if (type == "requestCameraPermission") {
                runOnUiThread {
                    if (ContextCompat.checkSelfPermission(
                            this@PostLoginWebActivity,
                            Manifest.permission.CAMERA
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this@PostLoginWebActivity,
                            arrayOf(Manifest.permission.CAMERA),
                            CAMERA_REQUEST_CODE
                        )
                    }
                }
            }
            if (type == "detectCameraFrame") {
                detectCameraFrame(
                    body.optString("dataUrl"),
                    body.optBoolean("mirrored", false)
                )
            }
            if (type == "feedChannelChanged" || type == "feedContentAdvanced") {
                // Real inventory follows a post change as well as a channel
                // change. A completed clip used to be invisible to this bridge,
                // which made long feed sessions miss every native-ad chance.
                val channel = body.optString("channelId", "unknown")
                val index = body.optInt("index", 0)
                val isContentAdvance = type == "feedContentAdvanced"
                val namespace = if (isContentAdvance) {
                    "web_${channel}_content"
                } else {
                    "web_$channel"
                }
                val config = FeedAdProbability.currentConfig(this@PostLoginWebActivity)
                // A fixed three-post cadence mirrors the 30% network share
                // while guaranteeing small channels do not hash into an
                // all-Poplur session.
                val shouldPresent = if (isContentAdvance) {
                    val sequence = body.optInt("sequence", 0)
                    sequence > 0 && sequence % 3 == 0
                } else {
                    FeedAdProbability.placement(index, namespace, config) ==
                        FeedAdPlacementKind.GoogleMobileAd
                }
                if (shouldPresent) {
                    runOnUiThread { showFeedNativeAd() }
                }
            }
            if (type == "radioScope") {
                val scope = body.optString("scope")
                if (scope == "screen" || scope == "app" || scope == "outside") {
                    getSharedPreferences("poplur_audio", MODE_PRIVATE)
                        .edit()
                        .putString("radio_scope", scope)
                        .apply()
                }
            }
            if (type == "fetchProductImage") {
                PoplurProductImage.handle(body.optString("url"), homeWebView)
            }
            if (type == "openInAppBrowser") {
                openInAppBrowser(body.optString("url"))
            }
            // SettingsScreen.tsx has posted this since sign-out was added, and
            // Android silently dropped it — the SIGN OUT button did nothing here.
            // With MainActivity routing straight past login whenever Firebase
            // reports a user, a session that is restored from cache and then
            // invalidated server-side left no route back to the login screen at
            // all: signed in enough to skip login, signed out enough that
            // deliverWebProfile finds no currentUser and the profile stays empty.
            if (type == "signOut") {
                runOnUiThread {
                    FirebaseAuth.getInstance().signOut()
                    startActivity(
                        Intent(this@PostLoginWebActivity, PreloginWebActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    finish()
                }
            }
            if (type == "cameraLiveVisibilityChanged") {
                cameraLiveVisible = body.optBoolean("visible", false)
            }
            if (type == "cameraShutter") {
                if (cameraLiveVisible) {
                    runOnUiThread { launchWebPhotoCapture() }
                } else {
                    Log.w(TAG, "Ignoring camera shutter outside the live camera")
                }
            }
            if (type == "cameraControl" && body.optString("control") == "record") {
                if (cameraLiveVisible) {
                    runOnUiThread { launchWebVideoCapture() }
                } else {
                    Log.w(TAG, "Ignoring video capture outside the live camera")
                }
            }
            if (type == "publishPost") runOnUiThread { uploadClayPost(body) }
        }
    }

    private fun deliverWebProfile() {
        // Diagnostic: on the A03 no profile payload EVER reached the web — not even
        // the username the pre-existing code sent — and every failure mode on this
        // path is silent. Each branch is logged so "no profile set" can be told
        // apart from "no signed-in user" and "Firestore never completed".
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.w(TAG, "deliverWebProfile: no FirebaseAuth.currentUser — nothing sent")
            return
        }
        Log.i(TAG, "deliverWebProfile: uid=${user.uid} requesting Firestore users doc")
        val fallbackUsername = user.email
            ?.substringBefore("@")
            ?.takeIf { it.isNotBlank() }
            ?: "mark"
        FirebaseFirestore.getInstance().collection("users").document(user.uid).get()
            .addOnCompleteListener { task ->
                Log.i(
                    TAG,
                    "deliverWebProfile: Firestore complete success=${task.isSuccessful} " +
                        "exists=${task.result?.exists()} err=${task.exception?.javaClass?.simpleName}: " +
                        "${task.exception?.message}"
                )
                val document = if (task.isSuccessful) task.result else null
                val accountType = document?.getString("accountType")
                val business = accountType.equals("business", ignoreCase = true)
                    || document?.getBoolean("isBusinessAccount") == true
                val payload = JSONObject().apply {
                    put("username", document?.getString("username")
                        ?: document?.getString("handle")
                        ?: fallbackUsername)
                    put("business", business)
                    // `profilePictureURL` is what EditProfile actually writes (iOS
                    // EditProfileView.swift:276 and the same document here), so it has to
                    // lead. Reading only avatarUrl/photoURL is why a fresh sign-in on
                    // another device showed no picture even though one was set.
                    val photo = document?.getString("profilePictureURL")
                        ?: document?.getString("avatarUrl")
                        ?: document?.getString("photoURL")
                        ?: user.photoUrl?.toString()
                    if (!photo.isNullOrBlank()) put("photo", photo)
                    // The profile is a ten-second selfie VIDEO, not a still. Without this
                    // the web falls back to its localStorage copy, which is per-device —
                    // so signing in on a second device rendered "NO PROFILE PHOTO SET".
                    // iOS delivers the same key from the same field
                    // (PostLoginHomeWebView.swift:1276).
                    val video = document?.getString("profileSelfieVideoURL")
                    if (!video.isNullOrBlank()) put("video", video)
                }.toString()
                Log.i(TAG, "deliverWebProfile: dispatching payload=$payload")
                homeWebView.post {
                    homeWebView.evaluateJavascript(
                        "window.poplurProfile?.($payload);" +
                            "window.dispatchEvent(new CustomEvent('poplur-native-profile',{detail:$payload}));true;",
                        null
                    )
                }
            }
    }

    private fun openInAppBrowser(rawUrl: String) {
        val uri = runCatching { Uri.parse(rawUrl.trim()) }.getOrNull()
        if (uri == null || !uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank()) {
            Log.w(TAG, "Ignoring invalid in-app browser URL")
            return
        }

        runOnUiThread {
            runCatching {
                val colors = CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(0xFF1A0C17.toInt())
                    .setNavigationBarColor(darkHome)
                    .build()
                CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .setUrlBarHidingEnabled(false)
                    .setDefaultColorSchemeParams(colors)
                    .build()
                    .launchUrl(this@PostLoginWebActivity, uri)
            }.onFailure { error ->
                Log.e(TAG, "Unable to open the in-app browser", error)
            }
        }
    }

    private fun deliverNativeVoiceCommand(command: PoplurAICommand) {
        val detail = JSONObject()
        // Native TTS is outside the WebView audio graph. Include the exact
        // spoken line so the shared Home can hold its Voice owner for the
        // utterance and prevent Feed/Map from continuing underneath it.
        detail.put("feedback", PoplurAIParser.feedback(command))
        when (command) {
            is PoplurAICommand.OpenTab -> when (command.tab) {
                AppTab.Map -> detail.put("tab", "map")
                AppTab.Camera, AppTab.CreateEntry, AppTab.CapturedOutput -> detail.put("tab", "post")
                AppTab.Feed -> detail.put("tab", "feed")
                AppTab.Profile -> detail.put("tab", "profile")
                else -> Unit
            }
            is PoplurAICommand.Camera -> detail.put("tab", "post")
            is PoplurAICommand.GoToChannel -> {
                detail.put("tab", "feed")
                detail.put("channel", command.query)
            }
            is PoplurAICommand.PostToLocation -> detail.put("tab", "post")
            is PoplurAICommand.SortPosts, PoplurAICommand.ShowAnalytics -> detail.put("tab", "profile")
            is PoplurAICommand.OpenPage,
            is PoplurAICommand.SetTourActive,
            PoplurAICommand.Logout,
            is PoplurAICommand.Unknown -> Unit
        }
        val json = detail.toString()
        if (!homeReady.value) {
            pendingNativeVoiceCommand = json
            return
        }
        dispatchNativeVoiceCommand(json)
    }

    private fun flushPendingNativeVoiceCommand() {
        val json = pendingNativeVoiceCommand ?: return
        pendingNativeVoiceCommand = null
        dispatchNativeVoiceCommand(json)
    }

    private fun dispatchNativeVoiceCommand(json: String) {
        val deliver = {
            homeWebView.evaluateJavascript(
                "window.dispatchEvent(new CustomEvent('poplur-native-voice-command', { detail: $json })); true;",
                null
            )
        }
        if (Looper.myLooper() == Looper.getMainLooper()) deliver() else homeWebView.post(deliver)
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
                PoplurWebCameraRoll.push(homeWebView, ::registerWebCapture)
            } else {
                PoplurWebCameraRoll.deliverDenied(homeWebView)
            }
        }
        if (requestCode == CAMERA_REQUEST_CODE &&
            grantResults.any { it == PackageManager.PERMISSION_GRANTED }
        ) {
            homeWebView.evaluateJavascript(
                "window.dispatchEvent(new Event('poplur-camera-permission-granted')); true;",
                null
            )
        }
    }

    override fun onDestroy() {
        if (::performanceController.isInitialized) performanceController.close()
        revealRunnable?.let { loadingWebView.removeCallbacks(it) }
        hideVolumeRunnable?.let { homeWebView.removeCallbacks(it) }
        // An ad outliving its screen keeps a MediaView (and its audio) alive.
        // Drop the view first: the web is going away, so telling it to restore
        // its chrome would evaluate against a dying WebView.
        feedAdContainer?.let { rootLayout.removeView(it) }
        feedAdContainer = null
        runCatching { feedAd?.mediaContent?.videoController?.stop() }
        feedAd?.destroy()
        feedAd = null
        synchronized(webCapturesLock) { webCaptures.clear() }
        homeWebView.destroy()
        // The loading WebView is destroyed at reveal time; only destroy here if
        // the activity dies before the reveal (destroy() twice throws).
        if (loadingWebView.parent != null) loadingWebView.destroy()
        cameraFaceDetector.close()
        super.onDestroy()
    }

    private fun detectCameraFrame(dataUrl: String, mirrored: Boolean) {
        if (!dataUrl.startsWith("data:image/") || dataUrl.length > MAX_CAMERA_FRAME_DATA_URL_CHARS) return
        if (!faceFrameBusy.compareAndSet(false, true)) return
        val bitmap = runCatching {
            val bytes = Base64.decode(dataUrl.substringAfter(','), Base64.DEFAULT)
            if (bytes.size > MAX_CAMERA_FRAME_BYTES) return@runCatching null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val sourceWidth = bounds.outWidth.takeIf { it > 0 } ?: return@runCatching null
            val sourceHeight = bounds.outHeight.takeIf { it > 0 } ?: return@runCatching null
            var sampleSize = 1
            while (max(sourceWidth, sourceHeight) / sampleSize > MAX_CAMERA_FRAME_EDGE * 2) {
                sampleSize *= 2
            }
            BitmapFactory.decodeByteArray(
                bytes,
                0,
                bytes.size,
                BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                },
            )
        }.getOrNull()
        if (bitmap == null) {
            faceFrameBusy.set(false)
            return
        }

        runCatching {
            cameraFaceDetector.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { faces ->
                    val width = bitmap.width.toDouble().coerceAtLeast(1.0)
                    val height = bitmap.height.toDouble().coerceAtLeast(1.0)
                    val payload = JSONArray()
                    faces.forEachIndexed { index, face ->
                        val box = face.boundingBox
                        val normalizedX = if (mirrored) 1.0 - (box.right / width) else box.left / width
                        payload.put(JSONObject().apply {
                            put("id", "face-${face.trackingId ?: index}")
                            put("x", normalizedX.coerceIn(0.0, 1.0))
                            put("y", (box.top / height).coerceIn(0.0, 1.0))
                            put("w", (box.width() / width).coerceIn(0.0, 1.0))
                            put("h", (box.height() / height).coerceIn(0.0, 1.0))
                            put("label", "person")
                            put("confidence", 0.95)
                        })
                    }
                    homeWebView.post {
                        homeWebView.evaluateJavascript(
                            "window.dispatchEvent(new CustomEvent('poplur-native-detections',{detail:$payload}));true;",
                            null
                        )
                    }
                }
                .addOnCompleteListener {
                    bitmap.recycle()
                    faceFrameBusy.set(false)
                }
        }.onFailure {
            bitmap.recycle()
            faceFrameBusy.set(false)
        }
    }

    private companion object {
        const val LOADING_HOLD_MS = 3500L
        const val HOME_READY_TIMEOUT_MS = 12000L
        const val REVEAL_DURATION_MS = 450L
        const val VOLUME_HIDE_MS = 1500L
        const val CAMERA_REQUEST_CODE = 7403
        const val MAX_WEB_CAPTURE_ROUTES = 16
        const val MAX_CAMERA_FRAME_DATA_URL_CHARS = 4_000_000
        const val MAX_CAMERA_FRAME_BYTES = 3_000_000
        const val MAX_CAMERA_FRAME_EDGE = 960
    }
}

private fun matchParentParams(): FrameLayout.LayoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
    )
