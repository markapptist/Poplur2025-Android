package com.apptists.poplur2025.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.Log
import com.mapbox.bindgen.DataRef
import com.mapbox.maps.Image
import com.mapbox.maps.Style
import java.nio.ByteBuffer

/**
 * Generates theme-aware marker bitmaps (location pin, landmark pin) and
 * registers them with the active Mapbox Style.
 *
 * The existing MapScreen.kt draws its own pin bitmaps with hard-coded colors.
 * This renderer provides an enhanced alternative used by the theme engine. To
 * activate it, call [registerThemeImages] after the style loads and the theme
 * has been applied — it replaces the images for the same image IDs that
 * MapScreen.kt uses, so the existing symbol layers pick them up automatically.
 *
 * Call this after [PoplurMapThemeManager.applyCurrentTheme] so the colors are
 * already correct.
 */
object MarkerRenderer {

    private const val TAG       = "MarkerRenderer"
    private const val PIN_SIZE  = 168   // px — must match MapScreen.kt createGdxPin size
    private const val GLOW_SIZE = 172   // slightly larger for avatar/business markers

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Builds and registers themed pin bitmaps into [style].
     * Safe to call any time after the style is loaded.
     */
    fun registerThemeImages(style: Style, theme: MapTheme) {
        style.safe("register-pin-images") {
            val locPin  = buildLocationPin(theme)
            val lmkPin  = buildLandmarkPin(theme)
            addImage(BLUE_PIN_IMAGE_ID, locPin.toMapboxImage())
            addImage(PINK_PIN_IMAGE_ID, lmkPin.toMapboxImage())
        }
    }

    // ── Location pin ─────────────────────────────────────────────────────────

    /**
     * Draws a rounded-teardrop location pin with a glowing base circle and a
     * spire. Colors are derived from the current theme's location pin accent.
     */
    fun buildLocationPin(theme: MapTheme): Bitmap {
        val c = theme.colors
        val base    = Color.parseColor(c.locationPin)
        val midTone = blendWith(base, Color.WHITE, 0.40f)
        val glow    = Color.argb(110, Color.red(base), Color.green(base), Color.blue(base))
        return buildPin(PinColors(base, midTone, Color.WHITE, glow))
    }

    /**
     * Draws the landmark / post pin (pink/purple accent family).
     */
    fun buildLandmarkPin(theme: MapTheme): Bitmap {
        val c = theme.colors
        val base    = Color.parseColor(c.landmarkPin)
        val midTone = blendWith(base, Color.WHITE, 0.40f)
        val glow    = Color.argb(118, Color.red(base), Color.green(base), Color.blue(base))
        return buildPin(PinColors(base, midTone, Color.WHITE, glow))
    }

    // ── Core pin renderer ────────────────────────────────────────────────────

    private data class PinColors(
        val base: Int,
        val mid: Int,
        val highlight: Int,
        val glow: Int,
    )

    private fun buildPin(p: PinColors): Bitmap {
        val size   = PIN_SIZE
        val bmp    = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx     = size / 2f
        val cy     = size / 2f
        val paint  = Paint(Paint.ANTI_ALIAS_FLAG)

        // ── Outer glow halo ────────────────────────────────────────────────
        paint.xfermode = null
        paint.shader = RadialGradient(
            cx, cy, size * 0.46f,
            intArrayOf(p.glow, Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, size * 0.46f, paint)
        paint.shader = null

        // ── Ring layers (depth) ────────────────────────────────────────────
        val rings = listOf(0.40f to 0.60f, 0.32f to 0.78f, 0.24f to 0.90f)
        rings.forEach { (radius, alpha) ->
            paint.color = Color.argb((alpha * 255).toInt(), Color.red(p.base), Color.green(p.base), Color.blue(p.base))
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = size * 0.018f
            canvas.drawCircle(cx, cy, size * radius, paint)
        }

        // ── Solid base circle ──────────────────────────────────────────────
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            cx - size * 0.07f, cy - size * 0.07f, size * 0.28f,
            intArrayOf(p.mid, p.base),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, size * 0.26f, paint)
        paint.shader = null

        // ── Inner white highlight ──────────────────────────────────────────
        paint.color = Color.argb(160, 255, 255, 255)
        canvas.drawCircle(cx - size * 0.06f, cy - size * 0.08f, size * 0.08f, paint)

        // ── Spire ──────────────────────────────────────────────────────────
        val spireTop    = cy + size * 0.26f
        val spireBottom = cy + size * 0.46f
        val spireHalf   = size * 0.055f
        val spirePath   = Path().apply {
            moveTo(cx - spireHalf, spireTop)
            lineTo(cx + spireHalf, spireTop)
            lineTo(cx, spireBottom)
            close()
        }
        paint.shader = LinearGradient(
            cx, spireTop, cx, spireBottom,
            intArrayOf(p.mid, p.base),
            null, Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.FILL
        canvas.drawPath(spirePath, paint)
        paint.shader = null

        // Left facet highlight on spire
        val leftFacet = Path().apply {
            moveTo(cx - spireHalf, spireTop)
            lineTo(cx, spireTop + (spireBottom - spireTop) * 0.35f)
            lineTo(cx, spireBottom)
            close()
        }
        paint.color = Color.argb(55, 255, 255, 255)
        canvas.drawPath(leftFacet, paint)

        return bmp
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Linear blend between two ARGB colors. fraction=0 → a, fraction=1 → b */
    private fun blendWith(a: Int, b: Int, fraction: Float): Int {
        val inv = 1f - fraction
        return Color.argb(
            (Color.alpha(a) * inv + Color.alpha(b) * fraction).toInt(),
            (Color.red(a)   * inv + Color.red(b)   * fraction).toInt(),
            (Color.green(a) * inv + Color.green(b) * fraction).toInt(),
            (Color.blue(a)  * inv + Color.blue(b)  * fraction).toInt(),
        )
    }

    private fun Bitmap.toMapboxImage(): Image {
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)
        val buffer = ByteBuffer.allocateDirect(pixels.size * 4)
        pixels.forEach { pixel ->
            buffer.put(Color.red(pixel).toByte())
            buffer.put(Color.green(pixel).toByte())
            buffer.put(Color.blue(pixel).toByte())
            buffer.put(Color.alpha(pixel).toByte())
        }
        buffer.flip()
        return Image(width, height, DataRef(buffer))
    }

    private inline fun Style.safe(label: String, block: Style.() -> Unit) {
        runCatching { block() }.onFailure {
            Log.w(TAG, "[$label] ${it.message}")
        }
    }
}
