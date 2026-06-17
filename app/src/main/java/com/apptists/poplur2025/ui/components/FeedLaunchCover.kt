package com.apptists.poplur2025.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun FeedLaunchCover(
    onPopSound: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val logoBitmap = remember {
        runCatching {
            context.assets.open("logo.png").use { BitmapFactory.decodeStream(it).asImageBitmap() }
        }.getOrNull()
    }

    val logoScale = remember { Animatable(1f) }
    var popGeneration by remember { mutableIntStateOf(0) }
    val confettiProgress = remember { Animatable(1f) }
    val confettiParticles = remember(popGeneration) { buildLaunchConfetti(popGeneration) }

    // Drive the confetti canvas whenever a new pop fires
    LaunchedEffect(popGeneration) {
        if (popGeneration == 0) return@LaunchedEffect
        confettiProgress.snapTo(0f)
        confettiProgress.animateTo(1f, animationSpec = tween(2_200, easing = LinearEasing))
    }

    // Pop loop — runs while the composable is in the composition
    LaunchedEffect(Unit) {
        delay(500)
        while (true) {
            // Fire
            onPopSound()
            popGeneration++

            // Pop animation
            logoScale.snapTo(1f)
            logoScale.animateTo(1.42f, animationSpec = tween(90))
            logoScale.animateTo(0.82f, animationSpec = tween(80))
            logoScale.animateTo(1.12f, animationSpec = tween(70))
            logoScale.animateTo(0.94f, animationSpec = tween(60))
            logoScale.animateTo(1.00f, animationSpec = tween(50))

            // Idle pulse until the next pop (~2.8 s total interval)
            val idleEndMs = System.currentTimeMillis() + 2_800L
            var goUp = true
            while (System.currentTimeMillis() + 850 < idleEndMs) {
                logoScale.animateTo(
                    targetValue = if (goUp) 1.05f else 0.95f,
                    animationSpec = tween(durationMillis = 850)
                )
                goUp = !goUp
            }
            val remaining = idleEndMs - System.currentTimeMillis()
            if (remaining > 0) delay(remaining)
            logoScale.snapTo(1f)
        }
    }

    Box(
        modifier = modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // Confetti layer — redraws on every confettiProgress animation frame
        Canvas(modifier = Modifier.fillMaxSize()) {
            val p = confettiProgress.value
            if (p >= 1f) return@Canvas

            confettiParticles.forEach { piece ->
                val local = ((p - piece.delay) / (1f - piece.delay)).coerceIn(0f, 1f)
                if (local <= 0f || local >= 1f) return@forEach

                val burstEnd = 0.35f
                val burstP = (local / burstEnd).coerceIn(0f, 1f)
                val fallP  = ((local - burstEnd) / (1f - burstEnd)).coerceIn(0f, 1f)

                val bEased = easeOutQ(burstP)
                val fEased = easeInQ(fallP)

                val x = size.width  * (piece.startXFrac + piece.vxFrac * bEased + piece.vxFrac * 0.06f * fEased)
                val y = size.height * (piece.startYFrac - piece.vyFrac * bEased + piece.fallFrac * fEased)

                val alpha = if (local > 0.72f) ((1f - local) / 0.28f).coerceIn(0f, 1f) else 1f

                rotate(
                    degrees = piece.startRotation + local * piece.spin,
                    pivot = Offset(x, y)
                ) {
                    val wPx = piece.widthDp.toPx()
                    val hPx = piece.heightDp.toPx()
                    when (piece.shapeType) {
                        1 -> drawCircle(
                            color = piece.color.copy(alpha = alpha),
                            radius = wPx * 0.48f,
                            center = Offset(x, y)
                        )
                        2 -> drawRoundRect( // thin streamer
                            color = piece.color.copy(alpha = alpha),
                            topLeft = Offset(x - 1.5f.dp.toPx(), y - hPx),
                            size = Size(3.dp.toPx(), hPx * 2f),
                            cornerRadius = CornerRadius(1.dp.toPx())
                        )
                        else -> drawRoundRect(
                            color = piece.color.copy(alpha = alpha),
                            topLeft = Offset(x - wPx * 0.5f, y - hPx * 0.5f),
                            size = Size(wPx, hPx),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                    }
                }
            }
        }

        // Logo
        logoBitmap?.let { bmp ->
            Image(
                bitmap = bmp,
                contentDescription = "Poplur",
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer {
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                    }
            )
        }
    }
}

// ── Confetti data ─────────────────────────────────────────────────────────────

private data class LaunchConfettiPiece(
    val startXFrac: Float,
    val startYFrac: Float,
    val vxFrac: Float,     // burst x-displacement as fraction of canvas width
    val vyFrac: Float,     // burst y-displacement as fraction of canvas height (upward positive)
    val fallFrac: Float,   // fall y-displacement as fraction of canvas height (downward)
    val widthDp: Dp,
    val heightDp: Dp,
    val shapeType: Int,    // 0=rect  1=circle  2=strip
    val delay: Float,      // [0, 0.04] stagger within the overall progress
    val startRotation: Float,
    val spin: Float,       // total degrees spun over lifetime
    val color: Color
)

private fun buildLaunchConfetti(seed: Int): List<LaunchConfettiPiece> {
    val rng = Random(seed * 6_329 + 1)
    val palette = listOf(
        Color(0xFFFA3D3D.toInt()),
        Color(0xFFFF9400.toInt()),
        Color(0xFFFFE000.toInt()),
        Color(0xFF2ECC40.toInt()),
        Color(0xFF3478F6.toInt()),
        Color(0xFFC244E8.toInt()),
        Color(0xFFF73EA0.toInt()),
        Color(0xFF00C5C8.toInt())
    )
    return List(52) { i ->
        val angle = rng.nextDouble() * 2.0 * PI
        val speed = 0.24f + rng.nextFloat() * 0.18f
        LaunchConfettiPiece(
            startXFrac  = 0.50f,
            startYFrac  = 0.46f,
            vxFrac      = cos(angle).toFloat() * speed,
            vyFrac      = sin(angle).toFloat() * speed * 0.75f,
            fallFrac    = 0.28f + rng.nextFloat() * 0.22f,
            widthDp     = (6 + rng.nextInt(10)).dp,
            heightDp    = (5 + rng.nextInt(6)).dp,
            shapeType   = when { i % 6 == 0 -> 1; i % 6 == 3 -> 2; else -> 0 },
            delay       = rng.nextFloat() * 0.04f,
            startRotation = rng.nextFloat() * 360f,
            spin        = 200f + rng.nextFloat() * 480f,
            color       = palette[i % palette.size]
        )
    }
}

// Quadratic easing helpers
private fun easeOutQ(t: Float): Float = 1f - (1f - t) * (1f - t)
private fun easeInQ(t: Float): Float = t * t
