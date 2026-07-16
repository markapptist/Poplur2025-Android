package com.apptists.poplur2025.ui.components

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.apptists.poplur2025.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.max

private val INK = Color(0xFF0A0709)
private val PINK = Color(0xFFF53C99)
private val PINK_MID = Color(0xFFFF86C4)
private val PINK_PALE = Color(0xFFFFE1F1)

/** The approved prototype splash: clay video, fixed copy pool, and barber-pole progress bar. */
@Composable
fun PoplurClaySplashOverlay(
    isSignedIn: Boolean?,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit,
) {
    val authState by rememberUpdatedState(isSignedIn)
    val lines = remember { SplashLoadingLines.sessionLines() }
    var lineIndex by remember { mutableIntStateOf(0) }
    var didFinish by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier.fillMaxSize().background(INK)) {
        SplashVideo()
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.55f),
                        Color.Black.copy(alpha = 0.20f),
                        Color.Transparent,
                    ),
                    center = Offset.Unspecified,
                    radius = 620f,
                )
            )
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = maxHeight * 0.18f)
                .fillMaxWidth(0.58f)
                .widthIn(max = 215.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SplashMessage(lines[lineIndex])
            BarberPoleLoadingTrack()
        }
    }

    LaunchedEffect(lines) {
        while (isActive) {
            delay(850)
            lineIndex = (lineIndex + 1) % lines.size
        }
    }

    LaunchedEffect(Unit) {
        delay(3200)
        while (authState == null && isActive) delay(50)
        if (!didFinish && isActive) {
            didFinish = true
            onFinished()
        }
    }
}

@Composable
private fun SplashMessage(text: String) {
    val shimmer = rememberInfiniteTransition(label = "splashMessage")
    val phase by shimmer.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "messageSheen",
    )
    val shift = phase * 300f
    Text(
        text = text,
        style = TextStyle(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFD3EA),
                    Color(0xFFFF7CC0),
                    PINK,
                    Color(0xFFFF7CC0),
                    Color(0xFFFFD3EA),
                ),
                start = Offset(shift - 180f, 0f),
                end = Offset(shift + 180f, 0f),
            )
        ),
        fontSize = 20.sp,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
        lineHeight = 24.sp,
        maxLines = 2,
    )
}

@Composable
private fun BarberPoleLoadingTrack() {
    val animation = rememberInfiniteTransition(label = "splashBarberPole")
    val fill by animation.animateFloat(
        initialValue = 0.04f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = CubicBezierEasing(0f, 0f, 0.58f, 1f)),
            repeatMode = RepeatMode.Restart,
        ),
        label = "splashFill",
    )
    val barberPhase by animation.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "barberPhase",
    )
    val density = LocalDensity.current
    val bandWidth = with(density) { 8.dp.toPx() }
    val barberTravel = with(density) { 34.dp.toPx() }
    val shape = RoundedCornerShape(50)

    Box(
        Modifier
            .fillMaxWidth()
            .height(9.dp)
            .clip(shape)
            .background(Color(0x8C1C0812))
            .border(1.dp, PINK.copy(alpha = 0.28f), shape)
    ) {
        Canvas(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fill.coerceIn(0.04f, 1f))
                .clip(shape)
        ) {
            val colors = listOf(PINK_PALE, PINK_MID, PINK)
            val first = -barberTravel * barberPhase - size.height - bandWidth * 3f
            var x = first
            var index = 0
            while (x < size.width + size.height + bandWidth * 3f) {
                val path = Path().apply {
                    moveTo(x, 0f)
                    lineTo(x + bandWidth, 0f)
                    lineTo(x + bandWidth - size.height, size.height)
                    lineTo(x - size.height, size.height)
                    close()
                }
                drawPath(path, colors[index % colors.size])
                x += bandWidth
                index++
            }
            drawLine(
                color = Color.White.copy(alpha = 0.60f),
                start = Offset(0f, 0.7f),
                end = Offset(size.width, 0.7f),
                strokeWidth = 1.4f,
            )
            drawLine(
                color = Color(0xFF8C124A).copy(alpha = 0.50f),
                start = Offset(0f, size.height - 0.7f),
                end = Offset(size.width, size.height - 0.7f),
                strokeWidth = 1.4f,
            )
        }
    }
}

@Composable
private fun SplashVideo() {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val texture = TextureView(ctx)
            var videoW = 0
            var videoH = 0
            val player = MediaPlayer()

            fun applyCover() {
                val viewWidth = texture.width.toFloat()
                val viewHeight = texture.height.toFloat()
                if (viewWidth == 0f || viewHeight == 0f || videoW == 0 || videoH == 0) return
                val scale = max(viewWidth / videoW, viewHeight / videoH)
                val matrix = Matrix()
                matrix.setScale(
                    videoW * scale / viewWidth,
                    videoH * scale / viewHeight,
                    viewWidth / 2f,
                    viewHeight / 2f,
                )
                texture.setTransform(matrix)
            }

            texture.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                    runCatching {
                        context.resources.openRawResourceFd(R.raw.poplur_splash).use { fd ->
                            player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                        }
                        player.setSurface(Surface(surfaceTexture))
                        player.isLooping = true
                        player.setVolume(0f, 0f)
                        player.setOnVideoSizeChangedListener { _, widthPixels, heightPixels ->
                            videoW = widthPixels
                            videoH = heightPixels
                            applyCover()
                        }
                        player.setOnPreparedListener { it.start() }
                        player.prepareAsync()
                    }
                }

                override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) = applyCover()
                override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit
                override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                    runCatching { player.release() }
                    return true
                }
            }
            texture
        },
    )
}
