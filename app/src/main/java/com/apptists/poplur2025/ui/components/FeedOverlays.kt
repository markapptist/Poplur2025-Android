package com.apptists.poplur2025.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UserInfoOverlay(
    displayName: String,
    location: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🇯🇵", fontSize = 32.sp)
            Spacer(Modifier.width(10.dp))
            Text("🍔", fontSize = 32.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = displayName,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = location,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun TrendingDisplay(
    postsCount: Int,
    radiusChange: Int,
    modifier: Modifier = Modifier
) {
    val changeColor = if (radiusChange >= 0) Color.Green else Color.Red
    val changeLabel = if (radiusChange >= 0) "UP" else "DOWN"

    Column(modifier = modifier) {
        Text(
            text = "TRENDING",
            color = Color.White.copy(alpha = 0.7f),
            letterSpacing = 1.sp,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = changeLabel,
            color = changeColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        HorizontalDivider(
            color = Color.White, 
            thickness = 1.dp, 
            modifier = Modifier.width(120.dp).padding(vertical = 8.dp)
        )
        Text(
            text = postsCount.toString(),
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
}
