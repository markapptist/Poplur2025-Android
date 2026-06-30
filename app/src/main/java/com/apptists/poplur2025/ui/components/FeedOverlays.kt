package com.apptists.poplur2025.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import com.apptists.poplur2025.ui.components.PoplurText
import com.apptists.poplur2025.ui.components.ShaderNumberText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ceil

private val channelGreen = Color(0xFF4EFF40)

@Composable
fun UserInfoOverlay(
    displayName: String,
    location: String,
    channelNumber: Int,
    channelEmoji: String,
    channelCategory: String,
    channelIconResId: Int? = null,
    modifier: Modifier = Modifier
) {
    var showChannel by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2_400)
            showChannel = true
            delay(1_800)
            showChannel = false
        }
    }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PoplurText("🇯🇵", fontSize = 32.sp)
            Spacer(Modifier.width(10.dp))

            AnimatedContent(
                targetState = channelIconResId to channelEmoji,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
                label = "channel-emoji"
            ) { (iconResId, emoji) ->
                ChannelIcon(iconResId = iconResId, fallbackEmoji = emoji, size = 38)
            }

            Spacer(Modifier.width(10.dp))
            Column {
                // Main label: username ↔ CH XXX (green)
                AnimatedContent(
                    targetState = showChannel,
                    transitionSpec = {
                        (fadeIn(tween(280)) + slideInVertically { -it / 3 })
                            .togetherWith(fadeOut(tween(280)) + slideOutVertically { it / 3 })
                    },
                    label = "channel-name"
                ) { isChannel ->
                    if (isChannel) {
                        ShaderNumberText(
                            text = "CH $channelNumber",
                            color = channelGreen,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        ShaderNumberText(
                            text = displayName,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Sub text: location ↔ category name
                AnimatedContent(
                    targetState = showChannel,
                    transitionSpec = {
                        (fadeIn(tween(280)) + slideInVertically { -it / 3 })
                            .togetherWith(fadeOut(tween(280)) + slideOutVertically { it / 3 })
                    },
                    label = "channel-sub"
                ) { isChannel ->
                    if (isChannel) {
                        ShaderNumberText(
                            text = channelCategory.uppercase(),
                            color = channelGreen.copy(alpha = 0.82f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        ShaderNumberText(
                            text = location,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelIcon(iconResId: Int?, fallbackEmoji: String, size: Int) {
    if (iconResId != null) {
        Image(
            painter = painterResource(iconResId),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(size.dp)
        )
    } else {
        PoplurText(fallbackEmoji, fontSize = size.sp)
    }
}

@Composable
fun TrendingDisplay(
    postsCount: Int,
    radiusChange: Int,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White
) {
    val changeColor = if (radiusChange >= 0) Color.Green else Color.Red
    val changeIcon = if (radiusChange >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown
    val animatedPosts = rememberAnimatedCounter(postsCount, label = "trending-posts-count")
    val countPulse by animateFloatAsState(
        targetValue = if (animatedPosts == postsCount) 1f else 1.08f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "trending-posts-pulse"
    )

    Column(
        modifier = modifier.height(PoplurCounterLayout.counterHeight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ShaderNumberText(
            text = "TRENDING",
            color = contentColor.copy(alpha = 0.7f),
            letterSpacing = 1.4.sp,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            modifier = Modifier.height(PoplurCounterLayout.labelHeight)
        )
        Spacer(Modifier.height(PoplurCounterLayout.labelToNumeratorSpacing))

        Box(
            modifier = Modifier
                .width(PoplurCounterLayout.separatorWidth)
                .height(PoplurCounterLayout.numeratorHeight),
            contentAlignment = Alignment.BottomCenter
        ) {
            Icon(
                imageVector = changeIcon,
                contentDescription = null,
                tint = changeColor,
                modifier = Modifier.size(38.dp)
            )
        }
        Spacer(Modifier.height(PoplurCounterLayout.numeratorToSeparatorSpacing))

        HorizontalDivider(
            color = contentColor,
            thickness = PoplurCounterLayout.separatorHeight,
            modifier = Modifier.width(PoplurCounterLayout.separatorWidth)
        )
        Spacer(Modifier.height(PoplurCounterLayout.separatorToDenominatorSpacing))

        ShaderNumberText(
            text = NumberFormat.getNumberInstance(Locale.US).format(animatedPosts),
            color = contentColor,
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 38.sp,
            modifier = Modifier.graphicsLayer {
                scaleX = countPulse
                scaleY = countPulse
            }.height(PoplurCounterLayout.denominatorHeight)
        )
        ShaderNumberText(
            text = "posts",
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun PoplurVolumeBars(
    modifier: Modifier = Modifier,
    volumeFraction: Float
) {
    val meterGreen = Color(0xFF3BFF2E)
    val totalBars = 18
    val clampedVolume = volumeFraction.coerceIn(0f, 1f)
    val activeBars = if (clampedVolume <= 0f) 0 else ceil(clampedVolume * totalBars).toInt()

    Canvas(
        modifier = modifier
            .size(width = 80.dp, height = 280.dp)
    ) {
        val barWidth = 48.dp.toPx()
        val barHeight = 8.dp.toPx()
        val gap = 6.4.dp.toPx()
        val totalHeight = totalBars * barHeight + (totalBars - 1) * gap
        val startX = (size.width - barWidth) / 2f
        val startY = (size.height - totalHeight) / 2f
        val corner = 2.dp.toPx()

        repeat(totalBars) { index ->
            val top = startY + (totalBars - 1 - index) * (barHeight + gap)
            val active = index < activeBars
            val color = if (active) meterGreen else meterGreen.copy(alpha = 0.18f)

            if (active) {
                drawRoundRect(
                    color = meterGreen.copy(alpha = 0.42f),
                    topLeft = Offset(startX - 1.dp.toPx(), top - 1.dp.toPx()),
                    size = Size(barWidth + 2.dp.toPx(), barHeight + 2.dp.toPx()),
                    cornerRadius = CornerRadius(corner + 1.dp.toPx())
                )
            }

            drawRoundRect(
                color = color,
                topLeft = Offset(startX, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(corner)
            )
        }
    }
}

@Composable
fun FeedVolumeBars(
    modifier: Modifier = Modifier,
    volumeFraction: Float
) {
    PoplurVolumeBars(
        modifier = modifier,
        volumeFraction = volumeFraction
    )
}

@Composable
fun rememberAnimatedCounter(
    targetValue: Int,
    label: String,
    durationMillis: Int = 1200
): Int {
    var animatedTarget by remember { mutableIntStateOf(0) }
    LaunchedEffect(targetValue) {
        animatedTarget = targetValue
    }
    val animatedValue by animateIntAsState(
        targetValue = animatedTarget,
        animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
        label = label
    )
    return animatedValue
}
