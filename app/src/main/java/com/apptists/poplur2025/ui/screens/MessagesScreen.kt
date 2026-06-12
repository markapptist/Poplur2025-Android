package com.apptists.poplur2025.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptists.poplur2025.data.DemoPostStore
import com.apptists.poplur2025.data.Post
import com.apptists.poplur2025.ui.components.RadiusDisplay
import com.apptists.poplur2025.ui.components.TrendingDisplay
import java.util.Locale

@Composable
fun MessagesScreen(onBack: () -> Unit = {}) {
    val posts = DemoPostStore.posts
    
    // Using a vertical pager style with LazyColumn where each item fills the screen
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        items(posts) { post ->
            Box(
                modifier = Modifier
                    .fillParentMaxSize()
                    .padding(16.dp)
                    .statusBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back Button
                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            color = Color.White.copy(alpha = 0.1f),
                            onClick = onBack
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.ArrowBackIosNew,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF8E8EFA), Color(0xFFE08EFA))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = post.displayName.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = post.displayName,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Navigation,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = post.locationName,
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Text(
                            text = "24h",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Caption
                    Text(
                        text = post.caption,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 40.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Footer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp, start = 4.dp, end = 4.dp), 
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Trending
                        TrendingDisplay(
                            postsCount = post.likes, // Assuming likes is the trending count here
                            radiusChange = 0, // Placeholder
                            modifier = Modifier,
                            contentColor = Color.White
                        )

                        // Radius
                        RadiusDisplay(
                            radius = post.radius,
                            totalRadius = post.totalRadius,
                            modifier = Modifier.widthIn(min = 100.dp)
                        )
                    }

                    // Coordinates and bottom instruction
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 200.dp), // Massive padding to clear the cube
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format(Locale.US, "%.4f°N %.4f°E", post.latitude ?: 0.0, post.longitude ?: 0.0),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Hold to view on map",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun MessagesScreenPreview() {
    MaterialTheme {
        MessagesScreen()
    }
}
