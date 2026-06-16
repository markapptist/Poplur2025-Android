package com.apptists.poplur2025.ui.screens

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptists.poplur2025.call.CallSessionManager

@Composable
fun IncomingCallScreen(
    callerName: String,
    channelId: String,
    documentId: String,
    modifier: Modifier = Modifier
) {
    val pulseTransition = rememberInfiniteTransition(label = "incoming-pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = EaseInOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse-scale"
    )
    val pulseAlpha = (2f - pulseScale).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            // Pulsing avatar ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(130.dp)
            ) {
                // Outer pulse ring
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(pulseScale)
                        .background(
                            color = Color(0xFF00C853).copy(alpha = 0.25f * pulseAlpha),
                            shape = CircleShape
                        )
                )
                // Avatar circle with initial
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(Color(0xFF2D2D2D), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = callerName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = callerName,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Poplur Call",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 15.sp
            )

            Spacer(Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.spacedBy(64.dp),
                modifier = Modifier.padding(bottom = 64.dp)
            ) {
                CallActionButton(
                    icon = Icons.Default.CallEnd,
                    label = "Decline",
                    tint = Color(0xFFE53935)
                ) {
                    CallSessionManager.declineCall(documentId)
                }

                CallActionButton(
                    icon = Icons.Default.Call,
                    label = "Accept",
                    tint = Color(0xFF00C853)
                ) {
                    CallSessionManager.acceptCall(
                        channelId = channelId,
                        documentId = documentId,
                        callerName = callerName
                    )
                }
            }
        }
    }
}

@Composable
private fun CallActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .background(tint, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onClick, modifier = Modifier.size(70.dp)) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 13.sp
        )
    }
}
