package com.apptists.poplur2025.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

data class CubeMenuWheelItem(
    val label: String,
    val icon: ImageVector,
    val accent: Color,
    val onSelected: () -> Unit
)

@Stable
class CubeMenuWheelController(
    private val scope: CoroutineScope,
    private val onTick: () -> Unit
) {
    var items by mutableStateOf<List<CubeMenuWheelItem>>(emptyList())
        private set
    var selectedIndex by mutableIntStateOf(0)
        private set
    var isVisible by mutableStateOf(false)
        private set

    val selectedItem: CubeMenuWheelItem?
        get() = items.getOrNull(selectedIndex)

    private var cycleJob: Job? = null
    private val tickMillis = 620L

    fun beginHold(nextItems: List<CubeMenuWheelItem>) {
        if (nextItems.isEmpty()) return
        cycleJob?.cancel()
        items = nextItems
        selectedIndex = 0
        isVisible = true
        onTick()
        cycleJob = scope.launch {
            while (isActive) {
                delay(tickMillis)
                advance()
            }
        }
    }

    fun endHold(activateSelected: Boolean) {
        val selectedAction = if (activateSelected && isVisible) {
            items.getOrNull(selectedIndex)?.onSelected
        } else {
            null
        }
        cycleJob?.cancel()
        cycleJob = null
        isVisible = false
        selectedAction?.invoke()
    }

    private fun advance() {
        if (items.isEmpty()) return
        selectedIndex = (selectedIndex + 1) % items.size
        onTick()
    }
}

@Composable
fun BoxScope.CubeMenuWheelOverlay(
    controller: CubeMenuWheelController,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = controller.isVisible,
        enter = fadeIn(tween(140)),
        exit = fadeOut(tween(180)),
        modifier = modifier
            .fillMaxSize()
            .align(Alignment.Center)
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.58f))
            )
            AnimatedVisibility(
                visible = controller.isVisible,
                enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.72f, animationSpec = spring(dampingRatio = 0.62f)),
                exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.78f, animationSpec = tween(140)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 226.dp)
            ) {
                CubeMenuWheel(
                    items = controller.items,
                    selectedIndex = controller.selectedIndex
                )
            }
        }
    }
}

@Composable
private fun CubeMenuWheel(
    items: List<CubeMenuWheelItem>,
    selectedIndex: Int
) {
    BoxWithConstraints(
        modifier = Modifier
            .width(390.dp)
            .height(205.dp)
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val itemSize = 82.dp
        val itemSizePx = with(density) { itemSize.toPx() }
        val centerSlot = items.size / 2

        Canvas(Modifier.matchParentSize()) {
            val arcWidth = min(size.width * 0.92f, size.height * 1.92f)
            val arcHeight = arcWidth * 0.72f
            val topLeft = Offset((size.width - arcWidth) / 2f, size.height * 0.36f)
            val arcSize = Size(arcWidth, arcHeight)
            val colors = listOf(
                Color(0xFF4DFF58),
                Color(0xFF26D9F6),
                Color(0xFF7076FF),
                Color(0xFFFF8C1A),
                Color(0xFF4DFF58)
            )
            val sweep = 120f / colors.size
            colors.forEachIndexed { index, color ->
                drawArc(
                    color = color,
                    startAngle = 210f + index * sweep,
                    sweepAngle = sweep + 2f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        items.forEachIndexed { index, item ->
            val slot = ((index - selectedIndex + centerSlot) % items.size + items.size) % items.size
            val angleDegrees = 210f + (slot.toFloat() / (items.size - 1).coerceAtLeast(1).toFloat()) * 120f
            val angle = angleDegrees * PI.toFloat() / 180f
            val radius = widthPx * 0.42f
            val centerX = widthPx / 2f
            val centerY = heightPx * 1.04f
            val x = centerX + cos(angle) * radius
            val y = centerY + sin(angle) * radius
            val selected = index == selectedIndex

            CubeMenuWheelButton(
                item = item,
                selected = selected,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (x - itemSizePx / 2f).roundToInt(),
                            (y - itemSizePx / 2f).roundToInt()
                        )
                    }
                    .size(itemSize)
            )
        }
    }
}

@Composable
private fun CubeMenuWheelButton(
    item: CubeMenuWheelItem,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.18f else 0.94f,
        animationSpec = spring(dampingRatio = 0.55f),
        label = "cube-menu-wheel-item-scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.82f,
        animationSpec = tween(140),
        label = "cube-menu-wheel-item-alpha"
    )

    Box(modifier.scale(scale), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = Color(0xF208090D),
            shadowElevation = if (selected) 14.dp else 6.dp,
            border = androidx.compose.foundation.BorderStroke(
                width = if (selected) 4.dp else 3.dp,
                color = item.accent.copy(alpha = alpha)
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    item.icon,
                    contentDescription = item.label,
                    tint = Color.White,
                    modifier = Modifier.size(if (selected) 38.dp else 32.dp)
                )
            }
        }
        if (selected) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 26.dp),
                shape = RoundedCornerShape(999.dp),
                color = item.accent
            ) {
                Text(
                    item.label,
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
