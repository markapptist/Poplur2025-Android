package com.apptists.poplur2025.ui

import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import com.apptists.poplur2025.data.AppTab
import com.apptists.poplur2025.ui.screens.*
import android.view.View as AndroidViewItem

@Composable fun PoplurRoot() {
    var tab by remember { mutableStateOf(AppTab.Feed) }
    
    val onTabChange: (Int) -> Unit = { index ->
        tab = when (index) {
            0 -> AppTab.Feed
            1 -> AppTab.Map
            2 -> AppTab.Profile
            3 -> AppTab.Messages
            else -> AppTab.Feed
        }
    }

    Box(Modifier.fillMaxSize()) {
        // 1. App Content (Background)
        Box(Modifier.fillMaxSize()) {
            when (tab) {
                AppTab.Home -> HomeScreen(onOpenCamera = { /* ... */ }, onOpenMap = { /* ... */ })
                AppTab.Map -> MapScreen()
                AppTab.Camera -> CameraScreen()
                AppTab.Feed -> FeedScreen()
                AppTab.Messages -> MessagesScreen()
                AppTab.Profile -> ProfileScreen()
                AppTab.Settings -> SettingsScreen()
            }
        }

        // 2. Global Persistent Navigation Cube (Always in FRONT)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
            ) {
                AndroidView(
                    factory = { context ->
                        FragmentContainerView(context).apply {
                            id = AndroidViewItem.generateViewId()
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            val activity = context as? FragmentActivity
                            val fragment = GdxNavigationFragment(onTabChange)
                            activity?.supportFragmentManager?.beginTransaction()?.replace(id, fragment)?.commit()
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
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
}
