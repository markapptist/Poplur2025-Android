package com.apptists.poplur2025.wear

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.BatteryManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.badlogic.gdx.backends.android.AndroidFragmentApplication
import androidx.compose.material3.Text
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.apptists.poplur2025.wear.gdx.GlobeGdxFragment
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : FragmentActivity(), AndroidFragmentApplication.Callbacks {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(AndroidColor.BLACK))
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        setContent {
            val context = LocalContext.current
            val store = remember { WatchRadiusStore(context.applicationContext) }
            DisposableEffect(store) {
                store.start()
                onDispose { store.stop() }
            }
            RadiusWatchScreen(store = store)
        }
    }

    override fun exit() {
        finish()
    }
}

private enum class RadiusTrend { Up, Down, Stable }

@Stable
private class WatchRadiusStore(private val context: Context) : DataClient.OnDataChangedListener {
    private val prefs = context.getSharedPreferences("poplur_watch_radius", Context.MODE_PRIVATE)
    private val dataClient = Wearable.getDataClient(context)

    var radiusMeters by mutableLongStateOf(prefs.getLong(KEY_RADIUS, 0L))
        private set
    var postCount by mutableIntStateOf(prefs.getInt(KEY_POST_COUNT, 0))
        private set
    var updatedAt by mutableLongStateOf(prefs.getLong(KEY_UPDATED_AT, 0L))
        private set
    var trend by mutableStateOf(RadiusTrend.Stable)
        private set
    var batteryPercent by mutableIntStateOf(readBatteryPercent(context))
        private set

    init { recomputeTrend() }

    fun start() {
        dataClient.addListener(this)
        dataClient.dataItems.addOnSuccessListener { items ->
            items.use {
                for (item in it) {
                    if (item.uri.path == RADIUS_PATH) {
                        apply(DataMapItem.fromDataItem(item).dataMap)
                    }
                }
            }
        }
        batteryPercent = readBatteryPercent(context)
    }

    fun stop() { dataClient.removeListener(this) }

    override fun onDataChanged(events: DataEventBuffer) {
        events.use {
            for (event in it) {
                if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == RADIUS_PATH) {
                    apply(DataMapItem.fromDataItem(event.dataItem).dataMap)
                }
            }
        }
    }

    fun displayValue(metric: Boolean): String {
        if (radiusMeters <= 0L) return "0"
        val denominator = if (metric) WATCH_EARTH_RADIUS_KM else WATCH_EARTH_RADIUS_MI
        val value = (if (metric) radiusMeters / 1_000.0 else radiusMeters / 1_609.344)
            .coerceAtMost(denominator)
        return if (value >= 10.0) "%.0f".format(value) else "%.1f".format(value)
    }

    private fun apply(dataMap: DataMap) {
        val nextRadius = dataMap.radiusMeters()
        val nextPostCount = dataMap.getInt(KEY_POST_COUNT, 0).coerceAtLeast(0)
        val nextUpdatedAt = dataMap.getLong(KEY_UPDATED_AT, System.currentTimeMillis())
        val previous = radiusMeters

        prefs.edit()
            .putLong(KEY_PREVIOUS_RADIUS, previous)
            .putLong(KEY_RADIUS, nextRadius)
            .putInt(KEY_POST_COUNT, nextPostCount)
            .putLong(KEY_UPDATED_AT, nextUpdatedAt)
            .apply()

        radiusMeters = nextRadius
        postCount = nextPostCount
        updatedAt = nextUpdatedAt
        batteryPercent = readBatteryPercent(context)
        recomputeTrend()
    }

    private fun recomputeTrend() {
        val previous = prefs.getLong(KEY_PREVIOUS_RADIUS, 0L)
        trend = if (previous <= 0L || radiusMeters <= 0L) {
            RadiusTrend.Stable
        } else {
            val delta = radiusMeters - previous
            val threshold = max(radiusMeters * 0.03, 1_000.0).toLong()
            when {
                delta > threshold  -> RadiusTrend.Up
                delta < -threshold -> RadiusTrend.Down
                else               -> RadiusTrend.Stable
            }
        }
    }

    private fun DataMap.radiusMeters(): Long {
        if (containsKey(KEY_OVERALL_RADIUS_METERS)) return getLongCompatible(KEY_OVERALL_RADIUS_METERS)
        if (containsKey("radiusMeters")) return getLongCompatible("radiusMeters")
        if (containsKey("radius")) return getLongCompatible("radius")
        return 0L
    }

    private fun DataMap.getLongCompatible(key: String): Long {
        return (
            runCatching { getLong(key) }.getOrNull()
                ?: runCatching { getInt(key).toLong() }.getOrNull()
                ?: runCatching { getDouble(key).toLong() }.getOrNull()
                ?: runCatching { getString(key)?.toDoubleOrNull()?.toLong() }.getOrNull()
                ?: 0L
        ).coerceAtLeast(0L)
    }

    private companion object {
        const val RADIUS_PATH = "/poplur/radius"
        const val KEY_OVERALL_RADIUS_METERS = "overallRadiusMeters"
        const val KEY_RADIUS = "poplur.watch.overallRadiusMeters"
        const val KEY_PREVIOUS_RADIUS = "poplur.watch.previousRadiusMeters"
        const val KEY_POST_COUNT = "postCount"
        const val KEY_UPDATED_AT = "updatedAt"
    }
}

// ── Colors ───────────────────────────────────────────────────────────────────

private val LcdGreen  = Color(0.22f, 1.00f, 0.15f)
private val LcdOrange = Color(1.00f, 0.58f, 0.04f)
private val LcdBlue   = Color(0.24f, 0.78f, 1.00f)
private val LcdDivider = Color(0.10f, 0.45f, 0.10f)
private val LcdRed    = Color(1.00f, 0.18f, 0.12f)
private val LcdYellow = Color(1.00f, 0.86f, 0.10f)
private const val WATCH_EARTH_RADIUS_KM = 6_371.0
private const val WATCH_EARTH_RADIUS_MI = 3_959.0

// ── Main screen ──────────────────────────────────────────────────────────────

@Composable
private fun RadiusWatchScreen(store: WatchRadiusStore) {
    val transition = rememberInfiniteTransition(label = "lcd")

    val globePhase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "globe"
    )
    val arrowPhase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "arrow"
    )

    var showKm by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(4_000)
            showKm = !showKm
        }
    }

    var isShowingStatic by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isShowingStatic = true
                        val player = playStaticSound(context)
                        val released = tryAwaitRelease()
                        isShowingStatic = false
                        runCatching { player?.stop() }
                        player?.release()
                        if (released) showKm = !showKm
                    }
                )
            }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            val w = maxWidth
            val h = maxHeight

            Column(modifier = Modifier.fillMaxSize()) {
                // ── Header ──────────────────────────────────────────────────
                LCDHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(h * 0.13f)
                )

                // ── Globe + data ─────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(h * 0.60f)
                ) {
                    // Globe (54%) - Vector version matching recent iOS update
                    VectorGlobeView(
                        phase = globePhase,
                        trend = store.trend,
                        arrowPhase = arrowPhase,
                        modifier = Modifier
                            .width(w * 0.54f)
                            .fillMaxHeight()
                    )
                    // Data column (46%)
                    DataColumn(
                        store = store,
                        showKm = showKm,
                        modifier = Modifier
                            .width(w * 0.46f)
                            .fillMaxHeight()
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // ── Tab bar ──────────────────────────────────────────────────
                Canvas(modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)) {
                    drawLine(LcdDivider, Offset(0f, 0f), Offset(size.width, 0f))
                }
                TabBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(h * 0.12f)
                )

                // ── Stats bar ────────────────────────────────────────────────
                Canvas(modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)) {
                    drawLine(LcdDivider, Offset(0f, 0f), Offset(size.width, 0f))
                }
                StatsBar(
                    store = store,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(h * 0.11f)
                )
            }
        }

        if (isShowingStatic) {
            StaticOverlay(modifier = Modifier.fillMaxSize())
        }
    }
}

// ── Static overlay ───────────────────────────────────────────────────────────

@Composable
private fun StaticOverlay(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val logoBitmap: ImageBitmap? = remember(context) {
        runCatching {
            context.assets.open("poplur_logo.png").use {
                BitmapFactory.decodeStream(it)?.asImageBitmap()
            }
        }.getOrNull()
    }

    // Increment seed every 50ms to animate the noise (~20fps)
    var noiseSeed by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(50L)
            noiseSeed++
        }
    }

    // Pulse scale for the logo
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Canvas(modifier = modifier) {
        // TV static noise — each noiseSeed change redraws this canvas
        val block = 4f
        val cols = (size.width / block).toInt() + 1
        val rows = (size.height / block).toInt() + 1
        var s = (noiseSeed.toLong() xor 6_364_136_223_846_793_005L) and 0x7FFF_FFFF_FFFF_FFFFL
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                s = (s * 6_364_136_223_846_793_005L + 1_442_695_040_888_963_407L) and 0x7FFF_FFFF_FFFF_FFFFL
                val v = (s ushr 33).toFloat() / 0x7FFF_FFFFL.toFloat()
                drawRect(
                    color = Color(v, v, v, 0.90f),
                    topLeft = Offset(col * block, row * block),
                    size = Size(block, block)
                )
            }
        }

        // Poplur logo — circle-clipped, pulsing
        val bmp = logoBitmap ?: return@Canvas
        val logoSize = 68.dp.toPx() * pulseScale
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = logoSize / 2f
        val circlePath = Path().apply {
            addOval(Rect(cx - r, cy - r, cx + r, cy + r))
        }
        clipPath(circlePath) {
            drawImage(
                image = bmp,
                dstOffset = IntOffset((cx - r).roundToInt(), (cy - r).roundToInt()),
                dstSize = IntSize(logoSize.roundToInt(), logoSize.roundToInt())
            )
        }
    }
}

// ── Static sound ─────────────────────────────────────────────────────────────

private fun playStaticSound(context: Context): MediaPlayer? {
    val sampleRate = 22_050
    val numSamples = sampleRate * 650 / 1000  // 650ms
    val rng = java.util.Random()
    val samples = ShortArray(numSamples) { i ->
        val fadeIn  = minOf(1.0, i / 440.0)
        val fadeOut = minOf(1.0, (numSamples - i) / 880.0)
        val env     = fadeIn * fadeOut
        (rng.nextGaussian() * 0.62 * env * 32767.0)
            .toInt().coerceIn(-32768, 32767).toShort()
    }
    return try {
        val file = java.io.File(context.cacheDir, "poplur_static.wav")
        file.writeBytes(encodeWav(samples, sampleRate))
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setDataSource(file.absolutePath)
            setVolume(0.65f, 0.65f)
            prepare()   // synchronous; fine for a local cache file
            start()
        }
    } catch (e: Exception) {
        android.util.Log.e("PoplurWear", "Static sound failed", e)
        null
    }
}

private fun encodeWav(samples: ShortArray, sampleRate: Int): ByteArray {
    val dataBytes = samples.size * 2
    return java.nio.ByteBuffer
        .allocate(44 + dataBytes)
        .order(java.nio.ByteOrder.LITTLE_ENDIAN)
        .apply {
            put("RIFF".toByteArray()); putInt(36 + dataBytes)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16)
            putShort(1)                        // PCM
            putShort(1)                        // mono
            putInt(sampleRate)
            putInt(sampleRate * 2)             // byte rate
            putShort(2)                        // block align
            putShort(16)                       // bits per sample
            put("data".toByteArray()); putInt(dataBytes)
            for (s in samples) putShort(s)
        }
        .array()
}

// ── Header ───────────────────────────────────────────────────────────────────

@Composable
private fun LCDHeader(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = "DATA BANK 50",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.sp,
            color = LcdOrange,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "WATER RESIST",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = 6.sp,
            color = LcdBlue,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── Globe + trend ─────────────────────────────────────────────────────────────

// Continent shapes in equirectangular coords (lon -π..π → x, lat π/2..-π/2 → y)
// Each entry: (lonCenter, latCenter, lonHalfSpan, latHalfSpan)
private val continentBlobs = listOf(
    // North America
    floatArrayOf(-1.70f,  0.70f, 0.42f, 0.38f),
    // Central America
    floatArrayOf(-1.50f,  0.22f, 0.12f, 0.14f),
    // South America
    floatArrayOf(-1.10f, -0.30f, 0.28f, 0.50f),
    // Greenland
    floatArrayOf(-0.80f,  1.20f, 0.18f, 0.18f),
    // Europe
    floatArrayOf( 0.25f,  0.80f, 0.20f, 0.22f),
    // Africa
    floatArrayOf( 0.25f,  0.02f, 0.26f, 0.56f),
    // Middle East
    floatArrayOf( 0.60f,  0.40f, 0.16f, 0.20f),
    // Asia (main)
    floatArrayOf( 1.10f,  0.80f, 0.76f, 0.52f),
    // Indian subcontinent
    floatArrayOf( 0.90f,  0.20f, 0.18f, 0.26f),
    // SE Asia
    floatArrayOf( 1.70f,  0.22f, 0.18f, 0.20f),
    // Australia
    floatArrayOf( 2.20f, -0.46f, 0.26f, 0.20f),
)

@Composable
private fun VectorGlobeView(
    phase: Float,
    trend: RadiusTrend,
    arrowPhase: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f * 0.88f
            val cx = size.width / 2f
            val cy = size.height / 2f

            // 1. Atmosphere Glow
            drawCircle(
                color = LcdGreen.copy(alpha = 0.12f),
                radius = r * 1.12f,
                center = Offset(cx, cy)
            )

            // 2. Globe Body
            clipPath(Path().apply {
                addOval(Rect(cx - r, cy - r, cx + r, cy + r))
            }) {
                // Ocean / Dark side
                drawRect(color = Color.Black, size = size)

                // Grid lines (latitudes)
                val gridAlpha = 0.18f
                for (i in -3..3) {
                    val latY = cy + (i / 4f) * r
                    val latR = sqrt(max(0f, r * r - (latY - cy) * (latY - cy)))
                    drawOval(
                        color = LcdGreen.copy(alpha = gridAlpha),
                        topLeft = Offset(cx - latR, latY - (1.dp.toPx() / 2f)),
                        size = Size(latR * 2f, 1.dp.toPx())
                    )
                }

                // Continents (Vector Blobs)
                // Use a slightly faster rotation for the stylized look
                val rotation = phase * 2f * Math.PI.toFloat()
                continentBlobs.forEach { blob ->
                    val lonBase = blob[0]
                    val latBase = blob[1]
                    val lonHalf = blob[2]
                    val latHalf = blob[3]

                    // Wrap longitude with rotation
                    var lon = (lonBase + rotation) % (2f * Math.PI.toFloat())
                    if (lon < -Math.PI) lon += 2f * Math.PI.toFloat()
                    if (lon > Math.PI) lon -= 2f * Math.PI.toFloat()

                    // Project 3D -> 2D
                    // We simplify: x = cos(lat)*sin(lon), y = sin(lat), z = cos(lat)*cos(lon)
                    val cosLat = cos(latBase)
                    val sinLat = sin(latBase)
                    val x3d = cosLat * sin(lon)
                    val y3d = sinLat
                    val z3d = cosLat * cos(lon)

                    // Only draw if on the front half
                    if (z3d > -0.2f) {
                        val x2d = cx + x3d * r
                        val y2d = cy - y3d * r
                        
                        // Fade at edges of the sphere
                        val edgeFade = (z3d + 0.2f).coerceIn(0f, 1f)
                        
                        // Draw blob as a soft neon rectangle (stylized)
                        drawRect(
                            color = LcdGreen.copy(alpha = edgeFade * 0.85f),
                            topLeft = Offset(x2d - lonHalf * r, y2d - latHalf * r),
                            size = Size(lonHalf * 2f * r, latHalf * 2f * r)
                        )
                    }
                }

                // Scanning Line (Horizontal sweep)
                val scanY = cy + cos(phase * 2f * Math.PI.toFloat()) * r
                drawLine(
                    color = LcdGreen.copy(alpha = 0.25f),
                    start = Offset(cx - r, scanY),
                    end = Offset(cx + r, scanY),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 3. Globe Rim
            drawCircle(
                color = LcdGreen.copy(alpha = 0.5f),
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Overlay the trend arrow on top
        TrendOverlay(
            trend = trend,
            phase = arrowPhase,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun TrendOverlay(trend: RadiusTrend, phase: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.50f
        
        // Bounce animation matching iOS settle/overshoot feel
        val bounce = sin(phase * 2f * Math.PI.toFloat()) * 2.5.dp.toPx()
        val arrowCY = h * 0.85f + bounce

        val arrowW  = w * 0.35f
        val arrowH2 = h * 0.10f
        val al = cx - arrowW / 2f
        val ar = cx + arrowW / 2f

        when (trend) {
            RadiusTrend.Up -> {
                val p = Path().apply {
                    moveTo(cx, arrowCY - arrowH2)
                    lineTo(ar, arrowCY + arrowH2 * 0.2f)
                    lineTo(cx + arrowW * 0.18f, arrowCY + arrowH2 * 0.2f)
                    lineTo(cx + arrowW * 0.18f, arrowCY + arrowH2)
                    lineTo(cx - arrowW * 0.18f, arrowCY + arrowH2)
                    lineTo(cx - arrowW * 0.18f, arrowCY + arrowH2 * 0.2f)
                    lineTo(al, arrowCY + arrowH2 * 0.2f)
                    close()
                }
                drawPath(p, LcdGreen)
            }
            RadiusTrend.Down -> {
                val p = Path().apply {
                    moveTo(cx, arrowCY + arrowH2)
                    lineTo(ar, arrowCY - arrowH2 * 0.2f)
                    lineTo(cx + arrowW * 0.18f, arrowCY - arrowH2 * 0.2f)
                    lineTo(cx + arrowW * 0.18f, arrowCY - arrowH2)
                    lineTo(cx - arrowW * 0.18f, arrowCY - arrowH2)
                    lineTo(cx - arrowW * 0.18f, arrowCY - arrowH2 * 0.2f)
                    lineTo(al, arrowCY - arrowH2 * 0.2f)
                    close()
                }
                drawPath(p, LcdRed)
            }
            RadiusTrend.Stable -> {
                val dashW = arrowW * 0.70f
                drawLine(
                    color       = LcdYellow.copy(alpha = 0.85f),
                    start       = Offset(cx - dashW / 2f, arrowCY),
                    end         = Offset(cx + dashW / 2f, arrowCY),
                    strokeWidth = 2.5f
                )
            }
        }
    }
}

// ── Data column ───────────────────────────────────────────────────────────────

@Composable
private fun DataColumn(
    store: WatchRadiusStore,
    showKm: Boolean,
    modifier: Modifier = Modifier
) {
    val unit        = if (showKm) "km" else "mi"
    val denominator = if (showKm) "/${WATCH_EARTH_RADIUS_KM.toInt()}" else "/${WATCH_EARTH_RADIUS_MI.toInt()}"
    val radius      = if (showKm) store.displayValue(true) else store.displayValue(false)
    val timeFmt     = remember { SimpleDateFormat("H:mm", Locale.getDefault()) }

    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier.padding(end = 2.dp)
    ) {
        // World time
        Text(
            text = "WORLD TIME",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 6.sp,
            color = LcdBlue,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = timeFmt.format(Date()),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = LcdGreen,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )

        Canvas(modifier = Modifier.fillMaxWidth().height(0.5.dp)) {
            drawLine(LcdDivider, Offset(0f, 0f), Offset(size.width, 0f))
        }

        // Radius label
        Text(
            text = "RADIUS",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 6.sp,
            color = LcdBlue,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
        )

        // Radius value
        Text(
            text = radius,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = LcdGreen,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )

        // Post count
        Text(
            text = "${store.postCount}",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = LcdGreen.copy(alpha = 0.75f),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )

        // Earth denominator
        Text(
            text = denominator,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 8.sp,
            color = LcdGreen.copy(alpha = 0.85f),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )

        // Unit
        Text(
            text = unit,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = 6.sp,
            color = LcdGreen.copy(alpha = 0.70f),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

// ── Tab bar ───────────────────────────────────────────────────────────────────

@Composable
private fun TabBar(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        TabItem(label = "SCH", modifier = Modifier.weight(1f))
        Canvas(modifier = Modifier.width(0.5.dp).height(20.dp)) {
            drawLine(LcdDivider, Offset(0f, 0f), Offset(0f, size.height))
        }
        TabItem(label = "WORLD TIME", modifier = Modifier.weight(1f))
        Canvas(modifier = Modifier.width(0.5.dp).height(20.dp)) {
            drawLine(LcdDivider, Offset(0f, 0f), Offset(0f, size.height))
        }
        TabItem(label = "TEL", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TabItem(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 6.sp,
        color = LcdGreen,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

// ── Stats bar ─────────────────────────────────────────────────────────────────

@Composable
private fun StatsBar(store: WatchRadiusStore, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        StatChip(label = "↑${store.postCount}", modifier = Modifier.weight(0.28f))
        StatChip(label = "◾84°F",              modifier = Modifier.weight(0.27f))
        StatChip(label = "🔋${store.batteryPercent}%", modifier = Modifier.weight(0.25f))
        StatChip(label = "♥75",               modifier = Modifier.weight(0.20f))
    }
}

@Composable
private fun StatChip(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 5.5.sp,
        color = LcdGreen.copy(alpha = 0.80f),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

// ── Battery ───────────────────────────────────────────────────────────────────

private fun readBatteryPercent(context: Context): Int {
    val intent: Intent = context.registerReceiver(
        null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    ) ?: return 0
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    if (level < 0 || scale <= 0) return 0
    return ((level / scale.toFloat()) * 100f).toInt().coerceIn(0, 100)
}

suspend fun androidx.compose.ui.input.pointer.PointerInputScope.tryAwaitRelease(): Boolean {
    return try {
        awaitPointerEventScope {
            var released = false
            while (!released) {
                val event = awaitPointerEvent()
                if (event.changes.all { it.changedToUpIgnoreConsumed() }) {
                    released = true
                }
            }
            true
        }
    } catch (_: Exception) {
        false
    }
}
