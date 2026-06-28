package com.apptists.poplur2025.ui.games.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.apptists.poplur2025.ui.games.PoplurCharacter
import com.apptists.poplur2025.ui.games.PoplurGameConfig
import com.apptists.poplur2025.ui.games.PoplurGameService
import com.apptists.poplur2025.ui.games.PoplurGameType
import kotlin.math.cos
import kotlin.math.sin

private val BG = Color(0xFF0D0D14)

@Composable
fun GameLaunchScreen(onDismiss: () -> Unit) {
    var config by remember { mutableStateOf<PoplurGameConfig?>(null) }
    val loadingChar = remember { PoplurCharacter.all.random() }

    LaunchedEffect(Unit) {
        config = PoplurGameService.generateConfig()
    }

    val current = config
    if (current != null) {
        // Route to the correct game
        when (current.game) {
            PoplurGameType.SideScroller   -> PopRunnerGame(config = current, onDismiss = onDismiss)
            PoplurGameType.ArcadeShooter  -> StellaBlastGame(config = current, onDismiss = onDismiss)
            PoplurGameType.ActionAdventure -> ZylaQuestGame(config = current, onDismiss = onDismiss)
            PoplurGameType.Sports         -> RexHoopsGame(config = current, onDismiss = onDismiss)
        }
    } else {
        LoadingScreen(character = loadingChar, onDismiss = onDismiss)
    }
}

@Composable
private fun LoadingScreen(character: PoplurCharacter, onDismiss: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "loader")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_rotation"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BG),
        contentAlignment = Alignment.Center
    ) {
        // Close button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .zIndex(10f)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Pulsing character avatar with orbiting dot
            Canvas(modifier = Modifier.size(120.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val radius = size.minDimension / 2f * pulse

                // Body
                drawCircle(
                    color = character.primaryColor,
                    radius = radius * 0.55f,
                    center = Offset(cx, cy - radius * 0.05f)
                )
                // Head
                drawCircle(
                    color = character.primaryColor.copy(alpha = 0.85f),
                    radius = radius * 0.32f,
                    center = Offset(cx, cy - radius * 0.52f)
                )
                // Eyes
                drawCircle(color = Color.White, radius = radius * 0.07f, center = Offset(cx - radius * 0.10f, cy - radius * 0.54f))
                drawCircle(color = Color.White, radius = radius * 0.07f, center = Offset(cx + radius * 0.10f, cy - radius * 0.54f))
                drawCircle(color = Color.Black, radius = radius * 0.04f, center = Offset(cx - radius * 0.10f, cy - radius * 0.54f))
                drawCircle(color = Color.Black, radius = radius * 0.04f, center = Offset(cx + radius * 0.10f, cy - radius * 0.54f))

                // Orbiting accent dot
                val orbitR = radius * 0.88f
                val angleRad = Math.toRadians(rotation.toDouble())
                val dotX = cx + orbitR * cos(angleRad).toFloat()
                val dotY = cy + orbitR * sin(angleRad).toFloat()
                drawCircle(color = character.accentColor, radius = radius * 0.09f, center = Offset(dotX, dotY))
                // Trailing dot
                val trailAngle = Math.toRadians((rotation - 60.0))
                val trailX = cx + orbitR * cos(trailAngle).toFloat()
                val trailY = cy + orbitR * sin(trailAngle).toFloat()
                drawCircle(color = character.accentColor.copy(alpha = 0.4f), radius = radius * 0.06f, center = Offset(trailX, trailY))
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Generating your game…",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = character.displayName,
                color = character.primaryColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = character.role,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp
            )
        }
    }
}
