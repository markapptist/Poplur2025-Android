package com.apptists.poplur2025.ui

import android.app.Activity
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.apptists.poplur2025.data.AppTab
import com.apptists.poplur2025.ui.screens.*
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidGraphics

@Composable fun PoplurRoot() {
    var tab by remember { mutableStateOf(AppTab.Home) }
    
    Box(Modifier.fillMaxSize()) {
        // libGDX Cube Background for navigation
        AndroidView(
            factory = { context ->
                val config = AndroidApplicationConfiguration().apply {
                    r = 8; g = 8; b = 8; a = 8
                }
                val adapter = CubeNavigationAdapter { index ->
                    tab = when (index) {
                        0 -> AppTab.Home
                        1 -> AppTab.Feed
                        2 -> AppTab.Messages
                        3 -> AppTab.Profile
                        else -> AppTab.Home
                    }
                }
                // Use the context which is our MainActivity
                val gdxApp = context as AndroidApplication
                val view = gdxApp.initializeForView(adapter, config)
                
                if (view is GLSurfaceView) {
                    view.getHolder().setFormat(android.graphics.PixelFormat.TRANSLUCENT)
                }
                view
            },
            modifier = Modifier.fillMaxSize()
        )

        // Current Screen Overlay
        // IMPORTANT: Ensure child screens have transparent backgrounds
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
