package com.apptists.poplur2025.ui.components

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.apptists.poplur2025.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.max
import kotlin.math.min

private val SplashInk = Color(0xFF0A0709)
private val SplashPink = Color(0xFFF43B99)
private val SplashPinkHigh = Color(0xFFFF8AC5)
private val SplashPinkDeep = Color(0xFFC92E7D)
private val BarberCream = Color(0xFFFFE1F1)
private val BarberLightPink = Color(0xFFFF86C4)
private val BarberPink = Color(0xFFF53C99)

internal val SplashMessages = listOf(
    "Warming up the pinetrees",
    "Rolling out the clay roads",
    "Teaching the pin to drop",
    "Sprinkling pink stars",
    "Kneading the little mountains",
    "Waking up the tugboat",
    "Polishing the big P",
    "Fluffing the clouds"
)

/**
 * Approved Good Copy launch surface. Everything here is bundled locally: the looping clay clip,
 * the status lines, the animated text shader, and the barber-pole progress treatment.
 */
@Composable
fun PoplurClaySplashOverlay(
    isReady: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var authReady by remember { mutableStateOf(isReady) }
    var messageIndex by remember { mutableIntStateOf(0) }
    var didFinish by remember { mutableStateOf(false) }
    val messageAlpha = remember { Animatable(1f) }
    val progress = remember { Animatable(0.04f) }
    val currentOnFinished by rememberUpdatedState(onFinished)

    LaunchedEffect(isReady) {
        if (isReady) authReady = true
    }

    LaunchedEffect(Unit) {
        delay(850L)
        while (isActive) {
            messageAlpha.animateTo(0.18f, tween(220, easing = LinearEasing))
            messageIndex = (messageIndex + 1) % SplashMessages.size
            messageAlpha.animateTo(1f, tween(220, easing = LinearEasing))
            delay(410L)
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 0.90f,
            animationSpec = tween(2_800, easing = FastOutSlowInEasing)
        )
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(720, easing = FastOutSlowInEasing)
        )
        delay(480L)

        // Auth normally resolves during the four-second visual sequence. Keep a bounded safety
        // window so a Firebase initialization failure can never strand the user on the splash.
        var checks = 0
        while (!authReady && checks < 100) {
            delay(50L)
            checks++
        }
        delay(300L)
        if (!didFinish) {
            didFinish = true
            currentOnFinished()
        }
    }

    BoxWithConstraints(modifier.background(SplashInk)) {
        SplashVideo(modifier = Modifier.fillMaxSize())

        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Color.Black.copy(alpha = 0.55f),
                        0.52f to Color.Black.copy(alpha = 0.20f),
                        0.74f to Color.Transparent,
                        1f to Color.Transparent
                    ),
                    center = Offset(size.width * 0.50f, size.height * 0.56f),
                    radius = size.maxDimension * 0.62f
                ),
                size = size
            )
        }

        val loaderWidth = minOf(maxWidth * 0.58f, 215.dp)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = maxHeight * 0.68f - 26.dp)
                .width(loaderWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SheenLoadingText(
                text = SplashMessages[messageIndex],
                alpha = messageAlpha.value,
                modifier = Modifier.fillMaxWidth()
            )
            BarberProgress(
                progress = progress.value,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
            )
        }
    }
}

@Composable
internal fun SheenLoadingText(
    text: String,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "splashTextSheen")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "splashTextSheenPhase"
    )
    var textWidthPx by remember { mutableFloatStateOf(1f) }
    val startX = -textWidthPx + (textWidthPx * 2.2f * phase)
    val sheen = Brush.linearGradient(
        colorStops = arrayOf(
            0f to SplashPinkHigh,
            0.34f to SplashPink,
            0.50f to Color(0xFFFFF3FA),
            0.66f to SplashPink,
            1f to SplashPinkDeep
        ),
        start = Offset(startX, 0f),
        end = Offset(startX + textWidthPx * 2.2f, 0f)
    )

    Text(
        text = text,
        modifier = modifier
            .heightIn(min = 23.dp)
            .alpha(alpha.coerceIn(0f, 1f))
            .onSizeChanged { textWidthPx = it.width.toFloat().coerceAtLeast(1f) },
        style = TextStyle(
            brush = sheen,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            shadow = Shadow(
                color = SplashPink.copy(alpha = 0.50f),
                offset = Offset.Zero,
                blurRadius = 12f
            )
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
internal fun BarberProgress(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "splashBarber")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "splashBarberPhase"
    )

    Canvas(modifier) {
        val radius = size.height / 2f
        val corner = CornerRadius(radius, radius)
        drawRoundRect(
            color = Color(0x8C1C0812),
            size = size,
            cornerRadius = corner
        )

        val fillWidth = size.width * progress.coerceIn(0f, 1f)
        if (fillWidth > 0f) {
            val fillRadius = min(radius, fillWidth / 2f)
            val fillPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = 0f,
                        top = 0f,
                        right = fillWidth,
                        bottom = size.height,
                        cornerRadius = CornerRadius(fillRadius, fillRadius)
                    )
                )
            }

            clipPath(fillPath) {
                val stripeWidths = floatArrayOf(
                    15.dp.toPx(),
                    14.dp.toPx(),
                    15.dp.toPx()
                )
                val stripeColors = arrayOf(BarberCream, BarberLightPink, BarberPink)
                val period = stripeWidths.sum()
                var cycleStart = -size.height - period * 2f - phase * period

                while (cycleStart < fillWidth + size.height + period) {
                    var stripeStart = cycleStart
                    stripeWidths.forEachIndexed { index, stripeWidth ->
                        val stripe = Path().apply {
                            moveTo(stripeStart, 0f)
                            lineTo(stripeStart + stripeWidth, 0f)
                            lineTo(stripeStart + stripeWidth + size.height, size.height)
                            lineTo(stripeStart + size.height, size.height)
                            close()
                        }
                        drawPath(stripe, stripeColors[index])
                        stripeStart += stripeWidth
                    }
                    cycleStart += period
                }

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.52f),
                            Color.Transparent,
                            Color(0xFF8C124A).copy(alpha = 0.45f)
                        )
                    ),
                    size = Size(fillWidth, size.height)
                )
            }
        }

        drawRoundRect(
            color = SplashPink.copy(alpha = 0.35f),
            size = size,
            cornerRadius = corner,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
private fun SplashVideo(
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val textureView = TextureView(context)
            val player = MediaPlayer()
            var videoWidth = 0
            var videoHeight = 0

            fun applyCenterCrop() {
                val viewWidth = textureView.width.toFloat()
                val viewHeight = textureView.height.toFloat()
                if (viewWidth <= 0f || viewHeight <= 0f || videoWidth <= 0 || videoHeight <= 0) {
                    return
                }
                val scale = max(viewWidth / videoWidth, viewHeight / videoHeight)
                Matrix().also { matrix ->
                    matrix.setScale(
                        videoWidth * scale / viewWidth,
                        videoHeight * scale / viewHeight,
                        viewWidth / 2f,
                        viewHeight / 2f
                    )
                    textureView.setTransform(matrix)
                }
            }

            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    surfaceTexture: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {
                    runCatching {
                        context.resources.openRawResourceFd(R.raw.poplur_splash).use { descriptor ->
                            player.setDataSource(
                                descriptor.fileDescriptor,
                                descriptor.startOffset,
                                descriptor.length
                            )
                        }
                        val surface = Surface(surfaceTexture)
                        player.setSurface(surface)
                        surface.release()
                        player.isLooping = true
                        player.setVolume(0f, 0f)
                        player.setOnVideoSizeChangedListener { _, newWidth, newHeight ->
                            videoWidth = newWidth
                            videoHeight = newHeight
                            applyCenterCrop()
                        }
                        player.setOnPreparedListener(MediaPlayer::start)
                        player.prepareAsync()
                    }
                }

                override fun onSurfaceTextureSizeChanged(
                    surfaceTexture: SurfaceTexture,
                    width: Int,
                    height: Int
                ) = applyCenterCrop()

                override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit

                override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                    runCatching { player.release() }
                    return true
                }
            }
            textureView
        }
    )
}
