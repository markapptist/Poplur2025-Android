package com.apptists.poplur2025.ui

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.apptists.poplur2025.R

data class CubeSkinUiTheme(
    val background: Color,
    val backgroundMid: Color,
    val backgroundDeep: Color,
    val surface: Color,
    val surfaceStrong: Color,
    val controlFill: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val accent: Color,
    val accent2: Color,
    val border: Color,
    val badgeFill: Color,
    val badgeText: Color,
    val shadow: Color,
    val textureAlpha: Float,
    val isLight: Boolean
)

fun CubeSkin.uiTheme(): CubeSkinUiTheme = when (this) {
    CubeSkin.Default, CubeSkin.Rainbow -> CubeSkinUiTheme(
        background = Color(0xFF02071F),
        backgroundMid = Color(0xFF063229),
        backgroundDeep = Color(0xFF29063D),
        surface = Color(0xD1081138),
        surfaceStrong = Color(0xEE08062F),
        controlFill = Color.White.copy(alpha = 0.12f),
        primaryText = Color.White,
        secondaryText = Color.White.copy(alpha = 0.68f),
        accent = Color(0xFF35F7FF),
        accent2 = Color(0xFFFF39E8),
        border = Color.White.copy(alpha = 0.34f),
        badgeFill = Color.Black.copy(alpha = 0.58f),
        badgeText = Color.White,
        shadow = Color(0xAA9D22FF),
        textureAlpha = 0.26f,
        isLight = false
    )
    CubeSkin.Carbon -> CubeSkinUiTheme(
        background = Color(0xFF230016),
        backgroundMid = Color(0xFF520638),
        backgroundDeep = Color(0xFF08000C),
        surface = Color(0xD12E041F),
        surfaceStrong = Color(0xEE47052D),
        controlFill = Color.White.copy(alpha = 0.14f),
        primaryText = Color.White,
        secondaryText = Color.White.copy(alpha = 0.68f),
        accent = Color(0xFFFF3DC8),
        accent2 = Color(0xFFFFB4E4),
        border = Color(0xFFFF52D1).copy(alpha = 0.62f),
        badgeFill = Color.Black.copy(alpha = 0.62f),
        badgeText = Color.White,
        shadow = Color(0xAAFF24B8),
        textureAlpha = 0.36f,
        isLight = false
    )
    CubeSkin.Gold -> CubeSkinUiTheme(
        background = Color(0xFF141003),
        backgroundMid = Color(0xFF332305),
        backgroundDeep = Color(0xFF050500),
        surface = Color(0xE0241C08),
        surfaceStrong = Color(0xF22E2108),
        controlFill = Color(0xFFF2C72E).copy(alpha = 0.16f),
        primaryText = Color(0xFFFFF0A0),
        secondaryText = Color(0xFFCFC18F),
        accent = Color(0xFFFAC72E),
        accent2 = Color(0xFFFFEF85),
        border = Color(0xFFFAD147).copy(alpha = 0.58f),
        badgeFill = Color.Black.copy(alpha = 0.58f),
        badgeText = Color(0xFFFFF0A0),
        shadow = Color(0x88FFB81F),
        textureAlpha = 0.28f,
        isLight = false
    )
    CubeSkin.Toy -> CubeSkinUiTheme(
        background = Color(0xFFFFF5DB),
        backgroundMid = Color(0xFFFFFAEB),
        backgroundDeep = Color(0xFFF0DCB3),
        surface = Color.White.copy(alpha = 0.74f),
        surfaceStrong = Color.White.copy(alpha = 0.90f),
        controlFill = Color.White.copy(alpha = 0.92f),
        primaryText = Color(0xFF181D26),
        secondaryText = Color(0xB83A414E),
        accent = Color(0xFFE91E1E),
        accent2 = Color(0xFF0741B8),
        border = Color(0xFFFFB900).copy(alpha = 0.66f),
        badgeFill = Color(0xBB181D26),
        badgeText = Color.White,
        shadow = Color.Black.copy(alpha = 0.12f),
        textureAlpha = 0.18f,
        isLight = true
    )
    CubeSkin.Chocolate -> CubeSkinUiTheme(
        background = Color(0xFF2E1404),
        backgroundMid = Color(0xFF572909),
        backgroundDeep = Color(0xFF140701),
        surface = Color(0xE0381905),
        surfaceStrong = Color(0xF2471F05),
        controlFill = Color(0xFFB8661A).copy(alpha = 0.20f),
        primaryText = Color(0xFFFFEBD1),
        secondaryText = Color(0xCFF2C79E),
        accent = Color(0xFFF28F33),
        accent2 = Color(0xFFFFCC8A),
        border = Color(0xFFDB7A29).copy(alpha = 0.58f),
        badgeFill = Color.Black.copy(alpha = 0.56f),
        badgeText = Color.White,
        shadow = Color(0x66F2661A),
        textureAlpha = 0.26f,
        isLight = false
    )
    CubeSkin.Camo -> CubeSkinUiTheme(
        background = Color(0xFF0F170A),
        backgroundMid = Color(0xFF293014),
        backgroundDeep = Color(0xFF050A04),
        surface = Color(0xE0171F0D),
        surfaceStrong = Color(0xF21F260F),
        controlFill = Color(0xFF7A8F38).copy(alpha = 0.16f),
        primaryText = Color(0xFFF0E6A8),
        secondaryText = Color(0xC9D6D19E),
        accent = Color(0xFFDBCC57),
        accent2 = Color(0xFF7B9E40),
        border = Color(0xFFCCC26B).copy(alpha = 0.52f),
        badgeFill = Color.Black.copy(alpha = 0.55f),
        badgeText = Color(0xFFF0E6A8),
        shadow = Color(0x557D8530),
        textureAlpha = 0.34f,
        isLight = false
    )
    CubeSkin.Space -> CubeSkinUiTheme(
        background = Color(0xFF030315),
        backgroundMid = Color(0xFF100838),
        backgroundDeep = Color(0xFF000004),
        surface = Color(0xDD09051F),
        surfaceStrong = Color(0xF20B0626),
        controlFill = Color.White.copy(alpha = 0.10f),
        primaryText = Color.White,
        secondaryText = Color.White.copy(alpha = 0.64f),
        accent = Color(0xFF9E4DFF),
        accent2 = Color(0xFF5AD8FF),
        border = Color(0xFFB35CFF).copy(alpha = 0.54f),
        badgeFill = Color.Black.copy(alpha = 0.64f),
        badgeText = Color.White,
        shadow = Color(0x889E4DFF),
        textureAlpha = 0.32f,
        isLight = false
    )
    CubeSkin.Ice -> CubeSkinUiTheme(
        background = Color(0xFF001229),
        backgroundMid = Color(0xFF05345F),
        backgroundDeep = Color(0xFF000613),
        surface = Color(0xD1052142),
        surfaceStrong = Color(0xEE08264D),
        controlFill = Color(0xFF9EE6FF).copy(alpha = 0.15f),
        primaryText = Color(0xFFD1F5FF),
        secondaryText = Color(0xC9B3E0FF),
        accent = Color(0xFF6BDFFF),
        accent2 = Color(0xFFD1F5FF),
        border = Color(0xFF9EE6FF).copy(alpha = 0.62f),
        badgeFill = Color.Black.copy(alpha = 0.52f),
        badgeText = Color(0xFFD1F5FF),
        shadow = Color(0x885FC7FF),
        textureAlpha = 0.40f,
        isLight = false
    )
}

fun skinBackdropBrush(skin: CubeSkin): Brush {
    val theme = skin.uiTheme()
    return Brush.linearGradient(
        colors = listOf(theme.background, theme.backgroundMid, theme.backgroundDeep)
    )
}

@DrawableRes
fun CubeSkin.backgroundDrawableRes(): Int = when (this) {
    CubeSkin.Default, CubeSkin.Space -> R.drawable.theme_bg_default
    CubeSkin.Rainbow -> R.drawable.theme_bg_rainbow
    CubeSkin.Carbon -> R.drawable.theme_bg_sparkle_pink
    CubeSkin.Gold -> R.drawable.theme_bg_gold
    CubeSkin.Toy -> R.drawable.theme_bg_toy
    CubeSkin.Chocolate -> R.drawable.theme_bg_chocolate
    CubeSkin.Camo -> R.drawable.theme_bg_camo
    CubeSkin.Ice -> R.drawable.theme_bg_ice
}

@Composable
fun CubeSkinBackgroundLayer(
    skin: CubeSkin,
    modifier: Modifier = Modifier,
    opacity: Float = 0.22f,
    baseColor: Color = Color.Transparent,
    scrimColor: Color = Color.Transparent
) {
    Box(modifier.background(baseColor)) {
        Image(
            painter = painterResource(id = skin.backgroundDrawableRes()),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = opacity,
            modifier = Modifier.fillMaxSize()
        )
        if (scrimColor.alpha > 0f) {
            Box(Modifier.fillMaxSize().background(scrimColor))
        }
    }
}

@Composable
fun CubeSkinThemedBackground(
    skin: CubeSkin,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val theme = skin.uiTheme()
    val banner = remember(context, skin) {
        runCatching {
            context.assets.open(skin.bannerAssetPath).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }

    Box(modifier.background(skinBackdropBrush(skin))) {
        if (banner != null) {
            Image(
                bitmap = banner,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = if (theme.isLight) 0.88f else 0.82f,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                theme.background.copy(alpha = if (theme.isLight) 0.02f else 0.18f),
                                theme.backgroundDeep.copy(alpha = if (theme.isLight) 0.16f else 0.52f)
                            )
                        )
                    )
                    .drawBehind {
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                listOf(
                                    theme.accent.copy(alpha = 0.58f),
                                    theme.accent2.copy(alpha = 0.38f),
                                    theme.accent.copy(alpha = 0.34f)
                                )
                            ),
                            topLeft = Offset(10.dp.toPx(), 10.dp.toPx()),
                            size = Size(size.width - 20.dp.toPx(), size.height - 20.dp.toPx()),
                            cornerRadius = CornerRadius(42.dp.toPx(), 42.dp.toPx()),
                            style = Stroke(if (skin == CubeSkin.Toy) 2.dp.toPx() else 1.5.dp.toPx())
                        )
                    }
            )
        } else {
            Box(Modifier.fillMaxSize().skinBackdrop(skin))
        }
    }
}

fun Modifier.skinBackdrop(skin: CubeSkin): Modifier {
    val theme = skin.uiTheme()
    return background(skinBackdropBrush(skin)).drawBehind {
        when (skin) {
            CubeSkin.Toy -> {
                drawRect(Color(0xFFFFF4DE).copy(alpha = 0.72f))
                val cornerSize = Size(72.dp.toPx(), 72.dp.toPx())
                val corners = listOf(
                    Offset.Zero to theme.accent,
                    Offset(size.width - cornerSize.width, 0f) to theme.accent2,
                    Offset(0f, size.height - cornerSize.height) to theme.accent,
                    Offset(size.width - cornerSize.width, size.height - cornerSize.height) to theme.accent2
                )
                corners.forEach { (offset, color) ->
                    drawRoundRect(
                        color = color.copy(alpha = 0.72f),
                        topLeft = offset,
                        size = cornerSize,
                        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                    )
                    drawRoundRect(
                        color = theme.border.copy(alpha = 0.84f),
                        topLeft = offset + Offset(7.dp.toPx(), 7.dp.toPx()),
                        size = Size(cornerSize.width - 14.dp.toPx(), cornerSize.height - 14.dp.toPx()),
                        cornerRadius = CornerRadius(17.dp.toPx(), 17.dp.toPx()),
                        style = Stroke(2.dp.toPx())
                    )
                }
                repeat(20) { i ->
                    val x = ((i * 109) % 997) / 997f * size.width
                    val y = ((i * 167) % 997) / 997f * size.height
                    drawCircle(theme.border.copy(alpha = 0.45f), 3.8.dp.toPx(), Offset(x, y))
                }
            }
            CubeSkin.Chocolate -> {
                val milk = Color(0xFF6A2E0E)
                val dark = Color(0xFF1F0A02)
                drawRect(milk.copy(alpha = 0.20f))
                var x = 0f
                while (x < size.width + 80.dp.toPx()) {
                    drawCircle(
                        Brush.verticalGradient(listOf(milk.copy(alpha = 0.86f), dark.copy(alpha = 0.70f))),
                        radius = 42.dp.toPx(),
                        center = Offset(x + 32.dp.toPx(), 42.dp.toPx())
                    )
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(milk.copy(alpha = 0.90f), dark.copy(alpha = 0.60f))),
                        topLeft = Offset(x, -16.dp.toPx()),
                        size = Size(56.dp.toPx(), 110.dp.toPx()),
                        cornerRadius = CornerRadius(30.dp.toPx(), 30.dp.toPx())
                    )
                    x += 74.dp.toPx()
                }
                var row = 190.dp.toPx()
                while (row < size.height) {
                    var col = 28.dp.toPx()
                    while (col < size.width) {
                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.08f),
                            topLeft = Offset(col, row),
                            size = Size(90.dp.toPx(), 70.dp.toPx()),
                            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                        )
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.07f),
                            topLeft = Offset(col, row),
                            size = Size(90.dp.toPx(), 70.dp.toPx()),
                            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                            style = Stroke(1.2.dp.toPx())
                        )
                        col += 118.dp.toPx()
                    }
                    row += 120.dp.toPx()
                }
            }
            CubeSkin.Camo -> {
                val palette = listOf(
                    Color(0xFF0D1408),
                    Color(0xFF263018),
                    Color(0xFF45431F),
                    Color(0xFF17220D)
                )
                repeat(95) { i ->
                    val x = ((i * 67) % 991) / 991f * size.width
                    val y = ((i * 137) % 991) / 991f * size.height
                    val w = (42 + (i * 17) % 92).dp.toPx()
                    val h = (24 + (i * 23) % 68).dp.toPx()
                    drawOval(palette[i % palette.size].copy(alpha = 0.64f), Offset(x - w / 2f, y - h / 2f), Size(w, h))
                }
            }
            CubeSkin.Ice -> {
                repeat(140) { i ->
                    val x = ((i * 89) % 1021) / 1021f * size.width
                    val y = ((i * 193) % 1021) / 1021f * size.height
                    drawCircle(theme.accent.copy(alpha = 0.34f), (0.8f + ((i * 11) % 5) * 0.32f).dp.toPx(), Offset(x, y))
                }
                repeat(34) { i ->
                    val sx = ((i * 83) % 997) / 997f * size.width
                    val sy = ((i * 197) % 997) / 997f * size.height
                    drawLine(theme.accent2.copy(alpha = 0.24f), Offset(sx, sy), Offset(sx + ((i % 7) - 3) * 18.dp.toPx(), sy + (28 + (i * 9) % 76).dp.toPx()), 1.dp.toPx())
                }
                drawRoundRect(theme.accent2.copy(alpha = 0.46f), Offset(6.dp.toPx(), 6.dp.toPx()), Size(size.width - 12.dp.toPx(), size.height - 12.dp.toPx()), CornerRadius(32.dp.toPx(), 32.dp.toPx()), style = Stroke(4.dp.toPx()))
            }
            CubeSkin.Carbon -> {
                repeat(160) { i ->
                    val x = ((i * 89) % 1021) / 1021f * size.width
                    val y = ((i * 193) % 1021) / 1021f * size.height
                    drawCircle(theme.accent.copy(alpha = 0.35f), (0.7f + ((i * 11) % 5) * 0.36f).dp.toPx(), Offset(x, y))
                }
            }
            CubeSkin.Rainbow -> {
                val colors = listOf(Color.Red, Color(0xFFFFA000), Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta)
                colors.forEachIndexed { index, color ->
                    drawOval(
                        color.copy(alpha = 0.18f),
                        topLeft = Offset(-size.width * 0.25f + index * size.width / 6f, -size.height * 0.12f),
                        size = Size(size.width * 0.50f, size.height * 1.22f)
                    )
                }
            }
            CubeSkin.Gold -> {
                repeat(24) { i ->
                    val y = i * size.height / 24f
                    drawLine(Color.White.copy(alpha = if (i % 3 == 0) 0.09f else 0.035f), Offset(-30f, y), Offset(size.width + 30f, y + size.height * 0.18f), 2.dp.toPx())
                }
            }
            CubeSkin.Default, CubeSkin.Space -> {
                var row = 24.dp.toPx()
                while (row < size.height + 32.dp.toPx()) {
                    var col = 20.dp.toPx()
                    while (col < size.width + 32.dp.toPx()) {
                        val seed = (row * 13 + col * 7).toInt()
                        val radius = (3 + seed % 13).dp.toPx()
                        val x = col + ((seed % 17) - 8).dp.toPx()
                        val y = row + ((seed % 19) - 9).dp.toPx()
                        drawCircle(Color.Black.copy(alpha = 0.34f), radius, Offset(x, y))
                        drawCircle(Color.White.copy(alpha = 0.05f), radius, Offset(x - 1.dp.toPx(), y - 1.dp.toPx()), style = Stroke(1.dp.toPx()))
                        col += 46.dp.toPx()
                    }
                    row += 42.dp.toPx()
                }
                repeat(5) { i ->
                    val x = size.width * (0.18f + i * 0.17f)
                    drawLine(theme.accent.copy(alpha = 0.12f), Offset(x, -size.height * 0.06f), Offset(x + 95.dp.toPx(), size.height * 0.98f), 18.dp.toPx())
                }
            }
        }
        drawRoundRect(
            brush = Brush.linearGradient(listOf(theme.accent.copy(alpha = 0.82f), theme.accent2.copy(alpha = 0.64f), theme.accent.copy(alpha = 0.44f))),
            topLeft = Offset(10.dp.toPx(), 10.dp.toPx()),
            size = Size(size.width - 20.dp.toPx(), size.height - 20.dp.toPx()),
            cornerRadius = CornerRadius(42.dp.toPx(), 42.dp.toPx()),
            style = Stroke(if (skin == CubeSkin.Toy) 2.dp.toPx() else 1.5.dp.toPx())
        )
    }
}
