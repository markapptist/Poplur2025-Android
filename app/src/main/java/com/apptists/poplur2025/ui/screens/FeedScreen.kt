package com.apptists.poplur2025.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.apptists.poplur2025.data.DemoPostStore
import com.apptists.poplur2025.ui.components.RadiusDisplay
import com.apptists.poplur2025.ui.components.TrendingDisplay
import com.apptists.poplur2025.ui.components.UserInfoOverlay

@Composable
fun FeedScreen() {
    val posts = DemoPostStore.posts
    var currentPostIndex by remember { mutableStateOf(0) }
    val currentPost = posts[currentPostIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        if (dragAmount > 50) { // Swipe Down
                            if (currentPostIndex > 0) currentPostIndex--
                            change.consume()
                        } else if (dragAmount < -50) { // Swipe Up
                            if (currentPostIndex < posts.size - 1) currentPostIndex++
                            change.consume()
                        }
                    }
                )
            }
    ) {
        // 1. Background Content (Replaced instantly like TV channels)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF120E0D))
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF1A1210)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "TV Channel ${currentPostIndex + 1}\n${currentPost.caption}", 
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )
        }

        // 2. Top Overlay (Live User Data)
        UserInfoOverlay(
            displayName = currentPost.displayName,
            location = currentPost.locationName,
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopStart)
        )

        // 3. Bottom Left Overlay (Live Post Data)
        TrendingDisplay(
            postsCount = currentPost.userPostsCount,
            radiusChange = currentPost.radiusChange,
            modifier = Modifier
                .padding(bottom = 80.dp, start = 20.dp)
                .align(Alignment.BottomStart)
        )

        // 4. Bottom Center placeholder space for the global Cube
        Spacer(
            modifier = Modifier
                .padding(bottom = 60.dp)
                .size(80.dp)
                .align(Alignment.BottomCenter)
        )

        // 5. Bottom Right Overlay (Live Radius Data)
        RadiusDisplay(
            radius = currentPost.radius,
            totalRadius = currentPost.totalRadius,
            modifier = Modifier
                .padding(bottom = 60.dp, end = 20.dp)
                .align(Alignment.BottomEnd)
        )
    }
}
