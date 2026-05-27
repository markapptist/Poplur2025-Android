package com.apptists.poplur2025.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.opengl.GLSurfaceView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.Color
import com.apptists.poplur2025.data.AppTab
import com.apptists.poplur2025.ui.screens.*
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidFragmentApplication

import android.view.View as AndroidViewItem
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView

@Composable fun PoplurRoot() {
    var tab by remember { mutableStateOf(AppTab.Home) }
    
    // Root container must be transparent or themed correctly
    Surface(color = MaterialTheme.colorScheme.background.copy(alpha = 0.1f)) {
        Box(Modifier.fillMaxSize()) {
            // libGDX Cube Background
            AndroidView(
                factory = { context ->
                    FragmentContainerView(context).apply {
                        id = AndroidViewItem.generateViewId()
                        val activity = context as? FragmentActivity
                        
                        // Create fragment
                        val fragment = GdxNavigationFragment { index ->
                            // Update tab state on main thread
                            val newTab = when (index) {
                                0 -> AppTab.Home
                                1 -> AppTab.Feed
                                2 -> AppTab.Messages
                                3 -> AppTab.Profile
                                else -> AppTab.Home
                            }
                            tab = newTab
                        }
                        
                        activity?.supportFragmentManager?.beginTransaction()?.replace(id, fragment)?.commit()
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    // Access the context provided by LocalContext in a safe way if needed, 
                    // but AndroidView already provides the view which has the context.
                    val activity = view.context as? FragmentActivity
                    val fragment = activity?.supportFragmentManager?.findFragmentById(view.id)
                    val glView = fragment?.view as? GLSurfaceView
                    glView?.let {
                        it.setZOrderOnTop(false)
                        it.holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT)
                    }
                }
            )

            // Current Screen Overlay
            // Child screens should use transparent colors to see the cube
            Box(Modifier.fillMaxSize()) {
                when (tab) {
                    AppTab.Home -> HomeScreen(onOpenCamera = { tab = AppTab.Camera }, onOpenMap = { tab = AppTab.Map })
                    AppTab.Map -> MapScreen()
                    AppTab.Camera -> CameraScreen()
                    AppTab.Feed -> FeedScreen()
                    AppTab.Messages -> MessagesScreen()
                    AppTab.Profile -> ProfileScreen()
                    AppTab.Settings -> SettingsScreen()
                }
            }
        }
    }
}
