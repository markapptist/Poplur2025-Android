package com.apptists.poplur2025.wear

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.BatteryManager
import android.os.Bundle
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(AndroidColor.BLACK))
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
        val value = if (metric) radiusMeters / 1_000.0 else radiusMeters / 1_609.344
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val w = maxWidth
        val h = maxHeight

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ──────────────────────────────────────────────────
            LCDHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(h * 0.115f)
            )

            // ── Globe + data ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(h * 0.635f)
            ) {
                // Globe (54%)
                GlobeTrendView(
                    trend = store.trend,
                    globePhase = globePhase,
                    arrowPhase = arrowPhase,
                    modifier = Modifier
                        .width(w * 0.54f)
                        .height(h * 0.635f)
                )
                // Data column (46%)
                DataColumn(
                    store = store,
                    showKm = showKm,
                    modifier = Modifier
                        .width(w * 0.46f)
                        .height(h * 0.635f)
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
                    .height(h * 0.115f)
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
                    .height(h * 0.105f)
            )
        }
    }
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
            fontSize = 10.sp,
            color = LcdOrange,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "WATER RESIST",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = 7.sp,
            color = LcdBlue,
            maxLines = 1,
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
private fun GlobeTrendView(
    trend: RadiusTrend,
    globePhase: Float,
    arrowPhase: Float,
    modifier: Modifier = Modifier
) {
    val ocean = Color(0.06f, 0.28f, 0.68f)
    val land  = Color(0.20f, 0.52f, 0.18f)
    val ice   = Color(0.88f, 0.93f, 1.00f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val globeBottom = h * 0.76f
        val r = min(w * 0.42f, globeBottom * 0.48f)
        val cx = w * 0.50f
        val cy = globeBottom - r

        // ── Ocean fill ───────────────────────────────────────────────────
        drawCircle(color = ocean, radius = r, center = Offset(cx, cy))

        // ── Continents: project each blob onto the visible hemisphere ────
        val lonOffset = globePhase * 2f * Math.PI.toFloat()

        for (blob in continentBlobs) {
            val lonC    = blob[0]; val latC    = blob[1]
            val lonSpan = blob[2]; val latSpan = blob[3]
            val effectiveLon = lonC + lonOffset
            // Visibility: cos of longitude angle from center (0 = center, ±π/2 = edge)
            val cosLon = cos(effectiveLon)
            if (cosLon < -0.1f) continue  // back hemisphere

            // Project center onto sphere surface
            val screenX = cx + r * cosLon * cos(latC)
            val screenY = cy - r * sin(latC)

            // Scale projected ellipse by depth (cosLon) and latitude compression
            val projW = r * lonSpan * cosLon
            val projH = r * latSpan
            if (projW < 1f) continue

            // Shade land darker toward edges (simple depth shading)
            val brightness = 0.55f + 0.45f * cosLon
            drawOval(
                color = land.copy(
                    red   = land.red   * brightness,
                    green = land.green * brightness,
                    blue  = land.blue  * brightness,
                    alpha = 1f
                ),
                topLeft = Offset(screenX - projW, screenY - projH),
                size    = Size(projW * 2f, projH * 2f)
            )
        }

        // ── Ice caps ─────────────────────────────────────────────────────
        // Arctic cap (top)
        val iceLatN = 1.22f  // ~70° N
        val iceYN = cy - r * sin(iceLatN)
        val iceRN = r * cos(iceLatN)
        if (iceRN > 1f) drawCircle(color = ice, radius = iceRN, center = Offset(cx, iceYN))
        // Antarctic cap (bottom)
        val iceLatS = -1.22f
        val iceYS = cy - r * sin(iceLatS)
        val iceRS = r * cos(-iceLatS)
        if (iceRS > 1f) drawCircle(color = ice, radius = iceRS, center = Offset(cx, iceYS))

        // ── Globe outline ────────────────────────────────────────────────
        drawCircle(
            color  = Color.White.copy(alpha = 0.25f),
            radius = r,
            center = Offset(cx, cy),
            style  = Stroke(width = 1.2f)
        )

        // ── Specular highlight (top-left glint) ──────────────────────────
        drawCircle(
            color  = Color.White.copy(alpha = 0.18f),
            radius = r * 0.38f,
            center = Offset(cx - r * 0.28f, cy - r * 0.30f)
        )

        // ── Trend indicator ───────────────────────────────────────────────
        val arrowCY = globeBottom + (h - globeBottom) * 0.40f
        val arrowW  = w * 0.28f
        val arrowH2 = (h - globeBottom) * 0.30f
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
                    color       = LcdGreen.copy(alpha = 0.75f),
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
    val denominator = if (showKm) "/6371" else "/3959"
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
            fontSize = 7.sp,
            color = LcdBlue,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = timeFmt.format(Date()),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = LcdGreen,
            maxLines = 1,
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
            fontSize = 7.sp,
            color = LcdBlue,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
        )

        // Radius value
        Text(
            text = radius,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = LcdGreen,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )

        // Post count
        Text(
            text = "${store.postCount}",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = LcdGreen.copy(alpha = 0.75f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )

        // Earth denominator
        Text(
            text = denominator,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            color = LcdGreen.copy(alpha = 0.85f),
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )

        // Unit
        Text(
            text = unit,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = 7.sp,
            color = LcdGreen.copy(alpha = 0.70f),
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
        TabItem(label = "SCH", weight = 1f)
        Canvas(modifier = Modifier.width(0.5.dp).height(20.dp)) {
            drawLine(LcdDivider, Offset(0f, 0f), Offset(0f, size.height))
        }
        TabItem(label = "WORLD TIME", weight = 1f)
        Canvas(modifier = Modifier.width(0.5.dp).height(20.dp)) {
            drawLine(LcdDivider, Offset(0f, 0f), Offset(0f, size.height))
        }
        TabItem(label = "TEL", weight = 1f)
    }
}

@Composable
private fun TabItem(label: String, weight: Float) {
    Text(
        text = label,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 7.sp,
        color = LcdGreen,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(weight)
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
        fontSize = 6.sp,
        color = LcdGreen.copy(alpha = 0.80f),
        maxLines = 1,
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
