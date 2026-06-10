package com.apptists.poplur2025.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

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
    val animatedPosts = rememberAnimatedCounter(postsCount, label = "trending-posts-count")
    val countPulse by animateFloatAsState(
        targetValue = if (animatedPosts == postsCount) 1f else 1.08f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "trending-posts-pulse"
    )

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
            text = NumberFormat.getNumberInstance(Locale.US).format(animatedPosts),
            color = contentColor,
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 38.sp,
            modifier = Modifier.graphicsLayer {
                scaleX = countPulse
                scaleY = countPulse
            }
        )
        Text(
            text = "posts",
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun rememberAnimatedCounter(
    targetValue: Int,
    label: String,
    durationMillis: Int = 1200
): Int {
    var animatedTarget by remember { mutableIntStateOf(0) }
    LaunchedEffect(targetValue) {
        animatedTarget = targetValue
    }
    val animatedValue by animateIntAsState(
        targetValue = animatedTarget,
        animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
        label = label
    )
    return animatedValue
}
