package com.apptists.poplur2025.ui

import android.media.MediaPlayer
import android.opengl.GLSurfaceView
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
import com.apptists.poplur2025.auth.AuthViewModel
import com.apptists.poplur2025.data.AppTab
import com.apptists.poplur2025.data.FakeChatStore
import com.apptists.poplur2025.ui.screens.*
import kotlin.random.Random

@Composable
fun PoplurRoot(authViewModel: AuthViewModel = viewModel()) {
    val context = LocalContext.current
    val isSignedIn by authViewModel.isSignedIn.collectAsState()
    if (!isSignedIn) {
        WelcomeAuthScreen(authViewModel)
        return
    }

    var tab by remember { mutableStateOf(AppTab.Feed) }
    var currentPostIndex by remember { mutableIntStateOf(Random.nextInt(1000000)) }
    var showingMessages by remember { mutableStateOf(false) }
    
    var logoFragment by remember { mutableStateOf<LogoAnimationFragment?>(null) }

    val mediaPlayer = remember {
        MediaPlayer().apply {
            try {
                val afd = context.assets.openFd("pop.mp3")
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                // Use prepareAsync to avoid blocking the main thread during composition
                prepareAsync()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { mediaPlayer.release() }
    }

    val playPopSound = {
        try {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
            }
            mediaPlayer.seekTo(0)
            mediaPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val containerId = remember { android.view.View.generateViewId() }
    val logoContainerId = remember { android.view.View.generateViewId() }
    var cameraInitialized by remember { mutableStateOf(false) }
    if (tab == AppTab.Camera) cameraInitialized = true

    val onTabChange: (Int) -> Unit = { index ->
        tab = when (index) {
            0 -> AppTab.Camera
            1 -> AppTab.Feed
            2 -> AppTab.Profile
            3 -> AppTab.Map
            else -> AppTab.Feed
        }
    }

    // Trigger logo pop on launch or when switching back to Feed
    LaunchedEffect(tab) {
        if (tab == AppTab.Feed) {
            logoFragment?.triggerLogoPop()
        }
    }

    val onVerticalSwipe: (Boolean) -> Unit = { isUp ->
        if (tab == AppTab.Feed) {
            if (isUp) currentPostIndex++ else currentPostIndex--
            playPopSound()
            logoFragment?.triggerLogoPop()
        }
    }

    val onLongPressCube: () -> Unit = {
        if (tab == AppTab.Feed) {
            showingMessages = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Content Layer (Lowest Z-Index)
        Box(Modifier.fillMaxSize().zIndex(0f)) {
            if (tab == AppTab.Feed) Box(Modifier.fillMaxSize().zIndex(1f)) {
                FeedScreen(
                    currentPostIndex = currentPostIndex,
                    onIndexChange = { newIndex ->
                        currentPostIndex = newIndex
                        playPopSound()
                        logoFragment?.triggerLogoPop()
                    }
                )
            }
            if (tab == AppTab.Profile) Box(Modifier.fillMaxSize().zIndex(2f)) { ProfileScreen(authViewModel = authViewModel) }
            if (tab == AppTab.Map) Box(Modifier.fillMaxSize().zIndex(3f)) { MapScreen(isVisible = true) }
            if (cameraInitialized) Box(Modifier.fillMaxSize().alpha(if (tab == AppTab.Camera) 1f else 0f).zIndex(if (tab == AppTab.Camera) 3f else 0f)) { CameraScreen() }
        }

        // 3D/GL Overlay Layer (Higher Z-Index)
        Box(Modifier.fillMaxSize().zIndex(10f)) {
            // 1. Full-Screen Logo Animation Overlay (Temporarily disabled to fix black screen)
            /*
            AndroidView(
                factory = { ctx ->
                    FragmentContainerView(ctx).apply {
                        id = logoContainerId
                        setBackgroundColor(0)
                        background = null
                        // Make it pass-through for touches
                        isClickable = false
                        isFocusable = false
                        isEnabled = false
                        
                        val activity = ctx as? FragmentActivity
                        val fragment = LogoAnimationFragment()
                        logoFragment = fragment
                        activity?.supportFragmentManager?.beginTransaction()
                            ?.replace(id, fragment)
                            ?.commit()
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (logoFragment != null) 1f else 0f), // Only show when fragment is ready
                update = { view ->
                    view.setBackgroundColor(0)
                    view.background = null
                }
            )
            */

            // 2. The Cube Container - FIXED 200dp x 200dp AT BOTTOM
            Box(
                modifier = Modifier.fillMaxSize().padding(bottom = 8.dp),
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
                                ?.replace(id, GdxNavigationFragment(1, onVerticalSwipe, onLongPressCube, onTabChange))
                                ?.commit()
                        }
                    },
                    modifier = Modifier.size(200.dp),
                    update = { view ->
                        view.setBackgroundColor(0)
                        view.background = null
                    }
                )
            }
        }

        if (showingMessages) {
            MessageTimelineOverlay(onDismiss = { showingMessages = false })
        }
    }
}

@Composable
fun MessageTimelineOverlay(onDismiss: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .padding(top = 100.dp)
            .zIndex(100f)
    ) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
