package com.apptists.poplur2025.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

/**
 * A shader-enhanced replacement for standard Compose Text.
 * Adds a glow and animated shimmer effect consistent with the Poplur aesthetic.
 */
@Composable
fun PoplurText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
    intensity: Float = 1f
) {
    val transition = rememberInfiniteTransition(label = "poplur-text-shader")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "poplur-text-shader-time"
    )

    val textColor = if (color != Color.Unspecified) color else style.color
    val effectiveGlowColor = remember(textColor) {
        val lum = textColor.red * 0.2126f + textColor.green * 0.7152f + textColor.blue * 0.0722f
        if (lum > 0.72f) Color.White else textColor
    }

    val highlightColor = remember(textColor) {
        Color(
            red = textColor.red + (1f - textColor.red) * 0.72f,
            green = textColor.green + (1f - textColor.green) * 0.72f,
            blue = textColor.blue + (1f - textColor.blue) * 0.72f,
            alpha = textColor.alpha
        )
    }

    val scan = ((time * 42f) % 220f) - 110f
    val shimmerBrush = remember(highlightColor, scan, intensity) {
        Brush.linearGradient(
            colors = listOf(
                textColor.copy(alpha = 0f),
                highlightColor.copy(alpha = 0.12f * intensity),
                Color.White.copy(alpha = 0.32f * intensity),
                highlightColor.copy(alpha = 0.12f * intensity),
                textColor.copy(alpha = 0f)
            ),
            start = Offset(scan, 0f),
            end = Offset(scan + 180f, 60f)
        )
    }

    Box(modifier = modifier) {
        // Subtle glow / diffuse layer
        Text(
            text = text,
            modifier = Modifier,
            color = Color.Transparent,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            onTextLayout = onTextLayout,
            style = style.copy(
                shadow = Shadow(
                    color = effectiveGlowColor.copy(alpha = 0.28f * intensity),
                    offset = Offset.Zero,
                    blurRadius = 14f
                )
            )
        )

        // Main text with shimmer and primary shadow
        Text(
            text = text,
            modifier = Modifier,
            color = textColor,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            onTextLayout = onTextLayout,
            style = style.copy(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.40f * intensity),
                    offset = Offset(1f, 1f),
                    blurRadius = 3f
                ),
                brush = if (intensity > 0.5f) shimmerBrush else null
            )
        )
    }
}
