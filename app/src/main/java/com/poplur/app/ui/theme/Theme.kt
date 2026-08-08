package com.poplur.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode

private val DarkColors = darkColorScheme()
private val LightColors = lightColorScheme()

@Composable
fun PoplurTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    // Keep it simple: default dark-ish look for feed/video vibe.
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
