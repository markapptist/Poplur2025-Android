package com.apptists.poplur2025.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class CubeMenuLayout {
    Horizontal,
    Vertical
}

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
    var tickCount by mutableIntStateOf(0)
        private set
    var isVisible by mutableStateOf(false)
        private set
    var isClosing by mutableStateOf(false)
        private set

    val selectedItem: CubeMenuWheelItem?
        get() = items.getOrNull(selectedIndex)

    val isOnScreen: Boolean
        get() = isVisible || isClosing

    private var cycleJob: Job? = null
    private var closeJob: Job? = null
    private val tickMillis = 620L

    fun beginHold(nextItems: List<CubeMenuWheelItem>) {
        if (nextItems.isEmpty()) return
        cycleJob?.cancel()
        closeJob?.cancel()
        items = nextItems
        selectedIndex = 0
        tickCount = 0
        isClosing = false
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
        closeJob?.cancel()
        isVisible = false
        isClosing = true
        closeJob = scope.launch {
            delay(180)
            isClosing = false
            selectedAction?.invoke()
        }
    }

    private fun advance() {
        if (items.isEmpty()) return
        selectedIndex = (selectedIndex + 1) % items.size
        tickCount += 1
        onTick()
    }
}

@Composable
fun BoxScope.CubeMenuWheelOverlay(
    controller: CubeMenuWheelController,
    modifier: Modifier = Modifier,
    layout: CubeMenuLayout = CubeMenuLayout.Horizontal
) {
    val menuProgress by animateFloatAsState(
        targetValue = if (controller.isVisible) 1f else 0f,
        animationSpec = if (controller.isVisible) {
            spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessMediumLow)
        } else {
            tween(durationMillis = 200)
        },
        label = "cube-menu-progress"
    )

    AnimatedVisibility(
        visible = controller.isOnScreen,
        enter = fadeIn(tween(140)),
        exit = fadeOut(tween(180)),
        modifier = modifier
            .fillMaxSize()
            .align(Alignment.Center)
    ) {
        CubeNeonMenu(
            items = controller.items,
            selectedIndex = controller.selectedIndex,
            tickCount = controller.tickCount,
            layout = layout,
            openProgress = menuProgress,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun CubeNeonMenu(
    items: List<CubeMenuWheelItem>,
    selectedIndex: Int,
    tickCount: Int,
    layout: CubeMenuLayout,
    openProgress: Float,
    modifier: Modifier = Modifier
) {
    val visibleItems = visibleMenuItems(items, selectedIndex)
    val panelShape = RoundedCornerShape(if (layout == CubeMenuLayout.Horizontal) 34.dp else 28.dp)

    BoxWithConstraints(modifier) {
        val horizontal = layout == CubeMenuLayout.Horizontal
        val panelWidth = if (horizontal) minDp(maxWidth - 28.dp, 560.dp) else 176.dp
        val panelHeight = if (horizontal) 146.dp else minDp(maxHeight - 120.dp, 620.dp)
        val centerX = maxWidth / 2f
        val centerY = if (horizontal) {
            maxHeight - clampDp(maxHeight * 0.24f, 320.dp, 380.dp)
        } else {
            maxHeight / 2f
        }
        val horizontalSpacing = clampDp(maxWidth * 0.23f, 86.dp, 112.dp)
        val verticalSpacing = clampDp(maxHeight * 0.11f, 92.dp, 118.dp)
        val panelScale = 0.78f + openProgress * 0.22f

        Surface(
            modifier = Modifier
                .offset(x = centerX - panelWidth / 2f, y = centerY - panelHeight / 2f)
                .size(width = panelWidth, height = panelHeight)
                .graphicsLayer {
                    alpha = openProgress
                    scaleX = panelScale
                    scaleY = panelScale
                    shadowElevation = 24.dp.toPx() * openProgress
                    shape = panelShape
                    clip = false
                },
            shape = panelShape,
            color = Color.Transparent,
            shadowElevation = 24.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.92f),
                                Color(0xFF110A29),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        ),
                        panelShape
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0x993E31FF),
                                Color(0xBDFF1AF2),
                                Color(0x944F35FF)
                            )
                        ),
                        shape = panelShape
                    )
            )
        }

        visibleItems.forEach { visibleItem ->
            val selected = visibleItem.index == selectedIndex
            val itemWidth = cubeMenuTileWidth(selected, layout)
            val itemHeight = cubeMenuTileHeight(selected, layout)
            val slotOffset = if (horizontal) horizontalSpacing * visibleItem.relativeSlot.toFloat() else verticalSpacing * visibleItem.relativeSlot.toFloat()
            val itemX = if (horizontal) centerX + slotOffset - itemWidth / 2f else centerX - itemWidth / 2f
            val itemY = if (horizontal) centerY - itemHeight / 2f else centerY + slotOffset - itemHeight / 2f
            val distance = abs(visibleItem.relativeSlot)
            val slotAlpha = when {
                distance > 2 -> 0f
                distance == 2 -> 0.72f
                else -> 1f
            }

            Box(
                modifier = Modifier
                    .offset(x = itemX, y = itemY)
                    .size(width = itemWidth, height = itemHeight)
                    .graphicsLayer {
                        alpha = slotAlpha * openProgress
                        val tileScale = 0.82f + openProgress * 0.18f
                        scaleX = tileScale
                        scaleY = tileScale
                    },
                contentAlignment = Alignment.Center
            ) {
                CubeNeonMenuTile(
                    item = visibleItem.item,
                    relativeSlot = visibleItem.relativeSlot,
                    selected = selected,
                    tickCount = tickCount,
                    layout = layout
                )
            }
        }
    }
}

@Composable
private fun CubeNeonMenuTile(
    item: CubeMenuWheelItem,
    relativeSlot: Int,
    selected: Boolean,
    tickCount: Int,
    layout: CubeMenuLayout
) {
    val density = LocalDensity.current
    val impulse = androidx.compose.runtime.remember { Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(tickCount, selected) {
        if (selected) {
            impulse.snapTo(1f)
            impulse.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }
    val baseScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cube-neon-menu-tile-scale"
    )
    val width = cubeMenuTileWidth(selected, layout)
    val height = cubeMenuTileHeight(selected, layout)
    val shape = RoundedCornerShape(if (selected) 24.dp else 18.dp)
    val direction = when {
        relativeSlot > 0 -> 1f
        relativeSlot < 0 -> -1f
        else -> 0f
    }
    val idleRotation = (abs(relativeSlot) * 4.5f).coerceAtMost(9f) * direction

    Surface(
        modifier = Modifier
            .size(width = width, height = height)
            .graphicsLayer {
                val impulsePx = with(density) { 9.dp.toPx() * impulse.value }
                val slotLagPx = with(density) { (relativeSlot * abs(relativeSlot) * 1.4f).dp.toPx() }

                rotationZ = if (selected) {
                    -1.2f * impulse.value
                } else if (layout == CubeMenuLayout.Horizontal) {
                    idleRotation
                } else {
                    -idleRotation
                }
                translationY = if (layout == CubeMenuLayout.Horizontal) slotLagPx - impulsePx else 0f
                translationX = if (layout == CubeMenuLayout.Vertical) slotLagPx - impulsePx else 0f
                scaleX = baseScale * (1f + impulse.value * 0.08f)
                scaleY = baseScale * (1f - impulse.value * 0.06f)
            },
        shape = shape,
        color = Color.Transparent,
        shadowElevation = if (selected) 24.dp else 12.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xF538084F),
                            Color(0xFA05050F)
                        )
                    ),
                    shape
                )
                .border(
                    width = if (selected) 3.5.dp else 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF7052FF),
                            Color(0xFFFF1EF2),
                            Color(0xFFBC75FF)
                        )
                    ),
                    shape = shape
                )
                .padding(if (selected) 12.dp else 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(6.dp)
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = if (selected) 0.26f else 0.18f),
                        shape = RoundedCornerShape(if (selected) 18.dp else 13.dp)
                    )
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    item.icon,
                    contentDescription = item.label,
                    tint = Color.White,
                    modifier = Modifier.size(if (selected) 38.dp else 28.dp)
                )
                Text(
                    item.label,
                    color = Color.White,
                    fontSize = if (selected) 17.sp else 13.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = if (selected) 8.dp else 5.dp)
                )
            }
        }
    }
}

private data class VisibleCubeMenuItem(
    val index: Int,
    val item: CubeMenuWheelItem,
    val relativeSlot: Int
)

private fun visibleMenuItems(
    items: List<CubeMenuWheelItem>,
    selectedIndex: Int
): List<VisibleCubeMenuItem> {
    if (items.isEmpty()) return emptyList()

    val centerSlot = (items.size / 2).coerceAtLeast(0)
    return items
        .mapIndexedNotNull { index, item ->
            val slotIndex = (index - selectedIndex + centerSlot + items.size * 4) % items.size
            val relativeSlot = slotIndex - centerSlot
            if (abs(relativeSlot) <= 2) {
                VisibleCubeMenuItem(index = index, item = item, relativeSlot = relativeSlot)
            } else {
                null
            }
        }
        .sortedBy { it.relativeSlot }
}

private fun cubeMenuTileWidth(selected: Boolean, layout: CubeMenuLayout): Dp = when {
    selected && layout == CubeMenuLayout.Vertical -> 152.dp
    selected -> 118.dp
    layout == CubeMenuLayout.Vertical -> 112.dp
    else -> 88.dp
}

private fun cubeMenuTileHeight(selected: Boolean, layout: CubeMenuLayout): Dp = when {
    selected && layout == CubeMenuLayout.Vertical -> 116.dp
    selected -> 118.dp
    layout == CubeMenuLayout.Vertical -> 96.dp
    else -> 88.dp
}

private fun minDp(a: Dp, b: Dp): Dp = if (a < b) a else b

private fun clampDp(value: Dp, minimum: Dp, maximum: Dp): Dp {
    return value.coerceAtLeast(minimum).coerceAtMost(maximum)
}
