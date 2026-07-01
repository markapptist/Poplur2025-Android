package com.apptists.poplur2025.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.apptists.poplur2025.R
import com.apptists.poplur2025.ui.screens.ProfileCubeMenuAction
import com.apptists.poplur2025.ui.screens.ProfileCubeMenuCommand
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.apptists.poplur2025.auth.AuthViewModel
import com.apptists.poplur2025.call.CallSessionManager
import com.apptists.poplur2025.data.AIVideoService
import com.apptists.poplur2025.data.AIVideoStyle
import com.apptists.poplur2025.data.AppTab
import com.apptists.poplur2025.data.DemoPostStore
import com.apptists.poplur2025.data.PoplurLanguage
import com.apptists.poplur2025.data.UserProfile
import com.apptists.poplur2025.data.businessProfileOrNull
import com.apptists.poplur2025.data.poplurAccountType
import com.apptists.poplur2025.PoplurVolumeEvents
import com.apptists.poplur2025.ui.screens.*
import com.apptists.poplur2025.ui.components.CubeMenuWheelController
import com.apptists.poplur2025.ui.components.CubeMenuWheelItem
import com.apptists.poplur2025.ui.components.CubeMenuLayout
import com.apptists.poplur2025.ui.components.CubeMenuWheelOverlay
import com.apptists.poplur2025.ui.components.PersistentCubeOverlay
import com.apptists.poplur2025.ui.components.PersistentCubeOverlayHost
import com.apptists.poplur2025.ui.components.PoplurText
import com.apptists.poplur2025.ui.components.FeedLaunchCover
import com.apptists.poplur2025.ui.components.PoplurVolumeBars
import com.apptists.poplur2025.ui.CubeSkin
import com.apptists.poplur2025.ui.PoplurRadioPrewarm
import com.apptists.poplur2025.ui.games.screens.GameLaunchScreen
import com.apptists.poplur2025.util.PoplurEQSync
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

private const val PROFILE_AVATAR_3D_DEFAULT_PREF = "profile_avatar_3d_default_applied"
private const val VOLUME_OVERLAY_MIN_RETRIGGER_MS = 900L
private const val VOLUME_OVERLAY_FAST_UPDATE_MS = 220L
private const val VOLUME_CHANGE_EPSILON = 0.005f

@Composable
fun PoplurRoot(
    authViewModel: AuthViewModel = viewModel(),
    mapViewModel: MapViewModel = viewModel()
) {
    android.util.Log.d("PoplurRoot", "Composing PoplurRoot...")
    BackHandler {}

    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val appContext = remember(context) { context.applicationContext }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val isSignedIn by authViewModel.isSignedIn.collectAsState()
    val settingsPrefs = remember(appContext) {
        appContext.getSharedPreferences("poplur_settings", Context.MODE_PRIVATE)
    }
    
    // --- Shared State (Must be declared before any early returns) ---
    var tab by remember { mutableStateOf(AppTab.Feed) }
    var previousTab by remember { mutableStateOf<AppTab?>(null) }
    var currentPostIndex by remember { mutableIntStateOf(0) }
    var feedTvGuideRequest by remember { mutableIntStateOf(0) }
    var feedTargetChannelQuery by remember { mutableStateOf("") }
    var feedTargetChannelToken by remember { mutableIntStateOf(0) }
    var feedNewsRequest by remember { mutableIntStateOf(0) }
    var feedLikeRequest by remember { mutableIntStateOf(0) }
    var feedOpenProfileRequest by remember { mutableIntStateOf(0) }
    var feedCommentSubmitRequest by remember { mutableIntStateOf(0) }
    var submittedFeedComment by remember { mutableStateOf("") }
    var showFeedCommentComposer by remember { mutableStateOf(false) }
    var preferredLanguageCode by rememberSaveable {
        mutableStateOf(settingsPrefs.getString("preferred_language", PoplurLanguage.English.code) ?: PoplurLanguage.English.code)
    }
    var closedCaptionsEnabled by rememberSaveable {
        mutableStateOf(settingsPrefs.getBoolean("closed_captions_enabled", true))
    }
    val preferredLanguage = PoplurLanguage.fromCode(preferredLanguageCode)
    var selectedCubeSkin by rememberSaveable {
        val storedSkinId = settingsPrefs.getString("selected_cube_skin", null)
        mutableStateOf(
            CubeSkin.entries.find { it.id == storedSkinId } ?: CubeSkin.Gold
        )
    }
    var cubeMenuLayout by rememberSaveable {
        mutableStateOf(
            runCatching {
                CubeMenuLayout.valueOf(
                    settingsPrefs.getString("cube_menu_layout", CubeMenuLayout.Horizontal.name)
                        ?: CubeMenuLayout.Horizontal.name
                )
            }.getOrDefault(CubeMenuLayout.Horizontal)
        )
    }
    var isDarkMode by rememberSaveable {
        mutableStateOf(settingsPrefs.getBoolean("dark_mode", true))
    }
    val initialProfileAvatarIs3D = remember(settingsPrefs) {
        if (!settingsPrefs.getBoolean(PROFILE_AVATAR_3D_DEFAULT_PREF, false)) {
            settingsPrefs.edit()
                .putBoolean("profile_avatar_3d", true)
                .putBoolean(PROFILE_AVATAR_3D_DEFAULT_PREF, true)
                .apply()
            true
        } else {
            settingsPrefs.getBoolean("profile_avatar_3d", true)
        }
    }
    var profileAvatarIs3D by rememberSaveable {
        mutableStateOf(initialProfileAvatarIs3D)
    }
    var tiltControlEnabled by rememberSaveable {
        mutableStateOf(settingsPrefs.getBoolean("tilt_control_enabled", false))
    }
    var showAppInitializationCover by remember { mutableStateOf(true) }
    var showSlowInitializationDetails by remember { mutableStateOf(false) }
    var feedInteractionTick by remember { mutableIntStateOf(0) }
    var isFeedChromeAsleep by remember { mutableStateOf(false) }
    var channelObjectPopToken by remember { mutableIntStateOf(0) }
    var channelObjectId by remember { mutableStateOf("star") }
    var selectedProfile by remember { mutableStateOf(UserProfile()) }
    var currentUserProfile by remember { mutableStateOf(UserProfile()) }
    var cameraPhotoTrigger by remember { mutableIntStateOf(0) }
    var cameraRecordTrigger by remember { mutableIntStateOf(0) }
    var cameraLiveTrigger by remember { mutableIntStateOf(0) }
    var cameraFiltersTrigger by remember { mutableIntStateOf(0) }
    var cameraObjectDetectionEnabled by rememberSaveable { mutableStateOf(false) }
    var isAppVisible by remember { mutableStateOf(true) }
    var filtersVisible by remember { mutableStateOf(false) }
    var isPhoneCallScreenVisible by remember { mutableStateOf(false) }
    var isPhoneActive by remember { mutableStateOf(false) }
    var isProfileSubActive by remember { mutableStateOf(false) }
    var isFeedTvGuideVisible by remember { mutableStateOf(false) }
    var isCubeSurfaceReady by remember { mutableStateOf(false) }
    var isCubeFirstFrameReady by remember { mutableStateOf(false) }
    var profileCubeCommandId by remember { mutableIntStateOf(0) }
    var profileCubeMenuCommand by remember { mutableStateOf<ProfileCubeMenuCommand?>(null) }
    var profileCubeCommittedIconTitle by remember { mutableStateOf<String?>(null) }
    var profilePanelIconTitle by remember { mutableStateOf<String?>(null) }
    var profileSortOrder by remember { mutableStateOf<PoplurAIProfileSortOrder?>(null) }
    var profileSortToken by remember { mutableIntStateOf(0) }
    var profileWakeTutorialPulse by remember { mutableIntStateOf(0) }
    var hasSeenProfileWakeTutorial by rememberSaveable {
        mutableStateOf(settingsPrefs.getBoolean("profile_hey_poplur_tutorial_seen", false))
    }
    var phoneColorwaySelected by remember { mutableStateOf<Color?>(null) }
    var showGames by remember { mutableStateOf(false) }
    
    var capturedMediaUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isCapturedVideo by remember { mutableStateOf(false) }
    var capturePostStep by remember { mutableStateOf(CapturePostStep.Preview) }
    var postCaptureCreationMode by remember { mutableStateOf(PostCaptureCreationMode.Original) }
    var captureFlowOrigin by remember { mutableStateOf(CaptureFlowOrigin.CameraFirst) }
    var pendingCaptureLocation by remember { mutableStateOf<CaptureMapPoint?>(null) }
    var selectedCaptureFilterId by remember { mutableStateOf("original") }
    var selectedCaptureTrimSeconds by remember { mutableFloatStateOf(10f) }
    var selectedCaptureMusicUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedCaptureChannel by remember { mutableStateOf<com.apptists.poplur2025.ui.screens.PostCaptureChannelOption?>(null) }
    var capturedAdBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var capturedAdVideoUrl by remember { mutableStateOf<String?>(null) }
    var capturedAdMeta by remember { mutableStateOf<com.apptists.poplur2025.data.TVStyleMetadata?>(null) }

    // AI Video Studio state
    var aiVideoPrompt by remember { mutableStateOf("") }
    var aiVideoStyle by remember { mutableStateOf(AIVideoStyle.PoplurTV) }
    var aiVideoLocationName by remember { mutableStateOf("") }

    val persistentCubeHost = context as? PersistentCubeOverlayHost

    val shouldHideCube = !isCubeSurfaceReady || isSignedIn == false
    LaunchedEffect(shouldHideCube) {
        if (shouldHideCube) persistentCubeHost?.hidePersistentCubeOverlay()
    }

    LaunchedEffect(preferredLanguageCode) {
        settingsPrefs.edit().putString("preferred_language", preferredLanguageCode).apply()
    }

    LaunchedEffect(closedCaptionsEnabled) {
        settingsPrefs.edit().putBoolean("closed_captions_enabled", closedCaptionsEnabled).apply()
    }

    LaunchedEffect(selectedCubeSkin) {
        settingsPrefs.edit()
            .putString("selected_cube_skin", selectedCubeSkin.id)
            .apply()
        // Applying icon changes can sometimes cause process restarts on certain devices.
        // Doing it here ensures it only happens when explicitly changed by the user,
        // rather than on every Application.onCreate() call.
        runCatching {
            AppIconThemeManager.apply(context, selectedCubeSkin)
        }
    }
    LaunchedEffect(cubeMenuLayout) {
        settingsPrefs.edit().putString("cube_menu_layout", cubeMenuLayout.name).apply()
    }
    LaunchedEffect(isDarkMode) {
        settingsPrefs.edit().putBoolean("dark_mode", isDarkMode).apply()
    }
    LaunchedEffect(profileAvatarIs3D) {
        settingsPrefs.edit().putBoolean("profile_avatar_3d", profileAvatarIs3D).apply()
    }
    LaunchedEffect(tiltControlEnabled) {
        settingsPrefs.edit().putBoolean("tilt_control_enabled", tiltControlEnabled).apply()
    }

    // --- Tab and Swipe Actions ---
    val onTabChange: (Int) -> Unit = { index ->
        val newTab = when (index) {
            0 -> AppTab.Map
            1 -> AppTab.CreateEntry
            2 -> AppTab.Feed
            3 -> AppTab.Profile
            else -> AppTab.Feed
        }
        if (tab != newTab) {
            previousTab = tab
            tab = newTab
        }
    }

    // Accelerometer tilt control - tab snap + cube lean
    val currentTiltTabChange by rememberUpdatedState(onTabChange)
    val currentTab by rememberUpdatedState(tab)
    DisposableEffect(tiltControlEnabled) {
        if (!tiltControlEnabled) {
            GdxState.tiltYawOffset = 0f
            return@DisposableEffect onDispose {}
        }
        val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as? android.hardware.SensorManager
        val accel = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
        if (sensorManager == null || accel == null) return@DisposableEffect onDispose {}

        val lastSnapMs = java.util.concurrent.atomic.AtomicLong(0L)
        val handler = android.os.Handler(android.os.Looper.getMainLooper())

        val listener = object : android.hardware.SensorEventListener {
            override fun onSensorChanged(event: android.hardware.SensorEvent) {
                val x = event.values[0] // positive = tilt left, negative = tilt right (portrait)
                // Lean: max ~15° at 45° device tilt, clamped so it doesn't rotate too far
                GdxState.tiltYawOffset = (-x * 0.55f).coerceIn(-15f, 15f)

                val absX = kotlin.math.abs(x)
                val threshold = 4.5f
                val cooldownMs = 1500L
                val now = System.currentTimeMillis()
                if (absX < threshold || now - lastSnapMs.get() < cooldownMs) return
                lastSnapMs.set(now)

                // x > 0 = tilted left, x < 0 = tilted right.
                val direction = if (x > 0f) -1 else 1
                val tabOrder = listOf(AppTab.Map, AppTab.CreateEntry, AppTab.Feed, AppTab.Profile)
                val currentIndex = tabOrder.indexOf(currentTab).takeIf { it >= 0 } ?: 2
                val newIndex = (currentIndex + direction + tabOrder.size) % tabOrder.size
                handler.post { currentTiltTabChange(newIndex) }
            }
            override fun onAccuracyChanged(sensor: android.hardware.Sensor, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, accel, android.hardware.SensorManager.SENSOR_DELAY_GAME)
        onDispose {
            sensorManager.unregisterListener(listener)
            GdxState.tiltYawOffset = 0f
        }
    }

    // --- Volume State ---
    var deviceVolumeFraction by remember(appContext) { mutableFloatStateOf(readDeviceMusicVolumeFraction(appContext)) }
    var showVolumeBars by remember { mutableStateOf(false) }
    var volumeShowTrigger by remember { mutableIntStateOf(0) }
    var lastVolumeShowUptimeMs by remember { mutableLongStateOf(0L) }
    var isVolumePopReady by remember { mutableStateOf(false) }
    val volumePopPlayer = remember(appContext) {
        MediaPlayer().apply {
            try {
                val afd = appContext.assets.openFd("pop.mp3")
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                setOnPreparedListener { isVolumePopReady = true }
                prepareAsync()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    val playVolumePopSound = volumePop@{
        if (!PoplurEQSync.shouldPlayTransientSound(minimumIntervalMs = 180L)) return@volumePop
        try {
            if (isVolumePopReady) {
                if (volumePopPlayer.isPlaying) volumePopPlayer.pause()
                volumePopPlayer.seekTo(0)
                volumePopPlayer.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    DisposableEffect(volumePopPlayer) {
        onDispose { volumePopPlayer.release() }
    }

    fun triggerVolumeBars(forceShow: Boolean, playPop: Boolean) {
        val now = SystemClock.elapsedRealtime()
        val nextVolume = readDeviceMusicVolumeFraction(appContext)
        val changed = abs(nextVolume - deviceVolumeFraction) > VOLUME_CHANGE_EPSILON
        if (!forceShow && !changed) return

        if (showVolumeBars && now - lastVolumeShowUptimeMs < VOLUME_OVERLAY_FAST_UPDATE_MS) {
            deviceVolumeFraction = nextVolume
            return
        }

        val duplicateForcedEvent = forceShow &&
            !changed &&
            now - lastVolumeShowUptimeMs < VOLUME_OVERLAY_MIN_RETRIGGER_MS
        if (duplicateForcedEvent) return

        deviceVolumeFraction = nextVolume
        lastVolumeShowUptimeMs = now
        volumeShowTrigger++
        if (playPop) playVolumePopSound()
    }

    DisposableEffect(appContext) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == SYSTEM_VOLUME_CHANGED_ACTION) {
                    triggerVolumeBars(forceShow = false, playPop = false)
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(SYSTEM_VOLUME_CHANGED_ACTION)
        }
        runCatching {
            ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        }
        onDispose {
            runCatching { appContext.unregisterReceiver(receiver) }
        }
    }

    DisposableEffect(appContext) {
        val listener = {
            triggerVolumeBars(forceShow = true, playPop = true)
            Unit
        }
        PoplurVolumeEvents.addListener(listener)
        onDispose {
            PoplurVolumeEvents.removeListener(listener)
        }
    }

    LaunchedEffect(volumeShowTrigger) {
        if (volumeShowTrigger > 0) {
            showVolumeBars = true
            kotlinx.coroutines.delay(1_100)
            showVolumeBars = false
        }
    }

    // Warm up the TTS engine early so map radio starts without delay
    val prewarmContext = LocalContext.current
    LaunchedEffect(Unit) {
        PoplurRadioPrewarm.start(prewarmContext)
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1_600)
        if (showAppInitializationCover) {
            showSlowInitializationDetails = true
        }
    }

    LaunchedEffect(isSignedIn) {
        when (isSignedIn) {
            true -> {
                showAppInitializationCover = true
                showSlowInitializationDetails = false
                isCubeSurfaceReady = true
            }
            false -> {
                isCubeSurfaceReady = false
                isCubeFirstFrameReady = false
                showAppInitializationCover = false
                showSlowInitializationDetails = false
            }
            null -> {
                // Wait for auth to resolve before starting heavy GL surfaces.
                isCubeSurfaceReady = false
            }
        }
    }

    LaunchedEffect(isSignedIn, isCubeFirstFrameReady) {
        if (isSignedIn == true && isCubeFirstFrameReady) {
            android.util.Log.d("PoplurRoot", "Cube first frame ready; hiding loading cover")
            showAppInitializationCover = false
            showSlowInitializationDetails = false
        }
    }

    LaunchedEffect(isSignedIn) {
        if (isSignedIn == false) {
            // Auth resolved as signed-out — reveal WelcomeScreen faster.
            kotlinx.coroutines.delay(150)
            if (isSignedIn == false) {
                showAppInitializationCover = false
                showSlowInitializationDetails = false
            }
        }
    }

    LaunchedEffect(isSignedIn) {
        if (isSignedIn != true) {
            currentUserProfile = UserProfile()
            return@LaunchedEffect
        }

        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return@LaunchedEffect
        val defaultProfile = UserProfile()
        val fallbackUsername = firebaseUser.email
            ?.substringBefore("@")
            ?.takeIf { it.isNotBlank() }
            ?: defaultProfile.username
        val fallbackProfile = defaultProfile.copy(
            id = firebaseUser.uid,
            username = fallbackUsername,
            displayName = firebaseUser.displayName?.takeIf { it.isNotBlank() }
                ?: fallbackUsername.toDisplayNameFallback(),
            avatarUrl = firebaseUser.photoUrl?.toString()
        )

        currentUserProfile = fallbackProfile
        if (selectedProfile.id == defaultProfile.id || selectedProfile.id == firebaseUser.uid) {
            selectedProfile = fallbackProfile
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(firebaseUser.uid)
            .get()
            .addOnSuccessListener { document ->
                val profileData = document.data.orEmpty()
                val docRadius = document.numberValue("radius", "totalRadius", "totalRadiusMeters")
                val docPosts = document.numberValue("posts", "postCount")
                val profileFromDocument = currentUserProfile.copy(
                    id = firebaseUser.uid,
                    username = document.getString("username")
                        ?: document.getString("handle")
                        ?: currentUserProfile.username,
                    displayName = document.getString("displayName")
                        ?: document.getString("name")
                        ?: currentUserProfile.displayName,
                    bio = document.getString("bio") ?: currentUserProfile.bio,
                    avatarUrl = document.getString("avatarUrl")
                        ?: document.getString("photoURL")
                        ?: currentUserProfile.avatarUrl,
                    radius = docRadius ?: currentUserProfile.radius,
                    followers = document.numberValue("followers", "followerCount") ?: currentUserProfile.followers,
                    posts = docPosts ?: currentUserProfile.posts,
                    accountType = profileData.poplurAccountType(),
                    businessProfile = profileData.businessProfileOrNull()
                )
                currentUserProfile = profileFromDocument
                if (selectedProfile.id == defaultProfile.id || selectedProfile.id == firebaseUser.uid) {
                    selectedProfile = profileFromDocument
                }
            }
            .addOnFailureListener { error ->
                android.util.Log.w("PoplurRoot", "Could not load current user profile", error)
            }

        db.collection("posts")
            .whereEqualTo("userID", firebaseUser.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val uniqueDocuments = snapshot.documents.distinctBy { it.id }
                val totalRadius = uniqueDocuments.sumOf { document ->
                    document.numberValue("radius") ?: 0
                }
                val profileWithRadius = currentUserProfile.copy(
                    radius = totalRadius.coerceAtLeast(0),
                    posts = uniqueDocuments.size
                )
                currentUserProfile = profileWithRadius
                if (selectedProfile.id == defaultProfile.id || selectedProfile.id == firebaseUser.uid) {
                    selectedProfile = profileWithRadius
                }
            }
            .addOnFailureListener { error ->
                android.util.Log.w("PoplurRoot", "Could not load current radius", error)
            }
    }

    LaunchedEffect(isSignedIn) {
        val ready = isSignedIn == true
        android.util.Log.d("PoplurRoot", "Cube Readiness Update: ready=$ready (isSignedIn=$isSignedIn, cover=$showAppInitializationCover)")
        if (isSignedIn == false) isCubeSurfaceReady = false
    }

    // --- Lifecycle and UI State ---
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isAppVisible = when (event) {
                Lifecycle.Event.ON_RESUME -> true
                Lifecycle.Event.ON_STOP -> false
                else -> isAppVisible
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var isMediaPlayerReady by remember { mutableStateOf(false) }
    val mediaPlayer = remember {
        MediaPlayer().apply {
            try {
                val afd = context.assets.openFd("pop.mp3")
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                setOnPreparedListener { isMediaPlayerReady = true }
                prepareAsync()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    DisposableEffect(Unit) { onDispose { mediaPlayer.release() } }

    val playPopMedia = {
        try {
            if (isMediaPlayerReady) {
                if (mediaPlayer.isPlaying) mediaPlayer.pause()
                mediaPlayer.seekTo(0)
                mediaPlayer.start()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
    val playPopSound = {
        if (PoplurEQSync.shouldPlayTransientSound(minimumIntervalMs = 160L)) {
            playPopMedia()
        }
    }

    // --- Cube Menu Controller ---
    val cubeMenuWheelController = remember(scope) {
        CubeMenuWheelController(scope = scope) {
            if (PoplurEQSync.shouldPlayMenuTick()) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                playPopMedia()
            }
        }
    }
    
    val backItem = CubeMenuWheelItem(
        label = "Back",
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        accent = Color.White,
        onSelected = {
            if (tab == AppTab.Profile && isProfileSubActive) {
                profileCubeCommandId += 1
                profileCubeMenuCommand = ProfileCubeMenuCommand(profileCubeCommandId, ProfileCubeMenuAction.GoHome)
            } else {
                val destination = previousTab ?: AppTab.Feed
                tab = destination
                if (destination == AppTab.Profile) {
                    profileCubeCommittedIconTitle = null
                    profilePanelIconTitle = null
                    isProfileSubActive = false
                    isPhoneActive = false
                }
            }
        }
    )

    val phoneColorWheelItems = listOf(
        Triple("Silver", Color(0xFFE2E2D8), R.drawable.poplur_bling_phone_01_silver_diamond),
        Triple("Gold", Color(0xFFF4B846), R.drawable.poplur_bling_phone_02_champagne_gold),
        Triple("Rose", Color(0xFFFF8CA1), R.drawable.poplur_bling_phone_03_rose_gold_pink),
        Triple("Ruby", Color(0xFFEA1010), R.drawable.poplur_bling_phone_04_ruby_red),
        Triple("Blue", Color(0xFF0B54F2), R.drawable.poplur_bling_phone_05_sapphire_blue),
        Triple("Green", Color(0xFF00BD40), R.drawable.poplur_bling_phone_06_emerald_green),
        Triple("Purple", Color(0xFF8F3AEB), R.drawable.poplur_bling_phone_07_amethyst_purple),
        Triple("Onyx", Color(0xFF151518), R.drawable.poplur_bling_phone_08_jet_black_onyx),
        Triple("Aqua", Color(0xFF33EBF2), R.drawable.poplur_bling_phone_09_icy_aqua_turquoise),
        Triple("Holo", Color(0xFFDBA3FF), R.drawable.poplur_bling_phone_10_holographic_iridescent)
    ).map { (label, color, resId) ->
        CubeMenuWheelItem(
            label = label,
            icon = Icons.Default.Circle,
            accent = color,
            onSelected = { phoneColorwaySelected = color }
        )
    }

    val profileCubeIconTitle by remember {
        derivedStateOf {
            val preview = if (tab in profileCubeTabs && cubeMenuWheelController.isVisible) {
                cubeMenuWheelController.selectedItem?.label
            } else {
                null
            }
            preview ?: profileCubeCommittedIconTitle ?: profilePanelIconTitle ?: "Profile"
        }
    }

    val mapCubeFaceTitle by remember {
        derivedStateOf {
            if (tab == AppTab.Map && cubeMenuWheelController.isVisible) {
                cubeMenuWheelController.selectedItem?.label ?: "Map"
            } else {
                "Map"
            }
        }
    }

    val cameraCubeFaceTitle by remember {
        derivedStateOf {
            if (tab in cameraCubeTabs && cubeMenuWheelController.isVisible) {
                cubeMenuWheelController.selectedItem?.label ?: "Camera"
            } else {
                when (tab) {
                    AppTab.CreateEntry -> "Create"
                    AppTab.CapturedOutput -> "Preview"
                    AppTab.AIVideoStudio -> "Poplur AI"
                    AppTab.AIVideoProgress -> "Generating"
                    AppTab.Camera -> "Camera"
                    else -> "Camera"
                }
            }
        }
    }

    val feedCubeFaceTitle by remember {
        derivedStateOf {
            if (tab == AppTab.Feed && cubeMenuWheelController.isVisible) {
                cubeMenuWheelController.selectedItem?.label ?: "Feed"
            } else if (tab == AppTab.Feed && showFeedCommentComposer) {
                "Comment"
            } else if (tab == AppTab.Feed && isFeedTvGuideVisible) {
                "TV Guide"
            } else {
                "Feed"
            }
        }
    }

    // --- Swipe Actions ---
    val onVerticalSwipe: (Boolean) -> Unit = { isUp ->
        if (tab == AppTab.Feed) {
            feedInteractionTick++
            if (isUp) currentPostIndex++ else currentPostIndex--
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            playPopSound()
        }
    }

    val sendProfileCubeCommand: (ProfileCubeMenuAction) -> Unit = { action ->
        profileCubeCommittedIconTitle = when (action) {
            ProfileCubeMenuAction.Phone -> "Phone"
            ProfileCubeMenuAction.Notifications -> "Alerts"
            ProfileCubeMenuAction.Store -> "Store"
            ProfileCubeMenuAction.Stats -> "Stats"
            ProfileCubeMenuAction.GoHome -> null
        }
        profileCubeCommandId += 1
        profileCubeMenuCommand = ProfileCubeMenuCommand(profileCubeCommandId, action)
    }

    val openPoplurAIPage: (String) -> Unit = { rawPage ->
        val page = rawPage.lowercase(Locale.US)
        when {
            "tv" in page -> {
                previousTab = tab
                tab = AppTab.Feed
                feedTvGuideRequest++
            }
            "message" in page || "mail" in page || "dm" in page -> {
                profileCubeCommittedIconTitle = "Mail"
                previousTab = AppTab.Profile
                tab = AppTab.Messages
            }
            "setting" in page -> {
                profileCubeCommittedIconTitle = "Settings"
                previousTab = AppTab.Profile
                tab = AppTab.Settings
            }
            "stat" in page || "analytic" in page -> {
                previousTab = tab
                tab = AppTab.Profile
                sendProfileCubeCommand(ProfileCubeMenuAction.Stats)
            }
            "camera" in page || "record" in page -> {
                previousTab = tab
                tab = AppTab.Camera
            }
            "map" in page -> {
                previousTab = tab
                tab = AppTab.Map
            }
            "profile" in page -> {
                previousTab = tab
                tab = AppTab.Profile
            }
            "feed" in page -> {
                previousTab = tab
                tab = AppTab.Feed
            }
        }
    }

    val handlePoplurAICommand: (PoplurAICommand) -> Unit = { command ->
        when (command) {
            is PoplurAICommand.OpenTab -> {
                previousTab = tab
                tab = command.tab
            }
            is PoplurAICommand.OpenPage -> openPoplurAIPage(command.page)
            is PoplurAICommand.GoToChannel -> {
                previousTab = tab
                tab = AppTab.Feed
                feedTargetChannelQuery = command.query
                feedTargetChannelToken++
            }
            is PoplurAICommand.PostToLocation -> {
                previousTab = tab
                tab = AppTab.Map
                pendingCaptureLocation = null
                capturedMediaUri = null
                captureFlowOrigin = CaptureFlowOrigin.MapFirst
                capturePostStep = CapturePostStep.Preview
                scope.launch { mapViewModel.triggerPostToMap(command.location) }
            }
            is PoplurAICommand.Camera -> {
                previousTab = tab
                tab = AppTab.Camera
                when (command.mode) {
                    PoplurAICameraMode.Open -> Unit
                    PoplurAICameraMode.Photo -> cameraPhotoTrigger++
                    PoplurAICameraMode.Video -> cameraRecordTrigger++
                }
            }
            is PoplurAICommand.SetTourActive -> {
                previousTab = tab
                tab = AppTab.Map
                mapViewModel.setTourPaused(!command.active)
            }
            PoplurAICommand.Logout -> authViewModel.signOut()
            is PoplurAICommand.SortPosts -> {
                previousTab = tab
                tab = AppTab.Profile
                profileSortOrder = command.order
                profileSortToken++
            }
            PoplurAICommand.ShowAnalytics -> {
                previousTab = tab
                tab = AppTab.Profile
                sendProfileCubeCommand(ProfileCubeMenuAction.Stats)
            }
            is PoplurAICommand.Unknown -> Unit
        }
    }

    val onLongPressCube: () -> Unit = {
        if (tab == AppTab.Feed) {
            feedInteractionTick++
            cubeMenuWheelController.beginHold(
                listOf(
                    CubeMenuWheelItem(
                        label = "Like",
                        icon = Icons.Default.Favorite,
                        accent = Color(0xFFFF365E),
                        onSelected = {
                            feedInteractionTick++
                            feedLikeRequest++
                        }
                    ),
                    CubeMenuWheelItem(
                        label = "Comment",
                        icon = Icons.AutoMirrored.Filled.Send,
                        accent = Color(0xFFFFD91A),
                        onSelected = {
                            feedInteractionTick++
                            isFeedChromeAsleep = true
                            showFeedCommentComposer = true
                        }
                    ),
                    CubeMenuWheelItem(
                        label = "Profile",
                        icon = Icons.Default.Circle,
                        accent = Color(0xFF7076FF),
                        onSelected = {
                            feedInteractionTick++
                            feedOpenProfileRequest++
                        }
                    ),
                    CubeMenuWheelItem(
                        label = if (closedCaptionsEnabled) "CC Off" else "CC On",
                        icon = Icons.Default.ClosedCaption,
                        accent = Color(0xFF18D8E8),
                        onSelected = {
                            feedInteractionTick++
                            closedCaptionsEnabled = !closedCaptionsEnabled
                        }
                    ),
                    CubeMenuWheelItem(
                        label = "News",
                        icon = Icons.AutoMirrored.Filled.Article,
                        accent = Color(0xFFFF3333),
                        onSelected = {
                            feedInteractionTick++
                            feedNewsRequest++
                        }
                    ),
                    CubeMenuWheelItem(
                        label = "TV Guide",
                        icon = Icons.Default.Tv,
                        accent = Color(0xFF4DFF58),
                        onSelected = {
                            feedInteractionTick++
                            feedTvGuideRequest++
                        }
                    )
                )
            )
        } else if (tab == AppTab.Map) {
            val tourPaused = mapViewModel.isTourPaused.value
            val use2D = mapViewModel.use2DMap.value
            cubeMenuWheelController.beginHold(
                listOf(
                    CubeMenuWheelItem(
                        label = "Search",
                        icon = Icons.Default.Search,
                        accent = Color(0xFF4DFF58),
                        onSelected = { scope.launch { mapViewModel.triggerOpenSearch() } }
                    ),
                    CubeMenuWheelItem(
                        label = "Location",
                        icon = Icons.Default.MyLocation,
                        accent = Color(0xFF18D8E8),
                        onSelected = { scope.launch { mapViewModel.triggerCenterOnUser() } }
                    ),
                    CubeMenuWheelItem(
                        label = "Post Here",
                        icon = Icons.Default.PhotoCamera,
                        accent = Color(0xFFFF2D8A),
                        onSelected = { scope.launch { mapViewModel.triggerPostToMap() } }
                    ),
                    CubeMenuWheelItem(
                        label = if (use2D) "3D Map" else "2D Map",
                        icon = Icons.Default.Map,
                        accent = Color(0xFF5B9BFF),
                        onSelected = { mapViewModel.toggleMapMode() }
                    ),
                    CubeMenuWheelItem(
                        label = if (tourPaused) "Start Tour" else "Stop Tour",
                        icon = Icons.Default.Explore,
                        accent = Color(0xFFFF8C1A),
                        onSelected = { mapViewModel.setTourPaused(!tourPaused) }
                    ),
                    CubeMenuWheelItem(
                        label = "Message",
                        icon = Icons.Default.Message,
                        accent = Color(0xFFFFD91A),
                        onSelected = { scope.launch { mapViewModel.triggerAddMessage() } }
                    )
                )
            )
        } else if (tab == AppTab.Camera) {
            cubeMenuWheelController.beginHold(
                listOf(
                    CubeMenuWheelItem(
                        label = "Photo",
                        icon = Icons.Default.PhotoCamera,
                        accent = Color(0xFF4DFF58),
                        onSelected = { cameraPhotoTrigger++ }
                    ),
                    CubeMenuWheelItem(
                        label = "Video",
                        icon = Icons.Default.Videocam,
                        accent = Color(0xFF7076FF),
                        onSelected = { cameraRecordTrigger++ }
                    ),
                    CubeMenuWheelItem(
                        label = "Live",
                        icon = Icons.Default.CellTower,
                        accent = Color(0xFFFF3333),
                        onSelected = { cameraLiveTrigger++ }
                    ),
                    CubeMenuWheelItem(
                        label = "AR",
                        icon = Icons.Default.AutoAwesome,
                        accent = Color(0xFF18D8E8),
                        onSelected = { cameraFiltersTrigger++ }
                    ),
                    CubeMenuWheelItem(
                        label = if (cameraObjectDetectionEnabled) "Detect On" else "Detect Off",
                        icon = Icons.Default.Search,
                        secondaryIcon = Icons.Default.Visibility,
                        accent = if (cameraObjectDetectionEnabled) Color(0xFF4DFF58) else Color(0xFFFF8C1A),
                        onSelected = { cameraObjectDetectionEnabled = !cameraObjectDetectionEnabled }
                    ),
                    CubeMenuWheelItem(
                        label = "Games",
                        icon = Icons.Default.SportsEsports,
                        accent = Color(0xFFAA44FF),
                        onSelected = { showGames = true }
                    )
                )
            )
        } else if (tab == AppTab.Profile) {
            val profileMenuItems = listOf(
                CubeMenuWheelItem(
                    label = "Mail",
                    icon = Icons.Default.Email,
                    accent = Color(0xFF4DFF58),
                    onSelected = {
                        previousTab = AppTab.Profile
                        tab = AppTab.Messages
                    }
                ),
                CubeMenuWheelItem(
                    label = "Settings",
                    icon = Icons.Default.Settings,
                    accent = Color(0xFF7076FF),
                    onSelected = {
                        previousTab = AppTab.Profile
                        tab = AppTab.Settings
                    }
                ),
                CubeMenuWheelItem(
                    label = "Stats",
                    icon = Icons.Default.Circle,
                    accent = Color(0xFF18D8E8),
                    onSelected = { sendProfileCubeCommand(ProfileCubeMenuAction.Stats) }
                ),
                CubeMenuWheelItem(
                    label = "Alerts",
                    icon = Icons.Default.Notifications,
                    accent = Color(0xFFFF8C1A),
                    onSelected = { sendProfileCubeCommand(ProfileCubeMenuAction.Notifications) }
                ),
                CubeMenuWheelItem(
                    label = "Phone",
                    icon = Icons.Default.Phone,
                    accent = Color(0xFF18D8E8),
                    onSelected = {
                        isPhoneActive = true
                        sendProfileCubeCommand(ProfileCubeMenuAction.Phone)
                    }
                ),
                CubeMenuWheelItem(
                    label = "Store",
                    icon = Icons.Default.ShoppingCart,
                    accent = Color(0xFFFF8C1A),
                    onSelected = { sendProfileCubeCommand(ProfileCubeMenuAction.Store) }
                )
            )
            if (isPhoneActive) {
                cubeMenuWheelController.beginHold(listOf(backItem) + phoneColorWheelItems)
            } else if (isProfileSubActive) {
                cubeMenuWheelController.beginHold(listOf(backItem) + profileMenuItems)
            } else {
                cubeMenuWheelController.beginHold(profileMenuItems)
            }
        } else if (tab == AppTab.Messages || tab == AppTab.Settings || tab == AppTab.CubeDebug || tab == AppTab.CapturedOutput ||
            tab == AppTab.CreateEntry || tab == AppTab.AIVideoStudio || tab == AppTab.AIVideoProgress) {
            cubeMenuWheelController.beginHold(listOf(backItem))
        }
    }

    val onLongPressEndCube: () -> Unit = {
        if (tab == AppTab.Feed || tab == AppTab.Camera || tab == AppTab.Profile || tab == AppTab.Map ||
            tab == AppTab.Messages || tab == AppTab.Settings || tab == AppTab.CubeDebug || tab == AppTab.CapturedOutput ||
            tab == AppTab.CreateEntry || tab == AppTab.AIVideoStudio || tab == AppTab.AIVideoProgress) {
            if (tab == AppTab.Profile) {
                profileCubeCommittedIconTitle = cubeMenuWheelController.selectedItem?.label
            }
            cubeMenuWheelController.endHold(activateSelected = true)
        }
    }

    val onLongPressMoveCube: (Float, Float) -> Unit = { deltaX, deltaY ->
        cubeMenuWheelController.scrub(deltaX = deltaX, deltaY = deltaY, layout = cubeMenuLayout)
    }

    val onTapCube: () -> Unit = {
        if (tab == AppTab.Camera) {
            cameraPhotoTrigger++
        }
    }

    val currentOnVerticalSwipe by rememberUpdatedState(onVerticalSwipe)
    val currentOnLongPressCube by rememberUpdatedState(onLongPressCube)
    val currentOnLongPressEndCube by rememberUpdatedState(onLongPressEndCube)
    val currentOnTapCube by rememberUpdatedState(onTapCube)
    val currentCubeTabChange by rememberUpdatedState(onTabChange)

    // --- Side Effects ---
    LaunchedEffect(tab) {
        if (tab != AppTab.Feed) {
            showFeedCommentComposer = false
        }
        if (tab != AppTab.Profile) {
            cubeMenuWheelController.endHold(activateSelected = false)
            isProfileSubActive = false
            isPhoneActive = false
        }
        if (tab == AppTab.Profile && profilePanelIconTitle == null && !cubeMenuWheelController.isVisible) {
            profileCubeCommittedIconTitle = null
        } else if (tab != AppTab.Profile && tab != AppTab.Messages && tab != AppTab.Settings) {
            profileCubeCommittedIconTitle = null
        }
    }

    LaunchedEffect(tab, isFeedTvGuideVisible, showFeedCommentComposer) {
        isFeedChromeAsleep = tab == AppTab.Feed && (showFeedCommentComposer || isFeedTvGuideVisible)
    }
    
    // Start listening for incoming calls as soon as the user is signed in.
    LaunchedEffect(isSignedIn) {
        if (isSignedIn == true) {
            CallSessionManager.startListeningForIncomingCalls()
        }
    }

    val callState by CallSessionManager.callState.collectAsState()

    // --- UI Structure ---
    Box(Modifier.fillMaxSize()) {
        if (!showAppInitializationCover) {
            if (isSignedIn == false) {
                // Signed Out
                WelcomeAuthScreen(authViewModel)
            } else if (isSignedIn == true) {
                // Signed In
                Box(Modifier.fillMaxSize().zIndex(0f)) {
                    SignedInContent(
                        tab = tab,
                        currentPostIndex = currentPostIndex,
                        isAppVisible = isAppVisible,
                        feedTvGuideRequest = feedTvGuideRequest,
                        feedTargetChannelQuery = feedTargetChannelQuery,
                        feedTargetChannelToken = feedTargetChannelToken,
                        feedNewsRequest = feedNewsRequest,
                        feedLikeRequest = feedLikeRequest,
                        feedOpenProfileRequest = feedOpenProfileRequest,
                        feedCommentSubmitRequest = feedCommentSubmitRequest,
                        submittedFeedComment = submittedFeedComment,
                        showFeedCommentComposer = showFeedCommentComposer,
                        preferredLanguage = preferredLanguage,
                        closedCaptionsEnabled = closedCaptionsEnabled,
                        isFeedChromeAsleep = isFeedChromeAsleep,
                        selectedProfile = selectedProfile,
                        currentUserProfile = currentUserProfile,
                        profileCubeCommand = profileCubeMenuCommand,
                        profileAvatarIs3D = profileAvatarIs3D,
                        selectedCubeSkin = selectedCubeSkin,
                        profileSortOrder = profileSortOrder,
                        profileSortToken = profileSortToken,
                        hasSeenProfileWakeTutorial = hasSeenProfileWakeTutorial,
                        profileWakeTutorialPulse = profileWakeTutorialPulse,
                        capturedMediaUri = capturedMediaUri,
                        capturePostStep = capturePostStep,
                        isCapturedVideo = isCapturedVideo,
                        selectedCaptureFilterId = selectedCaptureFilterId,
                        selectedCaptureTrimSeconds = selectedCaptureTrimSeconds,
                        selectedCaptureMusicUri = selectedCaptureMusicUri,
                        postCaptureCreationMode = postCaptureCreationMode,
                        capturedAdBitmap = capturedAdBitmap,
                        capturedAdVideoUrl = capturedAdVideoUrl,
                        capturedAdMeta = capturedAdMeta,
                        pendingCaptureLocation = pendingCaptureLocation,
                        captureFlowOrigin = captureFlowOrigin,
                        cameraPhotoTrigger = cameraPhotoTrigger,
                        cameraRecordTrigger = cameraRecordTrigger,
                        cameraLiveTrigger = cameraLiveTrigger,
                        cameraFiltersTrigger = cameraFiltersTrigger,
                        cameraObjectDetectionEnabled = cameraObjectDetectionEnabled,
                        aiVideoPrompt = aiVideoPrompt,
                        aiVideoStyle = aiVideoStyle,
                        aiVideoLocationName = aiVideoLocationName,
                        previousTab = previousTab,
                        authViewModel = authViewModel,
                        mapViewModel = mapViewModel,
                        scope = scope,
                        settingsPrefs = settingsPrefs,
                        playPopSound = playPopSound,
                        onTabChange = { tab = it },
                        onPreviousTabChange = { previousTab = it },
                        onCurrentPostIndexChange = { currentPostIndex = it },
                        onFeedTvGuideRequestChange = { feedTvGuideRequest = it },
                        onFeedTargetChannelQueryChange = { feedTargetChannelQuery = it },
                        onFeedTargetChannelTokenChange = { feedTargetChannelToken = it },
                        onFeedNewsRequestChange = { feedNewsRequest = it },
                        onFeedLikeRequestChange = { feedLikeRequest = it },
                        onFeedOpenProfileRequestChange = { feedOpenProfileRequest = it },
                        onFeedCommentSubmitRequestChange = { feedCommentSubmitRequest = it },
                        onSubmittedFeedCommentChange = { submittedFeedComment = it },
                        onShowFeedCommentComposerChange = { showFeedCommentComposer = it },
                        onSelectedProfileChange = { selectedProfile = it },
                        onProfileCubeCommittedIconTitleChange = { profileCubeCommittedIconTitle = it },
                        onProfilePanelIconTitleChange = { profilePanelIconTitle = it },
                        onPhoneColorwaySelectedChange = { phoneColorwaySelected = it },
                        onIsPhoneActiveChange = { isPhoneActive = it },
                        onIsProfileSubActiveChange = { isProfileSubActive = it },
                        onIsPhoneCallScreenVisibleChange = { isPhoneCallScreenVisible = it },
                        onHasSeenProfileWakeTutorialChange = { hasSeenProfileWakeTutorial = it },
                        onProfileWakeTutorialPulseChange = { profileWakeTutorialPulse = it },
                        onCapturedMediaUriChange = { capturedMediaUri = it },
                        onCapturePostStepChange = { capturePostStep = it },
                        onIsCapturedVideoChange = { isCapturedVideo = it },
                        onSelectedCaptureFilterIdChange = { selectedCaptureFilterId = it },
                        onSelectedCaptureTrimSecondsChange = { selectedCaptureTrimSeconds = it },
                        onSelectedCaptureMusicUriChange = { selectedCaptureMusicUri = it },
                        onPostCaptureCreationModeChange = { postCaptureCreationMode = it },
                        onCapturedAdBitmapChange = { capturedAdBitmap = it },
                        onCapturedAdVideoUrlChange = { capturedAdVideoUrl = it },
                        onCapturedAdMetaChange = { capturedAdMeta = it },
                        onPendingCaptureLocationChange = { pendingCaptureLocation = it },
                        onCaptureFlowOriginChange = { captureFlowOrigin = it },
                        onCameraPhotoTriggerChange = { cameraPhotoTrigger = it },
                        onCameraFiltersToggle = { filtersVisible = it },
                        onChannelObjectPop = { objectId ->
                            channelObjectId = objectId
                            channelObjectPopToken++
                        },
                        onAiVideoPromptChange = { aiVideoPrompt = it },
                        onAiVideoStyleChange = { aiVideoStyle = it },
                        onAiVideoLocationNameChange = { aiVideoLocationName = it },
                        onIsFeedTvGuideVisibleChange = { isFeedTvGuideVisible = it },
                        onPhoneColorwaySelected = phoneColorwaySelected
                    )
                }

            }
        } else {
            FeedLaunchCover(
                onPopSound = playPopSound,
                showSlowDetails = showSlowInitializationDetails,
                skinLogoPath = selectedCubeSkin.logoAssetPath,
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── Incoming call overlay ─────────────────
        val incoming = callState as? CallSessionManager.CallState.Incoming
        if (incoming != null) {
            IncomingCallScreen(
                callerName = incoming.callerName,
                channelId = incoming.channelId,
                documentId = incoming.documentId,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(200f)
            )
        }
        // ──────────────────────────────────────────

        CubeMenuWheelOverlay(
            controller = cubeMenuWheelController,
            modifier = Modifier.zIndex(300f),
            layout = cubeMenuLayout,
            cubeSkin = selectedCubeSkin
        )

        if (isCubeSurfaceReady) {
            PersistentCubeOverlay(
                modifier = Modifier.zIndex(200f),
                selectedTabIndex = tab.cubeTabIndex(),
                mapCubeFaceTitle = mapCubeFaceTitle,
                cameraCubeFaceTitle = cameraCubeFaceTitle,
                feedCubeFaceTitle = feedCubeFaceTitle,
                profileCubeIconTitle = profileCubeIconTitle,
                cubeSkin = selectedCubeSkin,
                isInteractive = isSignedIn == true && !showAppInitializationCover && !showFeedCommentComposer,
                onVerticalSwipe = { isUp -> if (!isPhoneActive) currentOnVerticalSwipe(isUp) },
                onLongPress = { currentOnLongPressCube() },
                onLongPressMove = { deltaX, deltaY -> onLongPressMoveCube(deltaX, deltaY) },
                onLongPressEnd = { currentOnLongPressEndCube() },
                onTap = { currentOnTapCube() },
                onTabSelected = { index -> if (!isPhoneActive) currentCubeTabChange(index) },
                onFirstFrameRendered = {
                    android.util.Log.d("PoplurRoot", "Cube first frame rendered")
                    isCubeFirstFrameReady = true
                },
                channelObjectId = channelObjectId,
                channelObjectPopToken = channelObjectPopToken
            )
        }

        PoplurAIAssistantOverlay(
            enabled = isSignedIn == true && !showAppInitializationCover,
            accent = selectedCubeSkin.uiTheme().accent,
            tutorialPulseToken = profileWakeTutorialPulse,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(500f),
            onCommand = handlePoplurAICommand
        )

        // Games full-screen overlay (above all other overlays)
        if (showGames) {
            Box(Modifier.fillMaxSize().zIndex(800f)) {
                GameLaunchScreen(onDismiss = { showGames = false })
            }
        }

        // Volume meter — rendered last so it sits above everything, including the games screen
        AnimatedVisibility(
            visible = showVolumeBars,
            enter = fadeIn(animationSpec = tween(120)) + scaleIn(initialScale = 0.94f, animationSpec = tween(120)),
            exit = fadeOut(animationSpec = tween(180)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp)
                .zIndex(9000f)
        ) {
            PoplurVolumeBars(volumeFraction = deviceVolumeFraction)
        }

    }
}

@Composable
private fun FeedCommentComposerOverlay(
    visible: Boolean,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by remember(visible) { mutableStateOf("") }
    val glitter = rememberInfiniteTransition(label = "feed-comment-glitter")
    val shimmer by glitter.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "feed-comment-glitter-phase"
    )
    val sparkles = remember { buildFeedComposerSparkles() }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(120)) + scaleIn(initialScale = 0.82f, animationSpec = tween(180)),
        exit = fadeOut(animationSpec = tween(120)) + scaleOut(targetScale = 0.86f, animationSpec = tween(120)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.54f))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {})
                },
            contentAlignment = Alignment.Center
        ) {
            val density = LocalDensity.current.density
            Box(
                modifier = Modifier
                    .padding(horizontal = 22.dp)
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .graphicsLayer {
                        cameraDistance = 18f * density
                        rotationX = 6f
                        rotationY = -2f
                        shadowElevation = 28f
                    }
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF100713),
                                Color(0xFF220D2C),
                                Color(0xFF05080F)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(30.dp))
                    .padding(18.dp)
            ) {
                ComposeCanvas(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(30.dp))
                ) {
                    sparkles.forEach { sparkle ->
                        val local = ((shimmer - sparkle.delay + 1f) % 1f)
                        val alpha = if (local < 0.5f) local * 2f else (1f - local) * 2f
                        val x = size.width * sparkle.xFraction + sparkle.driftPx * sin(local * Math.PI.toFloat() * 2f)
                        val y = size.height * ((sparkle.yFraction + local * 0.12f) % 1f)
                        val sparkleSize = sparkle.sizeDp.dp.toPx()
                        rotate(
                            degrees = sparkle.rotation + local * 220f,
                            pivot = Offset(x, y)
                        ) {
                            drawRect(
                                color = sparkle.color.copy(alpha = alpha.coerceIn(0f, 0.92f)),
                                topLeft = Offset(x - sparkleSize * 0.5f, y - sparkleSize * 0.5f),
                                size = Size(sparkleSize, sparkleSize)
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PoplurText(
                            text = "COMMENT",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(13.dp)
                                .background(Color(0xFFFFD91A).copy(alpha = 0.72f + shimmer * 0.28f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.64f), CircleShape)
                        )
                    }

                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it.take(220) },
                        placeholder = {
                            PoplurText(
                                text = "Say it",
                                color = Color.White.copy(alpha = 0.52f),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        minLines = 3,
                        maxLines = 4,
                        textStyle = LocalTextStyle.current.copy(
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFFFFD91A),
                            focusedBorderColor = Color(0xFFFFD91A),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.30f),
                            focusedContainerColor = Color.Black.copy(alpha = 0.38f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.32f)
                        ),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            PoplurText("Cancel", color = Color.White.copy(alpha = 0.82f), fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            enabled = draft.trim().isNotEmpty(),
                            onClick = {
                                val trimmed = draft.trim()
                                if (trimmed.isNotEmpty()) onSubmit(trimmed)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFD91A),
                                contentColor = Color.Black,
                                disabledContainerColor = Color.White.copy(alpha = 0.16f),
                                disabledContentColor = Color.White.copy(alpha = 0.42f)
                            ),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            PoplurText("Post", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

private data class FeedComposerSparkle(
    val xFraction: Float,
    val yFraction: Float,
    val sizeDp: Float,
    val delay: Float,
    val driftPx: Float,
    val rotation: Float,
    val color: Color
)

private fun buildFeedComposerSparkles(): List<FeedComposerSparkle> {
    val random = Random(2_626)
    val colors = listOf(
        Color(0xFFFFD91A),
        Color(0xFFFF4FD8),
        Color(0xFF62F7FF),
        Color.White
    )
    return List(54) {
        FeedComposerSparkle(
            xFraction = random.nextFloat(),
            yFraction = random.nextFloat(),
            sizeDp = 2.5f + random.nextFloat() * 5.5f,
            delay = random.nextFloat(),
            driftPx = random.nextFloat() * 38f - 19f,
            rotation = random.nextFloat() * 360f,
            color = colors[random.nextInt(colors.size)]
        )
    }
}

private fun saveBitmapToCache(context: android.content.Context, bitmap: android.graphics.Bitmap): String {
    val file = java.io.File(context.cacheDir, "ad_${System.currentTimeMillis()}.jpg")
    file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, it) }
    return file.toURI().toString()
}

private fun String.toDisplayNameFallback(): String =
    split('.', '_', '-', ' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            part.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
            }
        }
        .ifBlank { UserProfile().displayName }

private fun DocumentSnapshot.numberValue(vararg keys: String): Int? =
    keys.firstNotNullOfOrNull { key ->
        getLong(key)?.toInt() ?: getDouble(key)?.toInt()
    }

private val cameraCubeTabs = setOf(
    AppTab.Camera,
    AppTab.CreateEntry,
    AppTab.CapturedOutput,
    AppTab.AIVideoStudio,
    AppTab.AIVideoProgress
)

private val profileCubeTabs = setOf(
    AppTab.Profile,
    AppTab.Messages,
    AppTab.Settings,
    AppTab.CubeDebug
)

private fun AppTab.cubeTabIndex(): Int = when (this) {
    AppTab.Map -> 0
    AppTab.Camera, AppTab.CreateEntry, AppTab.CapturedOutput, AppTab.AIVideoStudio, AppTab.AIVideoProgress -> 1
    AppTab.Feed -> 2
    AppTab.Profile -> 3
    else -> 2
}

enum class CapturePostStep {
    CreateChoice,
    Preview,
    Location,
    Edit,
    Channel,
    AdDescribe,
    AdPreview
}

enum class PostCaptureCreationMode {
    Original,
    PoplurAI
}

enum class CaptureFlowOrigin {
    CameraFirst,
    MapFirst
}

data class CaptureMapPoint(
    val latitude: Double,
    val longitude: Double
)

private const val SYSTEM_VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"

private fun Modifier.rootFeedSwipeDetector(
    currentPostIndex: Int,
    onIndexChange: (Int) -> Unit
): Modifier = pointerInput(currentPostIndex) {
    var totalY = 0f
    var triggered = false
    detectVerticalDragGestures(
        onDragStart = {
            totalY = 0f
            triggered = false
        },
        onVerticalDrag = { change, dragAmount ->
            if (!triggered) {
                totalY += dragAmount
                if (abs(totalY) > 28f) {
                    onIndexChange(if (totalY > 0) currentPostIndex - 1 else currentPostIndex + 1)
                    triggered = true
                    change.consume()
                }
            }
        }
    )
}

private fun readDeviceMusicVolumeFraction(context: Context): Float {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return 0f
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    return (currentVolume.toFloat() / maxVolume.toFloat()).coerceIn(0f, 1f)
}
