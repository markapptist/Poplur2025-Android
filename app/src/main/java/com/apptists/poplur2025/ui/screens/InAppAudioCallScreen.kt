package com.apptists.poplur2025.ui.screens

import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptists.poplur2025.call.CallSessionManager
import kotlinx.coroutines.delay

private enum class AudioCallPhase { Ringing, Connected }

@Composable
fun InAppAudioCallScreen(
    calleeName: String,
    onEnd: () -> Unit
) {
    var phase by remember { mutableStateOf(AudioCallPhase.Ringing) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeaker by remember { mutableStateOf(false) }
    var elapsed by remember { mutableIntStateOf(0) }

    val pulseTransition = rememberInfiniteTransition(label = "audio-pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    LaunchedEffect(Unit) {
        delay(2_000)
        phase = AudioCallPhase.Connected
    }

    LaunchedEffect(phase) {
        if (phase == AudioCallPhase.Connected) {
            while (true) {
                delay(1_000)
                elapsed++
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0F1E), Color.Black)
                )
            )
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(Modifier.height(80.dp))

            // Status
            Text(
                text = when (phase) {
                    AudioCallPhase.Ringing -> "Calling…"
                    AudioCallPhase.Connected -> CallSessionManager.formattedDuration(elapsed)
                },
                color = Color.White.copy(alpha = if (phase == AudioCallPhase.Ringing) 0.55f else 0.8f),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(36.dp))

            // Avatar with pulse
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                if (phase == AudioCallPhase.Ringing) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .scale(pulseScale)
                            .background(
                                Color.White.copy(alpha = 0.12f * (1.5f - pulseScale).coerceIn(0f, 1f)),
                                CircleShape
                            )
                    )
                }
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .background(Color(0xFF1E1E2C), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = calleeName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        color = Color.White,
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = calleeName,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.weight(1f))

            // Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(36.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 60.dp)
            ) {
                AudioCallButton(
                    icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    label = if (isMuted) "Unmute" else "Mute",
                    tint = if (isMuted) Color(0xFFE0E0E8) else Color(0xFF333338),
                    iconTint = if (isMuted) Color.Black else Color.White
                ) { isMuted = !isMuted }

                AudioCallButton(
                    icon = Icons.Default.CallEnd,
                    label = "End",
                    tint = Color(0xFFE53935),
                    size = 72,
                    iconTint = Color.White
                ) { onEnd() }

                AudioCallButton(
                    icon = if (isSpeaker) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    label = "Speaker",
                    tint = if (isSpeaker) Color(0xFFE0E0E8) else Color(0xFF333338),
                    iconTint = if (isSpeaker) Color.Black else Color.White
                ) { isSpeaker = !isSpeaker }
            }
        }
    }
}

@Composable
private fun AudioCallButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    iconTint: Color,
    size: Int = 60,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(size.dp)
                .background(tint, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size((size * 0.45f).dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
