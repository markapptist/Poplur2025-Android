package com.apptists.poplur2025.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RadiusDisplay(
    radius: Int,
    totalRadius: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "RADIUS",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(6.dp))
            Text("🛡️", color = Color.White, fontSize = 14.sp)
        }
        Text(
            text = radius.toString(),
            color = Color.White,
            fontSize = 72.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 72.sp
        )
        HorizontalDivider(
            color = Color.White, 
            thickness = 1.dp, 
            modifier = Modifier.width(100.dp).padding(vertical = 4.dp)
        )
        Text(
            text = totalRadius.toString(),
            color = Color.White,
            fontSize = 56.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 56.sp
        )
        Text(
            text = "mi",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
