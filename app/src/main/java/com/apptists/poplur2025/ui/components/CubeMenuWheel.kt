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
import com.apptists.poplur2025.ui.components.PoplurText
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
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.apptists.poplur2025.ui.CubeSkin
import com.apptists.poplur2025.ui.holdMenuAssetPath
import com.apptists.poplur2025.ui.usePerItemTint
import com.apptists.poplur2025.util.PoplurEQSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class CubeMenuLayout {
    Horizontal,
    Vertical
}

data class CubeMenuWheelItem(
    val label: String,
    val icon: ImageVector,
    val secondaryIcon: ImageVector? = null,
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

    private var closeJob: Job? = null
    private var scrubRemainder = 0f
    private val horizontalScrubStepPx = 96f
    private val verticalScrubStepPx = 108f
    private val maxStepsPerScrubUpdate = 4

    fun beginHold(nextItems: List<CubeMenuWheelItem>) {
        if (nextItems.isEmpty()) return
        closeJob?.cancel()
        items = nextItems
        selectedIndex = 0
        tickCount = 0
        scrubRemainder = 0f
        isClosing = false
        isVisible = true
        PoplurEQSync.begin(PoplurEQSync.Lane.HoldMenu)
    }

    fun endHold(activateSelected: Boolean) {
        val selectedAction = if (activateSelected && isVisible) {
            items.getOrNull(selectedIndex)?.onSelected
        } else {
            null
        }
        closeJob?.cancel()
        isVisible = false
        isClosing = true
        closeJob = scope.launch {
            delay(180)
            isClosing = false
            PoplurEQSync.end(PoplurEQSync.Lane.HoldMenu)
            selectedAction?.invoke()
        }
    }

    fun scrub(deltaX: Float, deltaY: Float, layout: CubeMenuLayout) {
        if (!isVisible || items.size <= 1) return
        val primaryDelta = if (layout == CubeMenuLayout.Horizontal) deltaX else deltaY
        if (abs(primaryDelta) <= 0.25f) return

        val stepDistance = if (layout == CubeMenuLayout.Horizontal) horizontalScrubStepPx else verticalScrubStepPx
        scrubRemainder += primaryDelta

        var steps = 0
        while (scrubRemainder <= -stepDistance && steps < maxStepsPerScrubUpdate) {
            scrubRemainder += stepDistance
            moveSelection(1)
            steps += 1
        }
        while (scrubRemainder >= stepDistance && steps < maxStepsPerScrubUpdate) {
            scrubRemainder -= stepDistance
            moveSelection(-1)
            steps += 1
        }
        if (steps == maxStepsPerScrubUpdate && abs(scrubRemainder) > stepDistance) {
            scrubRemainder = stepDistance * if (scrubRemainder < 0f) -1f else 1f
        }
    }

    private fun moveSelection(offset: Int) {
        if (items.isEmpty() || offset == 0) return
        val count = items.size
        selectedIndex = (selectedIndex + offset + count) % count
        tickCount += 1
        onTick()
    }
}

// ── Skin-derived menu colors ──────────────────────────────────────────────────

private data class MenuSkinColors(
    val panelMidTint: Color,
    val borderDim: Color,
    val borderBright: Color,
    val tileBgAccent: Color,
    val tileBorderA: Color,
    val tileBorderB: Color,
    val tileBorderC: Color
)

private fun menuSkinColors(skin: CubeSkin): MenuSkinColors = when (skin) {
    CubeSkin.Gold -> MenuSkinColors(
        panelMidTint = Color(0.18f, 0.12f, 0.02f, 1f),
        borderDim    = Color(0.50f, 0.35f, 0.05f, 0.60f),
        borderBright = Color(0.95f, 0.78f, 0.18f, 0.82f),
        tileBgAccent = Color(0.22f, 0.14f, 0.03f, 0.96f),
        tileBorderA  = Color(0.70f, 0.50f, 0.10f, 1f),
        tileBorderB  = Color(0.95f, 0.78f, 0.18f, 1f),
        tileBorderC  = Color(0.80f, 0.60f, 0.12f, 1f)
    )
    CubeSkin.Chocolate -> MenuSkinColors(
        panelMidTint = Color(0.20f, 0.10f, 0.03f, 1f),
        borderDim    = Color(0.50f, 0.28f, 0.06f, 0.60f),
        borderBright = Color(0.72f, 0.40f, 0.08f, 0.82f),
        tileBgAccent = Color(0.25f, 0.12f, 0.03f, 0.96f),
        tileBorderA  = Color(0.55f, 0.30f, 0.06f, 1f),
        tileBorderB  = Color(0.72f, 0.40f, 0.08f, 1f),
        tileBorderC  = Color(0.60f, 0.34f, 0.07f, 1f)
    )
    CubeSkin.Camo -> MenuSkinColors(
        panelMidTint = Color(0.10f, 0.16f, 0.05f, 1f),
        borderDim    = Color(0.28f, 0.40f, 0.12f, 0.60f),
        borderBright = Color(0.48f, 0.62f, 0.22f, 0.82f),
        tileBgAccent = Color(0.12f, 0.18f, 0.06f, 0.96f),
        tileBorderA  = Color(0.34f, 0.48f, 0.15f, 1f),
        tileBorderB  = Color(0.48f, 0.62f, 0.22f, 1f),
        tileBorderC  = Color(0.38f, 0.52f, 0.18f, 1f)
    )
    CubeSkin.Ice -> MenuSkinColors(
        panelMidTint = Color(0.06f, 0.14f, 0.28f, 1f),
        borderDim    = Color(0.40f, 0.62f, 0.80f, 0.60f),
        borderBright = Color(0.55f, 0.88f, 1.00f, 0.82f),
        tileBgAccent = Color(0.06f, 0.16f, 0.30f, 0.96f),
        tileBorderA  = Color(0.45f, 0.72f, 0.92f, 1f),
        tileBorderB  = Color(0.55f, 0.88f, 1.00f, 1f),
        tileBorderC  = Color(0.50f, 0.80f, 0.96f, 1f)
    )
    CubeSkin.Space -> MenuSkinColors(
        panelMidTint = Color(0.06f, 0.04f, 0.18f, 1f),
        borderDim    = Color(0.30f, 0.24f, 0.60f, 0.60f),
        borderBright = Color(0.55f, 0.42f, 1.00f, 0.82f),
        tileBgAccent = Color(0.08f, 0.05f, 0.22f, 0.96f),
        tileBorderA  = Color(0.40f, 0.30f, 0.80f, 1f),
        tileBorderB  = Color(0.55f, 0.42f, 1.00f, 1f),
        tileBorderC  = Color(0.46f, 0.36f, 0.88f, 1f)
    )
    CubeSkin.Carbon -> MenuSkinColors(  // Sparkle Pink
        panelMidTint = Color(0.25f, 0.08f, 0.18f, 1f),
        borderDim    = Color(0.70f, 0.20f, 0.50f, 0.60f),
        borderBright = Color(1.00f, 0.35f, 0.72f, 0.82f),
        tileBgAccent = Color(0.28f, 0.08f, 0.18f, 0.96f),
        tileBorderA  = Color(0.80f, 0.25f, 0.60f, 1f),
        tileBorderB  = Color(1.00f, 0.35f, 0.72f, 1f),
        tileBorderC  = Color(0.86f, 0.28f, 0.65f, 1f)
    )
    CubeSkin.Toy, CubeSkin.Rainbow -> MenuSkinColors(  // neutral panel; tiles use per-item accent
        panelMidTint = Color(0.05f, 0.05f, 0.08f, 1f),
        borderDim    = Color(0.30f, 0.30f, 0.40f, 0.40f),
        borderBright = Color(0.55f, 0.55f, 0.65f, 0.55f),
        tileBgAccent = Color(0.08f, 0.08f, 0.10f, 0.96f),
        tileBorderA  = Color(0.40f, 0.40f, 0.50f, 1f),
        tileBorderB  = Color(0.55f, 0.55f, 0.65f, 1f),
        tileBorderC  = Color(0.46f, 0.46f, 0.56f, 1f)
    )
    else -> MenuSkinColors(  // Default
        panelMidTint = Color(0.07f, 0.04f, 0.16f, 1f),
        borderDim    = Color(0.25f, 0.19f, 0.64f, 0.60f),
        borderBright = Color(1.00f, 0.11f, 0.95f, 0.74f),
        tileBgAccent = Color(0.21f, 0.03f, 0.31f, 0.96f),
        tileBorderA  = Color(0.44f, 0.32f, 1.00f, 1f),
        tileBorderB  = Color(1.00f, 0.12f, 0.95f, 1f),
        tileBorderC  = Color(0.74f, 0.46f, 1.00f, 1f)
    )
}

/** Tile colors derived from a single per-item accent — used by Toy and Rainbow skins. */
private fun itemTileColors(accent: Color): MenuSkinColors {
    val r = accent.red; val g = accent.green; val b = accent.blue
    return MenuSkinColors(
        panelMidTint = Color(r * 0.05f, g * 0.05f, b * 0.08f, 1f),
        borderDim    = Color(r * 0.50f, g * 0.50f, b * 0.60f, 0.60f),
        borderBright = Color(r, g, b, 0.82f),
        tileBgAccent = Color(r * 0.18f, g * 0.18f, b * 0.22f, 0.96f),
        tileBorderA  = Color(r * 0.65f, g * 0.65f, b * 0.80f, 1f),
        tileBorderB  = Color(r, g, b, 1f),
        tileBorderC  = Color(r * 0.78f, g * 0.78f, b * 0.88f, 1f)
    )
}

private fun iconTintForSkin(skin: CubeSkin): Color = when (skin) {
    CubeSkin.Gold -> Color(1.00f, 0.95f, 0.60f, 1f)
    CubeSkin.Chocolate -> Color(0.92f, 0.65f, 0.25f, 1f)
    CubeSkin.Camo -> Color(0.82f, 0.84f, 0.56f, 1f)
    CubeSkin.Ice -> Color(0.82f, 0.96f, 1.00f, 1f)
    else -> Color.White
}

// ── Overlay ───────────────────────────────────────────────────────────────────

@Composable
fun BoxScope.CubeMenuWheelOverlay(
    controller: CubeMenuWheelController,
    modifier: Modifier = Modifier,
    layout: CubeMenuLayout = CubeMenuLayout.Horizontal,
    cubeSkin: CubeSkin = CubeSkin.Default
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
            cubeSkin = cubeSkin,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ── Floating menu tiles ───────────────────────────────────────────────────────

@Composable
private fun CubeNeonMenu(
    items: List<CubeMenuWheelItem>,
    selectedIndex: Int,
    tickCount: Int,
    layout: CubeMenuLayout,
    openProgress: Float,
    cubeSkin: CubeSkin = CubeSkin.Default,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val visibleItems = visibleMenuItems(items, selectedIndex)
        val skinColors = if (cubeSkin.usePerItemTint) {
            null
        } else {
            menuSkinColors(cubeSkin)
        }
        val assetPath = cubeSkin.holdMenuAssetPath
        val horizontalSpacing = clampDp(maxWidth * 0.23f, 86.dp, 112.dp)
        val verticalSpacing = clampDp(maxHeight * 0.11f, 92.dp, 118.dp)
        val horizontalLiftFromBottom = clampDp(maxHeight * 0.24f, 226.dp, 292.dp)
        val panelYOffset = when (layout) {
            CubeMenuLayout.Horizontal -> maxHeight * 0.5f - horizontalLiftFromBottom
            CubeMenuLayout.Vertical -> 0.dp
        }

        Box(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.10f * openProgress))
            )

            visibleItems.forEach { visibleItem ->
                val selected = visibleItem.index == selectedIndex
                val distance = abs(visibleItem.relativeSlot)
                val slotOpacity = if (distance == 2) 0.72f else 1f
                val slotOffsetX = when (layout) {
                    CubeMenuLayout.Horizontal -> horizontalSpacing * visibleItem.relativeSlot.toFloat()
                    CubeMenuLayout.Vertical -> 0.dp
                }
                val slotOffsetY = when (layout) {
                    CubeMenuLayout.Horizontal -> panelYOffset
                    CubeMenuLayout.Vertical -> verticalSpacing * visibleItem.relativeSlot.toFloat()
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = slotOffsetX, y = slotOffsetY)
                        .graphicsLayer {
                            alpha = openProgress * slotOpacity
                            val entranceScale = 0.3f + openProgress * 0.7f
                            scaleX = entranceScale
                            scaleY = entranceScale
                        }
                        .zIndex(if (selected) 3f else 3f - distance.toFloat())
                ) {
                    CubeNeonMenuTile(
                        item = visibleItem.item,
                        relativeSlot = visibleItem.relativeSlot,
                        selected = selected,
                        tickCount = tickCount,
                        layout = layout,
                        cubeSkin = cubeSkin,
                        skinColors = skinColors ?: itemTileColors(visibleItem.item.accent),
                        tileAssetPath = assetPath
                    )
                }
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
    layout: CubeMenuLayout,
    cubeSkin: CubeSkin,
    skinColors: MenuSkinColors,
    tileAssetPath: String
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
    val tileShape = RoundedCornerShape(if (selected) 24.dp else 18.dp)
    val contentTint = if (cubeSkin.usePerItemTint) item.accent else iconTintForSkin(cubeSkin)
    val borderAccent = if (cubeSkin.usePerItemTint) item.accent else skinColors.tileBorderB
    val direction = when {
        relativeSlot > 0 -> 1f
        relativeSlot < 0 -> -1f
        else -> 0f
    }
    val idleRotation = (abs(relativeSlot) * 4.5f).coerceAtMost(9f) * direction

    Box(
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
                shadowElevation = if (selected) 24.dp.toPx() else 12.dp.toPx()
                shape = tileShape
                clip = false
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = "file:///android_asset/$tileAssetPath",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(tileShape)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (selected) 0.32f else 0.20f),
                                Color.White.copy(alpha = 0.06f),
                                item.accent.copy(alpha = 0.14f),
                                Color.Transparent
                            )
                        ),
                        tileShape
                    )
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                    .border(
                        width = if (selected) 3.5.dp else 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (selected) 0.52f else 0.28f),
                                borderAccent.copy(alpha = if (selected) 1.0f else 0.78f),
                                Color.White.copy(alpha = if (selected) 0.38f else 0.18f),
                                borderAccent.copy(alpha = if (selected) 0.92f else 0.62f)
                            )
                        ),
                        shape = tileShape
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
                Box(
                    modifier = Modifier.size(if (selected) 42.dp else 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = contentTint,
                        modifier = Modifier.size(if (selected) 38.dp else 28.dp)
                    )
                    item.secondaryIcon?.let { secondaryIcon ->
                        Icon(
                            secondaryIcon,
                            contentDescription = null,
                            tint = contentTint,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(if (selected) 18.dp else 14.dp)
                        )
                    }
                }
                PoplurText(
                    item.label,
                    color = contentTint,
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

// ── Helpers ───────────────────────────────────────────────────────────────────

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
    selected && layout == CubeMenuLayout.Vertical -> 166.dp
    selected -> 132.dp
    layout == CubeMenuLayout.Vertical -> 124.dp
    else -> 98.dp
}

private fun cubeMenuTileHeight(selected: Boolean, layout: CubeMenuLayout): Dp = when {
    selected && layout == CubeMenuLayout.Vertical -> 128.dp
    selected -> 132.dp
    layout == CubeMenuLayout.Vertical -> 104.dp
    else -> 98.dp
}


private fun clampDp(value: Dp, minimum: Dp, maximum: Dp): Dp {
    return value.coerceAtLeast(minimum).coerceAtMost(maximum)
}
