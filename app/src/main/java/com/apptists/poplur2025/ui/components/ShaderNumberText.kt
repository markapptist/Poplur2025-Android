package com.apptists.poplur2025.ui.components

import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import com.apptists.poplur2025.ui.GdxState
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidFragmentApplication
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.graphics.Color as GdxColor

/**
 * Animated LibGDX shader text with the adaptive hue-detection glow ported from
 * PoplurNumberShaders.metal.
 */
@Composable
fun ShaderNumberText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
    lineHeight: TextUnit = TextUnit.Unspecified,
    intensity: Float = 1f,
    glowColor: Color = Color(0xFFFF8A1C),
    autoSize: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val context = LocalContext.current
    val isInspectionMode = LocalInspectionMode.current
    val activity = context as? FragmentActivity
    val density = LocalDensity.current
    val transition = rememberInfiniteTransition(label = "poplur-number-shader")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "poplur-number-shader-phase"
    )

    // Safety: Fallback if too many instances or on low-end devices
    val shaderCount = GdxState.activeShaderTextCount.get()
    val useFallback = activity == null || isInspectionMode || shaderCount > 6

    if (useFallback) {
        FallbackShaderNumberText(
            text = text,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            modifier = modifier,
            lineHeight = lineHeight,
            intensity = intensity,
            glowColor = glowColor,
            autoSize = autoSize,
            maxLines = maxLines,
            softWrap = softWrap,
            overflow = overflow
        )
        return
    }

    DisposableEffect(Unit) {
        GdxState.activeShaderTextCount.incrementAndGet()
        onDispose { GdxState.activeShaderTextCount.decrementAndGet() }
    }

    val fontDp = with(density) { fontSize.toDpOrDefault(24.dp) }
    val lineHeightDp = with(density) {
        if (lineHeight == TextUnit.Unspecified) fontSize.toDpOrDefault(fontDp * 1.08f)
        else lineHeight.toDpOrDefault(fontDp * 1.08f)
    }
    val estimatedWidth = estimateShaderTextWidth(
        text = text,
        fontDp = fontDp,
        autoSize = autoSize
    )
    val containerId = remember { View.generateViewId() }

    Box(
        modifier = Modifier
            .width(estimatedWidth)
            .height(lineHeightDp.coerceAtLeast(12.dp))
            .then(modifier)
    ) {
        AndroidView(
            modifier = Modifier
                .width(estimatedWidth)
                .height(lineHeightDp.coerceAtLeast(12.dp)),
            factory = { viewContext ->
                FragmentContainerView(viewContext).apply {
                    id = containerId
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    background = null

                    val fragment = GdxShaderNumberFragment().apply {
                        updateText(
                            text = text,
                            color = color,
                            intensity = intensity,
                            phase = phase,
                            fontScale = fontDp.value,
                            lineHeight = lineHeightDp.value
                        )
                    }
                    activity.supportFragmentManager.beginTransaction()
                        .replace(id, fragment)
                        .commitAllowingStateLoss()
                }
            },
            update = { view ->
                view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                view.background = null
                (activity.supportFragmentManager.findFragmentById(containerId) as? GdxShaderNumberFragment)
                    ?.updateText(
                        text = text,
                        color = color,
                        intensity = intensity,
                        phase = phase,
                        fontScale = fontDp.value,
                        lineHeight = lineHeightDp.value
                    )
            }
        )
    }
}

private fun estimateShaderTextWidth(
    text: String,
    fontDp: Dp,
    autoSize: Boolean
): Dp {
    val perCharacter = if (autoSize) 0.58f else 0.66f
    val raw = fontDp.value * (text.length.coerceAtLeast(1) * perCharacter) + 10f
    return raw.coerceAtLeast(fontDp.value + 8f).dp
}

private fun TextUnit.toDpOrDefault(defaultValue: Dp): Dp {
    return if (isSp) value.dp else defaultValue
}

@Composable
private fun FallbackShaderNumberText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    modifier: Modifier,
    lineHeight: TextUnit,
    intensity: Float,
    glowColor: Color,
    autoSize: Boolean,
    maxLines: Int,
    softWrap: Boolean,
    overflow: TextOverflow
) {
    val transition = rememberInfiniteTransition(label = "poplur-number-shader-fallback")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "poplur-number-shader-fallback-time"
    )
    val brush = remember(color, time) {
        Brush.linearGradient(
            colors = listOf(color, Color(0xFFFFA12B), Color(0xFF54D8FF), color),
            start = Offset.Zero,
            end = Offset.Infinite
        )
    }
    val onLayout: ((TextLayoutResult) -> Unit)? = if (autoSize) ({ }) else null

    Text(
        text = text,
        modifier = modifier,
        maxLines = if (autoSize) 1 else maxLines,
        softWrap = if (autoSize) false else softWrap,
        overflow = if (autoSize) TextOverflow.Visible else overflow,
        onTextLayout = onLayout,
        style = TextStyle(
            brush = brush,
            fontSize = fontSize,
            fontWeight = fontWeight,
            lineHeight = lineHeight,
            shadow = Shadow(
                color = glowColor.copy(alpha = 0.32f * intensity),
                offset = Offset.Zero,
                blurRadius = 9f * intensity
            )
        )
    )
}

class GdxShaderNumberFragment : AndroidFragmentApplication() {
    private val adapter = GdxShaderNumberAdapter()

    fun updateText(
        text: String,
        color: Color,
        intensity: Float,
        phase: Float,
        fontScale: Float,
        lineHeight: Float
    ) {
        adapter.update(
            text = text,
            color = color.toGdxColor(),
            intensity = intensity,
            phase = phase,
            fontScale = fontScale,
            lineHeight = lineHeight
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useImmersiveMode = false
            useGL30 = false
            r = 8
            g = 8
            b = 8
            a = 8
        }

        val view = initializeForView(adapter, config)
        view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        view.background = null
        view.isClickable = false
        view.isFocusable = false
        view.setOnTouchListener { _, _ -> false }

        if (view is GLSurfaceView) {
            view.setZOrderOnTop(true)
            view.holder.setFormat(PixelFormat.TRANSLUCENT)
        }
        if (view is SurfaceView) {
            view.setZOrderOnTop(true)
            view.holder.setFormat(PixelFormat.TRANSLUCENT)
        }

        return view
    }
}

private class GdxShaderNumberAdapter : ApplicationAdapter() {
    @Volatile private var text = ""
    @Volatile private var color = GdxColor.WHITE
    @Volatile private var intensity = 1f
    @Volatile private var phase = 0f
    @Volatile private var requestedFontScale = 24f
    @Volatile private var requestedLineHeight = 28f
    @Volatile private var dirty = false

    private var camera: OrthographicCamera? = null
    private var batch: SpriteBatch? = null
    private var font: BitmapFont? = null
    private var layout: GlyphLayout? = null
    private var shader: ShaderProgram? = null
    private var appInstance: com.badlogic.gdx.Application? = null

    fun update(
        text: String,
        color: GdxColor,
        intensity: Float,
        phase: Float,
        fontScale: Float,
        lineHeight: Float
    ) {
        this.text = text
        this.color = color
        this.intensity = intensity
        this.phase = phase
        this.requestedFontScale = fontScale
        this.requestedLineHeight = lineHeight
        this.dirty = true
        appInstance?.postRunnable { Gdx.graphics?.requestRendering() }
    }

    override fun create() {
        appInstance = Gdx.app
        Gdx.graphics.setContinuousRendering(false)
        camera = OrthographicCamera()
        batch = SpriteBatch()
        font = BitmapFont().apply {
            data.markupEnabled = false
            setUseIntegerPositions(false)
        }
        layout = GlyphLayout()
        shader = ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER).also { program ->
            if (!program.isCompiled) {
                Gdx.app.error("GdxShaderNumber", program.log)
            }
        }
        resetCamera()
    }

    override fun resize(width: Int, height: Int) {
        resetCamera()
    }

    override fun render() {
        val width = Gdx.graphics.width.toFloat().coerceAtLeast(1f)
        val height = Gdx.graphics.height.toFloat().coerceAtLeast(1f)
        val activeFont = font ?: return
        val activeBatch = batch ?: return
        val activeLayout = layout ?: return
        val activeShader = shader?.takeIf { it.isCompiled }

        Gdx.gl.glViewport(0, 0, width.toInt(), height.toInt())
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        try {
            val scale = ((height * 0.82f) / activeFont.lineHeight).coerceAtLeast(0.1f)
            activeFont.data.setScale(scale)
            activeFont.color = color
            activeLayout.setText(activeFont, text)

            val x = (width - activeLayout.width) * 0.5f
            val y = (height + activeLayout.height) * 0.5f

            activeBatch.projectionMatrix = camera?.combined
            activeBatch.shader = activeShader
            activeBatch.begin()
            if (activeShader != null) {
                if (activeShader.hasUniform("u_time")) activeShader.setUniformf("u_time", phase)
                if (activeShader.hasUniform("u_intensity")) activeShader.setUniformf("u_intensity", intensity)
                if (activeShader.hasUniform("u_size")) activeShader.setUniformf("u_size", width, height)
                if (activeShader.hasUniform("u_baseColor")) activeShader.setUniformf("u_baseColor", color.r, color.g, color.b, color.a)
            }

            drawGlow(activeBatch, activeFont, activeLayout, x, y, width, height)
            activeFont.color = color
            activeFont.draw(activeBatch, activeLayout, x, y)
            activeBatch.end()
        } catch (e: Exception) {
            // Ignore potential LibGDX internal drawing errors for one frame
            if (activeBatch.isDrawing) activeBatch.end()
        }
    }

    private fun drawGlow(
        activeBatch: SpriteBatch,
        activeFont: BitmapFont,
        activeLayout: GlyphLayout,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        val glowAlpha = (0.18f * intensity).coerceIn(0f, 0.42f)
        val offsets = floatArrayOf(-2.2f, 0f, 2.2f)
        activeFont.color = GdxColor(color.r, color.g, color.b, glowAlpha)
        offsets.forEach { dx ->
            offsets.forEach { dy ->
                if (dx != 0f || dy != 0f) {
                    activeFont.draw(activeBatch, activeLayout, x + dx * width / 140f, y + dy * height / 64f)
                }
            }
        }
    }

    private fun resetCamera() {
        val width = Gdx.graphics.width.toFloat().coerceAtLeast(1f)
        val height = Gdx.graphics.height.toFloat().coerceAtLeast(1f)
        camera?.setToOrtho(false, width, height)
        camera?.update()
    }

    override fun dispose() {
        batch?.dispose()
        font?.dispose()
        shader?.dispose()
    }
}

private fun Color.toGdxColor(): GdxColor = GdxColor(red, green, blue, alpha)

private const val VERTEX_SHADER = """
attribute vec4 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;

uniform mat4 u_projTrans;

varying vec4 v_color;
varying vec2 v_texCoords;

void main() {
    v_color = a_color;
    v_texCoords = a_texCoord0;
    gl_Position = u_projTrans * a_position;
}
"""

private const val FRAGMENT_SHADER = """
#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_texture;
uniform vec2 u_size;
uniform float u_time;
uniform float u_intensity;
uniform vec4 u_baseColor;

void main() {
    vec4 sampled = texture2D(u_texture, v_texCoords);
    float alpha = sampled.a * v_color.a * u_baseColor.a;
    if (alpha <= 0.01) {
        discard;
    }

    vec2 position = gl_FragCoord.xy;
    float shimmer = sin(position.x * 0.08 + position.y * 0.035 + u_time * 3.4) * 0.5 + 0.5;
    float scan = sin(position.y * 1.65 + u_time * 8.0) * 0.5 + 0.5;
    float pulse = sin(u_time * 2.2) * 0.5 + 0.5;

    float r = u_baseColor.r;
    float g = u_baseColor.g;
    float b = u_baseColor.b;
    float cmax = max(r, max(g, b));
    float cmin = min(r, min(g, b));
    float delta = cmax - cmin;

    float hue = 0.0;
    if (delta > 0.001) {
        if (cmax == r) {
            hue = mod((g - b) / delta + 6.0, 6.0);
        } else if (cmax == g) {
            hue = 2.0 + (b - r) / delta;
        } else {
            hue = 4.0 + (r - g) / delta;
        }
        hue /= 6.0;
    }

    float lum = dot(vec3(r, g, b), vec3(0.2126, 0.7152, 0.0722));
    float isRed = clamp((1.0 - smoothstep(0.0, 0.10, hue)) + smoothstep(0.90, 1.0, hue), 0.0, 1.0);
    float isGreen = smoothstep(0.25, 0.38, hue) * (1.0 - smoothstep(0.38, 0.50, hue));
    float isBlue = smoothstep(0.55, 0.65, hue) * (1.0 - smoothstep(0.65, 0.78, hue));
    float isNeutral = smoothstep(0.72, 0.90, lum) * (1.0 - isRed) * (1.0 - isGreen) * (1.0 - isBlue);
    float isOther = clamp(1.0 - isRed - isGreen - isBlue - isNeutral, 0.0, 1.0);

    vec3 glowA = isRed * vec3(1.00, 0.42, 0.06)
        + isGreen * vec3(0.65, 1.00, 0.10)
        + isBlue * vec3(0.40, 0.12, 1.00)
        + isNeutral * vec3(0.82, 0.88, 1.00)
        + isOther * vec3(1.00, 0.62, 0.18);

    vec3 glowB = isRed * vec3(1.00, 0.10, 0.28)
        + isGreen * vec3(0.18, 1.00, 0.55)
        + isBlue * vec3(0.12, 0.72, 1.00)
        + isNeutral * vec3(0.18, 0.75, 1.00)
        + isOther * vec3(0.18, 0.82, 1.00);

    vec3 hot = mix(glowA, glowB, shimmer * 0.55);
    float heat = (0.22 + shimmer * 0.38 + scan * 0.10 + pulse * 0.10) * u_intensity;
    vec3 base = vec3(r, g, b);
    vec3 boosted = mix(base, max(base, hot * max(lum, 0.42)), heat);
    boosted += scan * 0.055 * u_intensity;

    gl_FragColor = vec4(boosted, alpha);
}
"""
