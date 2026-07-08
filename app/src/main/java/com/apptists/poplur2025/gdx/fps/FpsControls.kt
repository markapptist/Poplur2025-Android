package com.apptists.poplur2025.gdx.fps

import com.badlogic.gdx.graphics.Color

// Orientation-aware on-screen controls.
//  - Landscape: translucent joystick + fire/interact floating over the fullscreen game.
//  - Portrait : an arcade control panel below the game "CRT" band, with a joystick and
//               the cabinet's coloured buttons (fire / use / weapon-switch).
// layout() defines the geometry once so draw() and input hit-testing stay in sync.

class FpsCtl(val cx: Float, val cy: Float, val r: Float) {
    fun contains(x: Float, y: Float): Boolean {
        val dx = x - cx; val dy = y - cy
        return dx * dx + dy * dy <= r * r
    }
}

class ControlLayout(
    val joy: FpsCtl,
    val fire: FpsCtl,
    val interact: FpsCtl,
    val weapons: List<FpsCtl>,
    val panelTop: Float,
    val marqueeBottom: Float,
)

object FpsControls {
    private val PINK = Color.valueOf("ff4db8")
    private val GOLD = Color.valueOf("ffd700")
    private val GREEN = Color.valueOf("00ff88")
    private val BLUE = Color.valueOf("4488ff")
    private val btnColors = listOf(GOLD, PINK, GREEN, BLUE, PINK)

    fun layout(sw: Float, sh: Float, portrait: Boolean,
               bandX: Float, bandY: Float, bandW: Float, bandH: Float): ControlLayout {
        if (portrait) {
            val panelTop = bandY + bandH
            val panelH = (sh - panelTop).coerceAtLeast(sh * 0.14f)
            val cy = panelTop + panelH * 0.5f
            val joyR = minOf(panelH * 0.34f, sw * 0.13f)
            val joy = FpsCtl(sw * 0.20f, cy, joyR)
            val btnR = joyR * 0.52f
            val bx0 = sw * 0.44f
            val gap = (sw * 0.90f - bx0) / 4f
            val weapons = (0 until 5).map { FpsCtl(bx0 + it * gap, cy, btnR) }
            return ControlLayout(joy, weapons[0], weapons[1], weapons, panelTop, bandY)
        } else {
            val joy = FpsCtl(sw * 0.13f, sh * 0.77f, sh * 0.14f)
            val fire = FpsCtl(sw * 0.90f, sh * 0.80f, sh * 0.12f)
            val interact = FpsCtl(sw * 0.75f, sh * 0.85f, sh * 0.075f)
            return ControlLayout(joy, fire, interact, emptyList(), 0f, 0f)
        }
    }

    fun draw(g: PoplurFpsGdxGame, lay: ControlLayout, portrait: Boolean, weapon: Int) {
        val sw = g.screenW(); val sh = g.screenH()
        if (portrait) {
            // Marquee above the CRT band
            g.fillRect(0f, 0f, sw, lay.marqueeBottom, 0.1f, 0f, 0.19f, 1f)
            g.fillRect(0f, lay.marqueeBottom - 2f, sw, 2f, GOLD.r, GOLD.g, GOLD.b, 1f)
            g.logoTexture()?.let {
                val ls = lay.marqueeBottom * 0.5f
                g.drawTex(it, 12f, (lay.marqueeBottom - ls) / 2, ls, ls)
                g.drawTex(it, sw - 12f - ls, (lay.marqueeBottom - ls) / 2, ls, ls)
            }
            g.text("poplur", sw / 2, lay.marqueeBottom * 0.28f, sw * 0.085f / 15f, GOLD, 0)

            // Control panel
            g.fillRect(0f, lay.panelTop, sw, sh - lay.panelTop, 0.08f, 0f, 0.14f, 1f)
            g.fillRect(0f, lay.panelTop, sw, 2f, GOLD.r, GOLD.g, GOLD.b, 1f)

            // Joystick
            g.drawCircle(lay.joy.cx, lay.joy.cy, lay.joy.r, Color.valueOf("1a1a26"), 1f)
            g.drawCircle(lay.joy.cx, lay.joy.cy, lay.joy.r * 0.55f, Color.valueOf("40404f"), 1f)

            // Cabinet buttons (0=FIRE, 1=USE, 2-4=weapon) — real neon panel sprites.
            val fireT = g.ctlTexture("fire")
            val useT = g.ctlTexture("use")
            val wpnT = g.ctlTexture("weapon")
            for ((i, b) in lay.weapons.withIndex()) {
                val side = b.r * 2.3f
                val bx = b.cx - side / 2f; val by = b.cy - side / 2f
                val tex = when (i) { 0 -> fireT; 1 -> useT; else -> wpnT }
                if (tex != null) {
                    if (i >= 2) {
                        val selected = weapon == (i - 2)
                        g.drawTexA(tex, bx, by, side, side, if (selected) 1f else 0.6f)
                        g.text("${i - 1}", b.cx, b.cy - side * 0.16f, side / 30f,
                               if (selected) GOLD else Color.WHITE, 0)
                    } else {
                        g.drawTexA(tex, bx, by, side, side, 1f)
                    }
                } else {
                    // Fallback: simple coloured circle.
                    val c = btnColors[i % btnColors.size]
                    g.drawCircle(b.cx, b.cy, b.r, c, 1f)
                }
            }
        } else {
            // Translucent floating controls
            g.drawCircle(lay.joy.cx, lay.joy.cy, lay.joy.r, Color.WHITE, 0.12f)
            g.drawCircle(lay.joy.cx, lay.joy.cy, lay.joy.r * 0.45f, PINK, 0.5f)
            g.drawCircle(lay.fire.cx, lay.fire.cy, lay.fire.r, PINK, 0.35f)
            g.drawCircle(lay.fire.cx, lay.fire.cy, lay.fire.r * 0.6f, PINK, 0.5f)
            g.drawCircle(lay.interact.cx, lay.interact.cy, lay.interact.r, BLUE, 0.35f)
        }
    }
}
