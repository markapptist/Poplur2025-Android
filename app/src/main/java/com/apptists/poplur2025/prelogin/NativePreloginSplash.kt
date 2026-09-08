package com.apptists.poplur2025.prelogin

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.apptists.poplur2025.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** One hardware-decoded copy of the approved splash clip; no browser host. */
@Composable
fun NativePreloginSplash(modifier: Modifier = Modifier, isReady: Boolean = false, onFinished: () -> Unit = {}) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var active by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var elapsed by remember { mutableStateOf(false) }
    val finish by rememberUpdatedState(onFinished)
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> active = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        lifecycle.addObserver(observer); onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { delay(3200); elapsed = true }
    LaunchedEffect(elapsed, isReady) { if (elapsed && isReady) finish() }
    BoxWithConstraints(modifier.fillMaxSize().background(Color(0xFF0A0709))) {
        coil.compose.AsyncImage("file:///android_asset/native-prelogin/NativePreloginLogo.png", null, Modifier.size(minOf(170.dp, maxWidth * .45f)).align(Alignment.Center))
        NativeSplashVideo(active)
        Canvas(Modifier.fillMaxSize()) { drawRect(Brush.radialGradient(listOf(Color.Black.copy(alpha = .55f), Color.Black.copy(alpha = .2f), Color.Transparent), center = Offset(size.width / 2, size.height * .56f), radius = size.height * .5f)) }
        Column(Modifier.align(Alignment.TopCenter).offset(y = maxHeight * .68f - 20.dp).width(minOf(215.dp, maxWidth * .58f)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Loading…", color = Color(0xFFFF8AC5), fontSize = 20.sp, fontWeight = FontWeight.Black)
            val time = remember { mutableFloatStateOf(0f) }
            LaunchedEffect(active) { while (active && isActive) { delay(42); time.floatValue += .042f } }
            Canvas(Modifier.fillMaxWidth().height(9.dp)) {
                drawRoundRect(Color(0x55433B99), cornerRadius = CornerRadius(5.dp.toPx()))
                val offset = (time.floatValue % 1f) * 24.dp.toPx()
                var x = offset - 24.dp.toPx()
                while (x < size.width) { drawRoundRect(Color(0xFFF43B99), Offset(x.coerceAtLeast(0f), 0f), Size((12.dp.toPx()).coerceAtMost(size.width - x.coerceAtLeast(0f)), size.height), CornerRadius(3.dp.toPx())); x += 24.dp.toPx() }
            }
        }
    }
}

private class NativeSplashPlayer(context: android.content.Context) : TextureView(context), TextureView.SurfaceTextureListener {
    private var player: MediaPlayer? = null
    private var prepared = false
    private var active = false
    private var videoWidth = 0; private var videoHeight = 0
    init { surfaceTextureListener = this }
    fun setActive(value: Boolean) { active = value; if (prepared) { if (value) player?.start() else player?.pause() } }
    fun releasePlayer() { prepared = false; player?.setOnPreparedListener(null); player?.release(); player = null }
    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
        val media = MediaPlayer(); player = media
        context.resources.openRawResourceFd(R.raw.poplur_splash).use { media.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
        val surface = Surface(texture); media.setSurface(surface); surface.release()
        media.isLooping = true; media.setVolume(0f, 0f)
        media.setOnVideoSizeChangedListener { _, w, h -> videoWidth = w; videoHeight = h; crop() }
        media.setOnPreparedListener { prepared = true; if (active) it.start() }
        media.prepareAsync()
    }
    private fun crop() {
        if (width <= 0 || height <= 0 || videoWidth <= 0 || videoHeight <= 0) return
        val scale = maxOf(width.toFloat() / videoWidth, height.toFloat() / videoHeight)
        setTransform(Matrix().apply { setScale(videoWidth * scale / width, videoHeight * scale / height, width / 2f, height / 2f) })
    }
    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) = crop()
    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean { releasePlayer(); return true }
}
@Composable private fun NativeSplashVideo(active: Boolean) { AndroidView(factory = { NativeSplashPlayer(it) }, modifier = Modifier.fillMaxSize(), update = { it.setActive(active) }, onRelease = { it.releasePlayer() }) }
