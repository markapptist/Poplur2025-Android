package com.apptists.poplur2025.ui.games.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptists.poplur2025.ui.components.PoplurText
import com.apptists.poplur2025.ui.games.PoplurCharacter
import com.apptists.poplur2025.ui.games.PoplurGameConfig
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

private data class BlastEnemy(val x: Float, val y: Float, val speed: Float, val size: Float)
private data class BlastBolt(val x: Float, val y: Float)

private fun parseStellaColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    Color(0xFF00F5FF)
}

@Composable
fun StellaBlastGame(config: PoplurGameConfig, onDismiss: () -> Unit) {
    val character = remember { PoplurCharacter.fromId(config.character) }
    val accentColor = remember { parseStellaColor(config.accentColor) }
    val shipColor = remember { character.primaryColor }
    val enemies = remember { mutableStateListOf<BlastEnemy>() }
    val bolts = remember { mutableStateListOf<BlastBolt>() }

    var shipX by remember { mutableFloatStateOf(0.5f) }
    var score by remember { mutableIntStateOf(0) }
    var shields by remember { mutableIntStateOf(3) }
    var restartKey by remember { mutableIntStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }
    var spawnTimer by remember { mutableFloatStateOf(0f) }
    var fireTimer by remember { mutableFloatStateOf(0f) }

    fun fire() {
        if (!isGameOver) {
            bolts.add(BlastBolt(shipX, 0.78f))
        }
    }

    fun resetGame() {
        enemies.clear()
        bolts.clear()
        shipX = 0.5f
        score = 0
        shields = 3
        spawnTimer = 0f
        fireTimer = 0f
        isGameOver = false
        restartKey++
    }

    LaunchedEffect(restartKey) {
        var lastTime = System.nanoTime()
        while (!isGameOver) {
            delay(16L)
            val now = System.nanoTime()
            val dt = ((now - lastTime) / 1_000_000_000f).coerceAtMost(0.05f)
            lastTime = now

            spawnTimer += dt
            fireTimer += dt
            if (fireTimer >= 0.26f) {
                fireTimer = 0f
                fire()
            }

            val spawnEvery = (0.92f - score * 0.003f).coerceAtLeast(0.34f)
            if (spawnTimer >= spawnEvery) {
                spawnTimer = 0f
                enemies.add(
                    BlastEnemy(
                        x = Random.nextFloat().coerceIn(0.08f, 0.92f),
                        y = -0.06f,
                        speed = Random.nextDouble(0.12, 0.30).toFloat() + score * 0.0008f,
                        size = Random.nextDouble(0.032, 0.055).toFloat()
                    )
                )
            }

            for (i in enemies.indices.reversed()) {
                val enemy = enemies[i]
                val moved = enemy.copy(y = enemy.y + enemy.speed * dt)
                if (moved.y > 0.92f) {
                    enemies.removeAt(i)
                    shields -= 1
                    if (shields <= 0) {
                        isGameOver = true
                    }
                } else {
                    enemies[i] = moved
                }
            }

            for (i in bolts.indices.reversed()) {
                val moved = bolts[i].copy(y = bolts[i].y - 0.72f * dt)
                if (moved.y < -0.05f) {
                    bolts.removeAt(i)
                } else {
                    bolts[i] = moved
                }
            }

            val hitBolts = mutableSetOf<Int>()
            val hitEnemies = mutableSetOf<Int>()
            enemies.forEachIndexed { enemyIndex, enemy ->
                bolts.forEachIndexed { boltIndex, bolt ->
                    if (
                        boltIndex !in hitBolts &&
                        enemyIndex !in hitEnemies &&
                        abs(bolt.x - enemy.x) < enemy.size &&
                        abs(bolt.y - enemy.y) < enemy.size
                    ) {
                        hitBolts.add(boltIndex)
                        hitEnemies.add(enemyIndex)
                        score += 25
                    }
                }
            }
            hitBolts.sortedDescending().forEach { bolts.removeAt(it) }
            hitEnemies.sortedDescending().forEach { enemies.removeAt(it) }
        }
    }

    val shipPath = remember { Path() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050713))
            .pointerInput(restartKey, isGameOver) {
                detectTapGestures { fire() }
            }
            .pointerInput(restartKey, isGameOver) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    if (!isGameOver) {
                        shipX = (shipX + dragAmount.x / size.width.toFloat()).coerceIn(0.08f, 0.92f)
                    }
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF050713), Color(0xFF111B3D), Color(0xFF050713))
                )
            )

            repeat(36) { index ->
                val x = ((index * 37) % 100) / 100f * w
                val y = ((index * 53) % 100) / 100f * h
                drawCircle(Color.White.copy(alpha = 0.18f + (index % 4) * 0.08f), 1.5f + (index % 3), Offset(x, y))
            }

            bolts.forEach { bolt ->
                drawRoundRect(
                    color = accentColor,
                    topLeft = Offset(w * bolt.x - 3f, h * bolt.y - 22f),
                    size = Size(6f, 26f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
            }

            enemies.forEach { enemy ->
                val center = Offset(w * enemy.x, h * enemy.y)
                drawCircle(Color(0xFFFF4B7D).copy(alpha = 0.28f), w * enemy.size * 1.7f, center)
                drawCircle(Color(0xFFFF4B7D), w * enemy.size, center)
                drawCircle(Color.White.copy(alpha = 0.78f), w * enemy.size * 0.28f, center + Offset(w * enemy.size * 0.22f, -w * enemy.size * 0.2f))
            }

            val shipCenter = Offset(w * shipX, h * 0.84f)
            shipPath.reset()
            shipPath.apply {
                moveTo(shipCenter.x, shipCenter.y - h * 0.055f)
                lineTo(shipCenter.x - w * 0.052f, shipCenter.y + h * 0.052f)
                lineTo(shipCenter.x, shipCenter.y + h * 0.020f)
                lineTo(shipCenter.x + w * 0.052f, shipCenter.y + h * 0.052f)
                close()
            }
            drawPath(shipPath, shipColor)
            drawCircle(accentColor.copy(alpha = 0.72f), w * 0.018f, shipCenter + Offset(0f, h * 0.02f))
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PoplurText("Score $score", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
            PoplurText("Shields $shields", color = accentColor, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }

        if (isGameOver) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PoplurText("Blast Complete", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                PoplurText("Score $score", color = accentColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GameButton("Replay", accentColor) { resetGame() }
                    GameButton("Exit", Color.White.copy(alpha = 0.18f)) { onDismiss() }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GameButton("Left", Color.White.copy(alpha = 0.18f)) {
                shipX = (shipX - 0.10f).coerceAtLeast(0.08f)
            }
            Spacer(Modifier.width(10.dp))
            GameButton("Fire", accentColor) { fire() }
            Spacer(Modifier.width(10.dp))
            GameButton("Right", Color.White.copy(alpha = 0.18f)) {
                shipX = (shipX + 0.10f).coerceAtMost(0.92f)
            }
        }
    }
}

@Composable
private fun GameButton(label: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = if (color == Color.White.copy(alpha = 0.18f)) Color.White else Color.Black
        )
    ) {
        PoplurText(label, fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}
