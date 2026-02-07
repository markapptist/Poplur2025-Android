package com.poplur.app.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.poplur.app.ui.screens.CameraScreen
import com.poplur.app.ui.screens.FeedScreen
import com.poplur.app.ui.screens.MapScreen
import com.poplur.app.ui.screens.MessagesScreen
import com.poplur.app.ui.screens.ProfileScreen
import com.poplur.app.ui.screens.ThreadScreen

sealed class Route(val path: String, val label: String) {
    data object Feed : Route("feed", "Feed")
    data object Map : Route("map", "Map")
    data object Camera : Route("camera", "Camera")
    data object Messages : Route("messages", "Messages")
    data object Profile : Route("profile", "Profile")
    data object Thread : Route("thread/{threadId}", "Thread") {
        fun create(threadId: String) = "thread/$threadId"
    }
}

@Composable
fun PoplurApp(navController: NavHostController = rememberNavController()) {
    val items = listOf(Route.Feed, Route.Map, Route.Camera, Route.Messages, Route.Profile)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.path } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.path) {
                                popUpTo(Route.Feed.path) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            val icon = when (item) {
                                Route.Feed -> Icons.Default.PlayArrow
                                Route.Map -> Icons.Default.Map
                                Route.Camera -> Icons.Default.CameraAlt
                                Route.Messages -> Icons.Default.Message
                                Route.Profile -> Icons.Default.Person
                                else -> Icons.Default.PlayArrow
                            }
                            Icon(icon, contentDescription = item.label)
                        },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        AppNavHost(navController = navController, padding = padding)
    }
}

@Composable
private fun AppNavHost(navController: NavHostController, padding: PaddingValues) {
    NavHost(
        navController = navController,
        startDestination = Route.Feed.path,
        modifier = Modifier.padding(padding)
    ) {
        composable(Route.Feed.path) { FeedScreen() }
        composable(Route.Map.path) { MapScreen() }
        composable(Route.Camera.path) { CameraScreen() }
        composable(Route.Messages.path) { MessagesScreen(onOpenThread = { navController.navigate(Route.Thread.create(it)) }) }
        composable(Route.Profile.path) { ProfileScreen() }
        composable(Route.Thread.path) { backStack ->
            val threadId = backStack.arguments?.getString("threadId").orEmpty()
            ThreadScreen(threadId = threadId, onBack = { navController.popBackStack() })
        }
    }
}
