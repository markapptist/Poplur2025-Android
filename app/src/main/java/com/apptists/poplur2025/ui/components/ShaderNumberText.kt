package com.apptists.poplur2025.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Animated shader text with adaptive hue-detection glow.
 *
 * When [autoSize] is true the composable iteratively scales down [fontSize]
 * until the text fits in one line — preserving animation throughout.
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
    glowColor: Color = Color(0xFFFF8A1C),
    autoSize: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val transition = rememberInfiniteTransition(label = "poplur-number-shader")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "poplur-number-shader-time"
    )

    // When autoSize is active, track the current font size independently
    var scaledFontSize by remember(text, fontSize) { mutableStateOf(fontSize) }

    val brush = remember(color, time, intensity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PoplurNumberShaderBrush(baseColor = color, time = time, intensity = intensity)
        } else {
            Brush.linearGradient(
                colors = listOf(color, Color(0xFFFFA12B), Color(0xFF54D8FF), color),
                start = Offset.Zero,
                end = Offset.Infinite
            )
        }
    }

    val onLayout: ((TextLayoutResult) -> Unit)? = if (autoSize) {
        { result ->
            if (result.didOverflowWidth || result.didOverflowHeight) {
                // Shrink by 8% per overflow event — converges in ≤5 recompositions
                scaledFontSize = scaledFontSize * 0.92f
            }
        }
    } else null

    Text(
        text = text,
        modifier = modifier,
        maxLines = if (autoSize) 1 else maxLines,
        softWrap = if (autoSize) false else softWrap,
        overflow = if (autoSize) TextOverflow.Visible else overflow,
        onTextLayout = onLayout,
        style = TextStyle(
            brush = brush,
            fontSize = if (autoSize) scaledFontSize else fontSize,
            fontWeight = fontWeight,
            lineHeight = if (lineHeight == TextUnit.Unspecified) TextUnit.Unspecified
                         else if (autoSize) scaledFontSize else lineHeight,
            shadow = Shadow(
                color = glowColor.copy(alpha = 0.32f * intensity),
                offset = Offset.Zero,
                blurRadius = 9f * intensity
            )
        )
    )
}

@Stable
private class PoplurNumberShaderBrush(
    private val baseColor: Color,
    private val time: Float,
    private val intensity: Float
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        return RuntimeShader(POPLUR_NUMBER_SHADER).apply {
            setFloatUniform("size", size.width.coerceAtLeast(1f), size.height.coerceAtLeast(1f))
            setFloatUniform("time", time)
            setFloatUniform("intensity", intensity)
            setFloatUniform("baseColor", baseColor.red, baseColor.green, baseColor.blue, baseColor.alpha)
        }
    }
}

// Adaptive Poplur glow — detects hue of each pixel and selects
// the best complementary glow palette automatically.
//   Red   labels → warm amber-red glow
//   Green labels → lime-yellow glow
//   Blue  labels → electric cyan-purple glow
//   White labels → cool white-blue electric glow
private const val POPLUR_NUMBER_SHADER = """
uniform float2 size;
uniform float  time;
uniform float  intensity;
uniform float4 baseColor;

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / max(size, float2(1.0, 1.0));

    float shimmer = 0.5 + 0.5 * sin(uv.x * 11.0 + uv.y * 7.0 + time * 2.0);
    float scan    = 0.5 + 0.5 * sin(fragCoord.y * 1.65 + time * 7.0);
    float pulse   = 0.5 + 0.5 * sin(time * 1.7);

    // Hue detection (RGB → hue 0..1)
    float r = baseColor.r, g = baseColor.g, b = baseColor.b;
    float cmax = max(r, max(g, b));
    float cmin = min(r, min(g, b));
    float delta = cmax - cmin;

    float hue = 0.0;
    if (delta > 0.001) {
        if      (cmax == r) hue = mod((g - b) / delta + 6.0, 6.0);
        else if (cmax == g) hue = 2.0 + (b - r) / delta;
        else                hue = 4.0 + (r - g) / delta;
        hue /= 6.0;
    }
    float lum = dot(float3(r, g, b), float3(0.2126, 0.7152, 0.0722));

    // Per-hue masks
    float isRed     = clamp((1.0 - smoothstep(0.0,  0.10, hue)) + smoothstep(0.90, 1.0, hue), 0.0, 1.0);
    float isGreen   = smoothstep(0.25, 0.38, hue) * (1.0 - smoothstep(0.38, 0.50, hue));
    float isBlue    = smoothstep(0.55, 0.65, hue) * (1.0 - smoothstep(0.65, 0.78, hue));
    float isNeutral = smoothstep(0.72, 0.90, lum)  * (1.0 - isRed) * (1.0 - isGreen) * (1.0 - isBlue);
    float isOther   = clamp(1.0 - isRed - isGreen - isBlue - isNeutral, 0.0, 1.0);

    // Adaptive glow palettes
    float3 glowA = isRed     * float3(1.00, 0.42, 0.06)
                 + isGreen   * float3(0.65, 1.00, 0.10)
                 + isBlue    * float3(0.40, 0.12, 1.00)
                 + isNeutral * float3(0.82, 0.88, 1.00)
                 + isOther   * float3(1.00, 0.62, 0.18);

    float3 glowB = isRed     * float3(1.00, 0.10, 0.28)
                 + isGreen   * float3(0.18, 1.00, 0.55)
                 + isBlue    * float3(0.12, 0.72, 1.00)
                 + isNeutral * float3(0.18, 0.75, 1.00)
                 + isOther   * float3(0.18, 0.82, 1.00);

    float3 hot     = mix(glowA, glowB, shimmer * 0.55);
    float  heat    = (0.22 + shimmer * 0.38 + scan * 0.10 + pulse * 0.10) * intensity;
    float3 base3   = float3(r, g, b);
    float3 boosted = mix(base3, max(base3, hot * max(lum, 0.42)), heat);
    boosted       += scan * 0.055 * intensity;

    return half4(half3(boosted), half(baseColor.a));
}
"""
