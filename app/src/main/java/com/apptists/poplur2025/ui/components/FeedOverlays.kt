package com.apptists.poplur2025.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White
) {
    val changeColor = if (radiusChange >= 0) Color.Green else Color.Red
    val changeIcon = if (radiusChange >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "TRENDING",
            color = contentColor.copy(alpha = 0.7f),
            letterSpacing = 1.4.sp,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )
        Icon(
            imageVector = changeIcon,
            contentDescription = null,
            tint = changeColor,
            modifier = Modifier
                .size(38.dp)
                .padding(vertical = 4.dp)
        )
        HorizontalDivider(
            color = contentColor, 
            thickness = 2.dp, 
            modifier = Modifier.width(70.dp).padding(vertical = 4.dp)
        )
        Text(
            text = postsCount.toString(),
            color = contentColor,
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 38.sp
        )
        Text(
            text = "posts",
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black
        )
    }
}
