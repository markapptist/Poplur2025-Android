package com.apptists.poplur2025.ui

import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.apptists.poplur2025.data.AppTab
import com.apptists.poplur2025.ui.screens.*
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import android.view.View as AndroidViewItem

@Composable fun PoplurRoot() {
    var tab by remember { mutableStateOf(AppTab.Home) }
    
    Box(Modifier.fillMaxSize()) {
        // 1. Current Screen (Now in the Background)
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

        // 2. libGDX Cube (Now in the FRONT)
        AndroidView(
            factory = { context ->
                FragmentContainerView(context).apply {
                    id = AndroidViewItem.generateViewId()
                    val activity = context as? FragmentActivity
                    
                    val fragment = GdxNavigationFragment { index ->
                        tab = when (index) {
                            0 -> AppTab.Home
                            1 -> AppTab.Feed
                            2 -> AppTab.Messages
                            3 -> AppTab.Profile
                            else -> AppTab.Home
                        }
                    }
                    
                    activity?.supportFragmentManager?.beginTransaction()?.replace(id, fragment)?.commit()
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
