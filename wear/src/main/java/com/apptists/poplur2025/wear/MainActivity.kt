package com.apptists.poplur2025.wear

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
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

private enum class WatchFaceAsset(
    val usesAtlas: Boolean,
    val srcX: Int,
    val srcY: Int,
    val srcW: Int,
    val srcH: Int,
    val accent: Color
) {
    Closeup(false, 0, 0, 1136, 1385, Color(0xFFEAEAEA)),
    Silver(true, 0, 295, 342, 430, Color(0xFFEAEAEA)),
    Gold(true, 341, 295, 342, 430, Color(0xFFFFC21C)),
    Rose(true, 682, 295, 342, 430, Color(0xFFFF9078)),
    Sapphire(true, 0, 705, 342, 430, Color(0xFF35AFFF)),
    Ruby(true, 341, 705, 342, 430, Color(0xFFFF3030)),
    Frost(true, 682, 705, 342, 430, Color(0xFFD9F4FF))
}

private enum class RadiusTrend {
    Up,
    Down,
    Stable
}

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

    init {
        recomputeTrend()
    }

    fun start() {
        dataClient.addListener(this)
        dataClient.dataItems
            .addOnSuccessListener { items ->
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

    fun stop() {
        dataClient.removeListener(this)
    }

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
                delta > threshold -> RadiusTrend.Up
                delta < -threshold -> RadiusTrend.Down
                else -> RadiusTrend.Stable
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

@Composable
private fun RadiusWatchScreen(store: WatchRadiusStore) {
    val context = LocalContext.current
    val atlas = remember(context) {
        BitmapFactory.decodeResource(context.resources, R.drawable.poplur_watch_faces_atlas).asImageBitmap()
    }
    val closeup = remember(context) {
        BitmapFactory.decodeResource(context.resources, R.drawable.poplur_watch_face_closeup).asImageBitmap()
    }
    var face by remember { mutableStateOf(WatchFaceAsset.Closeup) }
    val transition = rememberInfiniteTransition(label = "watch-face")
    val shinePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4_800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "watch-shine"
    )

    fun browse(direction: Int) {
        val all = WatchFaceAsset.entries
        face = all[(all.indexOf(face) + direction + all.size) % all.size]
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(2.dp)
    ) {
        DirectWatchFace(
            face = face,
            atlas = atlas,
            closeup = closeup,
            shinePhase = shinePhase,
            modifier = Modifier.fillMaxSize()
        )
        WatchBrowseArrow(
            isLeft = true,
            color = face.accent,
            modifier = Modifier.align(Alignment.CenterStart)
        ) { browse(-1) }
        WatchBrowseArrow(
            isLeft = false,
            color = face.accent,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) { browse(1) }
    }
}

@Composable
private fun DirectWatchFace(
    face: WatchFaceAsset,
    atlas: ImageBitmap,
    closeup: ImageBitmap,
    shinePhase: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        drawRect(Color.Black)

        val bitmap = if (face.usesAtlas) atlas else closeup
        val srcAspect = face.srcW.toFloat() / face.srcH.toFloat()
        val maxW = size.width * 0.86f
        val maxH = size.height * 0.96f
        var dstW = maxW
        var dstH = dstW / srcAspect
        if (dstH > maxH) {
            dstH = maxH
            dstW = dstH * srcAspect
        }
        val left = ((size.width - dstW) / 2f).roundToInt()
        val top = ((size.height - dstH) / 2f).roundToInt()

        drawImage(
            image = bitmap,
            srcOffset = IntOffset(face.srcX, face.srcY),
            srcSize = IntSize(face.srcW, face.srcH),
            dstOffset = IntOffset(left, top),
            dstSize = IntSize(dstW.roundToInt(), dstH.roundToInt())
        )

        val sweep = shinePhase * 2.25f - 0.55f
        val sx = size.width * sweep
        drawRect(
            brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    0.42f to Color.White.copy(alpha = 0.00f),
                    0.50f to Color.White.copy(alpha = 0.38f),
                    0.58f to face.accent.copy(alpha = 0.16f),
                    1f to Color.Transparent
                ),
                start = Offset(sx - size.width * 0.34f, size.height * 1.08f),
                end = Offset(sx + size.width * 0.34f, -size.height * 0.08f)
            ),
            blendMode = BlendMode.Screen
        )
    }
}

@Composable
private fun WatchBrowseArrow(
    isLeft: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Canvas(
        modifier = modifier
            .size(width = 26.dp, height = 50.dp)
            .pointerInput(onClick) {
                detectTapGestures(onTap = { onClick() })
            }
    ) {
        val path = Path()
        if (isLeft) {
            path.moveTo(size.width * 0.72f, size.height * 0.12f)
            path.lineTo(size.width * 0.24f, size.height * 0.50f)
            path.lineTo(size.width * 0.72f, size.height * 0.88f)
        } else {
            path.moveTo(size.width * 0.28f, size.height * 0.12f)
            path.lineTo(size.width * 0.76f, size.height * 0.50f)
            path.lineTo(size.width * 0.28f, size.height * 0.88f)
        }
        drawPath(path, color.copy(alpha = 0.86f), style = Stroke(width = 4.dp.toPx()))
    }
}

private fun readBatteryPercent(context: Context): Int {
    val intent: Intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return 0
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    if (level < 0 || scale <= 0) return 0
    return ((level / scale.toFloat()) * 100f).toInt().coerceIn(0, 100)
}
