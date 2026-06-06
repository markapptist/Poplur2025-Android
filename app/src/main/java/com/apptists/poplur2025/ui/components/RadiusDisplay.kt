package com.apptists.poplur2025.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.*

@Composable
fun RadiusDisplay(
    modifier: Modifier = Modifier,
    radius: Int,
    unit: String = "km",
    totalRadius: Int = if (unit.equals("mi", ignoreCase = true)) 3959 else 6371
) {
    val animatedRadius by animateIntAsState(
        targetValue = radius,
        animationSpec = tween(durationMillis = 1400),
        label = "radius-count"
    )
    val formattedRadius = NumberFormat.getNumberInstance(Locale.US).format(animatedRadius)
    val formattedTotal = NumberFormat.getNumberInstance(Locale.US).format(totalRadius)

    Column(
        modifier = modifier.widthIn(min = 112.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = "RADIUS  🛡️",
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
        Text(
            text = formattedRadius,
            color = Color(0xFFD8892B),
            fontSize = 54.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 54.sp
        )
        HorizontalDivider(
            color = Color(0xFFD8892B),
            thickness = 2.dp,
            modifier = Modifier.width(104.dp).padding(vertical = 8.dp)
        )
        Text(
            text = formattedTotal,
            color = Color.White,
            fontSize = 42.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 42.sp
        )
        Text(
            text = unit,
            color = Color(0xFFD8892B),
            fontSize = 17.sp,
            fontWeight = FontWeight.Black
        )
    }
}
