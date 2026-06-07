package com.apptists.poplur2025.ui

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.apptists.poplur2025.auth.AuthViewModel
import com.apptists.poplur2025.data.AppTab
import com.apptists.poplur2025.data.FakeChatStore
import com.apptists.poplur2025.ui.screens.*
import kotlin.random.Random

@Composable
fun PoplurRoot(
    authViewModel: AuthViewModel = viewModel(),
    mapViewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isSignedIn by authViewModel.isSignedIn.collectAsState()
    if (!isSignedIn) {
        WelcomeAuthScreen(authViewModel)
        return
    }

    var tab by remember { mutableStateOf(AppTab.Feed) }
    var currentPostIndex by remember { mutableIntStateOf(Random.nextInt(1000000)) }
    var showingMessages by remember { mutableStateOf(false) }

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
            showingMessages = true
        } else if (tab == AppTab.Map) {
            scope.launch {
                mapViewModel.triggerCenterOnUser()
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().zIndex(0f)) {
            when (tab) {
                AppTab.Feed -> FeedScreen(currentPostIndex, onIndexChange = { currentPostIndex = it; playPopSound() })
                AppTab.Profile -> ProfileScreen(authViewModel = authViewModel)
                AppTab.Map -> MapScreen(isVisible = true, mapViewModel = mapViewModel)
                AppTab.Camera -> CameraScreen()
                AppTab.Home -> HomeScreen(onOpenCamera = { tab = AppTab.Camera }, onOpenMap = { tab = AppTab.Map })
                AppTab.Messages -> MessagesScreen()
                AppTab.Settings -> SettingsScreen(onSignOut = { authViewModel.signOut() })
            }
        }

        // Keep the navigation cube fixed in one bottom-center position across screens.
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
                            ?.replace(id, GdxNavigationFragment(CUBE_FEED_INDEX, onVerticalSwipe, onLongPressCube, onTabChange))
                            ?.commit()
                    }
                },
                modifier = Modifier
                    .padding(bottom = CUBE_BOTTOM_OFFSET)
                    .size(CUBE_CONTAINER_SIZE),
                update = { view ->
                    view.visibility = if (tab == AppTab.Camera) {
                        android.view.View.GONE
                    } else {
                        android.view.View.VISIBLE
                    }
                    view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    view.background = null
                }
            )
        }

        if (showingMessages) {
            MessageTimelineOverlay(onDismiss = { showingMessages = false })
        }
    }
}

private const val CUBE_FEED_INDEX = 1
private val CUBE_CONTAINER_SIZE = 300.dp
private val CUBE_BOTTOM_OFFSET = 86.dp

@Composable
fun MessageTimelineOverlay(onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).padding(top = 100.dp).zIndex(100f)) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Messages", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onDismiss) { Text("Close", color = Color.White) }
            }
            Spacer(Modifier.height(16.dp))
            FakeChatStore.threads.forEach { thread ->
                Column(Modifier.padding(vertical = 12.dp)) {
                    Text(thread.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(thread.lastMessage, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            }
        }
    }
}
