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
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
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
import com.apptists.poplur2025.data.UserProfile
import com.apptists.poplur2025.PoplurVolumeEvents
import com.apptists.poplur2025.ui.screens.*
import com.apptists.poplur2025.ui.previews.CubeDevelopmentScreen
import com.apptists.poplur2025.ui.components.CubeMenuWheelController
import com.apptists.poplur2025.ui.components.CubeMenuWheelItem
import com.apptists.poplur2025.ui.components.CubeMenuWheelOverlay
import com.apptists.poplur2025.ui.components.FeedVolumeBars
import kotlin.math.abs

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
    
    // --- Shared State (Must be declared before any early returns) ---
    var tab by remember { mutableStateOf(AppTab.Feed) }
    var previousTab by remember { mutableStateOf<AppTab?>(null) }
    var currentPostIndex by remember { mutableIntStateOf(0) }
    var feedTvGuideRequest by remember { mutableIntStateOf(0) }
    var feedNewsRequest by remember { mutableIntStateOf(0) }
    var feedInteractionTick by remember { mutableIntStateOf(0) }
    var isFeedChromeAsleep by remember { mutableStateOf(false) }
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
    var profileCubeCommandId by remember { mutableIntStateOf(0) }
    var profileCubeMenuCommand by remember { mutableStateOf<ProfileCubeMenuCommand?>(null) }
    var profileCubeCommittedIconTitle by remember { mutableStateOf<String?>(null) }
    var profilePanelIconTitle by remember { mutableStateOf<String?>(null) }
    var phoneColorwaySelected by remember { mutableStateOf<Color?>(null) }
    
    var capturedMediaUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isCapturedVideo by remember { mutableStateOf(false) }
    
    // --- Volume State ---
    var deviceVolumeFraction by remember(appContext) { mutableFloatStateOf(readDeviceMusicVolumeFraction(appContext)) }
    var showVolumeBars by remember { mutableStateOf(false) }
    var volumeShowTrigger by remember { mutableIntStateOf(0) }
    val volumePopPlayer = remember(appContext) {
        MediaPlayer().apply {
            try {
                val afd = appContext.assets.openFd("pop.mp3")
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepareAsync()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    val playVolumePopSound = {
        try {
            if (volumePopPlayer.isPlaying) volumePopPlayer.pause()
            volumePopPlayer.seekTo(0)
            volumePopPlayer.start()
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
        ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
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

    val mediaPlayer = remember {
        MediaPlayer().apply {
            try {
                val afd = context.assets.openFd("pop.mp3")
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepareAsync()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    DisposableEffect(Unit) { onDispose { mediaPlayer.release() } }

    val playPopSound = {
        try {
            if (mediaPlayer.isPlaying) mediaPlayer.pause()
            mediaPlayer.seekTo(0)
            mediaPlayer.start()
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
            0 -> AppTab.Camera
            1 -> AppTab.Feed
            2 -> AppTab.Profile
            3 -> AppTab.Map
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
                        label = "TV Guide",
                        icon = Icons.Default.Tv,
                        accent = Color(0xFF4DFF58),
                        onSelected = { feedTvGuideRequest++ }
                    ),
                    CubeMenuWheelItem(
                        label = "News",
                        icon = Icons.AutoMirrored.Filled.Article,
                        accent = Color(0xFFFF3333),
                        onSelected = { feedNewsRequest++ }
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
            if (isPhoneActive) {
                cubeMenuWheelController.beginHold(listOf(backItem) + phoneColorWheelItems)
            } else if (isProfileSubActive) {
                cubeMenuWheelController.beginHold(listOf(backItem))
            } else {
                cubeMenuWheelController.beginHold(
                    listOf(
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
                )
            }
        } else if (tab == AppTab.Messages || tab == AppTab.Settings || tab == AppTab.CubeDebug || tab == AppTab.CapturedOutput) {
            cubeMenuWheelController.beginHold(listOf(backItem))
        }
    }

    val onLongPressEndCube: () -> Unit = {
        if (tab == AppTab.Feed || tab == AppTab.Camera || tab == AppTab.Profile) {
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

    LaunchedEffect(tab, feedInteractionTick, isFeedTvGuideVisible) {
        if (tab == AppTab.Feed && !isFeedTvGuideVisible) {
            isFeedChromeAsleep = false
            kotlinx.coroutines.delay(30_000)
            isFeedChromeAsleep = true
        } else {
            isFeedChromeAsleep = false
        }
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
        if (isSignedIn == null) {
            // Transient guard — isSignedIn is set synchronously in AuthViewModel.init
            // so this branch should never render in practice.
            Box(Modifier.fillMaxSize().background(Color.Black))
        } else if (isSignedIn == false) {
            // Signed Out
            WelcomeAuthScreen(authViewModel)
        } else {
            // Signed In
            Box(Modifier.fillMaxSize().zIndex(0f)) {
                when (tab) {
                    AppTab.Feed -> FeedScreen(
                        currentPostIndex = currentPostIndex,
                        isVisible = isAppVisible,
                        tvGuideRequestToken = feedTvGuideRequest,
                        timelineToggleRequestToken = feedNewsRequest,
                        homeChromeInactive = isFeedChromeAsleep,
                        onOpenUser = { profile ->
                            selectedProfile = profile
                            previousTab = tab
                            tab = AppTab.Profile
                        },
                        onTvGuideVisibilityChanged = { isFeedTvGuideVisible = it },
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
                    AppTab.Map -> MapScreen(isVisible = isAppVisible, mapViewModel = mapViewModel)
                    AppTab.Camera -> CameraScreen(
                        photoTrigger = cameraPhotoTrigger,
                        recordTrigger = cameraRecordTrigger,
                        liveTrigger = cameraLiveTrigger,
                        filtersTrigger = cameraFiltersTrigger,
                        onFiltersToggle = { filtersVisible = it },
                        onMediaCaptured = { uri, isVideo ->
                            capturedMediaUri = uri
                            isCapturedVideo = isVideo
                            tab = AppTab.CapturedOutput
                        }
                    )
                    AppTab.CapturedOutput -> {
                        val uri = capturedMediaUri
                        if (uri != null) {
                            CapturedOutputScreen(
                                uri = uri,
                                isVideo = isCapturedVideo,
                                onBack = { tab = AppTab.Camera },
                                onPost = {
                                    // Logic to post would go here
                                    tab = AppTab.Feed
                                }
                            )
                        } else {
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
                        onOpenCubeDebug = { tab = AppTab.CubeDebug }
                    )
                    AppTab.CubeDebug -> CubeDevelopmentScreen(onBack = { tab = AppTab.Settings })
                }
            }
        }

        // --- Overlays (Always Present) ---
        
        CubeMenuWheelOverlay(
            controller = cubeMenuWheelController,
            modifier = Modifier.zIndex(6100f)
        )

        PersistentCubeOverlay(
            modifier = Modifier.alpha(if (tab == AppTab.Feed && isFeedChromeAsleep) 0.18f else 1f),
            profileCubeIconTitle = profileCubeIconTitle,
            isInteractive = isSignedIn == true && !(tab == AppTab.Feed && isFeedChromeAsleep),
            onVerticalSwipe = { isUp -> if (!isPhoneActive) currentOnVerticalSwipe(isUp) },
            onLongPress = { currentOnLongPressCube() },
            onLongPressEnd = { currentOnLongPressEndCube() },
            onTap = { currentOnTapCube() },
            onTabSelected = { index -> if (!isPhoneActive) currentOnTabChange(index) }
        )

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
                FeedVolumeBars(volumeFraction = deviceVolumeFraction)
            }
        }

        volumeOverlay()

        // Debug state (hidden in production typically)
        if (false) { // Set to true to see debug info on screen
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopCenter) {
                Surface(color = Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        "Auth: $isSignedIn | Tab: $tab",
                        color = Color.White,
                        modifier = Modifier.padding(8.dp),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PersistentCubeOverlay(
    modifier: Modifier = Modifier,
    profileCubeIconTitle: String = "Profile",
    isInteractive: Boolean = true,
    onVerticalSwipe: (Boolean) -> Unit = {},
    onLongPress: () -> Unit = {},
    onLongPressEnd: () -> Unit = {},
    onTap: () -> Unit = {},
    onTabSelected: (Int) -> Unit = {}
) {
    val containerId = remember { android.view.View.generateViewId() }
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeTick by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(6000f),
        contentAlignment = Alignment.BottomCenter
    ) {
        AndroidView(
            factory = { ctx ->
                FragmentContainerView(ctx).apply {
                    id = containerId
                    setBackgroundColor(0)
                    background = null
                    val activity = ctx as? FragmentActivity
                    activity?.supportFragmentManager?.beginTransaction()
                        ?.replace(
                            id,
                            GdxNavigationFragment(
                                initialTabIndex = CUBE_FEED_INDEX,
                                onVerticalSwipe = if (isInteractive) onVerticalSwipe else null,
                                onLongPress = if (isInteractive) onLongPress else null,
                                onLongPressEnd = if (isInteractive) onLongPressEnd else null,
                                onTap = if (isInteractive) onTap else null,
                                onTabSelected = if (isInteractive) onTabSelected else { _: Int -> }
                            ).apply {
                                setProfileIconTitle(profileCubeIconTitle)
                            }
                        )
                        ?.commitAllowingStateLoss()
                }
            },
            modifier = Modifier
                .padding(bottom = CUBE_BOTTOM_OFFSET)
                .size(CUBE_CONTAINER_SIZE),
            update = { view ->
                view.visibility = android.view.View.VISIBLE
                view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                view.background = null
                @Suppress("UNUSED_EXPRESSION") resumeTick  // read to re-run update on resume
                val activity = view.context as? FragmentActivity
                val verticalSwipeCallback = onVerticalSwipe
                val longPressCallback = onLongPress
                val longPressEndCallback = onLongPressEnd
                val tapCallback = onTap
                val tabSelectedCallback = onTabSelected
                val fragment = activity?.supportFragmentManager
                    ?.findFragmentById(containerId) as? GdxNavigationFragment
                if (fragment == null) {
                    // Fragment was lost after process death or system eviction — recreate it
                    activity?.supportFragmentManager?.beginTransaction()
                        ?.replace(
                            containerId,
                            GdxNavigationFragment(
                                initialTabIndex = CUBE_FEED_INDEX,
                                onVerticalSwipe = if (isInteractive) verticalSwipeCallback else null,
                                onLongPress = if (isInteractive) longPressCallback else null,
                                onLongPressEnd = if (isInteractive) longPressEndCallback else null,
                                onTap = if (isInteractive) tapCallback else null,
                                onTabSelected = if (isInteractive) tabSelectedCallback else { _: Int -> }
                            ).apply { setProfileIconTitle(profileCubeIconTitle) }
                        )
                        ?.commitAllowingStateLoss()
                } else {
                    fragment.apply {
                        setProfileIconTitle(profileCubeIconTitle)
                        this.onVerticalSwipe = if (isInteractive) verticalSwipeCallback else null
                        this.onLongPress = if (isInteractive) longPressCallback else null
                        this.onLongPressEnd = if (isInteractive) longPressEndCallback else null
                        this.onTap = if (isInteractive) tapCallback else null
                        this.onTabSelected = if (isInteractive) tabSelectedCallback else null
                    }
                }
            }
        )
    }
}

private const val CUBE_FEED_INDEX = 1
private val CUBE_CONTAINER_SIZE = 230.dp
private val CUBE_BOTTOM_OFFSET = 24.dp
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
