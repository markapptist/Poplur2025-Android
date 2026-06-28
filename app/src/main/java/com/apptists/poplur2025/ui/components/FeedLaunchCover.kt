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
import com.apptists.poplur2025.ui.components.PoplurText
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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

// Colors exactly match ConfettiAnimationAdapter.CONFETTI_COLORS
private val CONFETTI_COLORS_ALPHA = listOf(
    Color(0.96f, 0.30f, 0.34f, 0.92f),
    Color(0.98f, 0.48f, 0.23f, 0.90f),
    Color(1.00f, 0.72f, 0.22f, 0.92f),
    Color(0.93f, 0.20f, 0.40f, 0.86f)
)
// Opaque version for burst (alpha controlled separately)
private val CONFETTI_PALETTE = CONFETTI_COLORS_ALPHA.map { it.copy(alpha = 1f) }

private enum class LoadingParticleEffect {
    Confetti,
    Bubbles,
    Smoke,
    Rain,
    Snow,
    Sparkle
}

@Composable
fun FeedLaunchCover(
    onPopSound: () -> Unit = {},
    showSlowDetails: Boolean = true,
    skinLogoPath: String = "logo.png",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val logoBitmap = remember(context, skinLogoPath) {
        runCatching {
            context.assets.open(skinLogoPath).use { BitmapFactory.decodeStream(it).asImageBitmap() }
        }.getOrElse {
            runCatching {
                context.assets.open("logo.png").use { BitmapFactory.decodeStream(it).asImageBitmap() }
            }.getOrNull()
        }
    }
    val shuffledPhases = remember { LOADING_PHASES.shuffled() }
    val particleSeed = remember { Random.nextInt(1, Int.MAX_VALUE) }
    val particleEffects = remember(particleSeed) {
        LoadingParticleEffect.values().toList().shuffled(Random(particleSeed))
    }
    val ambientParticles = remember(particleSeed) { buildAmbientParticles(particleSeed, count = 150) }
    val confettiParticles = remember(particleSeed) { buildRainParticles(particleSeed + 17, count = 110) }
    val burstParticles = remember(particleSeed) { buildBurstConfetti(particleSeed + 29, count = 30) }

    // InfiniteTransition for particles + rotation — more efficient than while(true)+animateTo
    // and avoids the 1-billion-ms tween that can confuse Compose's animation scheduler.
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    // Cycles 0→1 over 15s — drives every particle effect from one stable phase.
    val particlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(15_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particles"
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
    var particleEffectIndex by remember { mutableIntStateOf(0) }
    var burstEffectIndex by remember { mutableIntStateOf(0) }
    val particleEffect = particleEffects[particleEffectIndex % particleEffects.size]
    val burstEffect = particleEffects[burstEffectIndex % particleEffects.size]

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
        burstEffectIndex = particleEffectIndex
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

    LaunchedEffect(Unit) {
        delay(900)
        while (true) {
            delay(2_400)
            particleEffectIndex = (particleEffectIndex + 1) % particleEffects.size
        }
    }

    Box(
        modifier = modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // Single Canvas: ambient particle effect + burst in one draw pass
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLoadingParticleEffect(
                effect = particleEffect,
                phase = particlePhase,
                ambientParticles = ambientParticles,
                confettiParticles = confettiParticles
            )

            // Burst particles match the currently active loading effect.
            val p = burstProgress.value
            if (p < 1f) {
                drawLoadingBurst(effect = burstEffect, progress = p, particles = burstParticles)
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
                PoplurText(
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

private data class AmbientParticle(
    val phaseFrac: Float,
    val startXFrac: Float,
    val startYFrac: Float,
    val driftFrac: Float,
    val sizeDp: Float,
    val rotation: Float,
    val spin: Float,
    val colorIndex: Int,
    val alpha: Float
)

private fun buildAmbientParticles(seed: Int, count: Int): List<AmbientParticle> {
    val rng = Random(seed * 2_147 + 71)
    return List(count) {
        AmbientParticle(
            phaseFrac = rng.nextFloat(),
            startXFrac = rng.nextFloat(),
            startYFrac = rng.nextFloat(),
            driftFrac = rng.nextFloat() * 2f - 1f,
            sizeDp = 2f + rng.nextFloat() * 16f,
            rotation = rng.nextFloat() * 360f,
            spin = rng.nextFloat() * 720f - 360f,
            colorIndex = rng.nextInt(CONFETTI_PALETTE.size),
            alpha = 0.42f + rng.nextFloat() * 0.50f
        )
    }
}

private fun DrawScope.drawLoadingParticleEffect(
    effect: LoadingParticleEffect,
    phase: Float,
    ambientParticles: List<AmbientParticle>,
    confettiParticles: List<RainParticle>
) {
    when (effect) {
        LoadingParticleEffect.Confetti -> drawConfettiParticles(phase, confettiParticles)
        LoadingParticleEffect.Bubbles -> drawBubbleParticles(phase, ambientParticles)
        LoadingParticleEffect.Smoke -> drawSmokeParticles(phase, ambientParticles)
        LoadingParticleEffect.Rain -> drawRainParticles(phase, ambientParticles)
        LoadingParticleEffect.Snow -> drawSnowParticles(phase, ambientParticles)
        LoadingParticleEffect.Sparkle -> drawSparkleParticles(phase, ambientParticles)
    }
}

private fun DrawScope.drawConfettiParticles(phase: Float, particles: List<RainParticle>) {
    particles.forEach { piece ->
        val progress = ((phase + piece.phaseFrac) % 1f).coerceIn(0f, 1f)
        val wPx = piece.sizeDp.dp.toPx()
        val hPx = wPx * 0.36f
        val x = piece.startXFrac * size.width + piece.driftFrac * size.width * progress
        val y = -wPx * 2f + (size.height + wPx * 4f) * progress
        val alpha = (if (progress > 0.88f) ((1f - progress) / 0.12f).coerceIn(0f, 1f) else 1f) * piece.colorAlpha
        val rotation = piece.startRotation + piece.totalSpin * progress

        rotate(degrees = rotation, pivot = Offset(x, y)) {
            drawRoundRect(
                color = piece.color.copy(alpha = alpha),
                topLeft = Offset(x - wPx * 0.5f, y - hPx * 0.5f),
                size = Size(wPx, hPx),
                cornerRadius = CornerRadius(1.5f.dp.toPx())
            )
        }
    }
}

private fun DrawScope.drawBubbleParticles(phase: Float, particles: List<AmbientParticle>) {
    particles.take(76).forEach { particle ->
        val progress = ((phase * 0.70f + particle.phaseFrac) % 1f).coerceIn(0f, 1f)
        val radius = (particle.sizeDp * 0.86f + 4f).dp.toPx()
        val wobble = sin(progress * PI * 2f + particle.rotation).toFloat()
        val x = particle.startXFrac * size.width + wobble * 34.dp.toPx() + particle.driftFrac * 14.dp.toPx()
        val y = size.height + radius * 2f - (size.height + radius * 4f) * progress
        val alpha = (1f - progress).coerceIn(0f, 1f) * particle.alpha
        drawCircle(
            color = Color(0xFF4AB8FF).copy(alpha = alpha * 0.58f),
            radius = radius,
            center = Offset(x, y),
            style = Stroke(width = 1.45.dp.toPx())
        )
        drawCircle(
            color = Color.White.copy(alpha = alpha * 0.14f),
            radius = radius * 0.72f,
            center = Offset(x - radius * 0.18f, y - radius * 0.18f)
        )
    }
}

private fun DrawScope.drawSmokeParticles(phase: Float, particles: List<AmbientParticle>) {
    particles.take(54).forEach { particle ->
        val progress = ((phase * 0.40f + particle.phaseFrac) % 1f).coerceIn(0f, 1f)
        val radius = (particle.sizeDp * 2.4f + 18f).dp.toPx() * (0.42f + progress * 1.55f)
        val x = particle.startXFrac * size.width + particle.driftFrac * size.width * 0.14f * progress
        val y = size.height + radius - size.height * 0.72f * progress
        val alpha = (1f - progress).coerceIn(0f, 1f)
        drawCircle(
            color = Color(0xFF5F6370).copy(alpha = alpha * 0.14f * particle.alpha),
            radius = radius,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawRainParticles(phase: Float, particles: List<AmbientParticle>) {
    particles.take(122).forEach { particle ->
        val progress = ((phase * 1.85f + particle.phaseFrac) % 1f).coerceIn(0f, 1f)
        val length = (18f + particle.sizeDp).dp.toPx()
        val x = particle.startXFrac * size.width + particle.driftFrac * 18.dp.toPx()
        val y = -length + (size.height + length * 2f) * progress
        val alpha = if (progress > 0.90f) ((1f - progress) / 0.10f).coerceIn(0f, 1f) else 1f
        drawLine(
            color = Color(0xFF188CFF).copy(alpha = alpha * 0.58f * particle.alpha),
            start = Offset(x, y),
            end = Offset(x + 5.dp.toPx(), y + length),
            strokeWidth = 1.45.dp.toPx()
        )
    }
}

private fun DrawScope.drawSnowParticles(phase: Float, particles: List<AmbientParticle>) {
    particles.take(96).forEach { particle ->
        val progress = ((phase * 0.55f + particle.phaseFrac) % 1f).coerceIn(0f, 1f)
        val radius = (1.7f + particle.sizeDp * 0.20f).dp.toPx()
        val sway = sin(progress * PI * 2.0 + particle.rotation).toFloat()
        val x = particle.startXFrac * size.width + sway * 26.dp.toPx() + particle.driftFrac * 18.dp.toPx()
        val y = -radius * 2f + (size.height + radius * 4f) * progress
        val alpha = if (progress > 0.92f) ((1f - progress) / 0.08f).coerceIn(0f, 1f) else 1f
        drawCircle(
            color = Color.White.copy(alpha = alpha * 0.82f * particle.alpha),
            radius = radius,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawSparkleParticles(phase: Float, particles: List<AmbientParticle>) {
    particles.take(84).forEach { particle ->
        val progress = ((phase * 0.95f + particle.phaseFrac) % 1f).coerceIn(0f, 1f)
        val twinkle = kotlin.math.abs(sin(progress * PI * 2.0).toFloat())
        val alpha = twinkle * particle.alpha
        val arm = (2.5f + particle.sizeDp * 0.35f).dp.toPx()
        val x = particle.startXFrac * size.width
        val y = particle.startYFrac * size.height
        val color = CONFETTI_PALETTE[particle.colorIndex].copy(alpha = alpha)
        rotate(degrees = particle.rotation + particle.spin * progress, pivot = Offset(x, y)) {
            drawLine(color, Offset(x - arm, y), Offset(x + arm, y), strokeWidth = 1.5.dp.toPx())
            drawLine(color, Offset(x, y - arm), Offset(x, y + arm), strokeWidth = 1.5.dp.toPx())
            drawCircle(Color.White.copy(alpha = alpha * 0.24f), arm * 0.38f, Offset(x, y))
        }
    }
}

private fun DrawScope.drawLoadingBurst(
    effect: LoadingParticleEffect,
    progress: Float,
    particles: List<BurstConfettiPiece>
) {
    particles.forEach { piece ->
        val local = ((progress - piece.delay) / (1f - piece.delay)).coerceIn(0f, 1f)
        if (local <= 0f || local >= 1f) return@forEach

        val burstEnd = 0.35f
        val bEased = easeOutQ((local / burstEnd).coerceIn(0f, 1f))
        val fEased = easeInQ(((local - burstEnd) / (1f - burstEnd)).coerceIn(0f, 1f))
        val x = size.width * (piece.startXFrac + piece.vxFrac * bEased + piece.vxFrac * 0.06f * fEased)
        val y = size.height * (piece.startYFrac - piece.vyFrac * bEased + piece.fallFrac * fEased)
        val alpha = if (local > 0.72f) ((1f - local) / 0.28f).coerceIn(0f, 1f) else 1f

        when (effect) {
            LoadingParticleEffect.Confetti -> drawConfettiBurstPiece(piece, local, x, y, alpha)
            LoadingParticleEffect.Bubbles -> drawCircle(
                color = Color(0xFF4AB8FF).copy(alpha = alpha * 0.62f),
                radius = piece.widthDp.toPx() * (0.8f + local),
                center = Offset(x, y),
                style = Stroke(width = 1.5.dp.toPx())
            )
            LoadingParticleEffect.Smoke -> drawCircle(
                color = Color(0xFF5F6370).copy(alpha = alpha * 0.16f),
                radius = piece.widthDp.toPx() * (1.8f + local * 3.2f),
                center = Offset(x, y)
            )
            LoadingParticleEffect.Rain -> drawLine(
                color = Color(0xFF188CFF).copy(alpha = alpha * 0.68f),
                start = Offset(x, y - piece.heightDp.toPx()),
                end = Offset(x + 5.dp.toPx(), y + piece.heightDp.toPx()),
                strokeWidth = 1.6.dp.toPx()
            )
            LoadingParticleEffect.Snow -> drawCircle(
                color = Color.White.copy(alpha = alpha * 0.88f),
                radius = piece.widthDp.toPx() * 0.34f,
                center = Offset(x, y)
            )
            LoadingParticleEffect.Sparkle -> {
                val arm = piece.widthDp.toPx() * (0.55f + local * 0.35f)
                val color = piece.color.copy(alpha = alpha)
                rotate(degrees = piece.startRotation + local * piece.spin, pivot = Offset(x, y)) {
                    drawLine(color, Offset(x - arm, y), Offset(x + arm, y), strokeWidth = 1.6.dp.toPx())
                    drawLine(color, Offset(x, y - arm), Offset(x, y + arm), strokeWidth = 1.6.dp.toPx())
                }
            }
        }
    }
}

private fun DrawScope.drawConfettiBurstPiece(
    piece: BurstConfettiPiece,
    local: Float,
    x: Float,
    y: Float,
    alpha: Float
) {
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

// ── Rain confetti — mirrors ConfettiAnimationAdapter exactly ──────────────────

private data class RainParticle(
    val phaseFrac: Float,      // starting offset within the 0..1 cycle
    val startXFrac: Float,     // starting x as fraction of screen width
    val driftFrac: Float,      // total horizontal drift as fraction of screen width over one cycle
    val sizeDp: Float,         // width in dp; height = sizeDp * 0.36 (matches libGDX rect ratio)
    val startRotation: Float,  // initial rotation degrees
    val totalSpin: Float,      // total rotation degrees over one full fall cycle
    val color: Color,
    val colorAlpha: Float
)

private fun buildRainParticles(seed: Int, count: Int): List<RainParticle> {
    val rng = Random(seed * 4_337 + 13)
    // Mirror ConfettiAnimationAdapter: size 16–34px, spin ±180 deg/sec, drift ±55 px/sec
    // At ~2.75x density over a 15s fall cycle:
    //   size: 16/2.75..34/2.75 ≈ 6–12dp
    //   totalSpin: ±180 * 15 = ±2700 degrees
    //   drift: ±(55/2.75)/390 ≈ ±0.05 screen-widths per second × 15s ≈ ±0.77 → clamp ±0.5
    return List(count) { i ->
        val colorIdx = rng.nextInt(4)
        val baseColor = CONFETTI_COLORS_ALPHA[colorIdx]
        RainParticle(
            phaseFrac     = i.toFloat() / count.toFloat(),
            startXFrac    = rng.nextFloat(),
            driftFrac     = (rng.nextFloat() * 1.0f - 0.5f).coerceIn(-0.5f, 0.5f),
            sizeDp        = 6f + rng.nextFloat() * 6f,
            startRotation = rng.nextFloat() * 360f,
            totalSpin     = rng.nextFloat() * 5400f - 2700f,
            color         = baseColor.copy(alpha = 1f),
            colorAlpha    = baseColor.alpha
        )
    }
}
