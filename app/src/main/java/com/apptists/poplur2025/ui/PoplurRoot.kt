package com.apptists.poplur2025.ui

import android.opengl.GLSurfaceView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import com.apptists.poplur2025.data.AppTab
import com.apptists.poplur2025.ui.screens.*

@Composable fun PoplurRoot() {
    var tab by remember { mutableStateOf(AppTab.Feed) }
    val containerId = remember { android.view.View.generateViewId() }
    
    // Track which heavy screens have been initialized to allow for fast cold start
    // but maintain persistence once loaded.
    var mapInitialized by remember { mutableStateOf(false) }
    var cameraInitialized by remember { mutableStateOf(false) }
    
    if (tab == AppTab.Map) mapInitialized = true
    if (tab == AppTab.Camera) cameraInitialized = true

    val onTabChange: (Int) -> Unit = { index ->
        tab = when (index) {
            0 -> AppTab.Feed
            1 -> AppTab.Profile
            2 -> AppTab.Map
            3 -> AppTab.Camera
            else -> AppTab.Feed
        }
    }

    Box(Modifier.fillMaxSize()) {
        // 1. App Content (Optimized persistence: only load heavy screens when first visited)
        Box(Modifier.fillMaxSize()) {
            // Feed Screen (Loads immediately, very light)
            FeedScreen() 
            
            // Profile Screen (Light enough to keep persistent)
            Box(Modifier.fillMaxSize().alpha(if (tab == AppTab.Profile) 1f else 0f).zIndex(if (tab == AppTab.Profile) 1f else 0f)) {
                ProfileScreen()
            }

            // Map Screen (Heaviest, deferred until first visit)
            if (mapInitialized) {
                Box(Modifier.fillMaxSize().alpha(if (tab == AppTab.Map) 1f else 0f).zIndex(if (tab == AppTab.Map) 2f else 0f)) {
                    MapScreen()
                }
            }
            
            // Camera Screen (Heavy/Privacy, deferred until first visit)
            if (cameraInitialized) {
                Box(Modifier.fillMaxSize().alpha(if (tab == AppTab.Camera) 1f else 0f).zIndex(if (tab == AppTab.Camera) 1f else 0f)) {
                    CameraScreen()
                }
            }
        }

        // 2. Global Persistent Navigation Cube (Always in FRONT)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp)
                .zIndex(10f),
            contentAlignment = Alignment.BottomCenter
        ) {
            AndroidView(
                factory = { context ->
                    FragmentContainerView(context).apply {
                        id = containerId
                        setBackgroundColor(0)
                        background = null
                        val activity = context as? FragmentActivity
                        val fragment = GdxNavigationFragment(onTabChange)
                        activity?.supportFragmentManager?.beginTransaction()?.replace(id, fragment)?.commit()
                    }
                },
                modifier = Modifier.size(80.dp),
                update = { view ->
                    view.setBackgroundColor(0)
                    view.background = null
                    val activity = view.context as? FragmentActivity
                    val fragment = activity?.supportFragmentManager?.findFragmentById(view.id)
                    val glView = fragment?.view as? GLSurfaceView
                    glView?.let {
                        it.setZOrderOnTop(true)
                        it.holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT)
                    }
                }
            )
        }
    }
}
