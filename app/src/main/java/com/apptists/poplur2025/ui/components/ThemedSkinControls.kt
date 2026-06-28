package com.apptists.poplur2025.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apptists.poplur2025.ui.CubeSkin
import com.apptists.poplur2025.ui.uiTheme
import kotlin.math.min

@Composable
fun ThemedSkinPlate(
    skin: CubeSkin,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    active: Boolean = false,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    val theme = skin.uiTheme()
    val shape = RoundedCornerShape(cornerRadius)
    val borderColor = when (skin) {
        CubeSkin.Toy -> theme.border
        CubeSkin.Ice -> Color(0xFFD9FAFF).copy(alpha = if (active) 0.96f else 0.74f)
        CubeSkin.Carbon -> Color(0xFFFF55D5).copy(alpha = if (active) 0.94f else 0.66f)
        CubeSkin.Rainbow, CubeSkin.Default -> Color.White.copy(alpha = if (active) 0.92f else 0.54f)
        else -> theme.border.copy(alpha = if (active) 0.94f else 0.68f)
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (active) 14.dp else 8.dp,
                shape = shape,
                ambientColor = theme.shadow,
                spotColor = theme.shadow
            )
            .clip(shape)
            .background(skinPlateBrush(skin))
            .border(if (active) 2.dp else 1.dp, borderColor, shape),
        contentAlignment = contentAlignment
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = cornerRadius.toPx()
            val stroke = if (active) 2.4.dp.toPx() else 1.2.dp.toPx()
            drawSkinPlateTexture(skin, active, radius)
            drawRoundRect(
                color = Color.White.copy(alpha = if (skin == CubeSkin.Ice) 0.32f else 0.16f),
                topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
                size = Size(
                    (size.width - 6.dp.toPx()).coerceAtLeast(0f),
                    (size.height - 6.dp.toPx()).coerceAtLeast(0f)
                ),
                cornerRadius = CornerRadius((radius - 3.dp.toPx()).coerceAtLeast(0f)),
                style = Stroke(width = 1.dp.toPx())
            )
            drawRoundRect(
                color = theme.accent.copy(alpha = if (active) 0.62f else 0.24f),
                cornerRadius = CornerRadius(radius),
                style = Stroke(width = stroke)
            )
        }
        content()
    }
}

fun skinPlateBrush(skin: CubeSkin): Brush {
    val theme = skin.uiTheme()
    return when (skin) {
        CubeSkin.Toy -> Brush.linearGradient(
            listOf(Color(0xFFFFFCF0), Color(0xFFFFE9BD), Color.White)
        )
        CubeSkin.Chocolate -> Brush.linearGradient(
            listOf(Color(0xFF7B350C), Color(0xFF2A0D02), Color(0xFF5C2306))
        )
        CubeSkin.Camo -> Brush.linearGradient(
            listOf(Color(0xFF2E3217), Color(0xFF0B1208), Color(0xFF4A4320))
        )
        CubeSkin.Ice -> Brush.linearGradient(
            listOf(Color(0xBFD8F8FF), Color(0x882B99D7), Color(0xBFFFFFFF))
        )
        CubeSkin.Gold -> Brush.linearGradient(
            listOf(Color(0xFFFFE978), Color(0xFF5B3502), Color(0xFFFFC02F))
        )
        CubeSkin.Carbon -> Brush.linearGradient(
            listOf(Color(0xFF4C0638), Color(0xFF090014), Color(0xFFFF32C9))
        )
        CubeSkin.Space -> Brush.linearGradient(
            listOf(Color(0xFF070012), Color(0xFF18043E), Color(0xFF05000A))
        )
        CubeSkin.Rainbow, CubeSkin.Default -> Brush.linearGradient(
            listOf(
                Color(0xFFFF2C2C),
                Color(0xFFFFD323),
                Color(0xFF10E85A),
                Color(0xFF05D9FF),
                Color(0xFFFF35D3)
            )
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSkinPlateTexture(
    skin: CubeSkin,
    active: Boolean,
    radius: Float
) {
    val w = size.width
    val h = size.height
    when (skin) {
        CubeSkin.Camo -> {
            val colors = listOf(
                Color(0xFF596034).copy(alpha = 0.42f),
                Color(0xFF1B2712).copy(alpha = 0.48f),
                Color(0xFF8A7A3E).copy(alpha = 0.24f),
                Color(0xFF0A0F07).copy(alpha = 0.52f)
            )
            repeat(18) { index ->
                val x = ((index * 31) % 100) / 100f * w - w * 0.12f
                val y = ((index * 47) % 100) / 100f * h - h * 0.12f
                drawOval(
                    color = colors[index % colors.size],
                    topLeft = Offset(x, y),
                    size = Size(w * (0.22f + (index % 4) * 0.035f), h * 0.18f)
                )
            }
            drawBolts(color = Color(0xFFC4BA70).copy(alpha = 0.66f))
        }
        CubeSkin.Ice -> {
            repeat(9) { index ->
                val x = ((index * 29) % 100) / 100f * w
                val startY = ((index * 41) % 100) / 100f * h
                drawLine(
                    color = Color.White.copy(alpha = 0.28f),
                    start = Offset(x, startY),
                    end = Offset((x + w * 0.24f).coerceAtMost(w), (startY + h * 0.16f).coerceAtMost(h)),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color(0xFF6EE6FF).copy(alpha = 0.24f),
                    start = Offset((x + w * 0.08f).coerceAtMost(w), startY),
                    end = Offset((x + w * 0.02f).coerceAtLeast(0f), (startY + h * 0.20f).coerceAtMost(h)),
                    strokeWidth = 0.8.dp.toPx()
                )
            }
        }
        CubeSkin.Carbon, CubeSkin.Rainbow, CubeSkin.Default, CubeSkin.Space -> {
            val sparkleColor = if (skin == CubeSkin.Ice) Color.White else Color.White.copy(alpha = 0.9f)
            repeat(if (active) 26 else 16) { index ->
                val x = ((index * 37) % 100) / 100f * w
                val y = ((index * 53) % 100) / 100f * h
                val r = 0.9.dp.toPx() + (index % 3) * 0.35.dp.toPx()
                drawCircle(sparkleColor.copy(alpha = 0.26f + (index % 4) * 0.09f), r, Offset(x, y))
                if (index % 5 == 0) {
                    drawLine(sparkleColor.copy(alpha = 0.52f), Offset(x - r * 2f, y), Offset(x + r * 2f, y), 0.8.dp.toPx())
                    drawLine(sparkleColor.copy(alpha = 0.52f), Offset(x, y - r * 2f), Offset(x, y + r * 2f), 0.8.dp.toPx())
                }
            }
        }
        CubeSkin.Toy -> {
            val corner = min(w, h) * 0.28f
            drawRoundRect(Color(0xFFE82828).copy(alpha = 0.92f), size = Size(corner, corner), cornerRadius = CornerRadius(radius * 0.72f))
            drawRoundRect(Color(0xFF0B58BE).copy(alpha = 0.92f), topLeft = Offset(w - corner, 0f), size = Size(corner, corner), cornerRadius = CornerRadius(radius * 0.72f))
            drawRoundRect(Color(0xFFFFC521).copy(alpha = 0.92f), topLeft = Offset(0f, h - corner), size = Size(corner, corner), cornerRadius = CornerRadius(radius * 0.72f))
            drawRoundRect(Color(0xFFE82828).copy(alpha = 0.92f), topLeft = Offset(w - corner, h - corner), size = Size(corner, corner), cornerRadius = CornerRadius(radius * 0.72f))
        }
        CubeSkin.Chocolate -> {
            drawRoundRect(
                Color.White.copy(alpha = 0.12f),
                topLeft = Offset(w * 0.08f, h * 0.08f),
                size = Size(w * 0.52f, h * 0.18f),
                cornerRadius = CornerRadius(radius)
            )
            drawCircle(Color(0xFF1B0801).copy(alpha = 0.25f), min(w, h) * 0.18f, Offset(w * 0.72f, h * 0.74f))
        }
        CubeSkin.Gold -> {
            drawRoundRect(
                Color.White.copy(alpha = 0.20f),
                topLeft = Offset(w * 0.08f, h * 0.06f),
                size = Size(w * 0.74f, h * 0.16f),
                cornerRadius = CornerRadius(radius)
            )
            drawLine(Color(0xFFFFF0A0).copy(alpha = 0.36f), Offset(w * 0.12f, h * 0.84f), Offset(w * 0.88f, h * 0.84f), 1.dp.toPx())
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBolts(color: Color) {
    val boltRadius = min(size.width, size.height) * 0.055f
    listOf(
        Offset(size.width * 0.16f, size.height * 0.16f),
        Offset(size.width * 0.84f, size.height * 0.16f),
        Offset(size.width * 0.16f, size.height * 0.84f),
        Offset(size.width * 0.84f, size.height * 0.84f)
    ).forEach { center ->
        drawCircle(Color.Black.copy(alpha = 0.42f), boltRadius * 1.25f, center)
        drawCircle(color, boltRadius, center)
    }
}
