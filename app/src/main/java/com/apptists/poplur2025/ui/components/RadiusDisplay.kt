package com.apptists.poplur2025.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.apptists.poplur2025.ui.components.PoplurText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.*

@Composable
fun RadiusDisplay(
    modifier: Modifier = Modifier,
    radius: Int,
    unit: String = "mi",
    totalRadius: Int = 3959,
    contentColor: Color = Color.White
) {
    val animatedRadius = rememberAnimatedCounter(
        targetValue = radius,
        label = "radius-count",
        durationMillis = 1300
    )
    val animatedTotal = rememberAnimatedCounter(
        targetValue = totalRadius,
        label = "radius-total-count",
        durationMillis = 1500
    )
    val radiusPulse by animateFloatAsState(
        targetValue = if (animatedRadius == radius) 1f else 1.08f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "radius-number-pulse"
    )
    val totalPulse by animateFloatAsState(
        targetValue = if (animatedTotal == totalRadius) 1f else 1.05f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "radius-total-pulse"
    )
    val (formattedRadius, displayUnit) = formatRadiusForDisplay(animatedRadius, unit)
    val (formattedTotal, denominatorUnit) = formatTotalRadiusForDisplay(animatedTotal, unit)

    Column(
        modifier = modifier.height(PoplurCounterLayout.counterHeight),
        horizontalAlignment = Alignment.End
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(PoplurCounterLayout.labelHeight)
        ) {
            PoplurText(
                text = "RADIUS",
                color = contentColor.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.4.sp
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                tint = Color(0xFFD8892B).copy(alpha = 0.8f),
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(Modifier.height(PoplurCounterLayout.labelToNumeratorSpacing))

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .height(PoplurCounterLayout.numeratorHeight)
                .graphicsLayer {
                    scaleX = radiusPulse
                    scaleY = radiusPulse
                }
        ) {
            ShaderNumberText(
                text = formattedRadius,
                color = Color(0xFFD8892B),
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 40.sp,
                intensity = 1.05f,
                autoSize = true
            )
            PoplurText(
                text = displayUnit,
                color = Color(0xFFD8892B),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Spacer(Modifier.height(PoplurCounterLayout.numeratorToSeparatorSpacing))

        HorizontalDivider(
            color = Color(0xFFD8892B),
            thickness = PoplurCounterLayout.separatorHeight,
            modifier = Modifier
                .width(PoplurCounterLayout.separatorWidth)
        )
        Spacer(Modifier.height(PoplurCounterLayout.separatorToDenominatorSpacing))

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .height(PoplurCounterLayout.denominatorHeight)
                .graphicsLayer {
                    scaleX = totalPulse
                    scaleY = totalPulse
                }
        ) {
            ShaderNumberText(
                text = formattedTotal,
                color = contentColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 28.sp,
                intensity = 0.72f,
                autoSize = true
            )
            PoplurText(
                text = denominatorUnit,
                color = contentColor.copy(alpha = 0.78f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }

        Spacer(Modifier.weight(1f))
    }
}

private fun formatRadiusForDisplay(radius: Int, unit: String): Pair<String, String> {
    val cleanUnit = unit.trim().ifBlank { "mi" }
    val currentRadius = radius.coerceAtLeast(0)
    if (cleanUnit.equals("m", ignoreCase = true) ||
        cleanUnit.equals("meter", ignoreCase = true) ||
        cleanUnit.equals("meters", ignoreCase = true)
    ) {
        return if (currentRadius >= 1_000) {
            formatKilometers(currentRadius / 1_000f) to "km"
        } else {
            NumberFormat.getNumberInstance(Locale.US).format(currentRadius) to "m"
        }
    }

    return NumberFormat.getNumberInstance(Locale.US).format(currentRadius) to cleanUnit
}

private fun formatKilometers(kilometers: Float): String {
    if (kilometers >= 100f) {
        return NumberFormat.getNumberInstance(Locale.US).format(kilometers.toInt())
    }
    return String.format(Locale.US, "%.1f", kilometers).removeSuffix(".0")
}

private fun formatTotalRadiusForDisplay(totalRadius: Int, unit: String): Pair<String, String> {
    val cleanUnit = unit.trim().ifBlank { "mi" }
    if (cleanUnit.equals("m", ignoreCase = true) ||
        cleanUnit.equals("meter", ignoreCase = true) ||
        cleanUnit.equals("meters", ignoreCase = true)
    ) {
        val kilometers = if (totalRadius == 3959) 6_371 else (totalRadius / 1_000).coerceAtLeast(1)
        return NumberFormat.getNumberInstance(Locale.US).format(kilometers) to "km"
    }

    return NumberFormat.getNumberInstance(Locale.US).format(totalRadius.coerceAtLeast(0)) to cleanUnit
}
