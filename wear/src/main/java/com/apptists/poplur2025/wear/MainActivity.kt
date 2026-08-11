package com.apptists.poplur2025.wear

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.BatteryManager
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.badlogic.gdx.backends.android.AndroidFragmentApplication
import com.apptists.poplur2025.wear.gdx.GlobeGdxFragment
import com.apptists.poplur.performance.PoplurPerformanceManager
import com.apptists.poplur.performance.PoplurPerformanceSnapshot
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.max
import kotlin.math.sin

class MainActivity : FragmentActivity(), AndroidFragmentApplication.Callbacks {
    override fun exit() { finish() }

    private lateinit var radioHost: WearRadioHost

    // Once the user grants location, refine the broadcast from "your area" to the real place.
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { if (::radioHost.isInitialized) radioHost.refreshLocation() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PoplurPerformanceManager.getInstance(this)
        window.setBackgroundDrawable(ColorDrawable(AndroidColor.BLACK))
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        radioHost = WearRadioHost(applicationContext)
        locationPermissionRequest.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
        setContent {
            val context = LocalContext.current
            val store = remember { WatchRadiusStore(context.applicationContext) }
            val performanceManager = remember { PoplurPerformanceManager.getInstance(context) }
            val performance by performanceManager.state.collectAsStateWithLifecycle()
            DisposableEffect(store) {
                store.start()
                onDispose { store.stop() }
            }
            RadiusWatchScreen(store = store, radioHost = radioHost, performance = performance)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::radioHost.isInitialized) radioHost.start()
    }

    override fun onPause() {
        super.onPause()
        if (::radioHost.isInitialized) radioHost.stop()
    }

    override fun onDestroy() {
        if (::radioHost.isInitialized) radioHost.release()
        super.onDestroy()
    }
}

// Hosts the libGDX clay-globe fragment inside Compose over a transparent surface.
@Composable
private fun GlobeFragmentHost(modifier: Modifier = Modifier) {
    val activity = LocalContext.current as FragmentActivity
    val containerId = remember { View.generateViewId() }
    AndroidView(
        modifier = modifier,
        factory = { ctx -> FragmentContainerView(ctx).apply { id = containerId } },
        update = {
            val fm = activity.supportFragmentManager
            if (fm.findFragmentById(containerId) == null && !fm.isStateSaved) {
                fm.beginTransaction()
                    .replace(containerId, GlobeGdxFragment())
                    .commitAllowingStateLoss()
            }
        }
    )
    DisposableEffect(activity, containerId) {
        onDispose {
            val fragment = activity.supportFragmentManager.findFragmentById(containerId)
            if (fragment != null) {
                activity.supportFragmentManager.beginTransaction()
                    .remove(fragment)
                    .commitAllowingStateLoss()
            }
        }
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

private const val WATCH_EARTH_RADIUS_KM = 6_371.0
private const val WATCH_EARTH_RADIUS_MI = 3_959.0

private fun readBatteryPercent(context: Context): Int {
    val intent: Intent = context.registerReceiver(
        null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    ) ?: return 0
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    if (level < 0 || scale <= 0) return 0
    return ((level / scale.toFloat()) * 100f).toInt().coerceIn(0, 100)
}

// ── Clay palette (phone welcome / loading aesthetic, blue end) ─────────────────

private val ClaySlab   = Color(0.122f, 0.420f, 1.0f)   // #1F6BFF
private val ClaySlabHi = Color(0.184f, 0.482f, 1.0f)   // #2F7BFF
private val ClayDot    = Color(0.839f, 0.063f, 0.424f, 0.34f)
private val ClayGreen  = Color(0.541f, 1.0f, 0.651f)
private val ClayRed    = Color(1.0f, 0.353f, 0.353f)
private val ClayInk    = Color(0.92f, 0.95f, 1.0f)
private val ConfettiColors = listOf(
    Color(1.00f, 0.176f, 0.333f),
    Color(1.00f, 0.584f, 0.00f),
    Color(1.00f, 0.800f, 0.00f),
    Color(0.204f, 0.780f, 0.349f),
    Color(0.00f, 0.478f, 1.00f),
    Color(0.686f, 0.322f, 0.871f)
)
private val ClayPink = listOf(
    Color(1.0f, 1.0f, 1.0f),
    Color(1.0f, 0.827f, 0.917f),
    Color(1.0f, 0.486f, 0.753f),
    Color(0.961f, 0.235f, 0.600f)
)

// ── Clay watch face ────────────────────────────────────────────────────────────

@Composable
private fun RadiusWatchScreen(
    store: WatchRadiusStore,
    radioHost: WearRadioHost,
    performance: PoplurPerformanceSnapshot,
) {
    var showKm by remember { mutableStateOf(true) }
    var showStatic by remember { mutableStateOf(false) }
    LaunchedEffect(performance.appInteractive) {
        if (!performance.appInteractive) return@LaunchedEffect
        while (true) {
            delay(4_000)
            showKm = !showKm
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // A quick tap toggles km/mi; a press held past the threshold drops the
                // face to TV static (picture + sound) until released.
                detectTapGestures(
                    onPress = {
                        val releasedQuickly = withTimeoutOrNull(300L) { tryAwaitRelease() }
                        if (releasedQuickly == null) {
                            showStatic = true
                            radioHost.startStatic()
                            tryAwaitRelease()
                            showStatic = false
                            radioHost.stopStatic()
                        }
                    },
                    onTap = { showKm = !showKm }
                )
            }
    ) {
        ClayBackground(
            performance = performance,
            modifier = Modifier.fillMaxSize(),
        )
        if (performance.particlesAllowed && performance.appInteractive && !showStatic) {
            ConfettiLayer(
                performance = performance,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            if (!showStatic) {
                GlobeFragmentHost(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.62f)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.62f)
                )
            }
            ClayReadout(
                store = store,
                showKm = showKm,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.38f)
            )
        }

        if (showStatic) {
            StaticOverlay(performance = performance, modifier = Modifier.fillMaxSize())
        }
    }
}

// ── TV static overlay (press-and-hold easter egg) ─────────────────────────────

@Composable
private fun StaticOverlay(
    performance: PoplurPerformanceSnapshot,
    modifier: Modifier = Modifier,
) {
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(performance.targetFps, performance.appInteractive) {
        if (!performance.appInteractive) return@LaunchedEffect
        while (true) {
            frame++
            delay(performance.frameIntervalMillis)
        }
    }
    Canvas(modifier = modifier) {
        val block = 5.dp.toPx()
        val cols = (size.width / block).toInt() + 1
        val rows = (size.height / block).toInt() + 1
        var s = (frame + 1).toULong() * 6364136223846793005uL xor 0x9E3779B97F4A7C15uL
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                s = s * 6364136223846793005uL + 1442695040888963407uL
                val v = ((s shr 33).toDouble() / (1L shl 31).toDouble()).toFloat().coerceIn(0f, 1f)
                drawRect(
                    color = Color(v, v, v, 0.92f),
                    topLeft = Offset(c * block, r * block),
                    size = Size(block, block)
                )
            }
        }
    }
}

// ── Blue clay-slab dot background ──────────────────────────────────────────────

@Composable
private fun ClayBackground(
    performance: PoplurPerformanceSnapshot,
    modifier: Modifier = Modifier,
) {
    var timeMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(
        performance.targetFps,
        performance.appInteractive,
        performance.decorativeRenderingAllowed,
    ) {
        if (!performance.appInteractive || !performance.decorativeRenderingAllowed) {
            timeMs = 0L
            return@LaunchedEffect
        }
        val start = SystemClock.uptimeMillis()
        while (true) {
            timeMs = SystemClock.uptimeMillis() - start
            delay(performance.frameIntervalMillis)
        }
    }
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(color = ClaySlab, size = size)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(ClaySlabHi.copy(alpha = 0.55f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.34f),
                radius = h * 0.55f
            ),
            size = size
        )
        val t = timeMs / 1000f
        val step = 9.dp.toPx()
        val baseR = 1.5.dp.toPx()
        var gy = 0
        var y = step / 2f
        while (y < h) {
            var gx = 0
            var x = step / 2f
            while (x < w) {
                val pulse = 0.5f + 0.5f * sin(t * 2f + (gx * 0.06f + gy * 0.05f))
                drawCircle(
                    color = ClayDot.copy(alpha = 0.16f + 0.24f * pulse),
                    radius = baseR * (0.7f + 0.5f * pulse),
                    center = Offset(x, y)
                )
                x += step; gx++
            }
            y += step; gy++
        }
    }
}

// ── Confetti (falling clay-color chips, phone auth-confetti feel) ──────────────

private class ConfettiChip(
    val x0: Float, val delay: Float, val dur: Float,
    val drift: Float, val size: Float, val color: Color
)

@Composable
private fun ConfettiLayer(
    performance: PoplurPerformanceSnapshot,
    modifier: Modifier = Modifier,
) {
    val chips = remember {
        var seed = 0x9E3779B97F4A7C15uL
        fun rnd(): Float {
            seed = seed * 6364136223846793005uL + 1442695040888963407uL
            // `seed shr 33` tops out at 2^31, so dividing by UInt.MAX_VALUE only ever
            // reached ~0.5 — which pinned every chip to the left half of the face.
            return ((seed shr 33).toDouble() / (1L shl 31).toDouble()).toFloat()
        }
        List(30) {
            ConfettiChip(
                x0 = rnd(),
                delay = rnd() * 3.2f,
                dur = 3.0f + rnd() * 2.2f,
                drift = (rnd() - 0.5f) * 0.28f,
                size = 6f + rnd() * 5f,
                color = ConfettiColors[(rnd() * ConfettiColors.size).toInt() % ConfettiColors.size]
            )
        }
    }
    var timeMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(performance.targetFps, performance.appInteractive) {
        if (!performance.appInteractive) return@LaunchedEffect
        val start = SystemClock.uptimeMillis()
        while (true) {
            timeMs = SystemClock.uptimeMillis() - start
            delay(performance.frameIntervalMillis)
        }
    }
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val t = timeMs / 1000f
        val activeChipCount = (chips.size * performance.particleDensityMultiplier)
            .toInt()
            .coerceIn(0, chips.size)
        for (index in 0 until activeChipCount) {
            val c = chips[index]
            val phase = ((t + c.delay) % c.dur) / c.dur
            val y = phase * (h + 30f) - 15f
            val x = c.x0 * w + sin(phase * 2f * Math.PI.toFloat()) * c.drift * w
            val alpha = if (phase < 0.85f) 1f else (1f - (phase - 0.85f) / 0.15f)
            val cw = c.size.dp.toPx()
            drawRect(
                color = c.color.copy(alpha = alpha.coerceIn(0f, 1f)),
                topLeft = Offset(x - cw / 2f, y - cw * 0.9f),
                size = Size(cw, cw * 1.8f)
            )
        }
    }
}

// ── Clay-pink radius / posts readout ───────────────────────────────────────────

@Composable
private fun ClayReadout(
    store: WatchRadiusStore,
    showKm: Boolean,
    modifier: Modifier = Modifier
) {
    val pink = Brush.verticalGradient(ClayPink)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "R A D I U S",
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Black,
            fontSize = 9.sp,
            color = Color(0.87f, 0.93f, 1.0f)
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = store.displayValue(showKm),
                fontWeight = FontWeight.Black,
                fontSize = 30.sp,
                style = TextStyle(brush = pink)
            )
            Text(
                text = if (showKm) "km" else "mi",
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                style = TextStyle(brush = pink),
                modifier = Modifier
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val (glyph, tint) = when (store.trend) {
                RadiusTrend.Up     -> "▲" to ClayGreen
                RadiusTrend.Down   -> "▼" to ClayRed
                RadiusTrend.Stable -> "–" to ClayGreen
            }
            Text(text = glyph, fontWeight = FontWeight.Black, fontSize = 9.sp, color = tint)
            Text(
                text = "${store.postCount} posts",
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                color = ClayInk,
                textAlign = TextAlign.Center
            )
        }
    }
}
