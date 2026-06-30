package com.apptists.poplur2025.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun ShaderIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    intensity: Float = 1f
) {
    val base = if (tint == Color.Unspecified) Color.White else tint
    val glow = remember(base) {
        val lum = base.red * 0.2126f + base.green * 0.7152f + base.blue * 0.0722f
        if (lum > 0.72f) Color.White else base
    }
    val highlight = remember(base) {
        Color(
            red = base.red + (1f - base.red) * 0.72f,
            green = base.green + (1f - base.green) * 0.72f,
            blue = base.blue + (1f - base.blue) * 0.72f,
            alpha = base.alpha
        )
    }
    val shimmerBrush = remember(base, highlight, intensity) {
        val scan = 18f
        Brush.linearGradient(
            colors = listOf(
                base.copy(alpha = 0f),
                highlight.copy(alpha = 0.16f * intensity),
                Color.White.copy(alpha = 0.38f * intensity),
                highlight.copy(alpha = 0.16f * intensity),
                base.copy(alpha = 0f)
            ),
            start = Offset(scan, 0f),
            end = Offset(scan + 190f, 90f)
        )
    }

    Box(modifier = modifier) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = glow.copy(alpha = 0.30f * intensity),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.08f
                    scaleY = 1.08f
                }
        )
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = base,
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    onDrawWithContent {
                        drawContent()
                        drawRect(shimmerBrush, blendMode = BlendMode.SrcAtop)
                    }
                }
        )
    }
}
