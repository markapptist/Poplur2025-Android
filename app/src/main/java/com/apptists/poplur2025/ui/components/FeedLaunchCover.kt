package com.apptists.poplur2025.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptists.poplur2025.R
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val LOADING_PHASES = listOf(
    "scanning your social radius",
    "calibrating your neural feed",
    "learning local creator signals",
    "mapping nearby content",
    "ranking posts by proximity",
    "discovering your community",
    "processing social graph data",
    "tuning your experience",
    "building your intelligent feed",
    "syncing real-time signals",
    "personalizing your world",
    "analyzing nearby activity",
    "warming up AI radio voice",
    "loading radio voice model",
    "preparing Poplur Radio",
    "priming map voice system",
    "caching AI voice patterns",
    "preloading city radio scripts",
    "tuning the AI broadcast",
    "syncing radio to your area",
    "connecting to local broadcasts",
    "loading ambient map audio",
    "indexing creator sound profiles",
    "fetching area audio landscape",
    "streaming radio AI model",
    "loading proximity sound data",
    "loading area content signals",
    "building your map experience",
    "fetching live location data",
    "resolving nearby creators",
    "compiling your local scene",
    "loading discovery engine",
    "wiring up real-time feeds",
    "computing social clusters",
    "preparing AI insights",
)

private val CONFETTI_PALETTE = listOf(
    Color(0xFFF54D57.toInt()),
    Color(0xFFFA7A3B.toInt()),
    Color(0xFFFFB838.toInt()),
    Color(0xFFED3366.toInt())
)

// Built once at process lifetime — zero per-composition allocation.
private val FIXED_BURST_PARTICLES = buildBurstConfetti(seed = 42, count = 20)

@Composable
fun FeedLaunchCover(
    onPopSound: () -> Unit = {},
    showSlowDetails: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val logoBitmap = remember(context) {
        runCatching {
            context.assets.open("logo.png").use { BitmapFactory.decodeStream(it).asImageBitmap() }
        }.getOrNull()
    }
    val shuffledPhases = remember { LOADING_PHASES.shuffled() }

    // InfiniteTransition for rain + rotation — more efficient than while(true)+animateTo
    // and avoids the 1-billion-ms tween that can confuse Compose's animation scheduler.
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    // Cycles 0→5 over 5s, looping — drives confetti rain phase offsets in Canvas
    val rainPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(5_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain"
    )
    val logoRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "logoRotation"
    )

    var popGeneration by remember { mutableIntStateOf(0) }
    val logoScale = remember { Animatable(0.35f) }
    val burstProgress = remember { Animatable(1f) }
    val loadingProgress = remember { Animatable(0f) }
    var phaseIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        logoScale.animateTo(1.16f, animationSpec = tween(420, easing = FastOutSlowInEasing))
        logoScale.animateTo(0.91f, animationSpec = tween(140, easing = FastOutSlowInEasing))
        logoScale.animateTo(1.04f, animationSpec = tween(100, easing = FastOutSlowInEasing))
        logoScale.animateTo(1.00f, animationSpec = tween(80, easing = FastOutSlowInEasing))
    }

    // Sound + burst — in its own LaunchedEffect so onPopSound() never blocks
    // the scale animation coroutine (MediaPlayer.seekTo can stall briefly).
    LaunchedEffect(popGeneration) {
        if (popGeneration == 0) return@LaunchedEffect
        onPopSound()
        logoScale.snapTo(1f)
        logoScale.animateTo(1.42f, animationSpec = tween(90, easing = FastOutSlowInEasing))
        logoScale.animateTo(0.82f, animationSpec = tween(80, easing = FastOutSlowInEasing))
        logoScale.animateTo(1.12f, animationSpec = tween(70, easing = FastOutSlowInEasing))
        logoScale.animateTo(0.94f, animationSpec = tween(60, easing = FastOutSlowInEasing))
        logoScale.animateTo(1.00f, animationSpec = tween(50, easing = FastOutSlowInEasing))
        logoScale.animateTo(1.05f, animationSpec = tween(640, easing = FastOutSlowInEasing))
        logoScale.animateTo(0.95f, animationSpec = tween(540, easing = FastOutSlowInEasing))
        logoScale.animateTo(1.00f, animationSpec = tween(470, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(popGeneration) {
        if (popGeneration == 0) return@LaunchedEffect
        burstProgress.snapTo(0f)
        burstProgress.animateTo(1f, animationSpec = tween(2_000, easing = LinearEasing))
    }

    // Bar loops continuously until the cover is dismissed; text advances each cycle.
    LaunchedEffect(Unit) {
        while (true) {
            loadingProgress.snapTo(0f)
            loadingProgress.animateTo(1f, animationSpec = tween(2_400, easing = FastOutSlowInEasing))
            delay(80) // brief hold at full before resetting
            phaseIndex = (phaseIndex + 1) % shuffledPhases.size
        }
    }

    // Keep the launch cover lively without rendering the app cube/logo.
    LaunchedEffect(Unit) {
        delay(1_000)
        while (true) {
            popGeneration++
            delay(2_500)
        }
    }

    Box(
        modifier = modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // Single Canvas: rain + burst in one draw pass
        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = rainPhase  // 0..5, cycling

            // 20 rain particles
            repeat(20) { i ->
                val progress = ((t / 5f + i * 0.05f) % 1f).coerceIn(0f, 1f)
                val x = ((sin(i * 3.71 + t * 0.15).toFloat() + 1f) / 2f) * size.width
                val y = -24f + (size.height + 60f) * progress
                val alpha = if (progress > 0.82f) ((1f - progress) / 0.18f).coerceIn(0f, 1f) else 1f
                val base = CONFETTI_PALETTE[i % CONFETTI_PALETTE.size].copy(alpha = alpha)
                val pieceW = (5 + i % 9).dp.toPx()
                val pieceH = (4 + i % 6).dp.toPx()

                rotate(degrees = progress * (200f + i * 5f), pivot = Offset(x, y)) {
                    when (i % 6) {
                        0 -> drawCircle(base, pieceW * 0.42f, Offset(x, y))
                        3 -> drawRoundRect(
                            base,
                            topLeft = Offset(x - 1.5f.dp.toPx(), y - pieceH),
                            size = Size(3.dp.toPx(), pieceH * 2f),
                            cornerRadius = CornerRadius(1.dp.toPx())
                        )
                        else -> drawRoundRect(
                            base,
                            topLeft = Offset(x - pieceW * 0.5f, y - pieceH * 0.4f),
                            size = Size(pieceW, pieceH),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                    }
                }
            }

            // Burst particles (fixed set — never reallocated)
            val p = burstProgress.value
            if (p < 1f) {
                FIXED_BURST_PARTICLES.forEach { piece ->
                    val local = ((p - piece.delay) / (1f - piece.delay)).coerceIn(0f, 1f)
                    if (local <= 0f || local >= 1f) return@forEach

                    val burstEnd = 0.35f
                    val bEased = easeOutQ((local / burstEnd).coerceIn(0f, 1f))
                    val fEased = easeInQ(((local - burstEnd) / (1f - burstEnd)).coerceIn(0f, 1f))

                    val x = size.width * (piece.startXFrac + piece.vxFrac * bEased + piece.vxFrac * 0.06f * fEased)
                    val y = size.height * (piece.startYFrac - piece.vyFrac * bEased + piece.fallFrac * fEased)
                    val alpha = if (local > 0.72f) ((1f - local) / 0.28f).coerceIn(0f, 1f) else 1f

                    rotate(
                        degrees = piece.startRotation + local * piece.spin,
                        pivot = Offset(x, y)
                    ) {
                        val wPx = piece.widthDp.toPx()
                        val hPx = piece.heightDp.toPx()
                        when (piece.shapeType) {
                            1 -> drawCircle(piece.color.copy(alpha = alpha), wPx * 0.48f, Offset(x, y))
                            2 -> drawRoundRect(
                                piece.color.copy(alpha = alpha),
                                topLeft = Offset(x - 1.5f.dp.toPx(), y - hPx),
                                size = Size(3.dp.toPx(), hPx * 2f),
                                cornerRadius = CornerRadius(1.dp.toPx())
                            )
                            else -> drawRoundRect(
                                piece.color.copy(alpha = alpha),
                                topLeft = Offset(x - wPx * 0.5f, y - hPx * 0.5f),
                                size = Size(wPx, hPx),
                                cornerRadius = CornerRadius(2.dp.toPx())
                            )
                        }
                    }
                }
            }
        }

        // --- Poplur Logo (raw image, matching iOS SKSpriteNode(imageNamed:"logo")) ---
        if (logoBitmap != null) {
            Image(
                bitmap = logoBitmap,
                contentDescription = "Poplur Logo",
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.Center)
                    .offset(y = (-60).dp)
                    .graphicsLayer {
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                        rotationZ = logoRotation
                    }
            )
        } else {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher),
                contentDescription = "Poplur Logo",
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.Center)
                    .offset(y = (-60).dp)
                    .graphicsLayer {
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                        rotationZ = logoRotation
                    }
            )
        }

        // AI loading bar + text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 132.dp)
                    .width(200.dp)
        ) {
            LinearProgressIndicator(
                progress = { loadingProgress.value },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF0078FF),
                trackColor = Color.Black.copy(alpha = 0.12f),
            )
            Spacer(Modifier.height(8.dp))
            AnimatedContent(
                targetState = phaseIndex,
                transitionSpec = {
                    fadeIn(tween(200)).togetherWith(fadeOut(tween(200)))
                },
                label = "loadingPhase"
            ) { idx ->
                Text(
                    text = "AI ${shuffledPhases[idx]}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black.copy(alpha = 0.60f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ── Burst confetti ─────────────────────────────────────────────────────────────

private data class BurstConfettiPiece(
    val startXFrac: Float,
    val startYFrac: Float,
    val vxFrac: Float,
    val vyFrac: Float,
    val fallFrac: Float,
    val widthDp: androidx.compose.ui.unit.Dp,
    val heightDp: androidx.compose.ui.unit.Dp,
    val shapeType: Int,
    val delay: Float,
    val startRotation: Float,
    val spin: Float,
    val color: Color
)

private fun buildBurstConfetti(seed: Int, count: Int = 20): List<BurstConfettiPiece> {
    val rng = Random(seed * 6_329 + 1)
    return List(count) { i ->
        val angle = rng.nextDouble() * 2.0 * PI
        val speed = 0.22f + rng.nextFloat() * 0.18f
        BurstConfettiPiece(
            startXFrac    = 0.50f,
            startYFrac    = 0.46f,
            vxFrac        = cos(angle).toFloat() * speed,
            vyFrac        = sin(angle).toFloat() * speed * 0.75f,
            fallFrac      = 0.26f + rng.nextFloat() * 0.22f,
            widthDp       = (6 + rng.nextInt(10)).dp,
            heightDp      = (5 + rng.nextInt(6)).dp,
            shapeType     = when { i % 6 == 0 -> 1; i % 6 == 3 -> 2; else -> 0 },
            delay         = rng.nextFloat() * 0.04f,
            startRotation = rng.nextFloat() * 360f,
            spin          = 200f + rng.nextFloat() * 480f,
            color         = CONFETTI_PALETTE[i % CONFETTI_PALETTE.size]
        )
    }
}

private fun easeOutQ(t: Float): Float = 1f - (1f - t) * (1f - t)
private fun easeInQ(t: Float): Float = t * t
