package com.apptists.poplur2025.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
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
import com.apptists.poplur2025.data.AppTab
import com.apptists.poplur2025.data.DemoPostStore
import com.apptists.poplur2025.data.PoplurLanguage
import com.apptists.poplur2025.data.UserProfile
import com.apptists.poplur2025.PoplurVolumeEvents
import com.apptists.poplur2025.ui.screens.*
import com.apptists.poplur2025.ui.previews.CubeDevelopmentScreen
import com.apptists.poplur2025.ui.components.CubeMenuWheelController
import com.apptists.poplur2025.ui.components.CubeMenuWheelItem
import com.apptists.poplur2025.ui.components.CubeMenuLayout
import com.apptists.poplur2025.ui.components.CubeMenuWheelOverlay
import com.apptists.poplur2025.ui.components.PersistentCubeOverlay
import com.apptists.poplur2025.ui.components.PersistentCubeOverlayHost
import com.apptists.poplur2025.ui.components.FeedLaunchCover
import com.apptists.poplur2025.ui.components.PoplurVolumeBars
import com.apptists.poplur2025.ui.CubeSkin
import com.apptists.poplur2025.ui.PoplurRadioPrewarm
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun PoplurRoot(
    authViewModel: AuthViewModel = viewModel(),
    mapViewModel: MapViewModel = viewModel()
) {
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
        mutableStateOf(
            CubeSkin.entries.find { it.id == settingsPrefs.getString("selected_cube_skin", CubeSkin.Default.id) }
                ?: CubeSkin.Default
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
    var showAppInitializationCover by remember { mutableStateOf(true) }
    var showSlowInitializationDetails by remember { mutableStateOf(false) }
    var feedInteractionTick by remember { mutableIntStateOf(0) }
    var isFeedChromeAsleep by remember { mutableStateOf(false) }
    var channelObjectPopToken by remember { mutableIntStateOf(0) }
    var channelObjectId by remember { mutableStateOf("star") }
    var selectedProfile by remember { mutableStateOf(UserProfile()) }
    var cameraPhotoTrigger by remember { mutableIntStateOf(0) }
    var cameraRecordTrigger by remember { mutableIntStateOf(0) }
    var cameraLiveTrigger by remember { mutableIntStateOf(0) }
    var cameraFiltersTrigger by remember { mutableIntStateOf(0) }
    var isAppVisible by remember { mutableStateOf(true) }
    var filtersVisible by remember { mutableStateOf(false) }
    var isPhoneCallScreenVisible by remember { mutableStateOf(false) }
    var isPhoneActive by remember { mutableStateOf(false) }
    var isProfileSubActive by remember { mutableStateOf(false) }
    var isFeedTvGuideVisible by remember { mutableStateOf(false) }
    var isCubeSurfaceReady by remember { mutableStateOf(false) }
    var profileCubeCommandId by remember { mutableIntStateOf(0) }
    var profileCubeMenuCommand by remember { mutableStateOf<ProfileCubeMenuCommand?>(null) }
    var profileCubeCommittedIconTitle by remember { mutableStateOf<String?>(null) }
    var profilePanelIconTitle by remember { mutableStateOf<String?>(null) }
    var phoneColorwaySelected by remember { mutableStateOf<Color?>(null) }
    
    var capturedMediaUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isCapturedVideo by remember { mutableStateOf(false) }
    var capturePostStep by remember { mutableStateOf(CapturePostStep.Preview) }
    var captureFlowOrigin by remember { mutableStateOf(CaptureFlowOrigin.CameraFirst) }
    var pendingCaptureLocation by remember { mutableStateOf<CaptureMapPoint?>(null) }
    var selectedCaptureFilterId by remember { mutableStateOf("original") }
    var selectedCaptureTrimSeconds by remember { mutableFloatStateOf(10f) }
    var selectedCaptureChannel by remember { mutableStateOf<com.apptists.poplur2025.ui.screens.PostCaptureChannelOption?>(null) }
    var capturedAdBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var capturedAdMeta by remember { mutableStateOf<com.apptists.poplur2025.data.TVStyleMetadata?>(null) }
    val persistentCubeHost = context as? PersistentCubeOverlayHost

    SideEffect {
        if (showAppInitializationCover || !isCubeSurfaceReady || isSignedIn != true) {
            persistentCubeHost?.hidePersistentCubeOverlay()
        }
    }

    LaunchedEffect(preferredLanguageCode) {
        settingsPrefs.edit().putString("preferred_language", preferredLanguageCode).apply()
    }

    LaunchedEffect(closedCaptionsEnabled) {
        settingsPrefs.edit().putBoolean("closed_captions_enabled", closedCaptionsEnabled).apply()
    }

    LaunchedEffect(selectedCubeSkin) {
        settingsPrefs.edit().putString("selected_cube_skin", selectedCubeSkin.id).apply()
    }
    LaunchedEffect(cubeMenuLayout) {
        settingsPrefs.edit().putString("cube_menu_layout", cubeMenuLayout.name).apply()
    }
    
    // --- Volume State ---
    var deviceVolumeFraction by remember(appContext) { mutableFloatStateOf(readDeviceMusicVolumeFraction(appContext)) }
    var showVolumeBars by remember { mutableStateOf(false) }
    var volumeShowTrigger by remember { mutableIntStateOf(0) }
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
    
    val playVolumePopSound = {
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

    DisposableEffect(appContext) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == SYSTEM_VOLUME_CHANGED_ACTION) {
                    deviceVolumeFraction = readDeviceMusicVolumeFraction(appContext)
                    volumeShowTrigger++
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
            deviceVolumeFraction = readDeviceMusicVolumeFraction(appContext)
            volumeShowTrigger++
            playVolumePopSound()
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
                // Always show the loading cover when signed in — covers both app-launch
                // (null→true) and post-sign-in (false→true) cases so the feed always
                // appears with the loading screen first.
                showAppInitializationCover = true
                kotlinx.coroutines.delay(1200)
                showAppInitializationCover = false
                showSlowInitializationDetails = false
            }
            false -> {
                // Auth resolved as signed-out — brief pause then reveal WelcomeScreen.
                kotlinx.coroutines.delay(300)
                showAppInitializationCover = false
                showSlowInitializationDetails = false
            }
            null -> { /* Firebase still resolving — keep loading cover visible */ }
        }
    }

    LaunchedEffect(isSignedIn, showAppInitializationCover) {
        if (isSignedIn == true && !showAppInitializationCover) {
            isCubeSurfaceReady = true
        } else {
            isCubeSurfaceReady = false
        }
    }

    // Failsafe: Hide the loading cover after 4 seconds no matter what.
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(4000)
        if (showAppInitializationCover) {
            android.util.Log.w("PoplurRoot", "FORCE REMOVING LOADING COVER (Failsafe)")
            showAppInitializationCover = false
        }
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

    val playPopSound = {
        try {
            if (isMediaPlayerReady) {
                if (mediaPlayer.isPlaying) mediaPlayer.pause()
                mediaPlayer.seekTo(0)
                mediaPlayer.start()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // --- Cube Menu Controller ---
    val cubeMenuWheelController = remember(scope) {
        CubeMenuWheelController(scope = scope, onTick = playPopSound)
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

    val profileCubePreviewIconTitle = if (cubeMenuWheelController.isVisible) {
        cubeMenuWheelController.selectedItem?.label
    } else {
        null
    }
    
    val profileCubeIconTitle = profileCubePreviewIconTitle
        ?: profileCubeCommittedIconTitle
        ?: profilePanelIconTitle
        ?: "Profile"

    // --- Tab and Swipe Actions ---
    val onTabChange: (Int) -> Unit = { index ->
        val newTab = when (index) {
            0 -> AppTab.Map
            1 -> AppTab.Camera
            2 -> AppTab.Feed
            3 -> AppTab.Profile
            else -> AppTab.Feed
        }
        if (tab != newTab) {
            previousTab = tab
            tab = newTab
        }
    }

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
            scope.launch {
                mapViewModel.triggerOpenHoldMenu()
            }
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
        } else if (tab == AppTab.Messages || tab == AppTab.Settings || tab == AppTab.CubeDebug || tab == AppTab.CapturedOutput) {
            cubeMenuWheelController.beginHold(listOf(backItem))
        }
    }

    val onLongPressEndCube: () -> Unit = {
        if (tab == AppTab.Feed || tab == AppTab.Camera || tab == AppTab.Profile || tab == AppTab.Map) {
            if (tab == AppTab.Profile) {
                profileCubeCommittedIconTitle = cubeMenuWheelController.selectedItem?.label
            }
            cubeMenuWheelController.endHold(activateSelected = true)
        }
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
    val currentOnTabChange by rememberUpdatedState(onTabChange)

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
                    when (tab) {
                        AppTab.Feed -> FeedScreen(
                            currentPostIndex = currentPostIndex,
                            isVisible = isAppVisible,
                            tvGuideRequestToken = feedTvGuideRequest,
                            timelineToggleRequestToken = feedNewsRequest,
                            likeRequestToken = feedLikeRequest,
                            openProfileRequestToken = feedOpenProfileRequest,
                            submittedComment = submittedFeedComment,
                            commentSubmitToken = feedCommentSubmitRequest,
                            preferredLanguage = preferredLanguage,
                            closedCaptionsEnabled = closedCaptionsEnabled,
                            homeChromeInactive = isFeedChromeAsleep || showFeedCommentComposer,
                            onOpenUser = { profile ->
                                selectedProfile = profile
                                previousTab = tab
                                tab = AppTab.Profile
                            },
                            onTvGuideVisibilityChanged = { isFeedTvGuideVisible = it },
                            onChannelObjectPop = { objectId ->
                                channelObjectId = objectId
                                channelObjectPopToken++
                            },
                            onUserInteraction = { feedInteractionTick++ },
                            onIndexChange = {
                                feedInteractionTick++
                                currentPostIndex = it
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                playPopSound()
                            }
                        )
                        AppTab.Profile -> ProfileScreen(
                            profile = selectedProfile,
                            authViewModel = authViewModel,
                            onOpenSettings = {
                                profileCubeCommittedIconTitle = "Settings"
                                previousTab = AppTab.Profile
                                tab = AppTab.Settings
                            },
                            onOpenMessages = {
                                profileCubeCommittedIconTitle = "Mail"
                                previousTab = AppTab.Profile
                                tab = AppTab.Messages
                            },
                            cubeMenuCommand = profileCubeMenuCommand,
                            onProfilePanelIconChanged = { iconTitle ->
                                profilePanelIconTitle = iconTitle
                                profileCubeCommittedIconTitle = iconTitle
                                isPhoneActive = (iconTitle == "Phone")
                                isProfileSubActive = (iconTitle != null)
                            },
                            onPhoneCallVisibilityChanged = { isPhoneCallScreenVisible = it },
                            onColorwaySelected = { color -> phoneColorwaySelected = color },
                            forcedColorway = phoneColorwaySelected
                        )
                        AppTab.Map -> MapScreen(
                            isVisible = isAppVisible,
                            mapViewModel = mapViewModel,
                            isSelectingPostLocation = capturedMediaUri != null && capturePostStep == CapturePostStep.Location,
                            onCaptureLocationConfirmed = { point ->
                                pendingCaptureLocation = CaptureMapPoint(point.latitude(), point.longitude())
                                capturePostStep = CapturePostStep.Edit
                                tab = AppTab.CapturedOutput
                            },
                            onMapFirstPostLocationSelected = { point ->
                                pendingCaptureLocation = CaptureMapPoint(point.latitude(), point.longitude())
                                captureFlowOrigin = CaptureFlowOrigin.MapFirst
                                selectedCaptureFilterId = "original"
                                selectedCaptureTrimSeconds = 10f
                                capturedMediaUri = null
                                previousTab = AppTab.Map
                                tab = AppTab.Camera
                            }
                        )
                        AppTab.Camera -> CameraScreen(
                            photoTrigger = cameraPhotoTrigger,
                            recordTrigger = cameraRecordTrigger,
                            liveTrigger = cameraLiveTrigger,
                            filtersTrigger = cameraFiltersTrigger,
                            onFiltersToggle = { filtersVisible = it },
                            onMediaCaptured = { uri, isVideo ->
                                capturedMediaUri = uri
                                isCapturedVideo = isVideo
                                if (pendingCaptureLocation == null) {
                                    captureFlowOrigin = CaptureFlowOrigin.CameraFirst
                                }
                                capturePostStep = CapturePostStep.Preview
                                selectedCaptureFilterId = "original"
                                selectedCaptureTrimSeconds = 10f
                                tab = AppTab.CapturedOutput
                            }
                        )
                        AppTab.CapturedOutput -> {
                            val uri = capturedMediaUri
                            if (uri != null) {
                                when (capturePostStep) {
                                    CapturePostStep.Preview -> CapturedOutputScreen(
                                        uri = uri,
                                        isVideo = isCapturedVideo,
                                        onBack = { tab = AppTab.Camera },
                                        continueLabel = if (pendingCaptureLocation == null) "Location" else "Filters",
                                        onContinue = {
                                            if (pendingCaptureLocation == null) {
                                                capturePostStep = CapturePostStep.Location
                                                tab = AppTab.Map
                                            } else {
                                                capturePostStep = CapturePostStep.Edit
                                            }
                                        }
                                    )
                                    CapturePostStep.Location -> {
                                        tab = AppTab.Map
                                    }
                                    CapturePostStep.Edit -> CapturedMediaEditorScreen(
                                        uri = uri,
                                        isVideo = isCapturedVideo,
                                        selectedFilterId = selectedCaptureFilterId,
                                        trimSeconds = selectedCaptureTrimSeconds,
                                        onFilterSelected = { selectedCaptureFilterId = it },
                                        onTrimSecondsChanged = { selectedCaptureTrimSeconds = it },
                                        onBack = { capturePostStep = CapturePostStep.Preview },
                                        onNext = { capturePostStep = CapturePostStep.Channel }
                                    )
                                    CapturePostStep.Channel -> CapturedChannelScreen(
                                        uri = uri,
                                        isVideo = isCapturedVideo,
                                        filterId = selectedCaptureFilterId,
                                        trimSeconds = if (isCapturedVideo) selectedCaptureTrimSeconds else null,
                                        onBack = { capturePostStep = CapturePostStep.Edit },
                                        onGenerateAd = { channel ->
                                            selectedCaptureChannel = channel
                                            capturePostStep = CapturePostStep.AdDescribe
                                        },
                                        onPosted = { channel, meta ->
                                            val point = pendingCaptureLocation ?: CaptureMapPoint(0.0, 0.0)
                                            DemoPostStore.addUserPost(
                                                uri = uri.toString(),
                                                isVideo = isCapturedVideo,
                                                latitude = point.latitude,
                                                longitude = point.longitude,
                                                channelId = channel.id,
                                                channelTitle = channel.title,
                                                filterId = selectedCaptureFilterId,
                                                trimSeconds = if (isCapturedVideo) selectedCaptureTrimSeconds else null,
                                                tvMetadata = meta
                                            )
                                            capturedMediaUri = null
                                            pendingCaptureLocation = null
                                            captureFlowOrigin = CaptureFlowOrigin.CameraFirst
                                            capturePostStep = CapturePostStep.Preview
                                            selectedCaptureFilterId = "original"
                                            selectedCaptureTrimSeconds = 10f
                                            selectedProfile = UserProfile()
                                            currentPostIndex = 0
                                            tab = AppTab.Profile
                                        }
                                    )
                                    CapturePostStep.AdDescribe -> {
                                        val channel = selectedCaptureChannel ?: com.apptists.poplur2025.ui.screens.postCaptureChannels.first()
                                        val loc = pendingCaptureLocation
                                        com.apptists.poplur2025.ui.screens.DescribeAdScreen(
                                            uri = uri,
                                            isVideo = isCapturedVideo,
                                            selectedChannel = channel,
                                            coordinate = loc?.let { Pair(it.latitude, it.longitude) },
                                            onBack = { capturePostStep = CapturePostStep.Channel },
                                            onAdGenerated = { bitmap, meta ->
                                                capturedAdBitmap = bitmap
                                                capturedAdMeta = meta
                                                capturePostStep = CapturePostStep.AdPreview
                                            }
                                        )
                                    }
                                    CapturePostStep.AdPreview -> {
                                        val bitmap = capturedAdBitmap
                                        val meta = capturedAdMeta
                                        val channel = selectedCaptureChannel ?: com.apptists.poplur2025.ui.screens.postCaptureChannels.first()
                                        if (bitmap != null && meta != null) {
                                            com.apptists.poplur2025.ui.screens.AdPreviewScreen(
                                                adBitmap = bitmap,
                                                selectedChannel = channel,
                                                meta = meta,
                                                onBack = { capturePostStep = CapturePostStep.AdDescribe },
                                                onBroadcast = {
                                                    val point = pendingCaptureLocation ?: CaptureMapPoint(0.0, 0.0)
                                                    val adUri = saveBitmapToCache(context, bitmap)
                                                    DemoPostStore.addUserPost(
                                                        uri = adUri,
                                                        isVideo = false,
                                                        latitude = point.latitude,
                                                        longitude = point.longitude,
                                                        channelId = channel.id,
                                                        channelTitle = channel.title,
                                                        filterId = "original",
                                                        trimSeconds = null,
                                                        tvMetadata = meta
                                                    )
                                                    capturedMediaUri = null
                                                    capturedAdBitmap = null
                                                    capturedAdMeta = null
                                                    pendingCaptureLocation = null
                                                    captureFlowOrigin = CaptureFlowOrigin.CameraFirst
                                                    capturePostStep = CapturePostStep.Preview
                                                    selectedCaptureFilterId = "original"
                                                    selectedCaptureTrimSeconds = 10f
                                                    selectedProfile = UserProfile()
                                                    currentPostIndex = 0
                                                    tab = AppTab.Profile
                                                }
                                            )
                                        } else {
                                            capturePostStep = CapturePostStep.AdDescribe
                                        }
                                    }
                                }
                            } else {
                                capturePostStep = CapturePostStep.Preview
                                tab = AppTab.Camera
                            }
                        }
                        AppTab.Home -> HomeScreen(onOpenCamera = { tab = AppTab.Camera }, onOpenMap = { tab = AppTab.Map })
                        AppTab.Messages -> DMScreen(
                            onBack = {
                                val destination = previousTab ?: AppTab.Feed
                                tab = destination
                                if (destination == AppTab.Profile) {
                                    profileCubeCommittedIconTitle = null
                                    profilePanelIconTitle = null
                                }
                            },
                            onPhoneCallVisibilityChanged = { isPhoneCallScreenVisible = it }
                        )
                        AppTab.Settings -> SettingsScreen(
                            onBack = {
                                tab = previousTab ?: AppTab.Profile
                                if (tab == AppTab.Profile) {
                                    profileCubeCommittedIconTitle = null
                                    profilePanelIconTitle = null
                                }
                            },
                            onSignOut = { authViewModel.signOut() },
                            onOpenCubeDebug = { tab = AppTab.CubeDebug },
                            preferredLanguage = preferredLanguage,
                            closedCaptionsEnabled = closedCaptionsEnabled,
                            onPreferredLanguageChange = { preferredLanguageCode = it.code },
                            onClosedCaptionsChange = { closedCaptionsEnabled = it },
                            selectedCubeSkin = selectedCubeSkin,
                            onCubeSkinChange = { selectedCubeSkin = it },
                            cubeMenuLayout = cubeMenuLayout,
                            onCubeMenuLayoutChange = { cubeMenuLayout = it }
                        )
                        AppTab.CubeDebug -> CubeDevelopmentScreen(onBack = { tab = AppTab.Settings })
                    }
                }

            }
        } else {
            FeedLaunchCover(
                onPopSound = playPopSound,
                showSlowDetails = showSlowInitializationDetails,
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

        val volumeOverlay: @Composable BoxScope.() -> Unit = {
            AnimatedVisibility(
                visible = showVolumeBars,
                enter = fadeIn(animationSpec = tween(120)) + scaleIn(initialScale = 0.94f, animationSpec = tween(120)),
                exit = fadeOut(animationSpec = tween(180)),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp)
                    .zIndex(120f)
            ) {
                PoplurVolumeBars(volumeFraction = deviceVolumeFraction)
            }
        }

        volumeOverlay()

        CubeMenuWheelOverlay(
            controller = cubeMenuWheelController,
            modifier = Modifier.zIndex(300f),
            layout = cubeMenuLayout
        )

        if (isCubeSurfaceReady && !showAppInitializationCover) {
            PersistentCubeOverlay(
                modifier = Modifier.zIndex(200f),
                selectedTabIndex = tab.cubeTabIndex(),
                profileCubeIconTitle = profileCubeIconTitle,
                cubeSkin = selectedCubeSkin,
                isInteractive = isSignedIn == true && !showFeedCommentComposer,
                onVerticalSwipe = { isUp -> if (!isPhoneActive) currentOnVerticalSwipe(isUp) },
                onLongPress = { currentOnLongPressCube() },
                onLongPressEnd = { currentOnLongPressEndCube() },
                onTap = { currentOnTapCube() },
                onTabSelected = { index -> if (!isPhoneActive) currentOnTabChange(index) },
                channelObjectId = channelObjectId,
                channelObjectPopToken = channelObjectPopToken
            )
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
                        Text(
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
                            Text(
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
                            Text("Cancel", color = Color.White.copy(alpha = 0.82f), fontWeight = FontWeight.Bold)
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
                            Text("Post", fontWeight = FontWeight.Black)
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

private fun AppTab.cubeTabIndex(): Int = when (this) {
    AppTab.Map -> 0
    AppTab.Camera -> 1
    AppTab.Feed -> 2
    AppTab.Profile -> 3
    else -> 2
}

private enum class CapturePostStep {
    Preview,
    Location,
    Edit,
    Channel,
    AdDescribe,
    AdPreview
}

private enum class CaptureFlowOrigin {
    CameraFirst,
    MapFirst
}

private data class CaptureMapPoint(
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
