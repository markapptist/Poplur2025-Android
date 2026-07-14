package com.apptists.poplur2025.ui.components

import android.graphics.Matrix
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.Build
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.Text
import com.apptists.poplur2025.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

private val INK = Color(0xFF0A0709)
private val PINK = Color(0xFFF63B9A)
private val PINK_HI = Color(0xFFFF8AC6)
private val PINK_DEEP = Color(0xFFC92E7E)

/**
 * Cold-launch clay splash: plays the intro clip full-bleed at device resolution (center-crop,
 * 1:1) with the app's pop sound, then raises a centered loading bar whose lines are drawn with
 * an AGSL shader. Finishes once auth has resolved; the slow fill never gates that.
 */
@Composable
fun PoplurClaySplashOverlay(
    isSignedIn: Boolean?,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val lines = remember { SplashLoadingLines.sessionLines(context) }

    var currentMessage by remember { mutableStateOf(lines.firstOrNull() ?: "Loading Poplur") }
    var showLoader by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var authResolved by remember { mutableStateOf(isSignedIn != null) }
    var didFinish by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { SplashLoadingLines.refreshFromClaudeInBackground(context) }
    LaunchedEffect(isSignedIn) { if (isSignedIn != null) authResolved = true }

    Box(modifier.fillMaxSize().background(INK)) {

        SplashVideo(onEnded = { if (!showLoader) showLoader = true })

        if (showLoader) {
            // radial scrim so the shader text reads over the busy clay scene
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.72f), Color.Black.copy(alpha = 0.30f), Color.Transparent),
                        radius = 620f
                    )
                )
            )
            Column(
                modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                ShaderLoadingText(currentMessage)
                LoadingTrack(progress)
            }
        }
    }

    // Safety net: if the clip never signals completion, raise the loader anyway.
    LaunchedEffect(Unit) {
        delay(3400)
        if (!showLoader) showLoader = true
    }

    // Sequence: slow cosmetic fill + rotating lines, finish as soon as auth is known.
    LaunchedEffect(showLoader) {
        if (!showLoader) return@LaunchedEffect
        val shownAt = System.currentTimeMillis()

        val rotate = launch {
            var i = 1
            while (isActive) {
                delay(850)
                currentMessage = lines[i % lines.size]
                i++
            }
        }
        // cosmetic slow creep toward near-full — animated separately, does not gate completion
        val fill = launch {
            val steps = 64
            val target = 0.9f
            for (s in 1..steps) {
                progress = target * (s.toFloat() / steps)
                delay(3200L / steps)
            }
        }

        while (!authResolved) delay(60)
        val elapsed = System.currentTimeMillis() - shownAt
        if (elapsed < 1100) delay(1100 - elapsed)

        rotate.cancel()
        fill.cancel()
        currentMessage = SplashLoadingLines.destinationLine(isSignedIn == true)
        // snap to full
        val from = progress
        val snapSteps = 18
        for (s in 1..snapSteps) {
            progress = from + (1f - from) * (s.toFloat() / snapSteps)
            delay(450L / snapSteps)
        }
        delay(700)
        if (!didFinish) {
            didFinish = true
            onFinished()
        }
    }
}

// MARK: loading bar (no shadow)

@Composable
private fun LoadingTrack(progress: Float) {
    Box(
        Modifier
            .fillMaxWidth(0.82f)
            .widthIn(max = 340.dp)
            .height(14.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, PINK.copy(alpha = 0.35f), RoundedCornerShape(50))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(PINK_HI, PINK, PINK_DEEP)))
        )
    }
}

// MARK: shader-driven loading text

private const val SPLASH_AGSL = """
uniform shader content;
uniform float iTime;
half4 main(float2 coord) {
    half4 c = content.eval(coord);
    if (c.a < 0.01) return c;
    half3 pink = half3(0.965, 0.231, 0.604);
    half3 hi   = half3(1.0, 0.541, 0.776);
    half3 deep = half3(0.788, 0.180, 0.494);
    float grad = clamp(coord.y * 0.010, 0.0, 1.0);
    half3 body = mix(hi, deep, grad);
    body = mix(body, pink, 0.35);
    float sweep = fract(iTime * 0.35);
    float bandPos = sweep * 520.0 - 60.0;
    float band = exp(-pow((coord.x - bandPos) / 42.0, 2.0));
    half3 col = body + half3(band * 0.55, band * 0.35, band * 0.45);
    col = mix(body, col, band * 0.6 + 0.4);
    return half4(col, 1.0) * c.a;
}
"""

@Composable
private fun ShaderLoadingText(text: String) {
    val infinite = rememberInfiniteTransition(label = "splashShader")
    val t by infinite.animateFloat(
        initialValue = 0f, targetValue = 60f,
        animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing)),
        label = "t"
    )

    if (Build.VERSION.SDK_INT >= 33) {
        val shader = remember { RuntimeShader(SPLASH_AGSL) }
        Text(
            text = text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            color = Color.White,
            modifier = Modifier.graphicsLayer {
                shader.setFloatUniform("iTime", t)
                renderEffect = AndroidRenderEffect
                    .createRuntimeShaderEffect(shader, "content")
                    .asComposeRenderEffect()
            }
        )
    } else {
        // Fallback: an animated pink gradient shader brush (still a Shader, no RuntimeShader).
        val phase = t * 60f
        val brush = Brush.linearGradient(
            colors = listOf(PINK_HI, PINK, PINK_DEEP, PINK, PINK_HI),
            start = Offset(phase % 400f - 200f, 0f),
            end = Offset(phase % 400f + 200f, 0f)
        )
        Text(
            text = text,
            style = TextStyle(brush = brush),
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// MARK: full-bleed center-crop video (1:1 device) with sound

@Composable
private fun SplashVideo(onEnded: () -> Unit) {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val texture = TextureView(ctx)
            var videoW = 0
            var videoH = 0
            val player = MediaPlayer()

            fun applyCover() {
                val vw = texture.width.toFloat()
                val vh = texture.height.toFloat()
                if (vw == 0f || vh == 0f || videoW == 0 || videoH == 0) return
                val scale = max(vw / videoW, vh / videoH)
                val m = Matrix()
                m.setScale(videoW * scale / vw, videoH * scale / vh, vw / 2f, vh / 2f)
                texture.setTransform(m)
            }

            texture.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                    runCatching {
                        ctx.resources.openRawResourceFd(R.raw.poplur_splash).use { fd ->
                            player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                        }
                        player.setSurface(Surface(st))
                        player.isLooping = false
                        player.setOnVideoSizeChangedListener { _, vw, vh ->
                            videoW = vw; videoH = vh; applyCover()
                        }
                        player.setOnPreparedListener { it.start() }
                        player.setOnCompletionListener { onEnded() }
                        player.prepareAsync()
                    }.onFailure { onEnded() }
                }

                override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) = applyCover()
                override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                    runCatching { player.release() }
                    return true
                }
            }
            texture
        }
    )
}
