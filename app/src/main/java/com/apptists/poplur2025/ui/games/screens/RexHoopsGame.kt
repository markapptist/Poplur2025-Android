package com.apptists.poplur2025.ui.games.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.apptists.poplur2025.ui.components.PoplurText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptists.poplur2025.ui.games.PoplurCharacter
import com.apptists.poplur2025.ui.games.PoplurGameConfig
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private enum class ShotPhase { AIM, POWER, IN_FLIGHT, RESULT }

@Composable
fun RexHoopsGame(config: PoplurGameConfig, onDismiss: () -> Unit) {
    val character = remember { PoplurCharacter.fromId(config.character) }

    var shotNum by remember { mutableIntStateOf(1) }
    var totalScore by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(ShotPhase.AIM) }
    var aimAngle by remember { mutableFloatStateOf(0f) } // -90 to +90 degrees
    var power by remember { mutableFloatStateOf(0f) }    // 0 to 1
    var ballX by remember { mutableFloatStateOf(0.5f) }
    var ballY by remember { mutableFloatStateOf(0.82f) }
    var ballT by remember { mutableFloatStateOf(0f) }   // 0..1 flight progress
    var resultText by remember { mutableStateOf("") }
    var windX by remember { mutableFloatStateOf(0f) }
    var restartKey by remember { mutableIntStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }

    // Normalized positions
    val playerX = 0.5f
    val playerY = 0.82f
    val hoopX = 0.5f
    val hoopY = 0.22f
    val MAX_SHOTS = 10

    fun nextShot() {
        windX = ((-5..5).random() * 0.01f)
        phase = ShotPhase.AIM
        aimAngle = 0f
        power = 0f
        ballX = playerX
        ballY = playerY
        resultText = ""
    }

    LaunchedEffect(restartKey) {
        shotNum = 1
        totalScore = 0
        isGameOver = false
        windX = ((-5..5).random() * 0.01f)
        nextShot()
    }

    // Aim rotation loop
    LaunchedEffect(phase) {
        if (phase == ShotPhase.AIM) {
            var t = 0f
            while (phase == ShotPhase.AIM) {
                delay(16L)
                t += 0.016f
                aimAngle = (sin(t * 1.5f) * 70f) // oscillates -70..+70 degrees
            }
        }
        if (phase == ShotPhase.POWER) {
            power = 0f
            while (phase == ShotPhase.POWER) {
                delay(16L)
                power = (power + 0.012f).coerceAtMost(1f)
                if (power >= 1f) {
                    // Auto-release at max power
                    phase = ShotPhase.IN_FLIGHT
                }
            }
        }
        if (phase == ShotPhase.IN_FLIGHT) {
            ballT = 0f
            val startX = playerX
            val startY = playerY
            val arcHeight = 0.45f + power * 0.25f
            var flightT = 0f
            val flightDuration = 0.7f + power * 0.3f
            while (phase == ShotPhase.IN_FLIGHT && flightT < 1f) {
                delay(16L)
                flightT = (flightT + 0.016f / flightDuration).coerceAtMost(1f)
                // Parabolic arc with wind drift
                val angleRad = Math.toRadians(aimAngle.toDouble())
                val lateralDrift = (sin(angleRad) * 0.12f).toFloat()
                ballX = startX + (hoopX - startX + lateralDrift + windX * flightT) * flightT
                ballY = startY + (hoopY - startY) * flightT - (sin(flightT * PI.toFloat()) * arcHeight)
                ballT = flightT
            }

            // Score check
            val dx = abs(ballX - hoopX)
            val dy = abs(ballY - hoopY)
            val scored = dx < 0.07f && dy < 0.07f
            val farShot = abs(playerX - hoopX) + abs(playerY - hoopY) > 0.5f

            if (scored) {
                val pts = if (farShot && power > 0.7f) 3 else 2
                totalScore += pts
                resultText = if (pts == 3) "3-POINTER! +3" else "Score! +2"
            } else {
                resultText = "Miss!"
            }
            phase = ShotPhase.RESULT
            delay(1400L)
            if (shotNum >= MAX_SHOTS) {
                isGameOver = true
            } else {
                shotNum++
                nextShot()
            }
        }
    }

    fun onTap() {
        if (isGameOver) return
        when (phase) {
            ShotPhase.AIM -> phase = ShotPhase.POWER
            ShotPhase.POWER -> phase = ShotPhase.IN_FLIGHT
            else -> {}
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF1A2035))
            .pointerInput(isGameOver) {
                detectTapGestures { onTap() }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Court floor (trapezoid perspective)
            val courtPath = Path().apply {
                moveTo(w * 0.05f, h * 0.95f)
                lineTo(w * 0.95f, h * 0.95f)
                lineTo(w * 0.75f, h * 0.45f)
                lineTo(w * 0.25f, h * 0.45f)
                close()
            }
            drawPath(courtPath, Color(0xFF8B5E3C))
            drawPath(courtPath, Color(0xFFFFFFFF).copy(alpha = 0.12f), style = Stroke(3f))

            // 3-point arc
            val arcPath = Path().apply {
                val cx = w * 0.5f
                val cy = h * 0.95f
                val rx = w * 0.42f
                val ry = h * 0.38f
                moveTo(cx - rx, cy)
                cubicTo(cx - rx, cy - ry * 1.2f, cx + rx, cy - ry * 1.2f, cx + rx, cy)
            }
            drawPath(arcPath, Color.White.copy(alpha = 0.3f), style = Stroke(3f, cap = StrokeCap.Round))

            // Backboard
            drawRect(Color.White.copy(alpha = 0.9f), Offset(w * hoopX - w * 0.08f, h * hoopY - h * 0.05f), Size(w * 0.16f, h * 0.04f))

            // Hoop
            drawRect(Color(0xFFFF6B35), Offset(w * hoopX - w * 0.06f, h * hoopY), Size(w * 0.12f, h * 0.012f))
            // Net suggestion lines
            for (i in 0..4) {
                val nx = w * hoopX - w * 0.055f + i * w * 0.022f
                drawLine(Color.White.copy(alpha = 0.4f), Offset(nx, h * hoopY + h * 0.012f), Offset(nx + w * 0.005f, h * hoopY + h * 0.05f), 1.5f)
            }

            // Rex player (large orange circle head + rectangular body)
            val px = w * playerX
            val py = h * playerY
            drawRect(Color(0xFFFF6B35), Offset(px - w * 0.06f, py - h * 0.09f), Size(w * 0.12f, h * 0.09f))
            drawCircle(Color(0xFFFF6B35), w * 0.065f, Offset(px, py - h * 0.115f))
            drawCircle(Color.White, w * 0.018f, Offset(px - w * 0.025f, py - h * 0.125f))
            drawCircle(Color.White, w * 0.018f, Offset(px + w * 0.025f, py - h * 0.125f))
            drawCircle(Color.Black, w * 0.010f, Offset(px - w * 0.025f, py - h * 0.125f))
            drawCircle(Color.Black, w * 0.010f, Offset(px + w * 0.025f, py - h * 0.125f))

            // Aim arrow (during AIM phase)
            if (phase == ShotPhase.AIM) {
                val rad = Math.toRadians((aimAngle - 90.0))
                val arrowLen = w * 0.18f
                val ex = px + (arrowLen * kotlin.math.cos(rad)).toFloat()
                val ey = (py - h * 0.12f) + (arrowLen * kotlin.math.sin(rad)).toFloat()
                drawLine(Color(0xFFFFD700), Offset(px, py - h * 0.12f), Offset(ex, ey), 4f, cap = StrokeCap.Round)
                drawCircle(Color(0xFFFFD700), 8f, Offset(ex, ey))
            }

            // Power bar (during POWER phase)
            if (phase == ShotPhase.POWER) {
                val barW = w * 0.55f
                val barH = h * 0.022f
                val barX = (w - barW) / 2f
                val barY = h * 0.92f
                drawRect(Color(0xFF333344), Offset(barX, barY), Size(barW, barH))
                val fillColor = when {
                    power < 0.4f -> Color(0xFF4DFF58)
                    power < 0.7f -> Color(0xFFFFD700)
                    else -> Color(0xFFFF3333)
                }
                drawRect(fillColor, Offset(barX, barY), Size(barW * power, barH))
                drawRect(Color.White.copy(alpha = 0.3f), Offset(barX, barY), Size(barW, barH), style = Stroke(2f))
            }

            // Ball in flight
            if (phase == ShotPhase.IN_FLIGHT || phase == ShotPhase.RESULT) {
                val bx = w * ballX
                val by = h * ballY
                val ballR = w * 0.035f
                drawCircle(Color(0xFFFF8C1A), ballR, Offset(bx, by))
                drawCircle(Color(0xFF8B4513).copy(alpha = 0.5f), ballR * 0.5f, Offset(bx, by))
            }
        }

        // HUD
        Row(Modifier.fillMaxWidth().padding(12.dp)) {
            PoplurText("Score: $totalScore", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            PoplurText("Shot $shotNum/$MAX_SHOTS", color = Color(0xFFFF6B35), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }

        // Wind indicator
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 36.dp)) {
            PoplurText(
                "Wind: ${if (windX > 0) "→" else if (windX < 0) "←" else "–"} ${String.format("%.0f", abs(windX) * 100)}%",
                color = Color.Cyan.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }

        // Phase instruction
        val instruction = when (phase) {
            ShotPhase.AIM -> "Tap to lock aim angle"
            ShotPhase.POWER -> "Tap to shoot!"
            ShotPhase.IN_FLIGHT -> ""
            ShotPhase.RESULT -> resultText
        }
        if (instruction.isNotEmpty()) {
            Box(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 12.dp)) {
                PoplurText(instruction, color = Color(0xFFFFD700), fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }

        if (isGameOver) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PoplurText("Game Over!", color = Color(0xFFFF6B35), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    PoplurText("Final Score: $totalScore / ${MAX_SHOTS * 3}", color = Color.White, fontSize = 20.sp)
                    Spacer(Modifier.height(24.dp))
                    Row {
                        Button(onClick = { isGameOver = false; restartKey++ }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B35)), shape = RoundedCornerShape(12.dp)) {
                            PoplurText("Play Again", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(16.dp))
                        Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333344)), shape = RoundedCornerShape(12.dp)) {
                            PoplurText("Exit", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
