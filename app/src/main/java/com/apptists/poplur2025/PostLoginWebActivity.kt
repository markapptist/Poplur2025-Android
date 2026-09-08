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
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Base64
import android.view.KeyEvent
import android.view.View
import android.webkit.JavascriptInterface
import androidx.core.app.NotificationManagerCompat
import android.webkit.PermissionRequest
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.ConsoleMessage
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.apptists.poplur2025.ads.AdManager
import com.apptists.poplur2025.data.FeedAdPlacementKind
import com.apptists.poplur2025.data.FeedAdProbability
import com.apptists.poplur2025.data.FeedAdProbabilityConfig
import com.apptists.poplur2025.ui.screens.bindFullScreenNativeAd
import com.apptists.poplur2025.ui.screens.createFullScreenNativeAdView
import com.google.android.gms.ads.nativead.NativeAd
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
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
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.apptists.poplur2025.ui.components.PoplurVolumeBars
import com.apptists.poplur2025.billing.PoplurBillingManager
import com.apptists.poplur2025.billing.PoplurProducts
import com.apptists.poplur2025.billing.PurchaseEvent
import com.apptists.poplur2025.billing.PurchaseEventStatus
import com.apptists.poplur2025.ui.PoplurNativeSplashCover
import com.apptists.poplur2025.ui.profile.ProfileStatusBarIcons
import com.apptists.poplur2025.ui.profile.ProfileTheme
import com.apptists.poplur2025.ui.profile.ProfileThemeRepository
import androidx.compose.ui.graphics.toArgb
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.json.JSONArray
import org.json.JSONObject
import java.util.LinkedHashMap
import java.util.UUID
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.max
import kotlin.math.roundToInt

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

    /** The only shape the account accepts — mirrors the iOS sanitizer. */
    private fun sanitizeUsername(value: String?): String? = value
        ?.trim()
        ?.replace("@", "")
        ?.filter { it.isLetterOrDigit() || it == '_' || it == '.' }
        ?.take(30)
        ?.takeIf { it.isNotEmpty() }

    private var mapDiscoveryRequestId = ""
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
    private var phoneBootPlayer: MediaPlayer? = null
    private val billingManager by lazy { PoplurBillingManager.getInstance(this) }
    private val profileThemeRepository by lazy { ProfileThemeRepository.getInstance(this) }

    private data class StoreCheckoutSession(
        val requestId: String,
        val productQueue: List<String>,
        var position: Int = 0,
        val completed: MutableList<JSONObject> = mutableListOf(),
        var pendingCompletion: Boolean = false
    )

    private var storeCheckoutSession: StoreCheckoutSession? = null
    private val storeCheckoutPreferences by lazy {
        getSharedPreferences("poplur_store_checkout", MODE_PRIVATE)
    }

    private val homeReady = mutableStateOf(false)
    private val minimumElapsed = mutableStateOf(false)
    private val volumeVisible = mutableStateOf(false)
    private val volumeFraction = mutableStateOf(0f)
    // False until prelogin/splash.html has actually committed a frame. Until then the
    // native clay bar covers the WebView/Chromium startup window — see PoplurNativeSplashCover.
    private val splashWebPainted = mutableStateOf(false)
    @Volatile private var cameraLiveVisible = false
    // WebRTC's WebView permission request arrives after the page asks for
    // getUserMedia. Retain one request while Android shows its normal OS
    // camera/microphone sheet, then answer the original WebView request.
    private var pendingWebMediaPermissionRequest: PermissionRequest? = null
    private var hideVolumeRunnable: Runnable? = null
    private var revealRunnable: Runnable? = null
    private var pendingDMThreadId: String? = null
    private var pendingPostFocusId: String? = null
    private var pendingPostFocusDetails: JSONObject? = null
    private var inAppBrowserDialog: PoplurInAppBrowserDialog? = null
    private var homeParkedForInAppBrowser = false
    private var activityStarted = false
    private var activityResumed = false
    private var notificationAuthListener: FirebaseAuth.AuthStateListener? = null
    private var notificationRegistration: ListenerRegistration? = null
    /** The live feed-posts forward. See deliverWebFeedPosts. */
    private var feedPostsRegistration: ListenerRegistration? = null
    private var ownFeedRegistration: ListenerRegistration? = null
    private val authoredPostsBridge = AuthoredPostsBridge { script ->
        if (!isDestroyed && ::homeWebView.isInitialized) homeWebView.evaluateJavascript(script, null)
    }
    private var feedAuthListener: FirebaseAuth.AuthStateListener? = null
    private val feedSnapshot = WebFeedSnapshot<com.google.firebase.firestore.DocumentSnapshot>(
        id = { it.id },
        owner = { it.getString("userID") ?: it.getString("userId") ?: it.getString("ownerId") },
    )
    private var feedGeneration = 0L
    private var notificationObservedUid: String? = null
    private var notificationDocumentIds: Map<String, String> = emptyMap()
    private val notificationGeneration = NotificationListenerGeneration()

    private val darkHome = 0xFF050407.toInt()
    private val darkLoading = 0xFF0A0709.toInt()
    private val origin = "https://appassets.androidplatform.net/assets"
    private val backgroundAudioPreferences by lazy {
        getSharedPreferences("poplur_audio", MODE_PRIVATE)
    }

    /**
     * Scope is retained for Radio's existing app/outside distinction; the
     * boolean is the shared Radio + iPod preference and defaults to screen-only.
     */
    private fun backgroundAudioPayload(): JSONObject {
        val enabled = backgroundAudioPreferences.getBoolean("background_audio_enabled", false)
        val savedScope = backgroundAudioPreferences.getString("radio_scope", "screen")
        val scope = if (!enabled) {
            "screen"
        } else if (savedScope == "outside") {
            "outside"
        } else {
            "app"
        }
        return JSONObject()
            .put("enabled", enabled)
            .put("scope", scope)
    }

    private fun installBackgroundAudioBootstrap(webView: WebView) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return
        val payload = backgroundAudioPayload().toString()
        val script = """
            (function () {
              var state = $payload;
              window.__poplurNativeBackgroundAudio = state;
              try {
                localStorage.setItem('poplur.radio.scope', state.scope);
                localStorage.setItem('poplur.radio.background', state.enabled ? '1' : '0');
                if (state.enabled) localStorage.setItem('poplur.radio.scope.explicit.v1', '1');
                else localStorage.removeItem('poplur.radio.scope.explicit.v1');
              } catch (_) {}
            })();
        """.trimIndent()
        runCatching {
            WebViewCompat.addDocumentStartJavaScript(
                webView,
                script,
                setOf("https://appassets.androidplatform.net")
            )
        }.onFailure { error ->
            Log.w(TAG, "background audio document-start bootstrap unavailable", error)
        }
    }

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
        pendingDMThreadId = intent.getStringExtra("threadId")
            ?.takeIf { it.isNotBlank() && it.length <= 200 }
        readPendingPostFocus(intent)

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
        // Only home is managed. The controller treats its first view as the
        // active one and pauses the rest — attaching the loading cover made
        // the surface the user is actually watching the paused one (its
        // splash <video> included) while home booted "active" underneath.
        // The cover is transient and destroyed at reveal; let it just play.
        performanceController.attach(homeWebView)

        val root = FrameLayout(this)
        rootLayout = root
        root.addView(homeWebView, matchParentParams())
        root.addView(loadingWebView, matchParentParams())

        // Keep the app-owned volume bars consistent with the pre-login and native surfaces.
        val volumeOverlay = ComposeView(this).apply {
            setContent {
                val visible by volumeVisible
                val fraction by volumeFraction
                val webSplashPainted by splashWebPainted
                Box(Modifier.fillMaxSize()) {
                    // Drawn on the activity's FIRST frame, so the wait is never a bare
                    // black screen. Retires the moment the web splash can take over.
                    if (!webSplashPainted) {
                        PoplurNativeSplashCover(modifier = Modifier.fillMaxSize())
                    }
                    if (visible) {
                        PoplurVolumeBars(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 28.dp),
                            volumeFraction = fraction
                        )
                    }
                }
            }
        }
        root.addView(volumeOverlay, matchParentParams())
        setContentView(root)

        storeCheckoutSession = restorePendingStoreCheckout()
        billingManager.connect()
        lifecycleScope.launch {
            billingManager.purchaseEvents.collect(::handleStorePurchaseEvent)
        }

        // Both URLs are the exact bundles used by the iOS post-login path.
        val lowTier = PoplurLiteTier.isLowTier(this)
        val mapPerformanceQuery = if (lowTier) "&mapLite=1" else ""
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
                mapPerformanceQuery +
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
        // The APPROVED clay splash is the SHORT VIDEO CLIP — one hardware-decoded
        // mp4 with the candy-stripe clay bar over it. Not the globe Loading screen:
        // that is a different approved screen, kept for the watch/wear build, and
        // swapping it in here was wrong.
        //
        // What made this look broken was never the choice of document. It is that
        // Chromium takes 5-6s to initialise on an A03, so the clip cannot paint
        // yet and Android's own starting window (logo on black) is what you see.
        // PoplurNativeSplashCover fills exactly that window with the same clay bar
        // and hands over the moment the clip commits its first frame.
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
    /// Compass heading in degrees clockwise from north, or NaN on hardware that
    /// cannot supply one. Gravity alone gives tilt but not AIM: it cannot tell a
    /// phone panned to face a different wall from one that never moved, which is
    /// why the cube ignored being pointed around. TYPE_ROTATION_VECTOR is the
    /// fused source that can, and phones without it (the A03 has neither gyro
    /// nor magnetometer) simply keep the previous parallax-only behaviour.
    private var headingDeg = Float.NaN
    private var lastPostedHeading = Float.NaN
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    /// Shortest angular distance, so a pan across north (359.9° -> 0.1°) reads
    /// as 0.2 and not 359.8.
    private fun headingMovedSincePost(): Boolean {
        if (headingDeg.isNaN()) return false
        if (lastPostedHeading.isNaN()) return true
        var delta = (headingDeg - lastPostedHeading) % 360f
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        // Tighter than the 0.025 rad (~1.4°) used for roll/pitch above: that
        // number is raw-accelerometer noise, and any phone that reports a
        // rotation vector at all is fusing it from a real gyro/magnetometer and
        // is far quieter at rest.
        return kotlin.math.abs(delta) >= 0.5f
    }

    private val motionListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                // Only updates the reading. The accelerometer stream below is
                // continuous, so it carries whatever heading is current on its
                // next post rather than opening a second bridge path.
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                headingDeg = (azimuth + 360f) % 360f
                return
            }
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
            // Heading is part of the test, not just the payload: panning a level
            // phone is almost pure heading, so on a roll/pitch-only gate every
            // sample of the exact gesture compass facing exists for was dropped.
            if (!lastPostedRoll.isNaN()
                && kotlin.math.abs(roll - lastPostedRoll) < 0.025f
                && kotlin.math.abs(pitch - lastPostedPitch) < 0.025f
                && !headingMovedSincePost()
            ) return
            lastMotionPostUptimeMs = now
            lastPostedRoll = roll
            lastPostedPitch = pitch
            lastPostedHeading = headingDeg
            if (!::homeWebView.isInitialized) return
            // Omitted entirely when there is no heading, so the web keeps its
            // parallax-only behaviour instead of reading a bogus one.
            val headingField = if (headingDeg.isNaN()) "" else ",heading:$headingDeg"
            homeWebView.evaluateJavascript(
                "window.dispatchEvent(new CustomEvent('poplur-native-motion'," +
                    "{detail:{roll:$roll,pitch:$pitch$headingField}}));true;",
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
        // Absent on hardware without a gyro/magnetometer (the A03), in which
        // case heading stays NaN and nothing downstream changes.
        manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.let { rotationVector ->
            // Heading changes slowly compared to a tilt and is rate-limited by
            // the same 30Hz post cap, so the cheaper UI delay is plenty.
            manager.registerListener(motionListener, rotationVector, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun stopCubeMotion() {
        sensorManager?.unregisterListener(motionListener)
        hasGravity = false
        lastPostedRoll = Float.NaN
        lastPostedPitch = Float.NaN
        headingDeg = Float.NaN
        lastPostedHeading = Float.NaN
    }

    override fun onResume() {
        super.onResume()
        activityResumed = true
        // The advertiser WebView is separate from signed-in Home, so resume
        // only that isolated controller when it actually exists.
        val externalBrowser = inAppBrowserDialog
        if (externalBrowser != null) externalBrowser.hostOnResume()
        else if (!restoreHomeAfterInAppBrowserIfVisible()) startCubeMotion()
        // A push banner on top of the screen the user is already reading is
        // redundant: the web list already has the row.
        PoplurNotifications.appInForeground = true
        // While Home is on screen a push is drawn by the web layer as its own
        // banner (tappable, one at a time), never by the system over the app.
        PoplurNotifications.foregroundBannerSink = { row ->
            if (homeReady.value && ::homeWebView.isInitialized) {
                pushWebMessage(JSONObject().put("type", "poplurNotificationBanner").put("notification", row))
                true
            } else {
                false
            }
        }
        // Cube sensors are resumed above only when Home, rather than the
        // isolated external browser, is the visible heavy surface.
    }

    override fun onStart() {
        super.onStart()
        activityStarted = true
        com.apptists.poplur2025.util.PoplurEQSync.noteActivityStarted()
        // Process-global and idempotent; undoes a pauseTimers() from a full
        // app-hide. WebView.onPause() alone never stops JS timers.
        if (::homeWebView.isInitialized) homeWebView.resumeTimers()
        if (::performanceController.isInitialized && !homeParkedForInAppBrowser) {
            performanceController.setVisible(true)
        }
    }

    override fun onStop() {
        authoredPostsBridge.stopAll()
        activityStarted = false
        if (::performanceController.isInitialized) performanceController.setVisible(false)
        if (com.apptists.poplur2025.util.PoplurEQSync.noteActivityStopped() &&
            ::homeWebView.isInitialized
        ) {
            // The whole app left the screen: stop the bundle's setInterval/
            // setTimeout wheels too, not just drawing. Chromium's own
            // background throttling in a WebView is weaker than in Chrome.
            homeWebView.pauseTimers()
        }
        super.onStop()
    }

    override fun onPause() {
        activityResumed = false
        PoplurNotifications.appInForeground = false
        PoplurNotifications.foregroundBannerSink = null
        stopCubeMotion()
        // Park media/JS owned by the external destination without touching
        // the signed-in Home WebView's lifecycle.
        inAppBrowserDialog?.hostOnPause()
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
        if (receivesNativeEvents) installBackgroundAudioBootstrap(this)
        if (receivesNativeEvents) {
            webChromeClient = object : WebChromeClient() {
                // The home WebView had NO console capture, so "the feed never
                // loads" left no trail anywhere: a JS error inside the bundle
                // and a WebView that simply painted black were indistinguishable
                // from logcat. Errors and warnings only -- the bundle is chatty
                // at info level and this is a 3GB device.
                override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                    if (message.messageLevel() == ConsoleMessage.MessageLevel.ERROR ||
                        message.messageLevel() == ConsoleMessage.MessageLevel.WARNING
                    ) {
                        Log.w(
                            TAG,
                            "webConsole ${message.messageLevel()}: ${message.message()} " +
                                "(${message.sourceId()}:${message.lineNumber()})"
                        )
                    }
                    return true
                }

                override fun onPermissionRequest(request: PermissionRequest) {
                    runOnUiThread { requestWebMediaCapturePermission(request) }
                }
            }
        }
        webViewClient = object : WebViewClient() {
            // A 3GB device under memory pressure can have its WebView RENDERER
            // killed out from under the app. The view then stays mounted and
            // paints nothing -- a black screen with the native chrome and the
            // native ad SDK still running happily on top of it, which is exactly
            // what "the feed never loads but ads play over it" looks like.
            // Nothing was reporting it. Returning true keeps the app alive
            // rather than letting the system kill the whole process.
            override fun onRenderProcessGone(
                view: WebView,
                detail: RenderProcessGoneDetail
            ): Boolean {
                Log.e(
                    TAG,
                    "WEBVIEW RENDERER GONE crashed=${detail.didCrash()} " +
                        "priorityAtExit=${detail.rendererPriorityAtExit()} — the home surface is now blank"
                )
                return true
            }

            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? = interceptAssetRequest(assetLoader, request)

            override fun onPageCommitVisible(view: WebView, url: String?) {
                // First actual paint, not merely "load finished" — onPageFinished can fire
                // before anything is on screen, which would pull the native cover too early
                // and put the black gap back.
                if (url?.contains("/prelogin/splash.html") == true) {
                    runOnUiThread { splashWebPainted.value = true }
                }
            }

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
    /** Does this path exist in the APK's assets? Used to choose between a
     *  bundled public/ copy and the flattened one next to it. */
    private fun assetExists(path: String): Boolean =
        try { assets.open(path).close(); true } catch (_: Exception) { false }

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
            // The approved Loading screen asks for its models under
            // poplur-clay-map/PUBLIC/... because that is where they sit in the web
            // repo, but the sync flattens public/ away for some of them — the 2MB
            // globe lands at poplur-clay-map/poplur-globe-lite.glb. loader.load()
            // has no error callback there, so a 404 is silent: you get the approved
            // screen with no globe on it, which looks like a broken splash rather
            // than a missing file. Fall back to the flattened copy.
            relativePath.startsWith("poplur-clay-map/public/") ->
                if (assetExists(relativePath)) "/assets/$relativePath"
                else "/assets/poplur-clay-map/" + relativePath.removePrefix("poplur-clay-map/public/")
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
        val nativeUnitId = BuildConfig.ADMOB_FEED_NATIVE_UNIT_ID
        if (nativeUnitId.isBlank()) {
            // A production Native unit is intentionally separate from the
            // interstitial unit. Until it is configured, show the already
            // configured interstitial rather than submitting the wrong ad
            // format (which Google rejects) or silently dropping the slot.
            AdManager.presentInterstitial(this) { open ->
                if (!isDestroyed) setWebFeedAdSlotOpen(open)
            }
            return
        }
        AdManager.loadNativeAd(
            context = this,
            adUnitId = nativeUnitId,
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
            onFailed = { error ->
                Log.w(TAG, "Feed Google ad failed: ${error.code} ${error.message}")
            },
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

    private fun sendUploadProgress(requestId: String, fraction: Double, phase: String) {
        val payload = JSONObject().apply {
            put("requestId", requestId)
            put("fraction", fraction.coerceIn(0.0, 1.0))
            put("phase", phase)
        }
        homeWebView.post {
            homeWebView.evaluateJavascript(
                "window.dispatchEvent(new CustomEvent('poplur-upload-progress', { detail: $payload })); true;", null
            )
        }
    }

    /**
     * The studio posting a track: the bounced mix into Storage, a row into
     * tracks/ pointing at it, and the ground it was left on.
     *
     * Separate from uploadClayPost because a track is not a post — it lands in
     * a different collection under different rules and has no channel, caption
     * or feed placement. It answers on the same upload events, though, because
     * the web already waits on exactly those.
     */
    private fun uploadStudioTrack(body: JSONObject) {
        val requestId = body.optString("requestId")
        if (requestId.isBlank()) return
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            sendUploadResult(requestId, false, "Posting music needs you signed in to Poplur.")
            return
        }
        val title = body.optString("title").trim()
        if (title.isBlank()) {
            sendUploadResult(requestId, false, "Give the track a name first.")
            return
        }
        val data = body.optString("data")
        val encoded = if (data.startsWith("data:")) data.substringAfter(',', "") else ""
        if (encoded.isBlank()) {
            sendUploadResult(requestId, false, "That mix never reached the app.")
            return
        }
        val audio = runCatching {
            android.util.Base64.decode(encoded, android.util.Base64.DEFAULT)
        }.getOrNull()
        if (audio == null || audio.isEmpty()) {
            sendUploadResult(requestId, false, "That mix could not be decoded for upload.")
            return
        }
        // storage.rules refuses anything past 25MB. Saying so here beats
        // running the whole upload and being rejected at the end.
        if (audio.size >= 25 * 1024 * 1024) {
            sendUploadResult(requestId, false, "That mix is too long to post — keep it under 25MB.")
            return
        }
        if (!body.has("latitude") || !body.has("longitude")) {
            sendUploadResult(requestId, false, "Poplur could not tell where you are, and a track has to be left somewhere.")
            return
        }
        val lat = body.optDouble("latitude")
        val lng = body.optDouble("longitude")
        if (lat.isNaN() || lng.isNaN()) {
            sendUploadResult(requestId, false, "Poplur could not tell where you are, and a track has to be left somewhere.")
            return
        }

        val trackId = java.util.UUID.randomUUID().toString()
        val objectRef = FirebaseStorage.getInstance().reference.child("music/${user.uid}/$trackId.wav")
        // The bucket checks contentType on the way in and the player trusts it
        // on the way out, so it is set rather than guessed from the path.
        val meta = com.google.firebase.storage.StorageMetadata.Builder()
            .setContentType("audio/wav")
            .build()

        sendUploadProgress(requestId, 0.02, "PREPARING")
        objectRef.putBytes(audio, meta)
            .addOnProgressListener { snapshot ->
                if (snapshot.totalByteCount > 0) {
                    // Capped below 1.0 deliberately: the Firestore row still has
                    // to land, and a full bar with no row is a lie about whether
                    // the track exists.
                    val fraction = snapshot.bytesTransferred.toDouble() / snapshot.totalByteCount.toDouble()
                    sendUploadProgress(requestId, fraction.coerceIn(0.02, 0.95), "UPLOADING")
                }
            }
            .addOnFailureListener { error ->
                sendUploadResult(requestId, false, "The upload did not finish: ${error.localizedMessage}")
            }
            .addOnSuccessListener {
                sendUploadProgress(requestId, 1.0, "SAVING")
                objectRef.downloadUrl
                    .addOnFailureListener { error ->
                        sendUploadResult(requestId, false, "The track uploaded but Poplur could not get a link to it: ${error.localizedMessage}")
                    }
                    .addOnSuccessListener { url ->
                        val row = hashMapOf<String, Any>(
                            "userID" to user.uid,
                            "title" to title,
                            "audioURL" to url.toString(),
                            "lat" to lat,
                            "lng" to lng,
                            // The rules require the SERVER clock: "newest" must
                            // not be something a client can buy with a lie.
                            "createdAt" to FieldValue.serverTimestamp(),
                            "createdAtMillis" to System.currentTimeMillis(),
                            "licence" to (body.optString("licence").ifBlank { "own-work" }),
                            "artist" to (user.displayName ?: "Poplur artist"),
                            "postedBy" to user.uid
                        )
                        body.optString("genre").takeIf { it.isNotBlank() }
                            ?.let { row["mood"] = it.uppercase() }
                        body.optDouble("bpm").takeIf { !it.isNaN() }?.let { row["bpm"] = it }
                        body.optDouble("duration").takeIf { !it.isNaN() }?.let { row["duration"] = it }
                        body.optDouble("radiusKm").takeIf { !it.isNaN() }?.let { row["radiusKm"] = it }

                        FirebaseFirestore.getInstance().collection("tracks").document(trackId)
                            .set(row)
                            .addOnSuccessListener { sendUploadResult(requestId, true) }
                            .addOnFailureListener { error ->
                                // Audio in the bucket with nothing pointing at
                                // it is an orphan nobody can reach and nobody is
                                // accounting for. Take it back out.
                                objectRef.delete()
                                sendUploadResult(requestId, false, "The track could not be posted: ${error.localizedMessage}")
                            }
                    }
            }
    }

    private fun uploadClayPost(body: JSONObject) {
        val requestId = body.optString("requestId")
        if (requestId.isBlank()) return
        val postId = boundedBridgeId(body.opt("postId"))
        if (postId == null || !postId.startsWith("post-") || postId.length < 21) {
            sendUploadResult(requestId, false, "That post identity was invalid.")
            return
        }
        if (!body.optBoolean("rightsAttested", false)) {
            sendUploadResult(requestId, false, "Confirm that you have permission to share this media and music.")
            return
        }
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

        val mediaStoragePath = "posts/${user.uid}/$postId.${if (kind == "video") "mp4" else "jpg"}"
        val storageRef = FirebaseStorage.getInstance().reference.child(mediaStoragePath)
        val captureKey = Uri.parse(data).lastPathSegment
        val storedCapture = captureKey?.let { key ->
            synchronized(webCapturesLock) { webCaptures[key] }
        }
        val mediaMetadata = StorageMetadata.Builder()
            .setContentType(if (kind == "video") "video/mp4" else "image/jpeg")
            .setCustomMetadata("ownerUID", user.uid)
            .build()
        val upload = when {
            storedCapture != null -> storageRef.putFile(storedCapture.uri, mediaMetadata)
            data.startsWith("data:") -> {
                val encoded = data.substringAfter(',', "")
                if (encoded.isBlank()) {
                    sendUploadResult(requestId, false, "That media could not be prepared.")
                    return
                }
                storageRef.putBytes(
                    android.util.Base64.decode(encoded, android.util.Base64.DEFAULT),
                    mediaMetadata,
                )
            }
            else -> {
                sendUploadResult(requestId, false, "That media is no longer available on this device.")
                return
            }
        }

        sendUploadProgress(requestId, 0.04, "PREPARING")
        upload.addOnProgressListener { snapshot ->
            if (snapshot.totalByteCount > 0) {
                val transferred = snapshot.bytesTransferred.toDouble() / snapshot.totalByteCount.toDouble()
                sendUploadProgress(requestId, 0.08 + transferred.coerceIn(0.0, 1.0) * 0.72, "UPLOADING")
            }
        }.addOnFailureListener { error ->
            sendUploadResult(requestId, false, error.localizedMessage ?: "Your post could not be uploaded.")
        }.addOnSuccessListener {
            storageRef.downloadUrl.addOnFailureListener { error ->
                storageRef.delete()
                sendUploadResult(requestId, false, error.localizedMessage ?: "Your uploaded media could not be read.")
            }.addOnSuccessListener { mediaUrl ->
                val posterBytes = if (kind == "video" && storedCapture != null) {
                    videoPosterJpeg(storedCapture.uri)
                } else null
                if (posterBytes == null) {
                    saveClayPost(
                        body = body,
                        requestId = requestId,
                        postId = postId,
                        mediaUrl = mediaUrl.toString(),
                        mediaStoragePath = mediaStoragePath,
                        mediaReference = storageRef,
                        posterUrl = null,
                        posterStoragePath = null,
                        posterReference = null,
                        kind = kind,
                    )
                    return@addOnSuccessListener
                }

                val posterStoragePath = "posts/${user.uid}/$postId-poster.jpg"
                val posterRef = FirebaseStorage.getInstance().reference.child(posterStoragePath)
                val posterMetadata = StorageMetadata.Builder().setContentType("image/jpeg").build()
                sendUploadProgress(requestId, 0.84, "MAKING PREVIEW")
                posterRef.putBytes(posterBytes, posterMetadata)
                    .addOnFailureListener {
                        // A poster improves startup, but failure to make one
                        // must not throw away an otherwise valid recording.
                        saveClayPost(
                            body, requestId, postId, mediaUrl.toString(), mediaStoragePath,
                            storageRef, null, null, null, kind,
                        )
                    }
                    .addOnSuccessListener {
                        posterRef.downloadUrl
                            .addOnFailureListener {
                                posterRef.delete()
                                saveClayPost(
                                    body, requestId, postId, mediaUrl.toString(), mediaStoragePath,
                                    storageRef, null, null, null, kind,
                                )
                            }
                            .addOnSuccessListener { posterUrl ->
                                saveClayPost(
                                    body, requestId, postId, mediaUrl.toString(), mediaStoragePath,
                                    storageRef, posterUrl.toString(), posterStoragePath, posterRef, kind,
                                )
                            }
                    }
            }
        }
    }

    private suspend fun uploadDMAttachment(body: JSONObject) {
        val user = FirebaseAuth.getInstance().currentUser
        val otherId = body.optString("otherId")
        val threadId = body.optString("threadId")
        val clientMessageId = body.optString("clientMessageId")
        val kind = body.optString("kind")
        val source = body.optString("source")
        val label = when (kind) { "image" -> "Photo"; "video" -> "Video"; else -> "Voice message" }
        suspend fun finish(ok: Boolean) {
            pushWebMessage(
                JSONObject()
                    .put("type", if (ok) "poplurDMSent" else "poplurDMSendFailed")
                    .put("clientMessageId", clientMessageId)
                    .apply { if (!ok) put("text", label) }
            )
        }
        if (user == null || otherId.isBlank() || otherId == user.uid
            || threadId != PoplurDirectMessages.threadId(user.uid, otherId)
            || clientMessageId.length !in 20..100
            || kind !in setOf("image", "video", "audio")
        ) {
            finish(false)
            return
        }

        val capture = Uri.parse(source).lastPathSegment?.let { key ->
            synchronized(webCapturesLock) { webCaptures[key] }
        }
        val inlineHeader = source.substringBefore(',', "")
        val inlineBytes = if (source.startsWith("data:") && inlineHeader.endsWith(";base64")) {
            runCatching { Base64.decode(source.substringAfter(','), Base64.DEFAULT) }.getOrNull()
        } else null
        val mimeType = capture?.mimeType
            ?: inlineHeader.removePrefix("data:").removeSuffix(";base64")
        if (!mimeType.lowercase().startsWith("$kind/")) {
            finish(false)
            return
        }
        val limit = when (kind) {
            "image" -> 12L * 1024L * 1024L
            "audio" -> 8L * 1024L * 1024L
            else -> 40L * 1024L * 1024L
        }
        val size = capture?.let {
            runCatching { contentResolver.openAssetFileDescriptor(it.uri, "r")?.use { fd -> fd.length } }
                .getOrNull()
        } ?: inlineBytes?.size?.toLong()
        if (size == null || size <= 0 || size >= limit || (capture == null && inlineBytes == null)) {
            finish(false)
            return
        }

        val extension = when {
            mimeType.contains("png") -> "png"
            mimeType.contains("webm") -> "webm"
            mimeType.contains("quicktime") -> "mov"
            kind == "audio" -> "m4a"
            kind == "video" -> "mp4"
            else -> "jpg"
        }
        val reference = FirebaseStorage.getInstance().reference
            .child("dmMedia/$threadId/${user.uid}/$clientMessageId.$extension")
        val metadata = StorageMetadata.Builder().setContentType(mimeType.lowercase()).build()
        try {
            if (capture != null) reference.putFile(capture.uri, metadata).await()
            else reference.putBytes(inlineBytes!!, metadata).await()
            val mediaUrl = reference.downloadUrl.await().toString()
            val ok = PoplurDirectMessages.sendAttachment(
                otherUid = otherId,
                requestedThreadId = threadId,
                mediaUrl = mediaUrl,
                mimeType = mimeType,
                kind = kind,
                durationMillis = body.optLong("durationMillis").takeIf { body.has("durationMillis") },
                clientMessageId = clientMessageId
            )
            if (!ok) runCatching { reference.delete().await() }
            finish(ok)
        } catch (error: Exception) {
            Log.w(TAG, "DM attachment upload failed", error)
            runCatching { reference.delete().await() }
            finish(false)
        }
    }

    private fun saveClayPost(
        body: JSONObject,
        requestId: String,
        postId: String,
        mediaUrl: String,
        mediaStoragePath: String,
        mediaReference: StorageReference,
        posterUrl: String?,
        posterStoragePath: String?,
        posterReference: StorageReference?,
        kind: String,
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            mediaReference.delete()
            posterReference?.delete()
            sendUploadResult(requestId, false, "Your sign-in expired before the post was saved.")
            return
        }
        sendUploadProgress(requestId, 0.94, "SAVING POST")
        val targetLatitude = body.optDouble("targetLatitude", Double.NaN)
        val targetLongitude = body.optDouble("targetLongitude", Double.NaN)
        val hasTarget = targetLatitude.isFinite() && targetLongitude.isFinite()
            && kotlin.math.abs(targetLatitude) <= 90
            && kotlin.math.abs(targetLongitude) <= 180
        val database = FirebaseFirestore.getInstance()
        val userReference = database.collection("users").document(user.uid)

        // Every drop is born discoverable at the seed, earned or not. This read
        // the seed as optDouble's DEFAULT, which covered a request that carried
        // no radiusKm at all but not one carrying a smaller number — a shell on
        // an older web bundle can still send 2 km, and a post below the seed is
        // intercepted by fewer people than the account is entitled to reach.
        // Computed once because the two fields it feeds must agree: the create
        // rule refuses a seed larger than the radius it is a seed for.
        val requestedRadiusMeters = (body.optDouble("radiusKm", 0.0) * 1000).toInt()
        fun commitPost(accountHidesOrigin: Boolean, allowedRadiusMeters: Int) {
            val seedRadiusMeters = maxOf(POST_DISCOVERY_SEED_METERS, requestedRadiusMeters)
                .coerceAtMost(allowedRadiusMeters)
            val locationVisibility = if (
                hasTarget
                && !accountHidesOrigin
                && body.optString("locationVisibility") != "hidden"
            ) "approximate" else "hidden"
            val post = hashMapOf<String, Any>(
                "userID" to user.uid,
                "username" to (user.displayName ?: user.email ?: "Poplur user"),
                "mediaURL" to mediaUrl,
                "playbackURL" to mediaUrl,
                "mediaStoragePath" to mediaStoragePath,
                "mediaType" to kind,
                "viewCount" to 0,
                "radius" to seedRadiusMeters,
                // The creation radius is all seed (spent reach, never earned):
                // declared, with the earned pool at zero, exactly as iOS and
                // React Native declare it, so the engagement functions never
                // read a claim as radius earned from nowhere.
                "seedRadiusMeters" to seedRadiusMeters,
                "organicRadiusMeters" to 0,
                "audience" to "public",
                "locationVisibility" to locationVisibility,
                "processingStatus" to "ready",
                "status" to "published",
                "schemaVersion" to 2,
                "createdAt" to FieldValue.serverTimestamp(),
                "createdAtMillis" to System.currentTimeMillis(),
            )
            if (locationVisibility == "approximate") {
                val latitudeCell = (targetLatitude * 1_000).roundToInt()
                val longitudeCell = (targetLongitude * 1_000).roundToInt()
                post["latitude"] = latitudeCell / 1_000.0
                post["longitude"] = longitudeCell / 1_000.0
                post["latitudeCell"] = latitudeCell
                post["longitudeCell"] = longitudeCell
            }
            val photoSeconds = body.optDouble("photoViewSeconds", Double.NaN)
            if (kind == "photo" && photoSeconds.isFinite() && photoSeconds > 0) {
                post["photoViewSeconds"] = kotlin.math.round(photoSeconds).toInt().coerceIn(1, 60)
            }
            posterUrl?.let { post["posterURL"] = it }
            posterStoragePath?.let { post["posterStoragePath"] = it }
            body.optString("caption").takeIf { it.isNotBlank() }?.let { post["caption"] = it }
            body.optString("channelId").takeIf { it.isNotBlank() }?.let { post["channelId"] = it }
            body.optString("filterId").takeIf { it.isNotBlank() }?.let { post["filterId"] = it }
            body.optString("rightsAttestationVersion").takeIf { it.isNotBlank() }?.let {
                post["rightsAttestationVersion"] = it
                post["rightsAttestedAtMillis"] = System.currentTimeMillis()
            }
            if (body.optBoolean("usedAI")) post["usedAI"] = true

            val batch = database.batch()
            batch.set(database.collection("posts").document(postId), post)
            if (hasTarget) {
                batch.set(
                    userReference.collection("postLocations").document(postId),
                    mapOf(
                        "latitude" to targetLatitude,
                        "longitude" to targetLongitude,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "source" to "post-upload",
                    ),
                    SetOptions.merge(),
                )
            }
            batch.commit()
                .addOnSuccessListener {
                    sendUploadProgress(requestId, 1.0, "POSTED")
                    sendUploadResult(requestId, true)
                }
                .addOnFailureListener { error ->
                    // Do not leave paid Storage occupied by a post nobody can
                    // discover or delete from the product.
                    mediaReference.delete()
                    posterReference?.delete()
                    sendUploadResult(requestId, false, error.localizedMessage ?: "Your post could not be saved.")
                }
        }

        userReference.get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { account ->
                commitPost(
                    account.getBoolean("privateAccount") == true
                        || account.getBoolean("ghostMode") == true,
                    (account.getLong("totalPostRadiusMeters")?.coerceAtLeast(5_000)
                        ?: ((account.getLong("claimRadiusMeters") ?: 0).coerceAtLeast(0) + 5_000))
                        .coerceAtMost(6_371_000).toInt(),
                )
            }
            .addOnFailureListener { error ->
                mediaReference.delete()
                posterReference?.delete()
                sendUploadResult(
                    requestId,
                    false,
                    error.localizedMessage ?: "Privacy could not be confirmed before posting.",
                )
            }
    }

    private fun videoPosterJpeg(uri: Uri): ByteArray? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(this, uri)
            val bitmap = retriever.getFrameAtTime(100_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: return null
            val output = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
            bitmap.recycle()
            output.toByteArray()
        } catch (error: RuntimeException) {
            Log.w(TAG, "Could not create video poster", error)
            null
        } finally {
            try { retriever.release() } catch (_: RuntimeException) { }
        }
    }

    private fun revealIfReady() {
        if (!minimumElapsed.value || !homeReady.value || isFinishing) return
        // Home can become ready before the splash WebView commits its first
        // frame. In that race `onPageCommitVisible` never retires the native
        // first-frame cover, because the loading WebView is dismantled below.
        // Home readiness is now the owner of the handoff, so remove that cheap
        // Compose cover synchronously as reveal begins.
        splashWebPainted.value = true
        window.statusBarColor = darkHome
        window.navigationBarColor = darkHome
        loadingWebView.animate()
            .alpha(0f)
            .setDuration(REVEAL_DURATION_MS)
            .withEndAction {
                // DESTROY the loading WebView, don't just hide it: even though
                // this splash is video/CSS rather than Three/WebGL, its decoder,
                // compositor surface, and WebView are real memory on a 3GB
                // device. Free them as soon as Home owns the screen.
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

    private fun reconnectNotificationsForWeb() {
        stopNotificationsForWeb(clearIdentity = false)
        val generation = notificationGeneration.replace()
        val auth = FirebaseAuth.getInstance()
        val authListener = FirebaseAuth.AuthStateListener { state ->
            if (!notificationGeneration.isCurrent(generation)) return@AuthStateListener
            bindNotificationsForWeb(state.currentUser?.uid, generation)
        }
        notificationAuthListener = authListener
        auth.addAuthStateListener(authListener)
    }

    private fun bindNotificationsForWeb(uid: String?, generation: Long) {
        if (!notificationGeneration.isCurrent(generation)) return
        notificationRegistration?.remove()
        notificationRegistration = null
        if (notificationObservedUid != uid) {
            notificationDocumentIds = emptyMap()
            deliverNotificationsToWeb(JSONArray())
        }
        notificationObservedUid = uid
        if (uid == null) return

        notificationRegistration = FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(80)
            .addSnapshotListener { snapshot, error ->
                if (!notificationGeneration.isCurrent(generation)
                    || notificationObservedUid != uid
                ) return@addSnapshotListener
                if (error != null) {
                    Log.w(TAG, "notification inbox listener failed", error)
                    return@addSnapshotListener
                }
                val rows = JSONArray()
                val documentIds = mutableMapOf<String, String>()
                snapshot?.documents.orEmpty().forEach { document ->
                    val id = document.getString("id")?.trim()?.takeIf { it.isNotEmpty() }
                        ?: document.id
                    documentIds[id] = document.id
                    if (document.getBoolean("isDismissed") == true) return@forEach
                    val row = JSONObject()
                        .put("id", id)
                        .put("kind", document.getString("kind") ?: "engagement")
                        .put("title", document.getString("title") ?: "Poplur")
                        .put("detail", document.getString("detail") ?: "New activity on Poplur")
                        .put("createdAtMillis", document.getTimestamp("createdAt")?.toDate()?.time
                            ?: System.currentTimeMillis())
                        .put("unread", document.getBoolean("isUnread") != false)
                    document.getString("actorHandle")?.let { row.put("actorHandle", it) }
                    document.getString("postId")?.let { row.put("postId", it) }
                    document.getString("threadId")?.let { row.put("threadId", it) }
                    rows.put(row)
                }
                notificationDocumentIds = documentIds
                deliverNotificationsToWeb(rows)
            }
    }

    private fun stopNotificationsForWeb(clearIdentity: Boolean = true) {
        notificationGeneration.replace()
        notificationRegistration?.remove()
        notificationRegistration = null
        notificationAuthListener?.let { FirebaseAuth.getInstance().removeAuthStateListener(it) }
        notificationAuthListener = null
        if (clearIdentity) {
            notificationObservedUid = null
            notificationDocumentIds = emptyMap()
        }
    }

    private fun deliverNotificationsToWeb(rows: JSONArray) {
        val payload = JSONObject()
            .put("type", "poplurNotifications")
            .put("notifications", rows)
            .toString()
        homeWebView.post {
            homeWebView.evaluateJavascript("window.postMessage($payload,'*');true;", null)
        }
    }

    private fun updateNotificationInbox(
        ids: List<String>? = null,
        markRead: Boolean = false,
        dismiss: Boolean = false,
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (notificationObservedUid != uid) return
        val inbox = FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("notifications")
        val selected = (ids ?: notificationDocumentIds.keys.toList())
            .filter { it.isNotBlank() }
            .distinct()
        if (selected.isEmpty()) return
        val batch = FirebaseFirestore.getInstance().batch()
        var hasWrites = false
        selected.forEach { id ->
            val documentId = notificationDocumentIds[id] ?: return@forEach
            val fields = mutableMapOf<String, Any>()
            if (markRead) {
                fields["isUnread"] = false
                fields["readAt"] = FieldValue.serverTimestamp()
            }
            if (dismiss) {
                fields["isDismissed"] = true
                fields["dismissedAt"] = FieldValue.serverTimestamp()
            }
            if (fields.isNotEmpty()) {
                batch.update(inbox.document(documentId), fields)
                hasWrites = true
            }
        }
        if (!hasWrites) return
        batch.commit()
            .addOnFailureListener { error -> Log.w(TAG, "notification inbox update failed", error) }
    }

    inner class Bridge {
        @JavascriptInterface
        fun onEvent(json: String) {
            val body = runCatching { JSONObject(json) }.getOrNull() ?: return
            val type = body.optString("type")
            if (type == "homeReady" || type == "mapReady") {
                runOnUiThread {
                    homeReady.value = true
                    deliverPendingDMDeepLink()
                    deliverPendingPostFocus()
                    revealIfReady()
                }
            }
            if (type == "phoneBootEffects") {
                runOnUiThread { playPhoneBootSound() }
            }
            if (type == "phoneThemeEffects") {
                // The web surface owns the short Pop and blue LCD state. Stop a
                // still-playing entry cue so it cannot mask that interaction.
                runOnUiThread {
                    phoneBootPlayer?.stop()
                    phoneBootPlayer?.release()
                    phoneBootPlayer = null
                }
            }
            // The notifications screen lives in the web bundle, which forwards
            // each row it logs as {type:"notification", id, title, detail, kind}.
            // This process used to drop that message on the floor, so an Android
            // install got no push for a DM, a mention or an engagement — the web
            // list and the lock screen simply disagreed. Routed through the same
            // three tiers iOS uses.
            if (type == "notification") {
                val id = body.optString("id")
                val title = body.optString("title")
                if (id.isNotEmpty() && title.isNotEmpty()) {
                    val detail = body.optString("detail")
                    val kind = body.optString("kind").takeIf { it.isNotEmpty() }
                    runOnUiThread {
                        PoplurNotifications.scheduleFromWeb(
                            this@PostLoginWebActivity, id, title, detail, kind
                        )
                    }
                }
            }
            if (type == "notificationsRead") {
                // Reading the in-app list must clear what the launcher shows.
                // Android has no per-app badge API: the badge is a side effect
                // of active notifications (setShowBadge on the poplur_alerts
                // channel), so cancelling them IS clearing the badge. Leaving
                // them posted keeps a count on the icon for notifications the
                // user has already read inside the app.
                val rows = body.optJSONArray("ids")
                val ids = rows?.let {
                    buildList {
                        for (index in 0 until it.length()) {
                            it.optString(index).takeIf { id -> id.isNotBlank() }?.let(::add)
                        }
                    }
                }
                runOnUiThread {
                    NotificationManagerCompat.from(this@PostLoginWebActivity).cancelAll()
                    updateNotificationInbox(ids = ids, markRead = true)
                }
            }
            if (type == "notificationsRequested") {
                runOnUiThread { reconnectNotificationsForWeb() }
            }
            if (type == "notificationsStopped") {
                runOnUiThread { stopNotificationsForWeb() }
            }
            if (type == "notificationDismissed") {
                val id = body.optString("id")
                if (id.isNotBlank()) {
                    runOnUiThread { updateNotificationInbox(ids = listOf(id), dismiss = true) }
                }
            }
            if (type == "notificationsCleared") {
                val rows = body.optJSONArray("ids")
                val ids = buildList {
                    if (rows != null) for (index in 0 until rows.length()) {
                        rows.optString(index).takeIf { it.isNotBlank() }?.let(::add)
                    }
                }
                runOnUiThread { updateNotificationInbox(ids = ids, dismiss = true) }
            }
            // The bundled WebView cannot use Firebase directly. Hand it a
            // short-lived ID token on demand for authenticated Poplur tools;
            // it stays in JS memory and is never persisted.
            if (type == "requestAuthToken") {
                FirebaseAuth.getInstance().currentUser?.getIdToken(false)
                    ?.addOnSuccessListener { result ->
                        val token = result.token ?: return@addOnSuccessListener
                        val payload = JSONObject().put("token", token).put("expiresIn", 3600).toString()
                        val quotedPayload = JSONObject.quote(payload)
                        homeWebView.post {
                            homeWebView.evaluateJavascript(
                                "window.dispatchEvent(new CustomEvent('poplur-native-auth-token', { detail: JSON.parse($quotedPayload) })); true;",
                                null
                            )
                        }
                    }
            }
            if (type == "requestProfile") {
                deliverWebProfile()
            }
            if (type == "mapDiscoveryQuery") {
                val requestId = body.optString("requestId").take(100)
                val query = body.optJSONObject("query")
                val bounds = query?.optJSONObject("bounds")
                if (requestId.isNotEmpty() && bounds != null) {
                    mapDiscoveryRequestId = requestId
                    val input = mutableMapOf<String, Any>(
                        "bounds" to mapOf("west" to bounds.optDouble("west"), "east" to bounds.optDouble("east"),
                            "south" to bounds.optDouble("south"), "north" to bounds.optDouble("north")),
                        "zoom" to query.optDouble("zoom"), "includeTest" to query.optBoolean("includeTest", false),
                    )
                    if (!query.isNull("cursor")) input["cursor"] = query.optString("cursor").take(1500)
                    if (!query.isNull("selectedPostId")) input["selectedPostId"] = query.optString("selectedPostId").take(180)
                    FirebaseFunctions.getInstance("us-central1").getHttpsCallable("queryMapDiscovery").call(input)
                        .addOnSuccessListener { result ->
                            if (mapDiscoveryRequestId == requestId) pushMapDiscovery(JSONObject()
                                .put("requestId", requestId).put("result", JSONObject.wrap(result.getData())))
                        }
                        .addOnFailureListener {
                            if (mapDiscoveryRequestId == requestId) pushMapDiscovery(JSONObject()
                                .put("requestId", requestId).put("error", "Could not load this area."))
                        }
                }
            }
            if (type == "requestAccountPrivacy") {
                deliverAccountPrivacy()
            }
            if (type == "accountPrivacyChanged") {
                changeAccountPrivacy(
                    privateAccount = body.optBoolean("privateAccount", false),
                    ghostMode = body.optBoolean("ghostMode", false),
                )
            }
            if (type == "saveProfilePhoto") {
                val payload = body.optJSONObject("detail") ?: body
                persistWebProfilePhoto(payload.optString("photo"))
            }
            // The username had NO writer on the live path. The only one was the
            // native edit-profile screen, which the web home orphaned, so every
            // handle was per-device: invisible to other devices and to the
            // server that mints wallet passes.
            if (type == "saveProfileUsername") {
                val payload = body.optJSONObject("detail") ?: body
                val raw = sanitizeUsername(payload.optString("username")) ?: ""
                // Bounded and character-checked rather than trusted: this lands
                // in a document other people read, and it is what a pass, a chat
                // thread and the phone's address book will all render.
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (raw.isNotEmpty() && raw.length <= 30 &&
                    raw.all { it.isLetterOrDigit() || it == '_' || it == '.' } &&
                    uid != null
                ) {
                    FirebaseFirestore.getInstance()
                        .collection("users").document(uid)
                        .set(mapOf("username" to raw), SetOptions.merge())
                        .addOnSuccessListener { Log.d(TAG, "[PoplurProfile] username saved: $raw") }
                        .addOnFailureListener { Log.w(TAG, "[PoplurProfile] username save FAILED", it) }
                }
            }
            if (type == "requestProfilePosts") {
                deliverWebProfilePosts()
            }
            if (type == "watchAuthoredPosts" || type == "stopAuthoredPosts") {
                runOnUiThread { if (!isDestroyed) authoredPostsBridge.handle(body) }
            }
            if (type == "requestFeedPosts") {
                // Arm on the main thread, where onDestroy also runs, so the
                // isDestroyed check and the remove()/assign cannot interleave.
                runOnUiThread { deliverWebFeedPosts() }
            }
            if (type == "deleteProfilePost") {
                deleteWebProfilePost(body)
            }
            // Block / unblock: the durable record under users/{me}/blocked/{them},
            // which the Firestore rules enforce as a symmetric boundary on
            // messages and engagement. The web side already applied the local
            // consequence (their pops gone, or back) before asking. iOS writes
            // the same document; this keeps the two shells in step.
            if (type == "blockUser" || type == "unblockUser") {
                val otherUid = body.optString("userId").trim()
                val handle = body.optString("username").take(48)
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (otherUid.isNotEmpty() && otherUid.length <= 128 && uid != null && uid != otherUid) {
                    val record = FirebaseFirestore.getInstance()
                        .collection("users").document(uid)
                        .collection("blocked").document(otherUid)
                    if (type == "blockUser") {
                        record.set(mapOf("blockedAt" to FieldValue.serverTimestamp(), "username" to handle))
                            .addOnFailureListener { Log.w(TAG, "[PoplurSafety] block write FAILED", it) }
                    } else {
                        record.delete()
                            .addOnFailureListener { Log.w(TAG, "[PoplurSafety] unblock delete FAILED", it) }
                    }
                }
            }
            if (type == "requestProfileTracks") {
                deliverWebProfileTracks()
            }
            if (type == "requestNearbyTracks") {
                deliverWebNearbyTracks(body)
            }
            if (type == "publishTrack") {
                uploadStudioTrack(body)
            }
            // WebRTC call signalling. The shared clay-map bundle owns media
            // capture and rendering; Android owns Firestore and returns only
            // rendezvous/SDP/ICE messages. Keep this separate from the
            // Agora CallSessionManager so an ordinary phone call cannot become
            // a FaceTime call (or vice versa).
            if (type == "callStart") {
                val otherUid = body.optString("otherId")
                val callerName = body.optString("callerName").takeIf { it.isNotBlank() }
                val video = body.optBoolean("video", false)
                val context = body.optString("context").takeIf { it == "house" }
                val requestId = body.optString("requestId")
                com.apptists.poplur2025.call.PoplurWebCallSignalling.startCall(
                    otherUid = otherUid,
                    callerName = callerName,
                    video = video,
                    context = context,
                ) { callId ->
                    pushWebMessage(
                        JSONObject()
                            .put("type", "poplurCallStarted")
                            .put("callId", callId ?: "")
                            .put("ok", callId != null)
                            .put("requestId", requestId)
                    )
                }
                return
            }
            if (type == "callDescription") {
                com.apptists.poplur2025.call.PoplurWebCallSignalling.publishDescription(
                    callId = body.optString("callId"),
                    type = body.optString("sdpType", "offer"),
                    sdp = body.optString("sdp"),
                )
                return
            }
            if (type == "callCandidate") {
                com.apptists.poplur2025.call.PoplurWebCallSignalling.publishCandidate(
                    callId = body.optString("callId"),
                    candidate = webCallCandidate(body.optJSONObject("candidate")),
                )
                return
            }
            if (type == "callEnd") {
                com.apptists.poplur2025.call.PoplurWebCallSignalling.endCall(body.optString("callId"))
                return
            }
            if (type == "callDecline") {
                com.apptists.poplur2025.call.PoplurWebCallSignalling.declineCall(body.optString("callId"))
                return
            }
            if (type == "callAccept") {
                com.apptists.poplur2025.call.PoplurWebCallSignalling.acceptCall(body.optString("callId"))
                return
            }
            if (type == "callWatchIncoming") {
                com.apptists.poplur2025.call.PoplurWebCallSignalling.onEvent = ::pushWebMessage
                com.apptists.poplur2025.call.PoplurWebCallSignalling.observeIncoming()
                return
            }
            // The Poplur phone's address book: favourites, people you have a
            // chat with, and people your radius has actually crossed. Mirrors
            // the iOS bridge exactly, including the window.postMessage wire
            // format, because the same web bundle reads both.
            if (type == "requestPhoneContacts") {
                // The radius and the position ride on the request: the ledger is
                // the web's, and a second copy here would drift.
                val radiusKm = if (body.has("radiusKm")) body.optDouble("radiusKm") else null
                val latitude = if (body.has("latitude")) body.optDouble("latitude") else null
                val longitude = if (body.has("longitude")) body.optDouble("longitude") else null
                val requestId = body.opt("requestId") as? String
                lifecycleScope.launch {
                    val contacts = PoplurPhoneContacts.contacts(
                        radiusKm = radiusKm?.takeIf { !it.isNaN() },
                        latitude = latitude?.takeIf { !it.isNaN() },
                        longitude = longitude?.takeIf { !it.isNaN() }
                    )
                    val response = JSONObject()
                        .put("type", "poplurPhoneContacts")
                        .put("contacts", contacts)
                    requestId?.let { response.put("requestId", it) }
                    pushWebMessage(response)
                }
            }
            if (type == "togglePhoneFavourite") {
                val contactId = body.optString("contactId")
                val favourite = body.optBoolean("favourite")
                val name = body.optString("name").takeIf { it.isNotBlank() }
                lifecycleScope.launch {
                    PoplurPhoneContacts.setFavourite(contactId, favourite, name)
                    // Re-read rather than patching the page's copy: the sort
                    // order changes when a favourite moves to the top, and one
                    // source of truth is cheaper than two that drift.
                    val contacts = PoplurPhoneContacts.contacts()
                    pushWebMessage(
                        JSONObject()
                            .put("type", "poplurPhoneContacts")
                            .put("contacts", contacts)
                    )
                }
            }
            // Direct messages. The chat screen is a page inside this WebView and
            // the bundle has no Firebase, so every read and write crosses here.
            // Threads and messages arrive as LIVE snapshots rather than one-shot
            // fetches: a chat that only updates when you reopen it is not a chat.
            if (type == "requestDMThreads") {
                PoplurDirectMessages.observeThreads { threads ->
                    pushWebMessage(
                        JSONObject()
                            .put("type", "poplurDMThreads")
                            .put("threads", threads)
                    )
                }
            }
            if (type == "openDMThread") {
                val threadId = body.optString("threadId")
                if (threadId.isNotEmpty() && threadId.length <= 200) {
                    PoplurDirectMessages.observeThread(
                        threadId,
                        onChange = { messages ->
                            pushWebMessage(
                                JSONObject()
                                    .put("type", "poplurDMMessages")
                                    .put("threadId", threadId)
                                    .put("messages", messages)
                            )
                        },
                        onState = { state ->
                            pushWebMessage(state.put("type", "poplurDMThreadState"))
                        }
                    )
                }
            }
            if (type == "closeDMThread") {
                PoplurDirectMessages.closeThread()
            }
            if (type == "setDMTyping") {
                val threadId = body.optString("threadId")
                val typing = body.optBoolean("typing", false)
                if (threadId.isNotEmpty() && threadId.length <= 200) {
                    lifecycleScope.launch {
                        PoplurDirectMessages.setTyping(threadId, typing)
                    }
                }
            }
            if (type == "deleteDMThread") {
                val threadId = body.optString("threadId")
                lifecycleScope.launch {
                    val ok = PoplurDirectMessages.hideThread(threadId)
                    pushWebMessage(
                        JSONObject()
                            .put("type", "poplurDMThreadHidden")
                            .put("threadId", threadId)
                            .put("ok", ok)
                    )
                }
            }
            if (type == "sendDMAttachment") {
                lifecycleScope.launch { uploadDMAttachment(body) }
            }
            if (type == "sendDM") {
                val otherId = body.optString("otherId")
                val text = body.optString("text")
                val clientMessageId = body.optString("clientMessageId")
                lifecycleScope.launch {
                    val ok = PoplurDirectMessages.send(otherId, text, clientMessageId)
                    pushWebMessage(
                        JSONObject()
                            .put("type", if (ok) "poplurDMSent" else "poplurDMSendFailed")
                            .put("clientMessageId", clientMessageId)
                            .apply { if (!ok) put("text", text) }
                    )
                }
            }
            if (type == "profileThemeChanged") {
                val payload = body.optJSONObject("detail") ?: body
                val themeId = payload.optString("themeId").ifBlank { payload.optString("id") }
                runOnUiThread {
                    val accepted = profileThemeRepository.selectById(themeId)
                    val selectedTheme = profileThemeRepository.selectedTheme.value
                    if (accepted) applyProfileThemeSystemBars(selectedTheme)
                    deliverProfileTheme(selectedTheme, accepted, if (accepted) null else "Unknown profile theme ID.")
                }
            }
            if (type == "requestProfileTheme") {
                deliverProfileTheme(profileThemeRepository.selectedTheme.value, accepted = true)
            }
            if (type == "appIconChanged") {
                val payload = body.optJSONObject("detail") ?: body
                val iconId = payload.optString("iconId").ifBlank { payload.optString("id") }
                runOnUiThread {
                    val accepted = AppIconManager.select(this@PostLoginWebActivity, iconId)
                    deliverAppIcon(accepted, if (accepted) null else "Unknown app icon ID.")
                }
            }
            if (type == "requestAppIcon") {
                deliverAppIcon(accepted = true)
            }
            if (type == "poplurStoreCheckoutRequested") {
                runOnUiThread { requestStoreCheckout(body) }
            }
            if (type == "sharePost") {
                val title = body.optString("title").trim().ifEmpty { "Poplur" }
                val user = body.optString("user").trim()
                val line = if (user.isEmpty()) title else "$title — $user"
                val rawShareLink = body.optString("link").trim()
                    .ifEmpty { body.optString("url").trim() }
                val suppliedLink = rawShareLink.takeIf { raw ->
                    val uri = runCatching { Uri.parse(raw) }.getOrNull()
                    uri?.scheme.equals("https", ignoreCase = true) &&
                        !uri?.host.isNullOrBlank() && uri?.userInfo.isNullOrBlank()
                }
                val postLink = body.optString("postId").trim().takeIf { it.isNotEmpty() }?.let { postId ->
                    Uri.Builder()
                        .scheme("https")
                        .authority("poplur.com")
                        .appendPath("p")
                        .appendPath(postId)
                        .build()
                        .toString()
                }
                // postId is the immutable identity. Prefer its canonical URL
                // over any generic/channel link supplied by the web caller.
                // Link-only surfaces such as Radio still use suppliedLink.
                val link = postLink ?: suppliedLink
                val text = if (link == null) line else "$line\n$link"
                runOnUiThread {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        this.type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                        putExtra(Intent.EXTRA_SUBJECT, "Poplur")
                    }
                    startActivity(Intent.createChooser(intent, "Share this pop"))
                }
            }
            if (type == "shareReferral") {
                val code = body.optString("code", "")
                val link = body.optString("link", "https://poplur.com")
                val text = "Join me on Poplur — use my code $code and we both get credits and the whole map.\n$link"
                runOnUiThread {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        this.type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                        putExtra(Intent.EXTRA_SUBJECT, "Poplur")
                    }
                    startActivity(Intent.createChooser(intent, "Invite a friend"))
                }
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
            if (type == "requestProfileCameraRoll") {
                PoplurPermissionPrimer.markHandled(
                    this@PostLoginWebActivity,
                    PoplurPermissionPrimer.PHOTOS
                )
                // Profile/Mail invokes this from an explicit Camera Roll tap,
                // so this is the correct gesture for the Android system sheet.
                // The permission callback below publishes only real MediaStore
                // items; the web shell never substitutes bundled fixtures.
                runOnUiThread {
                    if (PoplurWebCameraRoll.hasPermission(this@PostLoginWebActivity)) {
                        PoplurWebCameraRoll.push(homeWebView, ::registerWebCapture)
                    } else {
                        PoplurWebCameraRoll.requestPermission(this@PostLoginWebActivity)
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
            if (type == "feedAdProbabilityChanged") {
                // The web feed mixer sets the blend now. SettingsScreen sits
                // behind USE_WEB_SURFACES, so on a shipped build the split
                // could otherwise only ever be its stored default.
                //
                // saveConfig normalizes, and every placement decision below
                // still reads FeedAdProbability rather than anything the page
                // sent, so the shell remains the source of truth. Photo cards
                // and video creatives are both Poplur creative from here and
                // arrive collapsed into one share.
                val poplur = body.optDouble("poplurAdPercent", Double.NaN)
                val google = body.optDouble("googleMobileAdPercent", Double.NaN)
                if (poplur.isNaN() || google.isNaN()) {
                    Log.w(TAG, "feed ad mix update missing percentages")
                } else {
                    FeedAdProbability.saveConfig(
                        this@PostLoginWebActivity,
                        FeedAdProbabilityConfig(
                            poplurAdPercent = poplur.toFloat(),
                            googleMobileAdPercent = google.toFloat()
                        )
                    )
                }
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
                // A fixed two-post cadence keeps a real Google placement at a
                // natural break in the stream while making it visible in a
                // normal viewing session.
                val shouldPresent = if (isContentAdvance) {
                    val sequence = body.optInt("sequence", 0)
                    sequence > 0 && sequence % 2 == 0
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
                    backgroundAudioPreferences
                        .edit()
                        .putString("radio_scope", scope)
                        .putBoolean("background_audio_enabled", scope != "screen")
                        .apply()
                }
            }
            if (type == "requestBackgroundAudio") {
                deliverBackgroundAudioPreference()
                return
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
            if (type == "analyticsEvent" || type == "analyticsUserProperty") com.apptists.poplur2025.analytics.AnalyticsRelay.handle(this@PostLoginWebActivity, type, body)
        }
    }

    private fun requestStoreCheckout(body: JSONObject) {
        val payload = body.optJSONObject("detail") ?: body
        val requestId = payload.optString("requestId").ifBlank { UUID.randomUUID().toString() }
        if (storeCheckoutSession != null) {
            sendStoreCheckoutResult(
                requestId = requestId,
                success = false,
                status = "busy",
                message = "Another store checkout is already in progress."
            )
            return
        }

        val items = payload.optJSONArray("items")
        if (items == null || items.length() == 0) {
            sendStoreCheckoutResult(requestId, false, "error", "No card products were requested.")
            return
        }

        val queue = mutableListOf<String>()
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index)
            val requestedProductId = item?.optString("androidProductId")
                ?.ifBlank { item.optString("cardId") }
                ?.ifBlank { item.optString("productId") }
                ?.ifBlank { item.optString("id") }
                ?: items.optString(index)
            val productId = PoplurProducts.resolveCardProductId(requestedProductId)
            val quantity = item?.optInt("quantity", 1) ?: 1
            if (productId == null) {
                sendStoreCheckoutResult(
                    requestId,
                    false,
                    "error",
                    "Unsupported card product: ${requestedProductId.ifBlank { "missing product ID" }}."
                )
                return
            }
            if (quantity !in 1..10) {
                sendStoreCheckoutResult(
                    requestId,
                    false,
                    "error",
                    "Each card quantity must be between 1 and 10."
                )
                return
            }
            repeat(quantity) { queue += productId }
            if (queue.size > MAX_STORE_CHECKOUT_ITEMS) {
                sendStoreCheckoutResult(
                    requestId,
                    false,
                    "error",
                    "A checkout can contain at most $MAX_STORE_CHECKOUT_ITEMS card items."
                )
                return
            }
        }

        val session = StoreCheckoutSession(requestId = requestId, productQueue = queue)
        storeCheckoutSession = session
        lifecycleScope.launch {
            val productsReady = withTimeoutOrNull(STORE_PRODUCT_WAIT_MS) {
                billingManager.productDetails.first { details ->
                    queue.all(details::containsKey)
                }
            } != null
            if (storeCheckoutSession !== session) return@launch
            if (!productsReady) {
                storeCheckoutSession = null
                val missing = queue.distinct().filterNot(billingManager.productDetails.value::containsKey)
                sendStoreCheckoutResult(
                    requestId,
                    false,
                    "unavailable",
                    "These products are not available for this Google Play build: ${missing.joinToString()}."
                )
                return@launch
            }
            if (!launchCurrentStoreProduct(session)) {
                storeCheckoutSession = null
                sendStoreCheckoutResult(
                    requestId,
                    false,
                    "unavailable",
                    billingManager.message.value ?: "Google Play checkout could not be opened."
                )
            }
        }
    }

    private fun launchCurrentStoreProduct(session: StoreCheckoutSession): Boolean {
        val productId = session.productQueue.getOrNull(session.position) ?: return false
        return billingManager.launchPurchase(this, productId)
    }

    private fun handleStorePurchaseEvent(event: PurchaseEvent) {
        val session = storeCheckoutSession ?: return
        val expectedProductId = session.productQueue.getOrNull(session.position) ?: return
        if (event.productId != expectedProductId) return

        when (event.status) {
            PurchaseEventStatus.Success -> {
                session.pendingCompletion = false
                clearPendingStoreCheckout()
                val pkg = PoplurProducts.cardPackage(event.productId)
                session.completed += JSONObject().apply {
                    put("productId", event.productId)
                    put("cardId", pkg?.cardId ?: JSONObject.NULL)
                    put("creditsAdded", event.credits)
                    event.transactionId?.let { put("transactionId", it) }
                }
                session.position += 1
                if (session.position >= session.productQueue.size) {
                    storeCheckoutSession = null
                    sendStoreCheckoutResult(
                        requestId = session.requestId,
                        success = true,
                        status = "completed",
                        message = "All card purchases were verified and delivered.",
                        completed = session.completed
                    )
                } else {
                    lifecycleScope.launch {
                        // Play receives the next launch only after the prior token has
                        // been verified and consumed by PoplurBillingManager.
                        delay(200)
                        if (storeCheckoutSession !== session) return@launch
                        if (!launchCurrentStoreProduct(session)) {
                            storeCheckoutSession = null
                            sendStoreCheckoutResult(
                                session.requestId,
                                false,
                                "unavailable",
                                billingManager.message.value ?: "The next Google Play item could not be opened.",
                                session.completed
                            )
                        }
                    }
                }
            }
            PurchaseEventStatus.Pending -> {
                session.pendingCompletion = true
                persistPendingStoreCheckout(session)
                sendStoreCheckoutResult(
                    session.requestId,
                    false,
                    "pending",
                    "Payment is pending in Google Play. Credits will be delivered only after payment completes.",
                    session.completed
                )
            }
            PurchaseEventStatus.Cancelled -> {
                storeCheckoutSession = null
                clearPendingStoreCheckout()
                sendStoreCheckoutResult(
                    session.requestId,
                    false,
                    "cancelled",
                    "Checkout was cancelled. Remaining items were not charged.",
                    session.completed
                )
            }
            PurchaseEventStatus.Failed -> {
                storeCheckoutSession = null
                clearPendingStoreCheckout()
                sendStoreCheckoutResult(
                    session.requestId,
                    false,
                    "error",
                    event.message,
                    session.completed
                )
            }
        }
    }

    private fun persistPendingStoreCheckout(session: StoreCheckoutSession) {
        val queue = JSONArray().apply { session.productQueue.forEach(::put) }
        val completed = JSONArray().apply { session.completed.forEach(::put) }
        val json = JSONObject().apply {
            put("requestId", session.requestId)
            put("productQueue", queue)
            put("position", session.position)
            put("completed", completed)
            put("pendingCompletion", session.pendingCompletion)
        }
        storeCheckoutPreferences.edit().putString(PENDING_STORE_CHECKOUT_KEY, json.toString()).apply()
    }

    private fun restorePendingStoreCheckout(): StoreCheckoutSession? {
        val raw = storeCheckoutPreferences.getString(PENDING_STORE_CHECKOUT_KEY, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            if (!json.optBoolean("pendingCompletion")) return@runCatching null
            val queueJson = json.getJSONArray("productQueue")
            val queue = List(queueJson.length()) { index -> queueJson.getString(index) }
            val position = json.optInt("position", 0)
            if (queue.isEmpty() || position !in queue.indices) return@runCatching null
            val completed = mutableListOf<JSONObject>()
            val completedJson = json.optJSONArray("completed") ?: JSONArray()
            for (index in 0 until completedJson.length()) {
                completedJson.optJSONObject(index)?.let(completed::add)
            }
            StoreCheckoutSession(
                requestId = json.getString("requestId"),
                productQueue = queue,
                position = position,
                completed = completed,
                pendingCompletion = true
            )
        }.getOrNull().also { restored ->
            if (restored == null) clearPendingStoreCheckout()
        }
    }

    private fun clearPendingStoreCheckout() {
        storeCheckoutPreferences.edit().remove(PENDING_STORE_CHECKOUT_KEY).apply()
    }

    private fun sendStoreCheckoutResult(
        requestId: String,
        success: Boolean,
        status: String,
        message: String,
        completed: List<JSONObject> = emptyList()
    ) {
        val canonicalStatus = when (status) {
            "completed", "success" -> "success"
            "pending" -> "pending"
            "cancelled" -> "cancelled"
            else -> "failure"
        }
        val completedCounts = linkedMapOf<String, Int>()
        completed.forEach { item ->
            val cardId = item.optString("cardId")
            if (cardId.isNotBlank()) completedCounts[cardId] = (completedCounts[cardId] ?: 0) + 1
        }
        val completedItems = JSONArray().apply {
            completedCounts.forEach { (cardId, quantity) ->
                put(JSONObject().put("cardId", cardId).put("quantity", quantity))
            }
        }
        val payload = JSONObject().apply {
            put("type", "poplurStoreCheckoutResult")
            put("requestId", requestId)
            put("success", success)
            put("ok", success)
            put("status", canonicalStatus)
            put("rawStatus", status)
            put("message", message)
            put("completedItems", completedItems)
        }
        homeWebView.post {
            homeWebView.evaluateJavascript(
                "window.dispatchEvent(new CustomEvent('poplurStoreCheckoutResult',{detail:$payload}));" +
                    "window.dispatchEvent(new CustomEvent('poplur-native-store-checkout-result',{detail:$payload}));" +
                    "window.postMessage($payload,'*');true;",
                null
            )
        }
    }

    /**
     * Hands one payload to the page the way iOS does — a plain
     * `window.postMessage`, because these features arrived on the web side
     * listening for messages rather than for a named CustomEvent.
     */
    private fun pushWebMessage(payload: JSONObject) {
        homeWebView.post {
            homeWebView.evaluateJavascript("window.postMessage($payload,'*');true;", null)
        }
    }

    private fun deliverPendingDMDeepLink() {
        if (!homeReady.value) return
        val threadId = pendingDMThreadId
            ?.takeIf { it.isNotBlank() && it.length <= 200 }
            ?: return
        pendingDMThreadId = null
        pushWebMessage(
            JSONObject()
                .put("type", "poplurOpenDM")
                .put("threadId", threadId)
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDMThreadId = intent.getStringExtra("threadId")
            ?.takeIf { it.isNotBlank() && it.length <= 200 }
        readPendingPostFocus(intent)
        deliverPendingDMDeepLink()
        deliverPendingPostFocus()
    }

    /** A tapped engagement/radius notification lands on the post's pin. Held
     *  until Home is ready, the same way a DM tap is. */
    private fun readPendingPostFocus(intent: Intent) {
        pendingPostFocusId = intent.getStringExtra("postId")
            ?.takeIf { it.isNotBlank() && it.length <= 180 }
        pendingPostFocusDetails = JSONObject().apply {
            for (key in listOf("notificationId", "kind", "title", "detail")) {
                intent.getStringExtra(key)?.take(1000)?.let { put(key, it) }
            }
        }
    }

    private fun deliverPendingPostFocus() {
        if (!homeReady.value) return
        val postId = pendingPostFocusId ?: return
        pendingPostFocusId = null
        val details = pendingPostFocusDetails ?: JSONObject()
        pendingPostFocusDetails = null
        pushWebMessage(
            details
                .put("type", "poplurOpenPost")
                .put("postId", postId)
        )
    }

    private fun deliverProfileTheme(
        theme: ProfileTheme,
        accepted: Boolean,
        error: String? = null
    ) {
        val payload = JSONObject().apply {
            put("accepted", accepted)
            put("id", theme.id)
            put("themeId", theme.id)
            put("displayName", theme.displayName)
            put("isDefault", theme.isDefault)
            put("primaryAccent", theme.primaryAccent.toWebHex())
            put("secondaryAccent", theme.secondaryAccent.toWebHex())
            put("foreground", theme.foreground.toWebHex())
            put("overlayColor", theme.overlayColor.toWebHex())
            put("overlayOpacity", theme.overlayOpacity.toDouble())
            put("statusBar", theme.statusBarIcons.name.lowercase())
            put("availability", theme.availability.name.lowercase())
            if (!error.isNullOrBlank()) put("error", error)
        }
        homeWebView.post {
            homeWebView.evaluateJavascript(
                "window.poplurProfileTheme?.($payload);" +
                    "window.dispatchEvent(new CustomEvent('poplur-native-profile-theme',{detail:$payload}));true;",
                null
            )
        }
    }

    private fun deliverAppIcon(accepted: Boolean, error: String? = null) {
        val payload = JSONObject().apply {
            put("accepted", accepted)
            put("id", AppIconManager.currentId(this@PostLoginWebActivity))
            if (!error.isNullOrBlank()) put("message", error)
        }
        homeWebView.post {
            homeWebView.evaluateJavascript(
                "window.dispatchEvent(new CustomEvent('poplur-native-app-icon',{detail:$payload}));true;",
                null
            )
        }
    }

    private fun deliverBackgroundAudioPreference() {
        val payload = backgroundAudioPayload().toString()
        homeWebView.post {
            homeWebView.evaluateJavascript(
                "window.__poplurNativeBackgroundAudio=$payload;" +
                    "window.dispatchEvent(new CustomEvent('poplur-native-background-audio',{detail:$payload}));true;",
                null
            )
        }
    }

    private fun applyProfileThemeSystemBars(theme: ProfileTheme) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (theme.isDefault) {
            window.statusBarColor = darkHome
            window.navigationBarColor = darkHome
            controller.isAppearanceLightStatusBars = false
        } else {
            window.statusBarColor = theme.overlayColor.toArgb()
            window.navigationBarColor = theme.overlayColor.toArgb()
            controller.isAppearanceLightStatusBars =
                theme.statusBarIcons == ProfileStatusBarIcons.Dark
        }
    }

    private fun Color.toWebHex(): String = String.format("#%06X", toArgb() and 0x00FFFFFF)

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
        // Two different things, deliberately kept apart. `derivedUsername` is a
        // name Auth genuinely knows and is the ONLY thing safe to write into the
        // account. `fallbackUsername` keeps the pre-existing hardcoded default
        // for DISPLAY, and must never be persisted — seeding it would stamp
        // "mark" onto the account of every user whose email is missing.
        val derivedUsername = sanitizeUsername(
            user.displayName?.takeIf { it.isNotBlank() } ?: user.email?.substringBefore("@")
        )
        val fallbackUsername = derivedUsername ?: "mark"
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
                val storedUsername = (document?.getString("username")
                    ?: document?.getString("handle"))?.takeIf { it.isNotBlank() }
                if (storedUsername == null && derivedUsername != null) {
                    // The account has never had a username, because nothing on
                    // the live path could set one: the only writer was the
                    // native edit-profile screen, which the web home orphaned.
                    // Every device showed a name derived from Auth while the
                    // DOCUMENT stayed empty — which is why a server-minted
                    // wallet pass fell back to a uid prefix and a handle never
                    // followed the user to a second device.
                    FirebaseFirestore.getInstance()
                        .collection("users").document(user.uid)
                        .set(mapOf("username" to derivedUsername), SetOptions.merge())
                        .addOnSuccessListener { Log.i(TAG, "[PoplurProfile] seeded username: $derivedUsername") }
                        .addOnFailureListener { Log.w(TAG, "[PoplurProfile] username seed FAILED", it) }
                }
                val payload = JSONObject().apply {
                    put("uid", user.uid)
                    put("username", storedUsername ?: fallbackUsername)
                    put("business", business)
                    put("privateAccount", document?.getBoolean("privateAccount") == true)
                    put("ghostMode", document?.getBoolean("ghostMode") == true)
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

    private fun deliverAccountPrivacy() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            pushAccountPrivacy(false, false, false, "Sign in to load privacy.")
            return
        }
        FirebaseFirestore.getInstance().collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                changeAccountPrivacy(
                    privateAccount = document.getBoolean("privateAccount") == true,
                    ghostMode = document.getBoolean("ghostMode") == true,
                )
            }
            .addOnFailureListener { error ->
                pushAccountPrivacy(
                    false,
                    false,
                    false,
                    error.localizedMessage ?: "Privacy could not be loaded.",
                )
            }
    }

    private fun pushMapDiscovery(payload: JSONObject) {
        homeWebView.post {
            homeWebView.evaluateJavascript(
                "window.dispatchEvent(new CustomEvent('poplur-map-discovery-result',{detail:$payload}));true;", null,
            )
        }
    }

    private fun changeAccountPrivacy(privateAccount: Boolean, ghostMode: Boolean) {
        if (FirebaseAuth.getInstance().currentUser == null) {
            pushAccountPrivacy(privateAccount, ghostMode, false, "Sign in to change privacy.")
            return
        }
        FirebaseFunctions.getInstance("us-central1")
            .getHttpsCallable("setAccountLocationPrivacy")
            .call(mapOf("privateAccount" to privateAccount, "ghostMode" to ghostMode))
            .addOnSuccessListener { result ->
                @Suppress("UNCHECKED_CAST")
                val data = result.getData() as? Map<String, Any>
                pushAccountPrivacy(
                    privateAccount = data?.get("privateAccount") as? Boolean ?: privateAccount,
                    ghostMode = data?.get("ghostMode") as? Boolean ?: ghostMode,
                )
            }
            .addOnFailureListener { error ->
                pushAccountPrivacy(
                    privateAccount,
                    ghostMode,
                    false,
                    error.localizedMessage ?: "Privacy could not be saved.",
                )
            }
    }

    private fun pushAccountPrivacy(
        privateAccount: Boolean,
        ghostMode: Boolean,
        ok: Boolean = true,
        message: String? = null,
    ) {
        val payload = JSONObject()
            .put("privateAccount", privateAccount)
            .put("ghostMode", ghostMode)
            .put("ok", ok)
        if (!message.isNullOrBlank()) payload.put("message", message)
        homeWebView.post {
            homeWebView.evaluateJavascript(
                "window.dispatchEvent(new CustomEvent('poplur-native-account-privacy',{detail:$payload}));true;",
                null,
            )
        }
    }

    /**
     * Persists the still poster captured by ProfileTwin as the account photo.
     *
     * The web layer already keeps a small localStorage copy, but that is only
     * this WebView on this device. Without this bridge writer Android could
     * record the profile selfie while leaving `users/{uid}.profilePictureURL`
     * empty, so a pull-to-refresh correctly fetched an account with no photo.
     */
    private fun persistWebProfilePhoto(dataUrl: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.w(TAG, "persistWebProfilePhoto: no signed-in user")
            return
        }
        if (!dataUrl.startsWith("data:image/jpeg;base64,") ||
            dataUrl.length > MAX_PROFILE_PHOTO_DATA_URL_CHARS
        ) {
            Log.w(TAG, "persistWebProfilePhoto: rejected invalid JPEG data URL")
            return
        }

        val bytes = runCatching {
            Base64.decode(dataUrl.substringAfter(','), Base64.DEFAULT)
        }.getOrNull()
        if (bytes == null || bytes.isEmpty() || bytes.size > MAX_PROFILE_PHOTO_BYTES) {
            Log.w(TAG, "persistWebProfilePhoto: rejected invalid or oversized photo")
            return
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            Log.w(TAG, "persistWebProfilePhoto: JPEG could not be decoded")
            return
        }

        val photoRef = FirebaseStorage.getInstance().reference
            .child("profile_pictures/${user.uid}.jpg")
        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()

        photoRef.putBytes(bytes, metadata)
            .addOnFailureListener { error ->
                Log.w(TAG, "persistWebProfilePhoto: upload failed", error)
            }
            .addOnSuccessListener {
                photoRef.downloadUrl
                    .addOnFailureListener { error ->
                        Log.w(TAG, "persistWebProfilePhoto: download URL failed", error)
                    }
                    .addOnSuccessListener { downloadUrl ->
                        FirebaseFirestore.getInstance()
                            .collection("users").document(user.uid)
                            .set(
                                mapOf("profilePictureURL" to downloadUrl.toString()),
                                SetOptions.merge()
                            )
                            .addOnFailureListener { error ->
                                Log.w(TAG, "persistWebProfilePhoto: profile update failed", error)
                            }
                            .addOnSuccessListener {
                                Log.i(TAG, "persistWebProfilePhoto: account photo saved")
                                // Reuse the exact response path pull-to-refresh
                                // uses, so the visible profile updates from the
                                // account record rather than a second contract.
                                deliverWebProfile()
                            }
                    }
            }
    }

    private fun deliverWebProfilePosts() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        FirebaseFirestore.getInstance().collection("posts")
            .whereEqualTo("userID", user.uid)
            .limit(64)
            .get()
            .addOnSuccessListener { snapshot ->
                val posts = JSONArray()
                snapshot.documents
                    .sortedByDescending { document ->
                        document.getLong("createdAtMillis")
                            ?: document.getTimestamp("createdAt")?.toDate()?.time
                            ?: 0L
                    }
                    .forEach { document ->
                        val mediaURL = document.getString("mediaURL")?.trim().orEmpty()
                        if (mediaURL.isBlank()) return@forEach
                        val lowerURL = mediaURL.lowercase()
                        val inferredType = if (lowerURL.contains(".mp4") || lowerURL.contains(".mov") || lowerURL.contains(".m4v") || lowerURL.contains(".webm")) "video" else "photo"
                        posts.put(JSONObject().apply {
                            put("id", document.id)
                            put("mediaURL", mediaURL)
                            put("mediaType", document.getString("mediaType") ?: inferredType)
                            document.getDouble("photoViewSeconds")?.let { put("photoViewSeconds", it) }
                            document.getString("posterURL")?.takeIf { it.isNotBlank() }
                                ?.let { put("posterURL", it) }
                            document.getString("caption")?.takeIf { it.isNotBlank() }
                                ?.let { put("caption", it) }
                            document.getString("channelId")?.takeIf { it.isNotBlank() }
                                ?.let { put("channelId", it) }
                        })
                    }
                val payload = JSONObject().put("posts", posts)
                homeWebView.post {
                    homeWebView.evaluateJavascript(
                        "window.dispatchEvent(new CustomEvent('poplur-native-profile-posts',{detail:$payload}));true;",
                        null
                    )
                }
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "deliverWebProfilePosts failed", error)
            }
    }

    private fun deliverWebFeedPosts() {
        if (isDestroyed || isFinishing) return
        val auth = FirebaseAuth.getInstance()
        if (feedAuthListener == null) {
            val listener = FirebaseAuth.AuthStateListener { state -> bindWebFeedPosts(state.currentUser?.uid) }
            feedAuthListener = listener
            auth.addAuthStateListener(listener)
        }
        bindWebFeedPosts(auth.currentUser?.uid)
    }

    private fun bindWebFeedPosts(uid: String?) {
        if (isDestroyed || isFinishing) return
        if (feedSnapshot.uid == uid && feedPostsRegistration != null && ownFeedRegistration != null) {
            forwardWebAccountRadius()
            forwardWebFeedPosts()
            return
        }
        feedGeneration += 1
        val generation = feedGeneration
        feedPostsRegistration?.remove()
        ownFeedRegistration?.remove()
        feedPostsRegistration = null
        ownFeedRegistration = null
        feedSnapshot.reset(uid)
        if (uid == null) return
        val posts = FirebaseFirestore.getInstance().collection("posts")
        // Keep all owned posts live, including posts older than the Feed window.
        // Each stream reuses the other snapshot; an update never re-arms a query.
        ownFeedRegistration = posts.whereEqualTo("userID", uid)
            .addSnapshotListener { snapshot, error ->
                if (isDestroyed || isFinishing || generation != feedGeneration) return@addSnapshotListener
                if (snapshot == null) {
                    Log.w(TAG, "Owned post listener failed", error)
                    return@addSnapshotListener
                }
                feedSnapshot.owned = snapshot.documents
                forwardWebAccountRadius()
                forwardWebFeedPosts()
            }
        feedPostsRegistration = posts.orderBy("createdAt", Query.Direction.DESCENDING).limit(96)
            .addSnapshotListener { snapshot, error ->
                if (isDestroyed || isFinishing || generation != feedGeneration) return@addSnapshotListener
                if (snapshot == null) {
                    Log.w(TAG, "Feed post listener failed", error)
                    return@addSnapshotListener
                }
                feedSnapshot.recent = snapshot.documents
                forwardWebFeedPosts()
            }
    }

    private fun forwardWebAccountRadius() {
        val uid = feedSnapshot.uid ?: return
        val owned = feedSnapshot.owned ?: return
        val totals = WebAccountRadius.fromPosts(owned.map { it.data.orEmpty() })
        val payload = JSONObject().put("uid", uid).put("radiusKm", totals.earnedMeters / 1000)
            .put("totalReachKm", totals.reachMeters / 1000).put("postCount", owned.size)
        val generation = feedGeneration
        homeWebView.post {
            if (isDestroyed || isFinishing || generation != feedGeneration) return@post
            homeWebView.evaluateJavascript(
                "window.dispatchEvent(new CustomEvent('poplur-native-account-radius',{detail:$payload}));true;", null,
            )
        }
    }

    private fun forwardWebFeedPosts() {
        if (!feedSnapshot.ready) return
        val documents = feedSnapshot.merged()
        val posts = JSONArray()
        documents
            .sortedByDescending { document ->
                document.getLong("createdAtMillis")
                    ?: document.getTimestamp("createdAt")?.toDate()?.time
                    ?: 0L
            }
            .forEach { document ->
                if ((document.getString("audience") ?: "public") != "public") return@forEach
                if ((document.getString("status") ?: "published") != "published") return@forEach
                val mediaURL = (document.getString("playbackURL")
                    ?: document.getString("mediaURL"))?.trim().orEmpty()
                if (mediaURL.isBlank()) return@forEach
                val lowerURL = mediaURL.lowercase()
                val inferredType = if (lowerURL.contains(".mp4") || lowerURL.contains(".mov") || lowerURL.contains(".m4v") || lowerURL.contains(".webm")) "video" else "photo"
                posts.put(JSONObject().apply {
                    put("id", document.id)
                    put("mediaURL", mediaURL)
                    put("mediaType", document.getString("mediaType") ?: inferredType)
                    document.getDouble("photoViewSeconds")?.let { put("photoViewSeconds", it) }
                    put("username", document.getString("username") ?: "Poplur user")
                    put("channelId", document.getString("channelId") ?: "live")
                    val ownerId = (document.getString("userID")
                        ?: document.getString("userId")
                        ?: document.getString("ownerId"))
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                    if (ownerId != null) put("ownerId", ownerId)
                    put(
                        "createdAtMillis",
                        document.getLong("createdAtMillis")
                            ?: document.getTimestamp("createdAt")?.toDate()?.time
                            ?: 0L,
                    )
                    document.getString("posterURL")?.takeIf { it.isNotBlank() }
                        ?.let { put("posterURL", it) }
                    document.getString("caption")?.takeIf { it.isNotBlank() }
                        ?.let { put("caption", it) }
                    // Provenance for test mode: the web's reach rule lets the synthetic
                    // beta cohort's pops reach every viewer while the simulation switch is on.
                    document.getString("syntheticJourney")?.takeIf { it.isNotBlank() }
                        ?.let { put("syntheticJourney", it) }
                    if (document.getBoolean("syntheticBeta") == true) put("syntheticBeta", true)
                    // radiusMeters first: a post carrying only that field used to
                    // cross with no radius, and every reach check fails closed on 0.
                    (document.getLong("radiusMeters") ?: document.getLong("radius") ?: document.getLong("organicRadiusMeters"))
                        ?.let { put("radiusMeters", it) }
                    document.getLong("viewCount")?.let { put("viewCount", it.coerceAtLeast(0)) }
                    document.getLong("likeCount")?.let { put("likeCount", it.coerceAtLeast(0)) }
                    document.getLong("commentCount")?.let { put("commentCount", it.coerceAtLeast(0)) }
                    document.getLong("shareCount")?.let { put("shareCount", it.coerceAtLeast(0)) }
                    when (document.getString("locationVisibility")) {
                        "hidden" -> put("locationVisibility", "hidden")
                        "approximate" -> {
                            val latitude = document.getDouble("latitude")
                            val longitude = document.getDouble("longitude")
                            if (latitude != null && longitude != null &&
                                latitude.isFinite() && longitude.isFinite() &&
                                kotlin.math.abs(latitude) <= 90 && kotlin.math.abs(longitude) <= 180
                            ) {
                                put("locationVisibility", "approximate")
                                put("originLatitude", latitude)
                                put("originLongitude", longitude)
                            }
                        }
                    }
                })
            }
        val payload = JSONObject().put("posts", posts)
        val generation = feedGeneration
        homeWebView.post {
            if (isDestroyed || isFinishing || generation != feedGeneration) return@post
            homeWebView.evaluateJavascript(
                "window.dispatchEvent(new CustomEvent('poplur-native-feed-posts',{detail:$payload}));true;",
                null,
            )
        }
    }

    /**
     * Deletes only a post owned by the currently authenticated account.
     * JavaScript is an untrusted caller, so identifiers are bounded before
     * becoming Firestore paths and ownership comes from the fetched document.
     */
    private fun deleteWebProfilePost(body: JSONObject) {
        val requestId = boundedBridgeId(body.opt("requestId"))
        val postId = boundedBridgeId(body.opt("postId"))
        if (requestId == null || postId == null) {
            deliverProfilePostDeleteResult(
                requestId = requestId.orEmpty(),
                postId = postId.orEmpty(),
                ok = false,
                error = "invalid_request",
            )
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            deliverProfilePostDeleteResult(requestId, postId, false, "signed_out")
            return
        }

        val reference = FirebaseFirestore.getInstance().collection("posts").document(postId)
        reference.get()
            .addOnFailureListener {
                deliverProfilePostDeleteResult(requestId, postId, false, "delete_failed")
            }
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    deliverProfilePostDeleteResult(requestId, postId, false, "not_found")
                    return@addOnSuccessListener
                }
                if ((snapshot.getString("userID") ?: snapshot.getString("userId")) != uid) {
                    deliverProfilePostDeleteResult(requestId, postId, false, "forbidden")
                    return@addOnSuccessListener
                }

                listOf("mediaStoragePath", "posterStoragePath")
                    .mapNotNull { key -> ownedPostStoragePath(snapshot.getString(key), uid) }
                    .forEach { path -> FirebaseStorage.getInstance().reference.child(path).delete() }
                reference.delete()
                    .addOnSuccessListener {
                        deliverProfilePostDeleteResult(requestId, postId, true)
                    }
                    .addOnFailureListener {
                        deliverProfilePostDeleteResult(requestId, postId, false, "delete_failed")
                    }
            }
    }

    private fun boundedBridgeId(value: Any?): String? {
        val id = value as? String ?: return null
        if (id.isEmpty() || id.toByteArray(Charsets.UTF_8).size > 128) return null
        if (id.any { character ->
                character.code !in 0x21..0x7E || character == '/' || character == '\\'
            }
        ) return null
        return id
    }

    private fun ownedPostStoragePath(value: String?, uid: String): String? {
        val path = value ?: return null
        val prefix = "posts/$uid/"
        if (path.length > 240 || !path.startsWith(prefix) || path.contains("..")) return null
        val basename = path.removePrefix(prefix)
        return path.takeIf { basename.isNotEmpty() && !basename.contains('/') }
    }

    private fun deliverProfilePostDeleteResult(
        requestId: String,
        postId: String,
        ok: Boolean,
        error: String? = null,
    ) {
        val payload = JSONObject()
            .put("requestId", requestId)
            .put("postId", postId)
            .put("ok", ok)
            .apply { if (error != null) put("error", error) }
            .toString()
        homeWebView.post {
            homeWebView.evaluateJavascript(
                "window.dispatchEvent(new CustomEvent('poplur-native-profile-post-delete-result',{detail:$payload}));true;",
                null,
            )
        }
    }

    /**
     * The signed-in user's posted MUSIC, for the iPod's shelf.
     *
     * Same shape as deliverWebProfilePosts and for the same reason: the web
     * layer holds no Firestore credentials, so the authenticated shell reads
     * and hands back rows. What travels is a POINTER — `url` for a file Poplur
     * hosts (a real stream URL the handheld fetches and decodes) or
     * `permalink` for a SoundCloud track, which plays in their embedded player
     * because their audio is not ours to fetch. A row naming neither is
     * dropped here rather than shipped to a client that could only fail on it.
     */
    private fun deliverWebProfileTracks() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        FirebaseFirestore.getInstance().collection("tracks")
            .whereEqualTo("userID", user.uid)
            .limit(128)
            .get()
            .addOnSuccessListener { snapshot ->
                val tracks = JSONArray()
                snapshot.documents
                    .sortedByDescending { document ->
                        document.getLong("createdAtMillis")
                            ?: document.getTimestamp("createdAt")?.toDate()?.time
                            ?: 0L
                    }
                    .forEach { document ->
                        val audioURL = document.getString("audioURL")?.trim().orEmpty()
                        val permalink = document.getString("permalink")?.trim().orEmpty()
                        if (audioURL.isBlank() && permalink.isBlank()) return@forEach
                        tracks.put(JSONObject().apply {
                            put("id", document.id)
                            put("title", document.getString("title")?.takeIf { it.isNotBlank() } ?: "Untitled")
                            put(
                                "postedAt",
                                document.getLong("createdAtMillis")
                                    ?: document.getTimestamp("createdAt")?.toDate()?.time
                                    ?: 0L
                            )
                            if (audioURL.isNotBlank()) put("url", audioURL)
                            if (permalink.isNotBlank()) put("permalink", permalink)
                            document.getString("artist")?.takeIf { it.isNotBlank() }
                                ?.let { put("artist", it) }
                            document.getString("place")?.takeIf { it.isNotBlank() }
                                ?.let { put("place", it) }
                            document.getString("licence")?.takeIf { it.isNotBlank() }
                                ?.let { put("licence", it) }
                            document.getString("mood")?.takeIf { it.isNotBlank() }
                                ?.let { put("mood", it) }
                            document.getDouble("lat")?.let { put("lat", it) }
                            document.getDouble("lng")?.let { put("lng", it) }
                            document.getDouble("bpm")?.let { put("bpm", it) }
                            document.getDouble("duration")?.let { put("duration", it) }
                            put(
                                "postedBy",
                                document.getString("postedBy")?.takeIf { it.isNotBlank() } ?: user.uid
                            )
                        })
                    }
                val payload = JSONObject().put("tracks", tracks)
                homeWebView.post {
                    homeWebView.evaluateJavascript(
                        "window.dispatchEvent(new CustomEvent('poplur-native-profile-tracks',{detail:$payload}));true;",
                        null
                    )
                }
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "deliverWebProfileTracks failed", error)
            }
    }

    /**
     * Returns playable Poplur Studio tracks posted near the phone's current
     * location. SoundCloud discovery intentionally stays out of this bridge:
     * those rows are attributed link-outs supplied by Poplur's server, while
     * this query is only for audio that Poplur itself hosts and may play.
     */
    private fun deliverWebNearbyTracks(body: JSONObject) {
        val latitude = body.optDouble("latitude", Double.NaN)
        val longitude = body.optDouble("longitude", Double.NaN)
        if (!latitude.isFinite() || !longitude.isFinite() ||
            kotlin.math.abs(latitude) > 90 || kotlin.math.abs(longitude) > 180
        ) return

        val requestedRadius = body.optDouble("radiusKm", 120.0)
        val radiusKm = if (requestedRadius.isFinite()) requestedRadius.coerceIn(1.0, 2_000.0) else 120.0
        val requestId = body.optString("requestId")

        fun distanceKm(otherLatitude: Double, otherLongitude: Double): Double {
            val latitudeDelta = Math.toRadians(otherLatitude - latitude)
            val longitudeDelta = Math.toRadians(otherLongitude - longitude)
            val latitude1 = Math.toRadians(latitude)
            val latitude2 = Math.toRadians(otherLatitude)
            val haversine = Math.pow(Math.sin(latitudeDelta / 2.0), 2.0) +
                Math.cos(latitude1) * Math.cos(latitude2) *
                Math.pow(Math.sin(longitudeDelta / 2.0), 2.0)
            return 6_371.0 * 2.0 * Math.asin(Math.min(1.0, Math.sqrt(haversine)))
        }

        fun push(tracks: JSONArray) {
            val payload = JSONObject()
                .put("requestId", requestId)
                .put("tracks", tracks)
            homeWebView.post {
                homeWebView.evaluateJavascript(
                    "window.dispatchEvent(new CustomEvent('poplur-native-nearby-tracks',{detail:$payload}));true;",
                    null
                )
            }
        }

        FirebaseFirestore.getInstance().collection("tracks")
            .limit(256)
            .get()
            .addOnSuccessListener { snapshot ->
                val nearby = snapshot.documents.mapNotNull { document ->
                    val audioURL = document.getString("audioURL")?.trim().orEmpty()
                    val audioUri = runCatching { Uri.parse(audioURL) }.getOrNull()
                    if (audioURL.isBlank() ||
                        !audioUri?.scheme.equals("https", ignoreCase = true) ||
                        audioUri?.host.isNullOrBlank()
                    ) return@mapNotNull null

                    val trackLatitude = (document.get("lat") as? Number)?.toDouble()
                        ?: return@mapNotNull null
                    val trackLongitude = (document.get("lng") as? Number)?.toDouble()
                        ?: return@mapNotNull null
                    if (!trackLatitude.isFinite() || !trackLongitude.isFinite()) return@mapNotNull null

                    val distance = distanceKm(trackLatitude, trackLongitude)
                    if (distance > radiusKm) return@mapNotNull null

                    val createdAt = document.getLong("createdAtMillis")
                        ?: document.getTimestamp("createdAt")?.toDate()?.time
                        ?: 0L
                    val row = JSONObject().apply {
                        put("id", document.id)
                        put("title", document.getString("title")?.takeIf { it.isNotBlank() } ?: "Untitled")
                        put("url", audioURL)
                        put("lat", trackLatitude)
                        put("lng", trackLongitude)
                        put("distanceKm", distance)
                        put(
                            "place",
                            document.getString("place")?.takeIf { it.isNotBlank() }
                                ?: String.format(java.util.Locale.US, "%.1f km away", distance)
                        )
                        put("postedAt", createdAt)
                        put(
                            "postedBy",
                            document.getString("postedBy")?.takeIf { it.isNotBlank() }
                                ?: document.getString("userID")?.takeIf { it.isNotBlank() }
                                ?: "Poplur artist"
                        )
                        document.getString("artist")?.takeIf { it.isNotBlank() }
                            ?.let { put("artist", it) }
                        document.getString("licence")?.takeIf { it.isNotBlank() }
                            ?.let { put("licence", it) }
                        document.getString("mood")?.takeIf { it.isNotBlank() }
                            ?.let { put("mood", it) }
                        (document.get("bpm") as? Number)?.toDouble()?.let { put("bpm", it) }
                        (document.get("duration") as? Number)?.toDouble()?.let { put("duration", it) }
                    }
                    distance to row
                }.sortedBy { it.first }

                val tracks = JSONArray()
                nearby.forEach { tracks.put(it.second) }
                push(tracks)
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "deliverWebNearbyTracks failed", error)
                push(JSONArray())
            }
    }

    private fun openInAppBrowser(rawUrl: String) {
        val destination = normalizePoplurInAppBrowserUrl(rawUrl)
        if (destination == null) {
            Log.w(TAG, "Ignoring invalid in-app browser URL")
            return
        }

        runOnUiThread {
            runCatching {
                // One external page means one isolated WebView. A second ad
                // replaces the first controller rather than stacking another
                // GPU-backed browser surface behind it.
                inAppBrowserDialog?.let { previous ->
                    inAppBrowserDialog = null
                    previous.setOnDismissListener(null)
                    previous.dismiss()
                }
                parkHomeForInAppBrowser()
                val dialog = PoplurInAppBrowserDialog(this@PostLoginWebActivity, destination)
                inAppBrowserDialog = dialog
                dialog.setOnDismissListener {
                    if (inAppBrowserDialog === dialog) {
                        inAppBrowserDialog = null
                        restoreHomeAfterInAppBrowserIfVisible()
                    }
                }
                dialog.show()
            }.onFailure { error ->
                inAppBrowserDialog?.let { failed ->
                    inAppBrowserDialog = null
                    failed.setOnDismissListener(null)
                    failed.dismiss()
                }
                restoreHomeAfterInAppBrowserIfVisible()
                Log.e(TAG, "Unable to open the in-app browser", error)
            }
        }
    }

    private fun parkHomeForInAppBrowser() {
        if (homeParkedForInAppBrowser) return
        homeParkedForInAppBrowser = true
        stopCubeMotion()
        // The controller owns Home's onPause/onResume calls. Marking it hidden
        // parks that one heavy renderer while the external WebView is visible.
        if (::performanceController.isInitialized) performanceController.setVisible(false)
    }

    private fun restoreHomeAfterInAppBrowserIfVisible(): Boolean {
        if (!homeParkedForInAppBrowser || !activityStarted || !activityResumed) return false
        homeParkedForInAppBrowser = false
        if (::performanceController.isInitialized) performanceController.setVisible(true)
        startCubeMotion()
        return true
    }

    /**
     * The post-login page requests camera and microphone together only when a
     * WebRTC call needs them. Never grant a resource WebView did not request,
     * and never grant an unknown resource merely because camera was allowed.
     */
    private fun requestWebMediaCapturePermission(request: PermissionRequest) {
        val requested = request.resources.toSet()
        val wantsVideo = PermissionRequest.RESOURCE_VIDEO_CAPTURE in requested
        val wantsAudio = PermissionRequest.RESOURCE_AUDIO_CAPTURE in requested
        if ((!wantsVideo && !wantsAudio)
            || requested.any {
                it != PermissionRequest.RESOURCE_VIDEO_CAPTURE
                    && it != PermissionRequest.RESOURCE_AUDIO_CAPTURE
            }) {
            request.deny()
            return
        }

        val missing = buildList {
            if (wantsVideo && ContextCompat.checkSelfPermission(
                    this@PostLoginWebActivity, Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.CAMERA)
            if (wantsAudio && ContextCompat.checkSelfPermission(
                    this@PostLoginWebActivity, Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.RECORD_AUDIO)
        }
        if (missing.isEmpty()) {
            request.grant(requested.toTypedArray())
            return
        }

        // A second prompt cannot safely answer the first request. Deny the
        // superseded one, then retain exactly this request for the OS result.
        pendingWebMediaPermissionRequest?.deny()
        pendingWebMediaPermissionRequest = request
        ActivityCompat.requestPermissions(
            this,
            missing.toTypedArray(),
            WEB_MEDIA_CAPTURE_REQUEST_CODE,
        )
    }

    private fun resolveWebMediaCapturePermission() {
        val request = pendingWebMediaPermissionRequest ?: return
        pendingWebMediaPermissionRequest = null
        val requested = request.resources.toSet()
        val wantsVideo = PermissionRequest.RESOURCE_VIDEO_CAPTURE in requested
        val wantsAudio = PermissionRequest.RESOURCE_AUDIO_CAPTURE in requested
        val videoGranted = !wantsVideo || ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val audioGranted = !wantsAudio || ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (videoGranted && audioGranted) {
            request.grant(requested.toTypedArray())
        } else {
            request.deny()
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
        if (requestCode == WEB_MEDIA_CAPTURE_REQUEST_CODE) {
            resolveWebMediaCapturePermission()
        }
    }

    override fun onDestroy() {
        authoredPostsBridge.stopAll()
        feedGeneration += 1
        feedAuthListener?.let { FirebaseAuth.getInstance().removeAuthStateListener(it) }
        feedAuthListener = null
        ownFeedRegistration?.remove()
        ownFeedRegistration = null
        feedPostsRegistration?.remove()
        feedPostsRegistration = null
        // The incoming-calls Firestore stream was process-lifetime before:
        // registered in onCreate, removed by nobody, streaming through
        // backgrounding and sign-out alike.
        com.apptists.poplur2025.call.CallSessionManager.stopListeningForIncomingCalls()
        // The WebRTC relay has its own incoming, description and candidate
        // listeners. It must not outlive the WebView it posts into.
        com.apptists.poplur2025.call.PoplurWebCallSignalling.stopAll()
        pendingWebMediaPermissionRequest?.deny()
        pendingWebMediaPermissionRequest = null
        activityResumed = false
        activityStarted = false
        inAppBrowserDialog?.setOnDismissListener(null)
        inAppBrowserDialog?.dismiss()
        inAppBrowserDialog = null
        homeParkedForInAppBrowser = false
        // Same reasoning for the DM streams: a thread listener that outlives
        // the activity keeps pushing into a WebView that is being destroyed.
        PoplurDirectMessages.stop()
        stopNotificationsForWeb()
        phoneBootPlayer?.stop()
        phoneBootPlayer?.release()
        phoneBootPlayer = null
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

    private fun playPhoneBootSound() {
        phoneBootPlayer?.stop()
        phoneBootPlayer?.release()
        phoneBootPlayer = null
        val player = MediaPlayer()
        runCatching {
            assets.openFd("popular_phone_bootup.mp3").use { asset ->
                player.setDataSource(asset.fileDescriptor, asset.startOffset, asset.length)
            }
            player.setOnCompletionListener { completed ->
                if (phoneBootPlayer === completed) phoneBootPlayer = null
                completed.release()
            }
            player.setVolume(1f, 1f)
            player.prepare()
            player.start()
            phoneBootPlayer = player
        }.onFailure {
            player.release()
            Log.w(TAG, "Phone boot sound failed", it)
        }
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
        const val WEB_MEDIA_CAPTURE_REQUEST_CODE = 7404
        const val MAX_WEB_CAPTURE_ROUTES = 16
        const val MAX_CAMERA_FRAME_DATA_URL_CHARS = 4_000_000
        const val MAX_CAMERA_FRAME_BYTES = 3_000_000
        const val MAX_CAMERA_FRAME_EDGE = 960
        // storage.rules caps profile_pictures below 8 MiB. Reject locally
        // before allocating an upload Firebase must refuse.
        const val MAX_PROFILE_PHOTO_BYTES = 8_000_000
        const val MAX_PROFILE_PHOTO_DATA_URL_CHARS = 10_700_000
        const val MAX_STORE_CHECKOUT_ITEMS = 20
        const val STORE_PRODUCT_WAIT_MS = 5_000L
        const val PENDING_STORE_CHECKOUT_KEY = "pending_checkout_json"
        // The ground every drop is born discoverable on, earned or not. The
        // same number by the same name in every shell that publishes a post:
        // POST_DISCOVERY_SEED_METERS in the React Native shell's
        // services/posts.ts and in the functions' accountRadius.ts,
        // postDiscoverySeedMeters in iOS's PostUploadService.swift, and
        // CLAIM_RADIUS_SEED_KM in poplur-clay-map's claimRadius.ts. The
        // Firestore create rule allows a seed of the account's earned total
        // plus this, so a post published at exactly the seed is accepted by
        // every account.
        const val POST_DISCOVERY_SEED_METERS = 5_000
    }
}

/** WebRTC candidates are shallow JSON objects, but copy only JSON-native
 *  values before handing them to Firestore rather than trusting the WebView
 *  object wholesale. */
private fun webCallCandidate(source: JSONObject?): Map<String, Any?> {
    if (source == null) return emptyMap()
    val candidate = linkedMapOf<String, Any?>()
    val keys = source.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        val value = source.opt(key)
        if (value !== JSONObject.NULL) candidate[key] = value
    }
    return candidate
}

private fun matchParentParams(): FrameLayout.LayoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
    )
