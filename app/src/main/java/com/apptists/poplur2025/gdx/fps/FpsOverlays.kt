package com.apptists.poplur2025.gdx.fps

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Matrix4
import kotlin.math.floor

// SpriteBatch overlay renderers (HUD + arcade menu), drawn in the game's y-down
// 640x360 space using PoplurFpsGdxGame's public draw helpers.

private val PINK = Color.valueOf("ff4db8")
private val GOLD = Color.valueOf("ffd700")
private val GREEN = Color.valueOf("00ff88")
private val RED = Color.valueOf("ff3333")

object FpsHudRenderer {
    fun draw(g: PoplurFpsGdxGame) {
        val W = g.widthI().toFloat(); val H = g.heightI().toFloat()
        val p = g.playerRef()
        val barH = 56f

        g.fillRect(0f, H - barH, W, barH, 0.04f, 0f, 0.08f, 0.88f)
        g.fillRect(0f, H - barH, W, 1.5f, PINK.r, PINK.g, PINK.b, 1f)

        g.hudTexture("face")?.let { g.drawTex(it, W / 2 - 23, H - 46 - 6 + 2, 46f, 46f) }

        // Health
        g.hudTexture("health")?.let { g.drawTex(it, 8f, H - barH + 6, 18f, 18f) }
        val hc = if (p.health > 50) GREEN else if (p.health > 25) Color.valueOf("ffaa00") else RED
        g.text("${p.health.toInt()}", 30f, H - barH + 6, 1.4f, hc, -1)
        g.fillRect(8f, H - barH + 28, 70f, 6f, 0.07f, 0.07f, 0.07f, 1f)
        g.fillRect(8f, H - barH + 28, 70f * (p.health / p.maxHealth).toFloat(), 6f, hc.r, hc.g, hc.b, 1f)

        // Armor
        val ax = 90f
        g.hudTexture("armor")?.let { g.drawTex(it, ax, H - barH + 6, 18f, 18f) }
        g.text("${p.armor.toInt()}", ax + 22, H - barH + 6, 1.4f, Color.valueOf("88aaff"), -1)
        g.fillRect(ax, H - barH + 28, 70f, 6f, 0.07f, 0.07f, 0.07f, 1f)
        g.fillRect(ax, H - barH + 28, 70f * (p.armor / p.maxArmor).toFloat(), 6f, 0.27f, 0.53f, 1f, 1f)

        // Weapon + ammo
        val wDef = FPS_WEAPONS[p.weapon]
        val wx = 175f
        g.hudTexture("ammo")?.let { g.drawTex(it, wx, H - barH + 6, 18f, 18f) }
        g.text(wDef.name, wx, H - barH + 30, 0.9f, GOLD, -1)
        val ammoLeft = p.ammo[wDef.ammoType]
        g.text("$ammoLeft", wx + 22, H - barH + 6, 1.4f, if (ammoLeft > 10) GOLD else RED, -1)

        // Weapon slots
        val slotX0 = W - 5 * 38 - 8
        for (i in 0 until 5) {
            val sx = slotX0 + i * 38
            val sy = H - 52
            if (i == p.weapon) g.fillRect(sx, sy, 34f, 30f, 0.7f, 0f, 0.35f, 0.7f)
            else g.fillRect(sx, sy, 34f, 30f, 0.08f, 0f, 0.16f, 0.6f)
            g.text("${i + 1}", sx + 3, sy + 2, 0.67f, if (i == p.weapon) GOLD else Color.valueOf("888888"), -1)
            g.text(FPS_WEAPONS[i].name.take(4), sx + 2, sy + 15, 0.5f, Color.valueOf("aaaaaa"), -1)
        }

        // Keys
        var kx = W - 5 * 38 - 8 - 28
        if (p.hasKey["gold"] == true) { g.keyTexture("gold")?.let { g.drawTex(it, kx, H - 50, 22f, 22f) }; kx -= 26 }
        if (p.hasKey["pink"] == true) { g.keyTexture("pink")?.let { g.drawTex(it, kx, H - 50, 22f, 22f) } }

        // Top bar
        g.fillRect(0f, 0f, W, 22f, 0.04f, 0f, 0.08f, 0.7f)
        g.text("SCORE: ${g.scoreVal().toString().padStart(7, '0')}", 8f, 2f, 0.87f, GOLD, -1)
        val t = g.gameTimeVal()
        g.text(String.format("%02d:%02d", (t / 60).toInt(), (t % 60).toInt()), W / 2 - 20, 2f, 0.87f, PINK, -1)
        g.text("KILLS: ${g.killsVal()}", W - 90, 2f, 0.87f, Color.valueOf("aaffdd"), -1)

        drawMinimap(g)

        if (p.flashAlpha > 0) g.fillRect(0f, 0f, W, H, 0.78f, 0f, 0f, (p.flashAlpha * 0.35).toFloat())

        // Crosshair
        val cx = W / 2; val cy = H / 2
        g.fillRect(cx - 8, cy, 16f, 1f, 1f, 1f, 1f, 0.7f)
        g.fillRect(cx, cy - 8, 1f, 16f, 1f, 1f, 1f, 0.7f)
    }

    private fun drawMinimap(g: PoplurFpsGdxGame) {
        val W = g.widthI().toFloat()
        val level = g.levelRef(); val p = g.playerRef()
        val mapSize = 80f; val tSize = 3f
        val mX = W - mapSize - 6; val mY = 26f
        val pMapX = floor(p.x).toInt(); val pMapY = floor(p.y).toInt()
        g.fillRect(mX, mY, mapSize, mapSize, 0.04f, 0f, 0.08f, 0.75f)
        val tilesVisible = (mapSize / tSize).toInt()
        val startTX = pMapX - tilesVisible / 2; val startTY = pMapY - tilesVisible / 2
        for (ty in 0 until tilesVisible) for (tx in 0 until tilesVisible) {
            val mx = startTX + tx; val my = startTY + ty
            val cell = level.getCell(mx.toDouble(), my.toDouble())
            if (cell == 0) continue
            val door = level.getDoor(mx.toDouble(), my.toDouble())
            when {
                (door?.open ?: 0.0) >= 0.5 -> g.fillRect(mX + tx * tSize, mY + ty * tSize, tSize - 1, tSize - 1, 0.2f, 0.2f, 0.27f, 1f)
                cell == 7 -> g.fillRect(mX + tx * tSize, mY + ty * tSize, tSize - 1, tSize - 1, 0.67f, 0.4f, 0f, 1f)
                cell == 8 || cell == 9 -> g.fillRect(mX + tx * tSize, mY + ty * tSize, tSize - 1, tSize - 1, 1f, 0.27f, 0f, 1f)
                else -> g.fillRect(mX + tx * tSize, mY + ty * tSize, tSize - 1, tSize - 1, 0.25f, 0.2f, 0.4f, 1f)
            }
        }
        val pdx = (pMapX - startTX) * tSize + (p.x - pMapX).toFloat() * tSize
        val pdy = (pMapY - startTY) * tSize + (p.y - pMapY).toFloat() * tSize
        g.fillRect(mX + pdx - 2, mY + pdy - 2, 4f, 4f, PINK.r, PINK.g, PINK.b, 1f)
    }
}

object FpsMenuRenderer {
    fun draw(g: PoplurFpsGdxGame, t: Double) {
        val W = g.widthI().toFloat(); val H = g.heightI().toFloat()

        // Dark room background (no zoom)
        g.fillRect(0f, 0f, W, H, 0.03f, 0f, 0.055f, 1f)

        // Cabinet layout
        val cw = W * 0.46f
        val cabL = (W - cw) / 2
        val cabTop = H * 0.03f
        val ch = H * 0.95f
        val marqH = H * 0.14f
        val marqY = cabTop + H * 0.02f
        val screenW = cw * 0.80f
        val screenX = cabL + (cw - screenW) / 2
        val screenY = marqY + marqH + H * 0.05f
        val screenH = screenW * 0.82f

        // Zoom transform (0 = full cabinet, 1 = into the CRT)
        val scaleTarget = W / screenW
        val txTarget = W / 2 - (screenX + screenW / 2) * scaleTarget
        val tyTarget = H / 2 - (screenY + screenH / 2) * scaleTarget
        val tt = t.coerceIn(0.0, 1.0).toFloat()
        val ease = if (tt < 1) tt * tt * (3 - 2 * tt) else 1f
        val sc = 1 + (scaleTarget - 1) * ease
        val tx = txTarget * ease
        val ty = tyTarget * ease
        val m = Matrix4().translate(tx, ty, 0f).scale(sc, sc, 1f)
        g.batch().transformMatrix = m

        // Cabinet body + gold frame
        g.fillRect(cabL - 3, cabTop + 23, cw + 6, ch - 20, 0.78f, 0.59f, 0.04f, 1f)
        g.fillRect(cabL, cabTop + 26, cw, ch - 26, 0.16f, 0.03f, 0.22f, 1f)

        // Marquee
        g.fillRect(cabL, marqY, cw, marqH, 0.1f, 0f, 0.19f, 1f)
        val logoSz = marqH * 0.62f
        g.logoTexture()?.let {
            g.drawTex(it, cabL + 8, marqY + (marqH - logoSz) / 2, logoSz, logoSz)
            g.drawTex(it, cabL + cw - 8 - logoSz, marqY + (marqH - logoSz) / 2, logoSz, logoSz)
        }
        g.text("poplur", cabL + cw / 2, marqY + marqH * 0.25f, cw * 0.15f / 15f, GOLD, 0)

        // CRT bezel + screen
        g.fillRect(screenX - 12, screenY - 12, screenW + 24, screenH + 24, 0.04f, 0f, 0.07f, 1f)
        g.menuTexture()?.let { g.drawTex(it, screenX, screenY, screenW, screenH) }
            ?: run { g.fillRect(screenX, screenY, screenW, screenH, 0.06f, 0f, 0.19f, 1f)
                     g.text("START GAME", screenX + screenW / 2, screenY + screenH * 0.45f, screenW * 0.11f / 15f, GOLD, 0) }
        if ((g.menuTickVal() * 2).toInt() % 2 == 0)
            g.fillRect(screenX, screenY + screenH * 0.60f, screenW, screenH * 0.16f, 1f, 0.84f, 0f, 0.10f)

        // Controls bar + buttons
        val ctrlY = screenY + screenH + 22
        g.fillRect(cabL + 6, ctrlY, cw - 12, 46f, 0.05f, 0f, 0.13f, 1f)
        val btnColors = listOf(GOLD, PINK, GREEN, Color.valueOf("4488ff"), PINK)
        for ((i, c) in btnColors.withIndex()) {
            val bx = cabL + 76 + i * 34
            g.fillRect(bx - 9, ctrlY + 12, 18f, 18f, c.r, c.g, c.b, 1f)
        }

        g.batch().transformMatrix = Matrix4()
    }
}
