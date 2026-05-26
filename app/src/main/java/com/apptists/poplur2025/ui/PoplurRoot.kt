package com.apptists.poplur2025.ui

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
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidFragmentApplication

import android.view.View as AndroidViewItem
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView

@Composable fun PoplurRoot() {
    var tab by remember { mutableStateOf(AppTab.Home) }
    
    Box(Modifier.fillMaxSize()) {
        // libGDX Cube Background for navigation
        AndroidView(
            factory = { context ->
                FragmentContainerView(context).apply {
                    id = AndroidViewItem.generateViewId()
                    val activity = context as? FragmentActivity
                    activity?.supportFragmentManager?.beginTransaction()?.replace(id, GdxNavigationFragment { index ->
                        val newTab = when (index) {
                            0 -> AppTab.Home
                            1 -> AppTab.Feed
                            2 -> AppTab.Messages
                            3 -> AppTab.Profile
                            else -> AppTab.Home
                        }
                        tab = newTab
                    })?.commit()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Current Screen Overlay
        // IMPORTANT: Child screens must NOT have solid backgrounds (e.g. solid Surface)
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
