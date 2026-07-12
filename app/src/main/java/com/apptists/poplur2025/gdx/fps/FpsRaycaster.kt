package com.apptists.poplur2025.gdx.fps

import kotlin.math.abs
import kotlin.math.floor

// Decoded sprite/texture: RGBA8888 pixels (0xRRGGBBAA) for fast CPU sampling.
class FpsImage(val w: Int, val h: Int, val px: IntArray) {
    fun pixel(x: Int, y: Int): Int {
        if (x < 0 || y < 0 || x >= w || y >= h) return 0
        return px[y * w + x]
    }
}

class FpsRenderSprite(
    val x: Double, val y: Double, val img: FpsImage?,
    val scale: Double = 1.0, val bright: Boolean = false, val alpha: Double = 1.0
)

// Software DDA raycaster (port of Raycaster.ts / FPSRenderer.swift scene pass).
// Writes RGBA8888 into `fb`, which PoplurFpsGdxGame uploads to a Texture each frame.
class FpsRaycaster(val w: Int, val h: Int) {
    val fb = IntArray(w * h)
    private val zbuf = DoubleArray(w)

    private fun pack(r: Int, g: Int, b: Int): Int =
        ((r and 0xFF) shl 24) or ((g and 0xFF) shl 16) or ((b and 0xFF) shl 8) or 0xFF

    fun renderScene(player: FpsPlayer, level: FpsLevel, tiles: List<FpsImage?>, sprites: List<FpsRenderSprite>) {
        drawFloorCeiling()
        drawWalls(player, level, tiles)
        drawSprites(player, sprites)
    }

    private fun drawFloorCeiling() {
        val half = h / 2
        for (y in 0 until half) {
            val t = y.toDouble() / half
            val r = (0x12 + (0x2a - 0x12) * t).toInt()
            val b = (0x20 + (0x45 - 0x20) * t).toInt()
            val c = pack(r, 0, b)
            val row = y * w
            for (x in 0 until w) fb[row + x] = c
        }
        for (y in half until h) {
            val t = (y - half).toDouble() / (h - half)
            val r = (0x0d + (0x05 - 0x0d) * t).toInt()
            val b = (0x18 + (0x08 - 0x18) * t).toInt()
            val c = pack(r, 0, b)
            val row = y * w
            for (x in 0 until w) fb[row + x] = c
        }
    }

    private fun texture(wallType: Int, isDoor: Boolean, tiles: List<FpsImage?>): FpsImage? {
        fun at(i: Int) = if (i in tiles.indices) tiles[i] else null
        if (isDoor || wallType == FpsK.WALL_DOOR) return at(9)
        if (wallType == FpsK.WALL_LOCKED) return at(10)
        if (wallType == FpsK.WALL_PINK) return at(11)
        return at(wallType)
    }

    private val wallColors = intArrayOf(0x000000, 0x4a1060, 0x1a3a6a, 0x6a2040, 0x2a1a5a, 0x5a3010, 0x103a30, 0x8a2060)

    private fun drawWalls(player: FpsPlayer, level: FpsLevel, tiles: List<FpsImage?>) {
        val posX = player.x; val posY = player.y
        val dirX = player.dirX; val dirY = player.dirY
        val planeX = player.planeX; val planeY = player.planeY
        val hh = h.toDouble(); val ww = w.toDouble()

        for (x in 0 until w) {
            val cameraX = 2 * x / ww - 1
            val rayDirX = dirX + planeX * cameraX
            val rayDirY = dirY + planeY * cameraX

            var mapX = floor(posX).toInt(); var mapY = floor(posY).toInt()
            val deltaDistX = if (rayDirX == 0.0) 1e30 else abs(1 / rayDirX)
            val deltaDistY = if (rayDirY == 0.0) 1e30 else abs(1 / rayDirY)

            var stepX: Int; var stepY: Int
            var sideDistX: Double; var sideDistY: Double
            if (rayDirX < 0) { stepX = -1; sideDistX = (posX - mapX) * deltaDistX }
            else { stepX = 1; sideDistX = (mapX + 1 - posX) * deltaDistX }
            if (rayDirY < 0) { stepY = -1; sideDistY = (posY - mapY) * deltaDistY }
            else { stepY = 1; sideDistY = (mapY + 1 - posY) * deltaDistY }

            var hit = false; var side = 0
            var perpWallDist = 0.0
            var wallType = 0
            var wallX = 0.0
            var isDoor = false
            var doorOpenAmt = 0.0

            while (!hit) {
                if (sideDistX < sideDistY) { sideDistX += deltaDistX; mapX += stepX; side = 0 }
                else { sideDistY += deltaDistY; mapY += stepY; side = 1 }

                val tile = level.getCell(mapX.toDouble(), mapY.toDouble())
                if (tile == 0) continue

                if (tile == FpsK.WALL_DOOR || tile == FpsK.WALL_LOCKED || tile == FpsK.WALL_PINK) {
                    val midDist = if (side == 0) sideDistX - deltaDistX * 0.5 else sideDistY - deltaDistY * 0.5
                    var dwx = if (side == 0) posY + midDist * rayDirY else posX + midDist * rayDirX
                    dwx -= floor(dwx)
                    val openAmt = level.getDoor(mapX.toDouble(), mapY.toDouble())?.open ?: 0.0
                    if (dwx < openAmt) continue
                    perpWallDist = midDist; wallX = dwx; wallType = tile
                    isDoor = true; doorOpenAmt = openAmt; hit = true; continue
                }

                perpWallDist = if (side == 0) sideDistX - deltaDistX else sideDistY - deltaDistY
                wallX = if (side == 0) posY + perpWallDist * rayDirY else posX + perpWallDist * rayDirX
                wallX -= floor(wallX)
                if (side == 0 && rayDirX > 0) wallX = 1 - wallX
                if (side == 1 && rayDirY < 0) wallX = 1 - wallX
                wallType = tile; hit = true
            }

            zbuf[x] = perpWallDist
            if (perpWallDist <= 0) continue

            val lineH = abs(hh / perpWallDist).toInt()
            val drawS = maxOf(0, h / 2 - lineH / 2)
            val drawE = minOf(h - 1, h / 2 + lineH / 2)
            if (drawE <= drawS) continue

            val tex = texture(wallType, isDoor, tiles)
            var texX = ((if (isDoor) wallX - doorOpenAmt else wallX) * FpsK.TEX_SIZE).toInt()
            texX = maxOf(0, minOf(FpsK.TEX_SIZE - 1, texX))

            val fog = minOf(0.88, perpWallDist / 16)
            val sideDark = if (side == 1) 0.28 else 0.0
            val keep = 1 - minOf(0.95, fog + sideDark)
            val strip = (drawE - drawS).toDouble()

            if (tex != null) {
                val tsx = minOf(tex.w - 1, texX * tex.w / FpsK.TEX_SIZE)
                val th = tex.h.toDouble()
                for (yy in drawS..drawE) {
                    val ty = ((yy - drawS) / strip * th).toInt()
                    val p = tex.pixel(tsx, minOf(tex.h - 1, ty))
                    if ((p and 0xFF) == 0) continue
                    val r = (((p ushr 24) and 0xFF) * keep).toInt()
                    val g = (((p ushr 16) and 0xFF) * keep).toInt()
                    val b = (((p ushr 8) and 0xFF) * keep).toInt()
                    fb[yy * w + x] = pack(r, g, b)
                }
            } else {
                val base = wallColors[wallType % wallColors.size]
                val r = (((base ushr 16) and 0xFF) * keep).toInt()
                val g = (((base ushr 8) and 0xFF) * keep).toInt()
                val b = ((base and 0xFF) * keep).toInt()
                val c = pack(r, g, b)
                for (yy in drawS..drawE) fb[yy * w + x] = c
            }
        }
    }

    private fun drawSprites(player: FpsPlayer, sprites: List<FpsRenderSprite>) {
        val sorted = sprites.sortedByDescending {
            (it.x - player.x) * (it.x - player.x) + (it.y - player.y) * (it.y - player.y)
        }
        val invDet = 1 / (player.planeX * player.dirY - player.dirX * player.planeY)
        val ww = w.toDouble(); val hh = h.toDouble()

        for (s in sorted) {
            val img = s.img ?: continue
            if (s.alpha <= 0) continue
            val sprX = s.x - player.x; val sprY = s.y - player.y
            val tx = invDet * (player.dirY * sprX - player.dirX * sprY)
            val ty = invDet * (-player.planeY * sprX + player.planeX * sprY)
            if (ty <= 0.05) continue

            val sprH = (abs(hh / ty) * s.scale).toInt()
            val sprW = sprH
            val scrX = ((ww / 2) * (1 + tx / ty)).toInt()
            val startX = maxOf(0, scrX - sprW / 2)
            val endX = minOf(w - 1, scrX + sprW / 2)
            val startY = maxOf(0, h / 2 - sprH / 2)
            val endY = minOf(h - 1, h / 2 + sprH / 2)
            if (endX <= startX || endY <= startY || sprW <= 0 || sprH <= 0) continue

            val iw = img.w; val ih = img.h
            for (sx in startX until endX) {
                if (zbuf[sx] <= ty) continue
                val texCol = minOf(iw - 1, maxOf(0, (sx - (scrX - sprW / 2)) * iw / sprW))
                for (sy in startY until endY) {
                    val texRow = minOf(ih - 1, maxOf(0, (sy - startY) * ih / (endY - startY)))
                    val p = img.pixel(texCol, texRow)
                    val pa = p and 0xFF
                    if (pa == 0) continue
                    var r = (p ushr 24) and 0xFF
                    var g = (p ushr 16) and 0xFF
                    var b = (p ushr 8) and 0xFF
                    if (s.bright) { r = minOf(255, r + 90); g = minOf(255, g + 20); b = minOf(255, b + 20) }
                    val idx = sy * w + sx
                    if (s.alpha < 1 || pa < 255) {
                        val a = s.alpha * pa / 255.0
                        val dst = fb[idx]
                        val dr = (dst ushr 24) and 0xFF; val dg = (dst ushr 16) and 0xFF; val db = (dst ushr 8) and 0xFF
                        r = (r * a + dr * (1 - a)).toInt()
                        g = (g * a + dg * (1 - a)).toInt()
                        b = (b * a + db * (1 - a)).toInt()
                    }
                    fb[idx] = pack(r, g, b)
                }
            }
        }
    }
}
