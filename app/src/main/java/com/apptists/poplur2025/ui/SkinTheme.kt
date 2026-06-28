package com.apptists.poplur2025.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

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
