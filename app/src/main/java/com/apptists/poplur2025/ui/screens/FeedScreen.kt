package com.apptists.poplur2025.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptists.poplur2025.data.DemoPostStore

@Composable
fun FeedScreen() {
    Box(Modifier.fillMaxSize()) {
        // 1. Full Screen Background Content (Restaurant Style)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF120E0D)) // Dark woody background color
        ) {
            // Placeholder for the restaurant interior
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF1A1210)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Feed Content View", color = Color.DarkGray)
                }
            }
            
            // Bottom shadow gradient
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

        // 2. Top Overlay (User Info as per screenshot)
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🇯🇵", fontSize = 32.sp)
                Spacer(Modifier.width(10.dp))
                Text("🍔", fontSize = 32.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "User food_u",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "35.6301, 139.6239",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 18.sp
                    )
                }
            }
        }

        // 3. Bottom Left Overlay (Trending)
        Column(
            modifier = Modifier
                .padding(bottom = 80.dp, start = 20.dp)
                .align(Alignment.BottomStart)
        ) {
            Text(
                text = "TRENDING",
                color = Color.White.copy(alpha = 0.7f),
                letterSpacing = 1.sp,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider(
                color = Color.White, 
                thickness = 1.dp, 
                modifier = Modifier.width(120.dp).padding(vertical = 8.dp)
            )
            Text(
                text = "0",
                color = Color.White,
                fontSize = 64.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "posts",
                color = Color.White,
                fontSize = 18.sp
            )
        }

        // 4. Bottom Center (Map Button)
        Column(
            modifier = Modifier
                .padding(bottom = 60.dp)
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                onClick = { /* Navigate to Map */ },
                color = Color(0xFF232336).copy(alpha = 0.9f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(72.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🗺️", fontSize = 32.sp)
                        Text("Map", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 5. Bottom Right Overlay (Radius values)
        Column(
            modifier = Modifier
                .padding(bottom = 60.dp, end = 20.dp)
                .align(Alignment.BottomEnd),
            horizontalAlignment = Alignment.End
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "RADIUS",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(6.dp))
                Text("🛡️", color = Color.White, fontSize = 14.sp)
            }
            Text(
                text = "2065",
                color = Color.White,
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 72.sp
            )
            HorizontalDivider(
                color = Color.White, 
                thickness = 1.dp, 
                modifier = Modifier.width(100.dp).padding(vertical = 4.dp)
            )
            Text(
                text = "3959",
                color = Color.White,
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 56.sp
            )
            Text(
                text = "mi",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
