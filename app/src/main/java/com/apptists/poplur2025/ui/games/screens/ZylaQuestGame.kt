package com.apptists.poplur2025.ui.games.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.apptists.poplur2025.ui.components.PoplurText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptists.poplur2025.ui.games.PoplurCharacter
import com.apptists.poplur2025.ui.games.PoplurGameConfig
import kotlinx.coroutines.delay

private val ROOMS = listOf(
    arrayOf(
        "#############",
        "#P..........#",
        "#.###.###...#",
        "#.#K#.#.#...#",
        "#.###.#.####.",
        "#.....#..E...",
        "#.#####.####.",
        "#...E.......#",
        "#############"
    ),
    arrayOf(
        "#############",
        "#...E.......#",
        "#.#########.#",
        "#.#.......#.#",
        "#.#.#####.#.#",
        "#...#.K.#...#",
        "###.#.###.###",
        "#P..E.....D.#",
        "#############"
    ),
    arrayOf(
        "#############",
        "#P.........D#",
        "#.#.#######.#",
        "#.#.#...#...#",
        "#.#.#.E.#.###",
        "#.#.#...#...#",
        "#.#.#####.#.#",
        "#...K...E...#",
        "#############"
    )
)

private const val COLS = 13
private const val ROWS = 9

private data class EnemyPos(var col: Int, var row: Int)

@Composable
fun ZylaQuestGame(config: PoplurGameConfig, onDismiss: () -> Unit) {
    val character = remember { PoplurCharacter.fromId(config.character) }
    val accentColor = remember {
        try { Color(android.graphics.Color.parseColor(config.accentColor)) } catch (e: Exception) { Color(0xFF2ECC71) }
    }

    var currentRoom by remember { mutableIntStateOf(0) }
    var playerCol by remember { mutableIntStateOf(1) }
    var playerRow by remember { mutableIntStateOf(1) }
    var hasKey by remember { mutableStateOf(false) }
    var health by remember { mutableIntStateOf(3) }
    var score by remember { mutableIntStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }
    var isVictory by remember { mutableStateOf(false) }
    var showMessage by remember { mutableStateOf("") }

    // Mutable grid for current room (tracks picked-up keys/opened doors)
    var roomGrid by remember(currentRoom) {
        mutableStateOf(ROOMS[currentRoom].map { it.toMutableList() }.toMutableList())
    }

    val enemies = remember(currentRoom) {
        mutableStateListOf<EnemyPos>().also { list ->
            ROOMS[currentRoom].forEachIndexed { r, row ->
                row.forEachIndexed { c, ch ->
                    if (ch == 'E') list.add(EnemyPos(c, r))
                }
            }
        }
    }

    // Find player start
    LaunchedEffect(currentRoom) {
        ROOMS[currentRoom].forEachIndexed { r, row ->
            row.forEachIndexed { c, ch ->
                if (ch == 'P') { playerCol = c; playerRow = r }
            }
        }
        hasKey = false
        showMessage = ""
    }

    // Enemy move timer
    LaunchedEffect(currentRoom, isGameOver) {
        while (!isGameOver && !isVictory) {
            delay(1000L)
            // Each enemy steps toward player
            for (e in enemies) {
                val dc = (playerCol - e.col).coerceIn(-1, 1)
                val dr = (playerRow - e.row).coerceIn(-1, 1)
                // Prefer horizontal
                val newCol = e.col + if (dc != 0) dc else 0
                val newRow = e.row + if (dc == 0 && dr != 0) dr else 0
                val tile = roomGrid.getOrNull(newRow)?.getOrNull(newCol)
                if (tile != null && tile != '#' && tile != 'D') {
                    e.col = newCol
                    e.row = newRow
                }
                if (e.col == playerCol && e.row == playerRow) {
                    health--
                    showMessage = "-1 HP!"
                    if (health <= 0) { isGameOver = true }
                }
            }
        }
    }

    fun tryMove(dc: Int, dr: Int) {
        if (isGameOver || isVictory) return
        val nc = playerCol + dc
        val nr = playerRow + dr
        val tile = roomGrid.getOrNull(nr)?.getOrNull(nc) ?: return
        when {
            tile == '#' -> return
            tile == 'D' && !hasKey -> { showMessage = "Need a key!"; return }
            tile == 'D' && hasKey -> {
                // Open door, advance room
                if (currentRoom + 1 >= ROOMS.size) {
                    isVictory = true
                } else {
                    currentRoom++
                    score += 100
                }
            }
            tile == 'K' -> {
                hasKey = true
                roomGrid[nr][nc] = '.'
                playerCol = nc; playerRow = nr
                showMessage = "Key collected!"
                score += 50
            }
            else -> {
                playerCol = nc; playerRow = nr
                showMessage = ""
            }
        }
    }

    fun attack() {
        if (isGameOver || isVictory) return
        val adjacent = enemies.filter { e ->
            kotlin.math.abs(e.col - playerCol) <= 1 && kotlin.math.abs(e.row - playerRow) <= 1
        }
        if (adjacent.isNotEmpty()) {
            enemies.removeAll(adjacent)
            score += 50 * adjacent.size
            showMessage = "Hit! +${50 * adjacent.size}"
        } else {
            showMessage = "Miss!"
        }
    }

    val tileW = remember { mutableStateOf(1f) }
    val tileH = remember { mutableStateOf(1f) }

    Box(Modifier.fillMaxSize().background(Color(0xFF0A0A12))) {
        Column(Modifier.fillMaxSize()) {
            // HUD
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                PoplurText("Score: $score", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                PoplurText(if (hasKey) "🗝 Key" else "Room ${currentRoom + 1}/3", color = if (hasKey) Color(0xFFFFD700) else accentColor, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                PoplurText("♥".repeat(health.coerceAtLeast(0)), color = Color(0xFFFF3355), fontSize = 15.sp)
            }

            if (showMessage.isNotEmpty()) {
                PoplurText(showMessage, color = Color(0xFFFFD700), fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp))
            }

            // Game canvas
            Canvas(Modifier.fillMaxWidth().weight(1f)) {
                val w = size.width
                val h = size.height
                val tw = w / COLS
                val th = h / ROWS
                tileW.value = tw
                tileH.value = th

                roomGrid.forEachIndexed { r, row ->
                    row.forEachIndexed { c, ch ->
                        val tx = c * tw
                        val ty = r * th
                        val tileColor = when (ch) {
                            '#' -> Color(0xFF333344)
                            'D' -> Color(0xFFD4AF37)
                            'K' -> Color(0xFF1a1a2e)
                            else -> Color(0xFF1A1A2E)
                        }
                        drawRect(tileColor, Offset(tx, ty), Size(tw - 1f, th - 1f))
                        if (ch == 'K') {
                            drawCircle(Color(0xFFFFD700), tw * 0.25f, Offset(tx + tw / 2f, ty + th / 2f))
                        }
                        if (ch == 'D') {
                            drawRect(Color(0xFFFFD700).copy(alpha = 0.5f), Offset(tx + tw * 0.3f, ty), Size(tw * 0.4f, th))
                        }
                    }
                }

                // Draw enemies
                for (e in enemies) {
                    val ex = e.col * tw + tw / 2f
                    val ey = e.row * th + th / 2f
                    drawCircle(Color(0xFFFF3333), tw * 0.35f, Offset(ex, ey))
                    drawCircle(Color.White, tw * 0.10f, Offset(ex - tw * 0.12f, ey - tw * 0.10f))
                    drawCircle(Color.White, tw * 0.10f, Offset(ex + tw * 0.12f, ey - tw * 0.10f))
                }

                // Draw player
                val px = playerCol * tw + tw / 2f
                val py = playerRow * th + th / 2f
                drawCircle(character.primaryColor, tw * 0.38f, Offset(px, py))
                drawCircle(Color.White, tw * 0.12f, Offset(px - tw * 0.13f, py - tw * 0.12f))
                drawCircle(Color.White, tw * 0.12f, Offset(px + tw * 0.13f, py - tw * 0.12f))
                drawCircle(character.accentColor, tw * 0.08f, Offset(px - tw * 0.13f, py - tw * 0.12f))
                drawCircle(character.accentColor, tw * 0.08f, Offset(px + tw * 0.13f, py - tw * 0.12f))
            }

            // Controls
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // D-pad
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DPadBtn("▲") { tryMove(0, -1) }
                    Row {
                        DPadBtn("◄") { tryMove(-1, 0) }
                        Spacer(Modifier.width(40.dp))
                        DPadBtn("►") { tryMove(1, 0) }
                    }
                    DPadBtn("▼") { tryMove(0, 1) }
                }

                // Attack
                Button(
                    onClick = { attack() },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    PoplurText("⚔", fontSize = 22.sp, textAlign = TextAlign.Center)
                }
            }
        }

        if (isGameOver) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PoplurText("Defeated!", color = Color(0xFFFF3355), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    PoplurText("Score: $score", color = Color.White, fontSize = 20.sp)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = accentColor), shape = RoundedCornerShape(12.dp)) {
                        PoplurText("Exit", color = Color.White)
                    }
                }
            }
        }

        if (isVictory) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PoplurText("Victory!", color = Color(0xFFFFD700), fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    PoplurText("Final Score: $score", color = Color.White, fontSize = 20.sp)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = accentColor), shape = RoundedCornerShape(12.dp)) {
                        PoplurText("Exit", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun DPadBtn(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A3A)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.size(44.dp)
    ) {
        PoplurText(label, fontSize = 16.sp, color = Color.White, textAlign = TextAlign.Center)
    }
}
