package com.apptists.poplur2025.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Panorama
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.apptists.poplur2025.ui.StreetViewUiState

/**
 * Circular 3D gradient icon for Street View — matches the iOS mapActionIcon style.
 * Only rendered when state is not Idle (caller gates visibility).
 */
@Composable
fun StreetViewButton(
    state: StreetViewUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isChecking = state is StreetViewUiState.Checking

    val gradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF00E5FF),
            Color(0xFF0080FF),
            Color(0xFF003080)
        )
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(52.dp)
            .shadow(
                elevation = 10.dp,
                shape = CircleShape,
                ambientColor = Color(0xFF00BFFF).copy(alpha = 0.55f),
                spotColor = Color(0xFF00BFFF).copy(alpha = 0.55f)
            )
            .clip(CircleShape)
            .background(gradient)
            .clickable(enabled = !isChecking, onClick = onClick)
    ) {
        if (isChecking) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.5.dp
            )
        } else {
            Icon(
                imageVector = Icons.Default.Panorama,
                contentDescription = "Street View",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
