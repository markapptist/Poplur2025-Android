package com.apptists.poplur2025.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.apptists.poplur2025.auth.AuthViewModel
import com.apptists.poplur2025.data.AppTab
import com.apptists.poplur2025.data.DemoPostStore
import com.apptists.poplur2025.data.Post
import com.apptists.poplur2025.PoplurVolumeEvents
import com.apptists.poplur2025.ui.screens.*
import com.apptists.poplur2025.ui.previews.CubeDevelopmentScreen
import com.apptists.poplur2025.ui.components.FeedVolumeBars
import com.mapbox.geojson.Point
import kotlin.math.abs

@Composable
fun PoplurRoot(
    authViewModel: AuthViewModel = viewModel(),
    mapViewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val isSignedIn by authViewModel.isSignedIn.collectAsState()
    var deviceVolumeFraction by remember(appContext) { mutableFloatStateOf(readDeviceMusicVolumeFraction(appContext)) }
    var showVolumeBars by remember { mutableStateOf(false) }
    var volumeShowTrigger by remember { mutableIntStateOf(0) }
    val volumePopPlayer = remember(appContext) {
        MediaPlayer().apply {
            try {
                val afd = appContext.assets.openFd("pop.mp3")
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepare()
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

    if (isSignedIn == null) {
        Box(Modifier.fillMaxSize().background(Color.White)) {
            volumeOverlay()
        }
        return
    }

    if (isSignedIn == false) {
        Box(Modifier.fillMaxSize()) {
            WelcomeAuthScreen(authViewModel)
            volumeOverlay()
        }
        return
    }

    var tab by remember { mutableStateOf(AppTab.Feed) }
    var previousTab by remember { mutableStateOf<AppTab?>(null) }
    var currentPostIndex by remember { mutableIntStateOf(0) }
    var showingPostTimeline by remember { mutableStateOf(false) }
    var cameraPhotoTrigger by remember { mutableIntStateOf(0) }
    var cameraRecordTrigger by remember { mutableIntStateOf(0) }
    var isAppVisible by remember { mutableStateOf(true) }
    var filtersVisible by remember { mutableStateOf(false) }
    
    var capturedMediaUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isCapturedVideo by remember { mutableStateOf(false) }

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

    val containerId = remember { android.view.View.generateViewId() }

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
            if (isUp) currentPostIndex++ else currentPostIndex--
            playPopSound()
        }
    }

    val onLongPressCube: () -> Unit = {
        if (tab == AppTab.Feed) {
            showingPostTimeline = true
        } else if (tab == AppTab.Map) {
            scope.launch {
                mapViewModel.triggerCenterOnUser()
            }
        } else if (tab == AppTab.Camera) {
            cameraRecordTrigger++
        }
    }

    val onTapCube: () -> Unit = {
        if (tab == AppTab.Camera) {
            cameraPhotoTrigger++
        }
    }

    val currentOnVerticalSwipe by rememberUpdatedState(onVerticalSwipe)
    val currentOnLongPressCube by rememberUpdatedState(onLongPressCube)
    val currentOnTapCube by rememberUpdatedState(onTapCube)
    val currentOnTabChange by rememberUpdatedState(onTabChange)

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().zIndex(0f)) {
            when (tab) {
                AppTab.Feed -> FeedScreen(currentPostIndex, isVisible = isAppVisible, onIndexChange = { currentPostIndex = it; playPopSound() })
                AppTab.Profile -> ProfileScreen(
                    authViewModel = authViewModel,
                    onOpenSettings = { tab = AppTab.Settings },
                    onOpenMessages = { tab = AppTab.Messages }
                )
                AppTab.Map -> MapScreen(isVisible = isAppVisible, mapViewModel = mapViewModel)
                AppTab.Camera -> CameraScreen(
                    photoTrigger = cameraPhotoTrigger,
                    recordTrigger = cameraRecordTrigger,
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
                        // Fallback if somehow URI is null
                        tab = AppTab.Camera
                    }
                }
                AppTab.Home -> HomeScreen(onOpenCamera = { tab = AppTab.Camera }, onOpenMap = { tab = AppTab.Map })
                AppTab.Messages -> MessagesScreen(onBack = { tab = previousTab ?: AppTab.Feed })
                AppTab.Settings -> SettingsScreen(onBack = { tab = AppTab.Profile }, onSignOut = { authViewModel.signOut() }, onOpenCubeDebug = { tab = AppTab.CubeDebug })
                AppTab.CubeDebug -> CubeDevelopmentScreen(onBack = { tab = AppTab.Settings })
            }
        }

        if (tab == AppTab.Feed && !showingPostTimeline) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(9f)
                    .rootFeedSwipeDetector(currentPostIndex) { nextIndex ->
                        currentPostIndex = nextIndex
                        playPopSound()
                    }
            )
        }

        // Keep the navigation cube fragment alive across screens. Destroying/recreating the
        // embedded LibGDX fragment can terminate the process on some devices.
        val isCubeVisible = tab != AppTab.Settings && tab != AppTab.CubeDebug && tab != AppTab.CapturedOutput && !filtersVisible
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f),
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
                                id, GdxNavigationFragment(CUBE_FEED_INDEX,
                                    { isUp -> currentOnVerticalSwipe(isUp) },
                                    { currentOnLongPressCube() },
                                    { currentOnTapCube() },
                                    { index -> currentOnTabChange(index) })
                            )
                            ?.commit()
                    }
                },
                modifier = Modifier
                    .padding(bottom = CUBE_BOTTOM_OFFSET)
                    .offset(y = if (isCubeVisible) 0.dp else CUBE_CONTAINER_SIZE + CUBE_BOTTOM_OFFSET + 80.dp)
                    .size(CUBE_CONTAINER_SIZE),
                update = { view ->
                    view.visibility = android.view.View.VISIBLE
                    view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    view.background = null
                }
            )
        }

        if (showingPostTimeline) {
            PostTimelineOverlay(
                posts = DemoPostStore.posts.take(24),
                onDismiss = { showingPostTimeline = false },
                onPostToMap = { post ->
                    val lat = post.latitude
                    val lng = post.longitude
                    if (lat != null && lng != null) {
                        showingPostTimeline = false
                        tab = AppTab.Map
                        scope.launch {
                            mapViewModel.triggerPostCoordinate(Point.fromLngLat(lng, lat))
                        }
                    }
                }
            )
        }

        volumeOverlay()
    }
}

private const val CUBE_FEED_INDEX = 1
private val CUBE_CONTAINER_SIZE = 240.dp
private val CUBE_BOTTOM_OFFSET = 120.dp
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

private fun Post.initial(): String = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: username.firstOrNull()?.uppercaseChar()?.toString() ?: "P"

@Composable
fun PostTimelineOverlay(
    posts: List<Post>,
    onDismiss: () -> Unit,
    onPostToMap: (Post) -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.86f))
            .clickable(onClick = onDismiss)
            .zIndex(100f)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .align(Alignment.BottomCenter)
                .clickable(onClick = {}),
            color = Color.Black,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            tonalElevation = 0.dp
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column {
                        Text("Posts", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                        Text("Choose a coordinate for the map", color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onDismiss) { Text("Close", color = Color.White, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.fillMaxSize()) {
                    items(posts, key = { it.id }) { post ->
                        PostTimelineRow(post = post, onPostToMap = { onPostToMap(post) })
                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PostTimelineRow(post: Post, onPostToMap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = Color(0xFF7C6CFF)) {
            Box(contentAlignment = Alignment.Center) {
                Text(post.initial(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(post.displayName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(6.dp))
                Text("@${post.username}", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Text(post.caption, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "${post.locationName}  ${"%.4f".format(post.latitude ?: 0.0)} N, ${"%.4f".format(post.longitude ?: 0.0)} E",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Button(
            onClick = onPostToMap,
            enabled = post.latitude != null && post.longitude != null,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
        ) {
            Text("Post", fontWeight = FontWeight.Black)
        }
    }
}
