package com.apptists.poplur2025.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.apptists.poplur2025.util.PoplurEQSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

// kept for backward-compat with settings persistence; no longer affects layout
enum class CubeMenuLayout { Horizontal, Vertical }

// ── Neon pink palette ──────────────────────────────────────────────────────────

private val NeonPink   = Color(1f, 0.08f, 0.55f, 1f)
private val CardBg     = Color(0.05f, 0.03f, 0.08f, 1f)
private val GlowPurple = Color(0.55f, 0.02f, 0.28f, 0.55f)

// ── Data model ────────────────────────────────────────────────────────────────

data class CubeMenuWheelItem(
    val label: String,
    val iconAssetPath: String,          // path under assets/, e.g. "hold_menu_icons/home.png"
    val fallbackIcon: ImageVector? = null,
    val fallbackTint: Color = Color.White,
    val onSelected: () -> Unit
)

// ── Controller ────────────────────────────────────────────────────────────────

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
    var isClosing by mutableStateOf(false)
        private set

    val selectedItem: CubeMenuWheelItem?
        get() = items.getOrNull(selectedIndex)

    val isOnScreen: Boolean
        get() = isVisible || isClosing

    private var closeJob: Job? = null
    private var scrubRemainder = 0f
    private val scrubStepPx = 200f

    fun beginHold(nextItems: List<CubeMenuWheelItem>) {
        if (nextItems.isEmpty()) return
        closeJob?.cancel()
        items = nextItems
        selectedIndex = 0
        scrubRemainder = 0f
        isClosing = false
        isVisible = true
        PoplurEQSync.begin(PoplurEQSync.Lane.HoldMenu)
    }

    fun endHold(activateSelected: Boolean) {
        val selectedAction = if (activateSelected && isVisible) {
            items.getOrNull(selectedIndex)?.onSelected
        } else null
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

    fun scrub(deltaX: Float, deltaY: Float) {
        if (!isVisible || items.size <= 1) return
        val primary = if (abs(deltaX) >= abs(deltaY)) deltaX else deltaY
        if (abs(primary) <= 0.25f) return
        scrubRemainder += primary
        while (scrubRemainder >= scrubStepPx) {
            scrubRemainder -= scrubStepPx
            moveSelection(1)
        }
        while (scrubRemainder <= -scrubStepPx) {
            scrubRemainder += scrubStepPx
            moveSelection(-1)
        }
    }

    private fun moveSelection(offset: Int) {
        if (items.isEmpty()) return
        val count = items.size
        selectedIndex = (selectedIndex + offset + count) % count
        onTick()
    }
}

// ── Overlay ───────────────────────────────────────────────────────────────────

@Composable
fun BoxScope.CubeMenuWheelOverlay(
    controller: CubeMenuWheelController,
    modifier: Modifier = Modifier
) {
    val openProgress by animateFloatAsState(
        targetValue = if (controller.isVisible) 1f else 0f,
        animationSpec = if (controller.isVisible) {
            spring(dampingRatio = 0.70f, stiffness = Spring.StiffnessMediumLow)
        } else {
            tween(durationMillis = 200)
        },
        label = "hold-menu-open"
    )

    AnimatedVisibility(
        visible = controller.isOnScreen,
        enter = fadeIn(tween(140)),
        exit = fadeOut(tween(180)),
        modifier = modifier.fillMaxSize().align(Alignment.Center)
    ) {
        HoldMenuCard(
            items = controller.items,
            selectedIndex = controller.selectedIndex,
            openProgress = openProgress
        )
    }
}

// ── Curved vertical wheel layout ─────────────────────────────────────────────
//
// The selected item sits at center; neighbors recede above/below along a
// gentle arc (offset + rotation + scale falloff), scrubbed with the hold
// gesture. Neon-pink icon-pack style with the spring scale/fade entrance.

@Composable
private fun HoldMenuCard(
    items: List<CubeMenuWheelItem>,
    selectedIndex: Int,
    openProgress: Float
) {
    // Matches the iOS CubeHoldMenuOverlay redesign: a dimmed backdrop with a compact
    // themed card holding a GRID of icon+label tiles (was a fullscreen vertical wheel),
    // plus a "SCRUB" indicator on the right.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.50f))
            .graphicsLayer { alpha = openProgress },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = 0.9f + openProgress * 0.1f
                    scaleY = 0.9f + openProgress * 0.1f
                }
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(CardBg.copy(alpha = 0.96f))
                .background(Brush.radialGradient(listOf(GlowPurple.copy(alpha = 0.35f), Color.Transparent)))
                .border(1.5.dp, NeonPink.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            HoldMenuGrid(items = items, selectedIndex = selectedIndex)
        }

        ScrubIndicator(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 22.dp)
        )
    }
}

@Composable
private fun HoldMenuGrid(items: List<CubeMenuWheelItem>, selectedIndex: Int) {
    if (items.isEmpty()) return
    val rows = items.chunked(4)   // up to 4 per row, like iOS CubeHoldMenuOverlay
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEachIndexed { colIndex, item ->
                    val globalIndex = rowIndex * 4 + colIndex
                    HoldMenuGridTile(item = item, selected = globalIndex == selectedIndex)
                }
            }
        }
    }
}

@Composable
private fun ScrubIndicator(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.32f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Box(Modifier.height(44.dp), contentAlignment = Alignment.Center) {
            Text(
                "SCRUB",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.rotate(-90f)
            )
        }
        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun HoldMenuGridTile(item: CubeMenuWheelItem, selected: Boolean) {
    val context = LocalContext.current
    val bitmap = remember(context, item.iconAssetPath) {
        runCatching {
            context.assets.open(item.iconAssetPath).use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
        }.getOrNull()
    }
    val tileShape = RoundedCornerShape(16.dp)
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "grid-tile"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(tileShape)
                .background(Color.Black, tileShape)
                .border(
                    width = if (selected) 2.5.dp else 1.5.dp,
                    color = if (selected) NeonPink else NeonPink.copy(alpha = 0.35f),
                    shape = tileShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = item.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(tileShape)
                )
            } else if (item.fallbackIcon != null) {
                Icon(
                    imageVector = item.fallbackIcon,
                    contentDescription = item.label,
                    tint = item.fallbackTint,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.label,
            color = if (selected) NeonPink else Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun WheelItems(items: List<CubeMenuWheelItem>, selectedIndex: Int) {
    val slotSpacing = 122.dp
    val arcDepth = 26.dp
    val count = items.size
    if (count == 0) return
    val centerSlot = count / 2

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        items.forEachIndexed { index, item ->
            val slot = ((index - selectedIndex + centerSlot + count * 4) % count) - centerSlot
            if (abs(slot) <= 2) {
                val selected = index == selectedIndex
                val distance = abs(slot)

                val targetY = slotSpacing * slot.toFloat()
                val targetX = -arcDepth * Math.pow(distance.toDouble(), 1.5).toFloat()
                val offsetY by androidx.compose.animation.core.animateDpAsState(
                    targetValue = targetY,
                    animationSpec = spring(
                        dampingRatio = 0.72f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "wheel-y"
                )
                val offsetX by androidx.compose.animation.core.animateDpAsState(
                    targetValue = targetX,
                    animationSpec = spring(
                        dampingRatio = 0.72f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "wheel-x"
                )
                val itemScale by animateFloatAsState(
                    targetValue = if (selected) 1f else (1f - distance * 0.13f).coerceAtLeast(0.78f),
                    animationSpec = spring(
                        dampingRatio = 0.72f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "wheel-scale"
                )

                Box(
                    modifier = Modifier
                        .offset(x = offsetX, y = offsetY)
                        .graphicsLayer {
                            scaleX = itemScale
                            scaleY = itemScale
                            rotationZ = slot * -5f
                            alpha = if (distance == 2) 0.55f else 1f
                        }
                        .zIndex(if (selected) 3f else 3f - distance.toFloat())
                ) {
                    HoldMenuWheelTile(item = item, selected = selected)
                }
            }
        }
    }
}

@Composable
private fun HoldMenuWheelTile(item: CubeMenuWheelItem, selected: Boolean) {
    val context = LocalContext.current
    val bitmap = remember(context, item.iconAssetPath) {
        runCatching {
            context.assets.open(item.iconAssetPath).use {
                BitmapFactory.decodeStream(it)?.asImageBitmap()
            }
        }.getOrNull()
    }
    val tileSize = if (selected) 104.dp else 88.dp
    val tileShape = RoundedCornerShape(20.dp)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(tileSize)
                .graphicsLayer {
                    shadowElevation = if (selected) 24.dp.toPx() else 8.dp.toPx()
                    shape = tileShape
                    clip = false
                }
                .background(Color.Black, tileShape)
                .border(
                    width = if (selected) 2.5.dp else 1.5.dp,
                    color = if (selected) NeonPink else NeonPink.copy(alpha = 0.35f),
                    shape = tileShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = item.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(tileShape)
                )
            } else if (item.fallbackIcon != null) {
                Icon(
                    imageVector = item.fallbackIcon,
                    contentDescription = item.label,
                    tint = item.fallbackTint,
                    modifier = Modifier.size(tileSize * 0.42f)
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = item.label,
            color = if (selected) NeonPink else Color.White,
            fontSize = if (selected) 13.sp else 11.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.6.sp
        )
    }
}

@Composable
private fun HeaderPill(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(CardBg, RoundedCornerShape(999.dp))
            .border(2.dp, NeonPink, RoundedCornerShape(999.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(NeonPink, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "P",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
        }
        Text(
            "HOLD MENU",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
private fun CloseButton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(56.dp)
            .background(CardBg, CircleShape)
            .border(2.dp, NeonPink, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "✕",
            color = NeonPink,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black
        )
    }
}

