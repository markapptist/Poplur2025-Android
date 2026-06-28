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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptists.poplur2025.ui.games.PoplurCharacter
import com.apptists.poplur2025.ui.games.PoplurGameConfig
import kotlinx.coroutines.delay

private fun parseColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    Color.Gray
}

private data class Obstacle(var x: Float, val type: String) // rock, gap, enemy
private data class Coin(var x: Float, val y: Float)

private const val GRAVITY = 1800f
private const val JUMP_VEL = -680f
private const val GROUND_Y = 0.82f
private const val PLAYER_X = 0.20f
private const val CHAR_HALF = 0.045f

@Composable
fun PopRunnerGame(config: PoplurGameConfig, onDismiss: () -> Unit) {
    val character = remember { PoplurCharacter.fromId(config.character) }
    val skyColor = remember { parseColor(config.skyColor) }
    val groundColor = remember { parseColor(config.groundColor) }
    val accentColor = remember { parseColor(config.accentColor) }

    var playerY by remember { mutableFloatStateOf(GROUND_Y) }
    var playerVelY by remember { mutableFloatStateOf(0f) }
    var isGrounded by remember { mutableStateOf(true) }
    var jumpCount by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }
    var gameSpeed by remember { mutableFloatStateOf(config.speed) }
    var restartKey by remember { mutableIntStateOf(0) }

    val obstacles = remember(restartKey) { mutableStateListOf<Obstacle>() }
    val coins = remember(restartKey) { mutableStateListOf<Coin>() }

    fun jump() {
        if (isGameOver) return
        if (jumpCount < 2) {
            playerVelY = JUMP_VEL
            isGrounded = false
            jumpCount++
        }
    }

    fun resetGame() {
        playerY = GROUND_Y
        playerVelY = 0f
        isGrounded = true
        jumpCount = 0
        score = 0
        isGameOver = false
        gameSpeed = config.speed
        restartKey++
    }

    LaunchedEffect(restartKey) {
        var lastTime = System.nanoTime()
        var spawnTimer = 0f
        var coinTimer = 0f
        var speedTimer = 0f

        while (!isGameOver) {
            delay(16L)
            val now = System.nanoTime()
            val dt = ((now - lastTime) / 1_000_000_000f).coerceAtMost(0.05f)
            lastTime = now

            if (isGameOver) break

            // Physics
            playerVelY += GRAVITY * dt
            playerY += playerVelY * dt
            if (playerY >= GROUND_Y) {
                playerY = GROUND_Y
                playerVelY = 0f
                isGrounded = true
                jumpCount = 0
            }

            // Speed ramp
            speedTimer += dt
            if (speedTimer > 5f) {
                gameSpeed = (gameSpeed + 8f).coerceAtMost(520f)
                speedTimer = 0f
            }

            val normSpeed = gameSpeed / 280f

            // Move obstacles
            obstacles.replaceAll { it.copy(x = it.x - normSpeed * 0.35f * dt) }
            obstacles.removeAll { it.x < -0.15f }

            // Move coins
            coins.replaceAll { it.copy(x = it.x - normSpeed * 0.30f * dt) }
            coins.removeAll { it.x < -0.1f }

            // Spawn obstacles
            spawnTimer += dt
            if (spawnTimer > (1.4f / normSpeed)) {
                spawnTimer = 0f
                val types = listOf("rock", "rock", "enemy", "gap")
                obstacles.add(Obstacle(1.1f, types.random()))
            }

            // Spawn coins
            coinTimer += dt
            if (coinTimer > 0.8f) {
                coinTimer = 0f
                val coinY = GROUND_Y - 0.08f - (0..2).random() * 0.12f
                coins.add(Coin(1.1f, coinY))
            }

            // Collision with obstacles
            val playerRect = Rect(
                left = PLAYER_X - CHAR_HALF + 0.01f,
                top = playerY - CHAR_HALF * 2f,
                right = PLAYER_X + CHAR_HALF - 0.01f,
                bottom = playerY + 0.005f
            )
            for (obs in obstacles) {
                val obsRect = when (obs.type) {
                    "gap" -> Rect(obs.x - 0.04f, GROUND_Y - 0.01f, obs.x + 0.04f, GROUND_Y + 0.15f)
                    else  -> Rect(obs.x - 0.03f, GROUND_Y - 0.08f, obs.x + 0.03f, GROUND_Y + 0.01f)
                }
                if (playerRect.overlaps(obsRect)) {
                    isGameOver = true
                    break
                }
            }

            // Collect coins
            coins.removeAll { coin ->
                val coinRect = Rect(coin.x - 0.03f, coin.y - 0.03f, coin.x + 0.03f, coin.y + 0.03f)
                if (playerRect.overlaps(coinRect)) { score += 10; true } else false
            }

            score++
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(skyColor)
            .pointerInput(isGameOver) {
                detectTapGestures { if (!isGameOver) jump() }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Sky gradient
            drawRect(Brush.verticalGradient(listOf(skyColor, skyColor.copy(alpha = 0.6f), groundColor.copy(alpha = 0.3f))))

            // Hills parallax (bg)
            drawHills(w, h, Color(0xFF1E2A45), 0.68f, 3)
            // Hills (fg)
            drawHills(w, h, Color(0xFF0F1C35), 0.75f, 5)

            // Ground
            drawRect(
                color = groundColor,
                topLeft = Offset(0f, h * GROUND_Y),
                size = Size(w, h * (1f - GROUND_Y))
            )
            // Ground line
            drawLine(accentColor.copy(alpha = 0.5f), Offset(0f, h * GROUND_Y), Offset(w, h * GROUND_Y), 2f)

            // Character
            drawCharacter(character, Offset(w * PLAYER_X, h * playerY), w * CHAR_HALF, accentColor)

            // Obstacles
            for (obs in obstacles) {
                val ox = w * obs.x
                val gy = h * GROUND_Y
                when (obs.type) {
                    "rock" -> drawRoundRect(
                        color = Color(0xFF8B6914),
                        topLeft = Offset(ox - w * 0.03f, gy - h * 0.07f),
                        size = Size(w * 0.06f, h * 0.07f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f)
                    )
                    "gap" -> drawRect(
                        color = Color(0xFF050A14),
                        topLeft = Offset(ox - w * 0.04f, gy - 4f),
                        size = Size(w * 0.08f, h * 0.2f)
                    )
                    "enemy" -> {
                        drawCircle(Color(0xFFFF3333), w * 0.035f, Offset(ox, gy - h * 0.04f))
                        drawCircle(Color.White, w * 0.010f, Offset(ox - w * 0.012f, gy - h * 0.045f))
                        drawCircle(Color.White, w * 0.010f, Offset(ox + w * 0.012f, gy - h * 0.045f))
                        drawCircle(Color.Black, w * 0.006f, Offset(ox - w * 0.012f, gy - h * 0.045f))
                        drawCircle(Color.Black, w * 0.006f, Offset(ox + w * 0.012f, gy - h * 0.045f))
                    }
                }
            }

            // Coins
            for (coin in coins) {
                val cx = w * coin.x
                val cy = h * coin.y
                drawCircle(Color(0xFFFFD700), w * 0.022f, Offset(cx, cy))
                drawCircle(Color(0xFFFFF59D), w * 0.010f, Offset(cx, cy))
            }
        }

        // HUD
        PoplurText(
            "Score: $score",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        if (isGameOver) {
            GameOverOverlay(score = score, onPlayAgain = { resetGame() }, onExit = onDismiss, accentColor = accentColor)
        }
    }
}

private fun DrawScope.drawHills(w: Float, h: Float, color: Color, baseY: Float, count: Int) {
    val path = Path()
    path.moveTo(0f, h * baseY)
    val step = w / count
    for (i in 0..count) {
        val hx = i * step
        val hy = h * baseY - h * 0.08f
        path.quadraticTo(hx - step / 2f, hy, hx, h * baseY)
    }
    path.lineTo(w, h)
    path.lineTo(0f, h)
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawCharacter(char: PoplurCharacter, center: Offset, r: Float, accent: Color) {
    // Body
    drawRoundRect(
        color = char.primaryColor,
        topLeft = Offset(center.x - r, center.y - r * 1.1f),
        size = Size(r * 2f, r * 1.1f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.3f)
    )
    // Head
    drawCircle(char.primaryColor, r * 0.75f, Offset(center.x, center.y - r * 1.35f))
    // Eyes
    drawCircle(Color.White, r * 0.18f, Offset(center.x - r * 0.25f, center.y - r * 1.40f))
    drawCircle(Color.White, r * 0.18f, Offset(center.x + r * 0.25f, center.y - r * 1.40f))
    drawCircle(Color.Black, r * 0.10f, Offset(center.x - r * 0.22f, center.y - r * 1.40f))
    drawCircle(Color.Black, r * 0.10f, Offset(center.x + r * 0.22f, center.y - r * 1.40f))
    // Accent stripe
    drawRect(accent.copy(alpha = 0.6f), Offset(center.x - r, center.y - r * 0.7f), Size(r * 2f, r * 0.25f))
}

@Composable
private fun GameOverOverlay(score: Int, onPlayAgain: () -> Unit, onExit: () -> Unit, accentColor: Color) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PoplurText("Game Over", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            PoplurText("Score: $score", color = accentColor, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(24.dp))
            Row {
                Button(onClick = onPlayAgain, colors = ButtonDefaults.buttonColors(containerColor = accentColor), shape = RoundedCornerShape(12.dp)) {
                    PoplurText("Play Again", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(16.dp))
                Button(onClick = onExit, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333344)), shape = RoundedCornerShape(12.dp)) {
                    PoplurText("Exit", color = Color.White)
                }
            }
        }
    }
}
