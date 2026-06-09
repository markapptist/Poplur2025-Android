package com.apptists.poplur2025.ui

import android.media.MediaPlayer
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
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
import com.apptists.poplur2025.ui.screens.*
import com.mapbox.geojson.Point
import kotlin.random.Random

@Composable
fun PoplurRoot(
    authViewModel: AuthViewModel = viewModel(),
    mapViewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val isSignedIn by authViewModel.isSignedIn.collectAsState()
    if (!isSignedIn) {
        WelcomeAuthScreen(authViewModel)
        return
    }

    var tab by remember { mutableStateOf(AppTab.Feed) }
    var currentPostIndex by remember { mutableIntStateOf(Random.nextInt(1000000)) }
    var showingPostTimeline by remember { mutableStateOf(false) }
    var cameraPhotoTrigger by remember { mutableIntStateOf(0) }
    var cameraRecordTrigger by remember { mutableIntStateOf(0) }
    var isAppVisible by remember { mutableStateOf(true) }
    var filtersVisible by remember { mutableStateOf(false) }

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
        tab = when (index) {
            0 -> AppTab.Camera
            1 -> AppTab.Feed
            2 -> AppTab.Profile
            3 -> AppTab.Map
            else -> AppTab.Feed
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

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().zIndex(0f)) {
            when (tab) {
                AppTab.Feed -> FeedScreen(currentPostIndex, isVisible = isAppVisible, onIndexChange = { currentPostIndex = it; playPopSound() })
                AppTab.Profile -> ProfileScreen(authViewModel = authViewModel)
                AppTab.Map -> MapScreen(isVisible = isAppVisible, mapViewModel = mapViewModel)
                AppTab.Camera -> CameraScreen(
                    photoTrigger = cameraPhotoTrigger,
                    recordTrigger = cameraRecordTrigger,
                    onFiltersToggle = { filtersVisible = it }
                )
                AppTab.Home -> HomeScreen(onOpenCamera = { tab = AppTab.Camera }, onOpenMap = { tab = AppTab.Map })
                AppTab.Messages -> MessagesScreen(onBack = { tab = AppTab.Profile })
                AppTab.Settings -> SettingsScreen(onSignOut = { authViewModel.signOut() })
            }
        }

        // Keep the navigation cube fixed in one bottom-center position across screens.
        // Hide it on the Messages screen and when Camera filters are active.
        if (tab != AppTab.Messages && !filtersVisible) {
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
                                ?.replace(id, GdxNavigationFragment(CUBE_FEED_INDEX, onVerticalSwipe, onLongPressCube, onTapCube, onTabChange))
                                ?.commit()
                        }
                    },
                    modifier = Modifier
                        .padding(bottom = CUBE_BOTTOM_OFFSET)
                        .size(CUBE_CONTAINER_SIZE),
                    update = { view ->
                        view.visibility = android.view.View.VISIBLE
                        view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        view.background = null
                    }
                )
            }
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
    }
}

private const val CUBE_FEED_INDEX = 1
private val CUBE_CONTAINER_SIZE = 300.dp
private val CUBE_BOTTOM_OFFSET = 86.dp

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
