package com.apptists.poplur2025.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import kotlin.math.max
import kotlin.math.min

/**
 * Animated shader-style number text with adaptive hue-detection glow.
 * Ported from PoplurNumberShaders.metal using layered Compose shadows + animated gradient.
 * Pure Compose — no LibGDX fragment, avoids conflict with the navigation cube's GL context.
 */
@Composable
fun ShaderNumberText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
    lineHeight: TextUnit = TextUnit.Unspecified,
    intensity: Float = 1f,
    glowColor: Color = DefaultPoplurGlowColor,
    autoSize: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Clip,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    @Suppress("UNUSED_PARAMETER") forceShader: Boolean = false
) {
    val effectiveGlowColor = remember(color, glowColor) {
        if (glowColor == DefaultPoplurGlowColor) poplurIlluminationColor(color) else glowColor
    }

    val onLayout: ((TextLayoutResult) -> Unit)? = if (autoSize) ({ }) else null
    val maxL = if (autoSize) 1 else maxLines
    val wrap = if (autoSize) false else softWrap
    val ovf = if (autoSize) TextOverflow.Visible else overflow

    Box(modifier = modifier) {
        // Diffuse illumination halo.
        Text(
            text = text,
            maxLines = maxL,
            softWrap = wrap,
            overflow = ovf,
            style = TextStyle(
                color = Color.Transparent,
                fontSize = fontSize,
                fontWeight = fontWeight,
                lineHeight = lineHeight,
                letterSpacing = letterSpacing,
                textAlign = textAlign ?: TextAlign.Start,
                shadow = Shadow(
                    color = effectiveGlowColor.copy(alpha = 0.35f * intensity),
                    offset = Offset.Zero,
                    blurRadius = 2f
                )
            )
        )
        // Main readable text
        Text(
            text = text,
            maxLines = maxL,
            softWrap = wrap,
            overflow = ovf,
            style = TextStyle(
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight,
                lineHeight = lineHeight,
                letterSpacing = letterSpacing,
                textAlign = textAlign ?: TextAlign.Start,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.45f * intensity),
                    offset = Offset(1f, 1f),
                    blurRadius = 4f
                )
            )
        )
    }
}

private val DefaultPoplurGlowColor = Color(0xFFFF8A1C)

private fun poplurIlluminationColor(color: Color): Color {
    val lum = color.red * 0.2126f + color.green * 0.7152f + color.blue * 0.0722f
    return if (lum > 0.72f) Color.White else color
}

private fun poplurHighlightColor(color: Color): Color = Color(
    red = color.red + (1f - color.red) * 0.72f,
    green = color.green + (1f - color.green) * 0.72f,
    blue = color.blue + (1f - color.blue) * 0.72f,
    alpha = color.alpha
)

private fun poplurGlowPalette(color: Color): Pair<Color, Color> {
    val r = color.red
    val g = color.green
    val b = color.blue
    val cmax = max(r, max(g, b))
    val cmin = min(r, min(g, b))
    val delta = cmax - cmin

    var hue = 0f
    if (delta > 0.001f) {
        hue = when (cmax) {
            r -> (((g - b) / delta + 6f) % 6f) / 6f
            g -> (2f + (b - r) / delta) / 6f
            else -> (4f + (r - g) / delta) / 6f
        }
    }

    val lum = r * 0.2126f + g * 0.7152f + b * 0.0722f
    val isRed = ((1f - smoothstep(0f, 0.10f, hue)) + smoothstep(0.90f, 1f, hue)).coerceIn(0f, 1f)
    val isGreen = smoothstep(0.25f, 0.38f, hue) * (1f - smoothstep(0.38f, 0.50f, hue))
    val isBlue = smoothstep(0.55f, 0.65f, hue) * (1f - smoothstep(0.65f, 0.78f, hue))
    val isNeutral = smoothstep(0.72f, 0.90f, lum) * (1f - isRed) * (1f - isGreen) * (1f - isBlue)
    val isOther = (1f - isRed - isGreen - isBlue - isNeutral).coerceIn(0f, 1f)

    val glowA = Color(
        red = isRed * 1.00f + isGreen * 0.65f + isBlue * 0.40f + isNeutral * 0.82f + isOther * 1.00f,
        green = isRed * 0.42f + isGreen * 1.00f + isBlue * 0.12f + isNeutral * 0.88f + isOther * 0.62f,
        blue = isRed * 0.06f + isGreen * 0.10f + isBlue * 1.00f + isNeutral * 1.00f + isOther * 0.18f
    )
    val glowB = Color(
        red = isRed * 1.00f + isGreen * 0.18f + isBlue * 0.12f + isNeutral * 0.18f + isOther * 0.18f,
        green = isRed * 0.10f + isGreen * 1.00f + isBlue * 0.72f + isNeutral * 0.75f + isOther * 0.82f,
        blue = isRed * 0.28f + isGreen * 0.55f + isBlue * 1.00f + isNeutral * 1.00f + isOther * 1.00f
    )
    return glowA to glowB
}

private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}
