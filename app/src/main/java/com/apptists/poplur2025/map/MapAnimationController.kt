package com.apptists.poplur2025.map

import android.content.Context
import android.graphics.Color
import android.util.Log
import com.mapbox.bindgen.Value
import com.mapbox.maps.Style
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Drives per-theme animated effects on Mapbox layer paint properties.
 *
 * Three animation modes are supported (from [MapTheme.AnimationType]):
 *
 *   PULSE          — slow sin-wave breathing of emissive strength on the post circle
 *                    layer. Used for most themes.
 *
 *   RAINBOW_CYCLE  — continuous hue rotation on the post circle color. Full HSV
 *                    sweep at ~15 seconds / revolution. Used by Rainbow theme.
 *
 *   GLITTER_SPARKLE — rapid random neon stroke-color cycling plus fast emissive
 *                     pulse. Used by Glitter theme.
 *
 *   NONE           — no animation. Static paint values from [applyThemeImmediate].
 *
 * API:
 *   start(theme, style, scope)  — launch the animation coroutine for [theme]
 *   stop()                      — cancel animation and clear references
 *   pause()                     — suppress layer updates (idle battery saving)
 *   resume()                    — re-enable layer updates
 */
class MapAnimationController {

    @Volatile private var isPaused  = false
    private var animJob: Job?       = null
    private var liveStyle: Style?   = null

    // ── Public API ────────────────────────────────────────────────────────────

    fun start(theme: MapTheme, style: Style, scope: CoroutineScope) {
        stop()
        liveStyle = style
        isPaused  = false
        animJob   = when (theme.styleConfig.animationType) {
            MapTheme.AnimationType.RAINBOW_CYCLE   -> scope.launch { rainbowLoop(style) }
            MapTheme.AnimationType.GLITTER_SPARKLE -> scope.launch { glitterLoop(style) }
            MapTheme.AnimationType.PULSE           -> scope.launch { pulseLoop(style, theme) }
            MapTheme.AnimationType.NONE            -> null
        }
    }

    fun stop() {
        animJob?.cancel()
        animJob   = null
        liveStyle = null
    }

    fun initialize(context: Context) { /* reserved for future preload work */ }

    fun setRenderingActive(active: Boolean) { if (active) resume() else pause() }

    fun pause()  { isPaused = true  }

    fun resume() { isPaused = false }

    // ── Animation loops ───────────────────────────────────────────────────────

    /** ~1 full hue revolution per 15 s at 60 fps increments of 0.4 °/frame */
    private suspend fun rainbowLoop(style: Style) {
        var hue = 0f
        while (true) {
            if (!isPaused) {
                style.safe("rainbow") {
                    if (styleLayerExists(POST_LAYER_ID)) {
                        setStyleLayerProperty(POST_LAYER_ID, "circle-color",            Value.valueOf(hsvHex(hue)))
                        setStyleLayerProperty(POST_LAYER_ID, "circle-stroke-color",     Value.valueOf(hsvHex((hue + 180f) % 360f)))
                        setStyleLayerProperty(POST_LAYER_ID, "circle-emissive-strength",Value.valueOf(1.2))
                    }
                }
            }
            hue = (hue + 0.4f) % 360f
            delay(16)   // ~60 fps
        }
    }

    /**
     * Cycles through a neon palette for the post stroke every ~80 ms;
     * pulses emissive strength with a fast sin wave.
     */
    private suspend fun glitterLoop(style: Style) {
        var frame = 0
        var paletteIndex = 0
        while (true) {
            if (!isPaused) {
                val emissive = 0.9 + 0.9 * (sin(frame * 0.18) * 0.5 + 0.5)   // 0.9..1.8
                val strokeColor = NEON_PALETTE[paletteIndex]
                style.safe("glitter") {
                    if (styleLayerExists(POST_LAYER_ID)) {
                        setStyleLayerProperty(POST_LAYER_ID, "circle-stroke-color",     Value.valueOf(strokeColor))
                        setStyleLayerProperty(POST_LAYER_ID, "circle-emissive-strength",Value.valueOf(emissive))
                    }
                }
            }
            // Advance palette color every 5 frames (~80 ms)
            if (frame % 5 == 0) paletteIndex = (paletteIndex + 1) % NEON_PALETTE.size
            frame++
            delay(16)   // ~60 fps
        }
    }

    /**
     * Slow breathing emissive pulse on the post circle layer.
     * Oscillation period is ~4 s; applied at ~30 fps.
     */
    private suspend fun pulseLoop(style: Style, theme: MapTheme) {
        var frame = 0
        val baseEmissive = theme.colors.postEmissive
        while (true) {
            if (!isPaused) {
                val pulse    = (sin(frame * 0.05) * 0.5 + 0.5).toFloat()    // 0..1
                val emissive = baseEmissive * (0.55 + 0.45 * pulse)          // 55%..100% of base
                style.safe("pulse") {
                    if (styleLayerExists(POST_LAYER_ID))
                        setStyleLayerProperty(POST_LAYER_ID, "circle-emissive-strength", Value.valueOf(emissive))
                }
            }
            frame++
            delay(33)   // ~30 fps — sufficient for a slow pulse
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Converts [hue] (0–360) to a CSS hex color string at high saturation/value. */
    private fun hsvHex(hue: Float): String {
        val color = Color.HSVToColor(floatArrayOf(hue, 0.88f, 0.96f))
        return String.format("#%06X", 0xFFFFFF and color)
    }

    private inline fun Style.safe(label: String, block: Style.() -> Unit) {
        runCatching { block() }.onFailure {
            Log.w(TAG, "[$label] ${it.message}")
        }
    }

    companion object {
        private const val TAG = "MapAnimationController"

        // Neon palette for glitter sparkle — warm, cool, and vivid tones
        private val NEON_PALETTE = listOf(
            "#FF00FF",   // magenta
            "#00FFFF",   // cyan
            "#FF6EFF",   // hot pink
            "#00FF88",   // neon green
            "#FF9500",   // neon orange
            "#FFFF00",   // yellow
            "#A78BFA",   // violet
            "#38BDF8",   // sky
            "#F472B6",   // pink
            "#4ADE80",   // lime
        )
    }
}
