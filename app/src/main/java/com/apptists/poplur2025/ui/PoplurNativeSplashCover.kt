package com.apptists.poplur2025.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

/**
 * The clay progress bar, drawn NATIVELY, for the window before any WebView exists.
 *
 * `prelogin/splash.html` already carries this bar, but it cannot show it until Chromium has
 * initialised and that document has painted — measured at 5-6s on an A03, between Android
 * destroying its own starting window and splash.html's first line of script. That gap is what
 * reads as "the app logo on a black screen with no progress": the bar was never broken, it simply
 * did not exist yet.
 *
 * This is Compose, so it draws on the activity's first frame, with no WebView, no Chromium and no
 * bundle in the way. It hands over to the web splash the moment that document commits its first
 * paint, so the two never both show.
 *
 * Deliberately cheap: two rounded rects and one animated float. No video, no image decode, nothing
 * that would compete with the very WebView startup it is covering for.
 */
@Composable
fun PoplurNativeSplashCover(
    modifier: Modifier = Modifier,
    background: Color = Color(0xFF0A0709),
) {
    val transition = rememberInfiniteTransition(label = "poplur-native-splash")
    // Matches splash.html's `splashFill`: 4s, ease-out, and it never quite completes — a bar that
    // hits 100% and sits there claims the app is ready when it is not.
    val progress by transition.animateFloat(
        initialValue = 0.04f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "fill",
    )
    // The barber-pole sweep, the same 0.7s cycle the web bar uses.
    val stripe by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "stripe",
    )

    Box(modifier = modifier.fillMaxSize().background(background)) {
        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.58f)
                .height(9.dp)
                .padding(top = 0.dp),
        ) {
            val radius = CornerRadius(size.height / 2f, size.height / 2f)
            val track = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = 0f, top = 0f, right = size.width, bottom = size.height,
                        cornerRadius = radius,
                    )
                )
            }
            drawPath(track, Color(0xFF1C0812).copy(alpha = 0.55f))

            val fillWidth = size.width * progress.coerceIn(0.04f, 1f)
            val fill = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = 0f, top = 0f, right = fillWidth, bottom = size.height,
                        cornerRadius = radius,
                    )
                )
            }
            clipPath(fill) {
                drawRect(Color(0xFFF53C99), size = Size(fillWidth, size.height))
                // Diagonal candy stripes, marching left the way the web bar's
                // background-position animation does.
                val band = 14f
                val shift = stripe * band * 2f
                var x = -size.height - band * 2f + shift
                while (x < fillWidth + size.height + band * 2f) {
                    val p = Path().apply {
                        moveTo(x, size.height)
                        lineTo(x + band, size.height)
                        lineTo(x + band + size.height, 0f)
                        lineTo(x + size.height, 0f)
                        close()
                    }
                    drawPath(p, Color(0xFFFFE1F1).copy(alpha = 0.55f))
                    x += band * 2f
                }
            }
            // Rim, so the bar reads as clay pressed into a groove rather than a flat meter.
            drawRoundRect(
                color = Color(0xFFF43B99).copy(alpha = 0.35f),
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = radius,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
            )
        }
    }
}
