package com.apptists.poplur2025.prelogin

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.apptists.poplur2025.R
import com.apptists.poplur2025.ui.components.BarberProgress
import com.apptists.poplur2025.ui.components.SheenLoadingText
import com.apptists.poplur2025.ui.components.SplashMessages
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// Same shape as the iOS launch cover (PoplurClaySplashView / SplashPlayback):
// the approved Good Copy splash (good-copy-splash.html) paints the clip edge
// to edge and floats the loader over it at 68% height behind a soft radial
// scrim. The clip itself ends the cover — real end-of-item completion, not a
// timer that used to run during decoder startup and cut the final logo
// reveal — and a first frame of this exact clip stands in before playback
// rather than a separate logo that flashed and vanished.
private const val MESSAGE_BEAT_MS = 850L
// Ten active seconds without the clip advancing is a decoder that will never
// deliver; let the app in rather than strand the person on the cover.
private const val STALL_LIMIT_MS = 10_000L

/** One hardware-decoded copy of the approved splash clip; no browser host. */
@Composable
fun NativePreloginSplash(modifier: Modifier = Modifier, isReady: Boolean = false, onFinished: () -> Unit = {}) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var active by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var playbackFinished by remember { mutableStateOf(false) }
    var didFinish by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var positionMs by remember { mutableLongStateOf(0L) }
    val player = remember { mutableStateOf<NativeSplashPlayer?>(null) }
    val finish by rememberUpdatedState(onFinished)
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> active = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        lifecycle.addObserver(observer); onDispose { lifecycle.removeObserver(observer) }
    }
    // Progress follows media time; the message beat follows it too, so a
    // paused clip holds its line instead of cycling over a frozen picture.
    LaunchedEffect(active, player.value) {
        val view = player.value ?: return@LaunchedEffect
        var lastAdvance = System.currentTimeMillis()
        var lastPosition = -1L
        while (active && isActive && !playbackFinished) {
            val position = view.positionMs
            val duration = view.durationMs
            if (position != lastPosition) { lastPosition = position; lastAdvance = System.currentTimeMillis() }
            positionMs = position
            progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
            if (view.failed || System.currentTimeMillis() - lastAdvance > STALL_LIMIT_MS) { playbackFinished = true; progress = 1f }
            delay(42)
        }
    }
    LaunchedEffect(playbackFinished, isReady) {
        if (playbackFinished && isReady && !didFinish) { didFinish = true; finish() }
    }
    BoxWithConstraints(modifier.fillMaxSize().background(Color(0xFF0A0709))) {
        // A frame from this exact clip avoids flashing a separate legacy logo.
        coil.compose.AsyncImage(
            "file:///android_asset/native-prelogin/SplashPoster.png", null,
            Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
        )
        AndroidView(
            factory = { context ->
                NativeSplashPlayer(context) { playbackFinished = true; progress = 1f }.also { player.value = it }
            },
            modifier = Modifier.fillMaxSize(),
            update = { it.setActive(active) },
            onRelease = { it.releasePlayer(); player.value = null },
        )
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color.Black.copy(alpha = .55f),
                    .52f to Color.Black.copy(alpha = .2f),
                    .74f to Color.Transparent,
                    1f to Color.Transparent,
                ),
                center = Offset(size.width / 2, size.height * .56f),
                radius = size.maxDimension * .5f,
            ))
        }
        Column(
            Modifier.align(Alignment.TopCenter).offset(y = maxHeight * .68f - 26.dp).width(minOf(215.dp, maxWidth * .58f)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SheenLoadingText(SplashMessages[((positionMs / MESSAGE_BEAT_MS) % SplashMessages.size).toInt()], alpha = 1f, modifier = Modifier.fillMaxWidth())
            BarberProgress(progress = progress.coerceAtLeast(.04f), modifier = Modifier.fillMaxWidth().height(9.dp))
        }
    }
}

private class NativeSplashPlayer(context: android.content.Context, private val onCompleted: () -> Unit) : TextureView(context), TextureView.SurfaceTextureListener {
    private var player: MediaPlayer? = null
    private var prepared = false
    private var active = false
    private var videoWidth = 0; private var videoHeight = 0
    /** An explicit media failure: the clip will never play, stop waiting for it. */
    var failed = false; private set
    val positionMs: Long get() = if (prepared) runCatching { player?.currentPosition?.toLong() ?: 0L }.getOrDefault(0L) else 0L
    val durationMs: Long get() = if (prepared) runCatching { player?.duration?.toLong() ?: 0L }.getOrDefault(0L) else 0L
    init { surfaceTextureListener = this }
    fun setActive(value: Boolean) { active = value; if (prepared) { if (value) player?.start() else player?.pause() } }
    fun releasePlayer() { prepared = false; player?.setOnPreparedListener(null); player?.setOnCompletionListener(null); player?.release(); player = null }
    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
        val media = MediaPlayer(); player = media
        runCatching {
            context.resources.openRawResourceFd(R.raw.poplur_splash).use { media.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            val surface = Surface(texture); media.setSurface(surface); surface.release()
            // Plays once and holds its final frame: the clip's closing logo is
            // the end of the cover, not a loop point.
            media.isLooping = false; media.setVolume(0f, 0f)
            media.setOnVideoSizeChangedListener { _, w, h -> videoWidth = w; videoHeight = h; crop() }
            media.setOnPreparedListener { prepared = true; if (active) it.start() }
            media.setOnCompletionListener { onCompleted() }
            media.setOnErrorListener { _, _, _ -> failed = true; true }
            media.prepareAsync()
        }.onFailure { failed = true }
    }
    // Edge to edge, matching Good Copy's `object-fit: cover`.
    private fun crop() {
        if (width <= 0 || height <= 0 || videoWidth <= 0 || videoHeight <= 0) return
        val scale = maxOf(width.toFloat() / videoWidth, height.toFloat() / videoHeight)
        setTransform(Matrix().apply { setScale(videoWidth * scale / width, videoHeight * scale / height, width / 2f, height / 2f) })
    }
    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) = crop()
    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean { releasePlayer(); return true }
}
