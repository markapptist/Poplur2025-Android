package com.apptists.poplur2025.prelogin

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.PixelCopy
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.apptists.poplur2025.ui.components.PerformanceSceneView
import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.sin

private data class NativeArtState(val model: ModelNode, var color: Color, var welcome: Boolean)

private object PreloginThumbnailCache { val images = mutableMapOf<String, Bitmap>() }

@Composable
fun PreloginModelArt(asset: String, modifier: Modifier, background: Color, welcome: Boolean = false, awake: Boolean = true, thumbnail: Boolean = false, onBack: () -> Unit = {}, onTouch: () -> Unit = {}) {
    var snapshot by remember(asset) { mutableStateOf(PreloginThumbnailCache.images[asset]) }
    val currentBack by rememberUpdatedState(onBack)
    val currentTouch by rememberUpdatedState(onTouch)
    val currentWelcome by rememberUpdatedState(welcome)
    val currentAwake by rememberUpdatedState(awake)
    if (thumbnail && snapshot != null) {
        Image(snapshot!!.asImageBitmap(), contentDescription = null, modifier = modifier.background(background))
    } else {
        key(asset) {
            AndroidView(modifier = modifier, factory = { context ->
                PerformanceSceneView(context, isOpaque = thumbnail).apply {
                    scene.skybox = if (thumbnail) Skybox.Builder().color(background.red, background.green, background.blue, 1f).build(engine) else null
                    scene.indirectLight = IndirectLight.Builder().irradiance(1, floatArrayOf(1f, 1f, 1.05f)).intensity(55_000f).build(engine)
                    val modelPath = when (asset) {
                        "MarkHead_TalkingRig" -> "Poplur2025-iOS/Poplur2025/MarkHead_TalkingRig.glb"
                        "zoom_buildings" -> "signup/zoom_buildings.glb"
                        else -> "claymap/$asset.glb"
                    }
                    val instance = modelLoader.createModelInstance(modelPath)
                    val model = ModelNode(instance, scaleToUnits = 1.55f, centerOrigin = Float3(0f, 0f, 0f))
                    model.rotation = Float3(-14f, if (thumbnail) { if (asset == "zoom_buildings") -32f else 12f } else if (welcome) -90f else 112f, 0f)
                    childNodes = listOf(model); cameraNode.position = Float3(0f, 0f, 3.4f); cameraNode.lookAt(model)
                    tag = NativeArtState(model, background, welcome)
                    var previousTime = 0L; var frames = 0; var capturePending = false; var captureAttempts = 0
                    onFrame = { time ->
                        if (!thumbnail && currentWelcome && currentAwake && previousTime != 0L) model.rotation = model.rotation.copy(y = model.rotation.y + ((time - previousTime) / 1_000_000_000f).coerceAtMost(0.1f) * 12f)
                        previousTime = time; frames++
                        if (thumbnail && frames >= 4 && !capturePending && captureAttempts < 5 && width > 0 && height > 0) {
                            capturePending = true; captureAttempts++
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            PixelCopy.request(this, bitmap, { result ->
                                if (result == PixelCopy.SUCCESS && isAttachedToWindow) { PreloginThumbnailCache.images[asset] = bitmap; snapshot = bitmap }
                                else { bitmap.recycle(); capturePending = false; if (captureAttempts >= 5) setRenderingRequested(false) }
                            }, Handler(Looper.getMainLooper()))
                        }
                    }
                    var startX = 0f; var startY = 0f; var downX = 0f
                    if (!thumbnail) setOnTouchListener { view, event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> { currentTouch(); setRenderingRequested(true); downX = event.x; startX = event.x; startY = model.rotation.y; true }
                            MotionEvent.ACTION_MOVE -> { model.rotation = model.rotation.copy(y = startY + (event.x - startX) * 0.75f); true }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                if (event.actionMasked == MotionEvent.ACTION_UP && abs(event.x - downX) < 14f && !currentWelcome) { view.performClick(); currentBack() }
                                if (!currentWelcome) model.rotation = Float3(-14f, 112f, 0f)
                                true
                            }
                            else -> false
                        }
                    }
                }
            }, update = { view ->
                view.setRenderingRequested(if (thumbnail) snapshot == null else awake)
                val state = view.tag as? NativeArtState
                if (state != null) {
                    if (!thumbnail && state.welcome != welcome) {
                        state.welcome = welcome
                        state.model.rotation = Float3(-14f, if (welcome) -90f else 112f, 0f)
                    }
                    if (thumbnail && state.color != background) {
                        state.color = background
                        val oldSky = view.scene.skybox
                        view.scene.skybox = Skybox.Builder().color(background.red, background.green, background.blue, 1f).build(view.engine)
                        oldSky?.let { view.engine.destroySkybox(it) }
                    }
                }
            }, onRelease = { it.destroy() })
        }
    }
}

@Composable
fun PreloginBackdrop(color: Color, awake: Boolean) {
    Box(Modifier.fillMaxSize().background(color)) {
        Canvas(Modifier.fillMaxSize()) {
            var x = 5.dp.toPx()
            while (x < size.width) {
                var y = 5.dp.toPx()
                while (y < size.height) { drawCircle(Color(0x60D6106C), 1.6.dp.toPx(), Offset(x, y)); y += 10.dp.toPx() }
                x += 10.dp.toPx()
            }
            drawRect(androidx.compose.ui.graphics.Brush.radialGradient(listOf(Color.White.copy(alpha = .15f), Color.Transparent, Color.Black.copy(alpha = .22f)), center = Offset(size.width / 2, size.height * .4f), radius = size.height * .8f))
        }
        NativeConfetti(awake)
    }
}

@Composable
private fun NativeConfetti(awake: Boolean) {
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(awake) { while (awake && isActive) { delay(42); time.floatValue += .042f } }
    Canvas(Modifier.fillMaxSize()) {
        val t = time.floatValue
        val colors = listOf(Color.White, Color.Yellow, Color.Cyan, Color(0xFFFF8BC2), Color(0xFFB1FF53))
        repeat(32) { i ->
            val x = (((i * 73 + 11) % 101) / 101f) * size.width + sin(t + i) * 13.dp.toPx()
            val y = ((i * 37.dp.toPx() + t * (14 + i % 8).dp.toPx()) % (size.height + 25.dp.toPx())) - 15.dp.toPx()
            rotate(t * 23 + i * 57f, Offset(x, y)) { drawRoundRect(colors[i % colors.size], Offset(x, y), Size(8.dp.toPx(), 13.dp.toPx()), CornerRadius(3.dp.toPx())) }
        }
    }
}
