package com.apptists.poplur2025.ui

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import kotlin.math.abs
import kotlin.math.roundToInt
import android.util.Log

class CombinedGdxAdapter(
    private val initialTabIndex: Int = 1,
    private val onVerticalSwipe: ((Boolean) -> Unit)? = null,
    private val onLongPress: (() -> Unit)? = null,
    private val onTap: (() -> Unit)? = null,
    private val onTabSelected: (Int) -> Unit
) : ApplicationAdapter() {

    private var camera: PerspectiveCamera? = null
    private var modelBatch: ModelBatch? = null
    private var model: Model? = null
    private var instance: ModelInstance? = null
    private var environment: Environment? = null

    private var currentYawDegrees = -90f * initialTabIndex
    private var targetYawDegrees = currentYawDegrees
    private var startYawDegrees = currentYawDegrees
    private var animationTime = 0f
    private var bouncePhase = BouncePhase.Idle

    private var dragging = false
    private var isVerticalSwipe = false
    private var isSwipeTriggered = false
    private var isRotation = false
    private var longPressSent = false
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var touchStartMillis = 0L

    private val textures = mutableListOf<Texture>()
    private var font: BitmapFont? = null

    override fun create() {
        try {
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
            Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
            Gdx.gl.glEnable(GL20.GL_BLEND)
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

            val w = Gdx.graphics.width.toFloat().coerceAtLeast(1f)
            val h = Gdx.graphics.height.toFloat().coerceAtLeast(1f)
            camera = PerspectiveCamera(40f, w, h).apply {
                position.set(0f, 0f, 5f)
                lookAt(0f, 0f, 0f)
                near = 0.1f
                far = 100f
                update()
            }

            modelBatch = ModelBatch()
            font = BitmapFont().apply {
                data.setScale(2.5f)
                setUseIntegerPositions(false)
            }

            val materials = buildFaceMaterials()
            model = buildCube(materials)
            instance = ModelInstance(model)

            environment = Environment().apply {
                set(ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f))
                add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))
            }

            Gdx.input.inputProcessor = object : InputAdapter() {
                override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                    dragging = true; isVerticalSwipe = false; isSwipeTriggered = false; isRotation = false; longPressSent = false; bouncePhase = BouncePhase.Idle; downX = screenX.toFloat(); downY = screenY.toFloat(); lastX = screenX.toFloat(); touchStartMillis = System.currentTimeMillis(); return true
                }
                override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                    if (!dragging) return true
                    val dx = screenX.toFloat() - downX; val dy = screenY.toFloat() - downY
                    if (abs(dx) > 10f || abs(dy) > 10f) longPressSent = true
                    if (!isVerticalSwipe && !isRotation) {
                        if (abs(dy) > 15f && abs(dy) > abs(dx) * 1.5f) isVerticalSwipe = true else if (abs(dx) > 15f) isRotation = true
                    }
                    if (isVerticalSwipe && !isSwipeTriggered && abs(dy) > 40f) {
                        isSwipeTriggered = true; Gdx.app.postRunnable { onVerticalSwipe?.invoke(dy < 0) }
                    }
                    if (isRotation) {
                        val deltaX = screenX.toFloat() - lastX; currentYawDegrees += deltaX * 0.8f; targetYawDegrees = currentYawDegrees
                    }
                    lastX = screenX.toFloat(); return true
                }
                override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                    dragging = false
                    if (isVerticalSwipe && !isSwipeTriggered) Gdx.app.postRunnable { onVerticalSwipe?.invoke((screenY - downY) < 0) }
                    val moved = abs(screenX - downX) > 15f || abs(screenY - downY) > 15f
                    if (!moved && !longPressSent) {
                        Gdx.app.postRunnable { onTap?.invoke() }
                        return true
                    }
                    val snappedIndex = snappedTabIndex(currentYawDegrees)
                    startBounceTo(-90f * snappedIndex)
                    if (isRotation || abs(screenX - downX) > 15f) Gdx.app.postRunnable { onTabSelected(snappedIndex) }
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e("CombinedGdxAdapter", "CRITICAL ERROR in create()", e)
        }
    }

    private fun buildFaceMaterials(): List<Material> {
        return listOf("Record", "Feed", "Profile", "Map").map { title ->
            val texture = createFaceTexture(title)
            textures += texture
            Material(
                TextureAttribute.createDiffuse(texture),
                ColorAttribute.createDiffuse(Color.WHITE),
                BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
            )
        }
    }

    private fun buildCube(materials: List<Material>): Model {
        val mb = ModelBuilder()
        val attr = (Usage.Position or Usage.Normal or Usage.TextureCoordinates).toLong()
        val s = 0.65f
        mb.begin()
        mb.part("f", GL20.GL_TRIANGLES, attr, materials[0]).rect(-s,-s,s, s,-s,s, s,s,s, -s,s,s, 0f,0f,1f)
        mb.part("r", GL20.GL_TRIANGLES, attr, materials[1]).rect(s,-s,s, s,-s,-s, s,s,-s, s,s,s, 1f,0f,0f)
        mb.part("bk", GL20.GL_TRIANGLES, attr, materials[2]).rect(s,-s,-s, -s,-s,-s, -s,s,-s, s,s,-s, 0f,0f,-1f)
        mb.part("l", GL20.GL_TRIANGLES, attr, materials[3]).rect(-s,-s,-s, -s,-s,s, -s,s,s, -s,s,-s, -1f,0f,0f)
        mb.part("t", GL20.GL_TRIANGLES, attr, materials[1]).rect(-s,s,s, s,s,s, s,s,-s, -s,s,-s, 0f,1f,0f)
        mb.part("bm", GL20.GL_TRIANGLES, attr, materials[2]).rect(-s,-s,-s, s,-s,-s, s,-s,s, -s,-s,s, 0f,-1f,0f)
        return mb.end()
    }

    private fun createFaceTexture(title: String): Texture {
        val size = 384
        val fbo = FrameBuffer(Pixmap.Format.RGBA8888, size, size, false)
        val projection = Matrix4().setToOrtho2D(0f, 0f, size.toFloat(), size.toFloat())
        val sr = ShapeRenderer().apply { setAutoShapeType(true) }
        val batch = SpriteBatch()
        
        fbo.begin()
        Gdx.gl.glViewport(0, 0, size, size)
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        sr.projectionMatrix = projection
        sr.begin(ShapeRenderer.ShapeType.Filled)
        
        // Soft rounded panel base
        val r = 92f
        val pad = 20f
        sr.color = Color(0.06f, 0.07f, 0.16f, 1f)
        sr.filledRoundRect(pad, pad, size - pad*2, size - pad*2, r)
        
        // Quilted Dots
        sr.set(ShapeRenderer.ShapeType.Filled)
        val step = 64f
        for (ix in 0..6) {
            for (iy in 0..6) {
                val px = ix * step; val py = iy * step
                if (px > pad + 38f && px < size - pad - 38f && py > pad + 38f && py < size - pad - 38f) {
                    sr.color = Color(0f, 0f, 0f, 0.5f); sr.circle(px + 1.5f, py - 1.5f, 4f, 10)
                    sr.color = Color(0.48f, 0.45f, 0.8f, 0.65f); sr.circle(px, py, 3.5f, 10)
                }
            }
        }

        // Inner icon area
        sr.color = Color(0.06f, 0.07f, 0.14f, 0.96f)
        sr.filledRoundRect(pad + 52f, pad + 52f, size - pad*2 - 104f, size - pad*2 - 104f, r - 32f)
        
        sr.color = Color(0.7f, 0.68f, 1f, 1f)
        val cx = size/2f; val cy = size * 0.64f; val isize = 88f
        when(title) {
            "Record" -> { sr.circle(cx, cy, isize*0.35f, 40); sr.color = Color(0.06f, 0.07f, 0.14f, 1f); sr.rect(cx-isize*0.06f, cy-isize*0.2f, isize*0.12f, isize*0.4f); sr.rect(cx-isize*0.2f, cy-isize*0.06f, isize*0.4f, isize*0.12f) }
            "Feed" -> { repeat(3) { i -> val y = cy + (i-1)*isize*0.23f; sr.circle(cx-isize*0.28f, y, isize*0.06f, 20); sr.rect(cx-isize*0.12f, y-isize*0.04f, isize*0.46f, isize*0.08f) } }
            "Profile" -> { sr.circle(cx, cy+isize*0.15f, isize*0.18f, 32); sr.filledRoundRect(cx-isize*0.35f, cy-isize*0.35f, isize*0.7f, isize*0.35f, isize*0.18f) }
            "Map" -> { val b = isize*0.1f; sr.rectLine(cx-isize*0.34f, cy-isize*0.28f, cx-isize*0.34f, cy+isize*0.26f, b); sr.rectLine(cx-isize*0.34f, cy+isize*0.26f, cx, cy-isize*0.04f, b); sr.rectLine(cx, cy-isize*0.04f, cx+isize*0.34f, cy+isize*0.26f, b); sr.rectLine(cx+isize*0.34f, cy+isize*0.26f, cx+isize*0.34f, cy-isize*0.28f, b) }
        }
        sr.end()

        batch.projectionMatrix = projection
        batch.begin()
        font?.let { f -> f.setColor(Color(0.88f, 0.86f, 1f, 1f)); val l = GlyphLayout(f, title); f.draw(batch, title, (size - l.width)/2f, size * 0.285f) }
        batch.end()

        val pixmap = Pixmap.createFromFrameBuffer(0, 0, size, size)
        fbo.end()
        val upright = Pixmap(size, size, Pixmap.Format.RGBA8888)
        for (y in 0 until size) upright.drawPixmap(pixmap, 0, size - y - 1, size, 1, 0, y, size, 1)
        val tex = Texture(upright)
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        sr.dispose(); batch.dispose(); pixmap.dispose(); upright.dispose(); fbo.dispose()
        return tex
    }

    private fun ShapeRenderer.filledRoundRect(x: Float, y: Float, w: Float, h: Float, r: Float) {
        val rr = r.coerceAtMost(w/2f).coerceAtMost(h/2f)
        rect(x+rr, y, w-2*rr, h); rect(x, y+rr, w, h-2*rr)
        circle(x+rr, y+rr, rr, 32); circle(x+w-rr, y+rr, rr, 32)
        circle(x+rr, y+h-rr, rr, 32); circle(x+w-rr, y+h-rr, rr, 32)
    }

    private fun startBounceTo(target: Float) {
        startYawDegrees = currentYawDegrees; targetYawDegrees = target; animationTime = 0f; bouncePhase = BouncePhase.Overshoot
    }

    private fun updateAnimation() {
        if (dragging && !longPressSent && System.currentTimeMillis() - touchStartMillis >= 800L) {
            longPressSent = true; Gdx.app.postRunnable { onLongPress?.invoke() }
        }
        when (bouncePhase) {
            BouncePhase.Idle -> Unit
            BouncePhase.Overshoot -> {
                animationTime += Gdx.graphics.deltaTime
                val progress = (animationTime / 0.18f).coerceIn(0f, 1f)
                val over = targetYawDegrees + if (targetYawDegrees >= startYawDegrees) 6.9f else -6.9f
                currentYawDegrees = Interpolation.sine.apply(startYawDegrees, over, progress)
                if (progress >= 1f) { startYawDegrees = currentYawDegrees; animationTime = 0f; bouncePhase = BouncePhase.Settle }
            }
            BouncePhase.Settle -> {
                animationTime += Gdx.graphics.deltaTime
                val progress = (animationTime / 0.22f).coerceIn(0f, 1f)
                currentYawDegrees = Interpolation.sine.apply(startYawDegrees, targetYawDegrees, progress)
                if (progress >= 1f) { currentYawDegrees = targetYawDegrees; bouncePhase = BouncePhase.Idle }
            }
        }
    }

    override fun resize(width: Int, height: Int) {
        camera?.viewportWidth = width.toFloat(); camera?.viewportHeight = height.toFloat(); camera?.update()
    }

    override fun render() {
        if (dragging || bouncePhase != BouncePhase.Idle) updateAnimation()
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height); Gdx.gl.glClearColor(0f, 0f, 0f, 0f); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        instance?.let { cube ->
            cube.transform.set(Vector3.Zero, Quaternion().setEulerAngles(currentYawDegrees, 0f, 0f))
            modelBatch?.begin(camera); modelBatch?.render(cube, environment); modelBatch?.end()
        }
    }

    override fun dispose() {
        modelBatch?.dispose(); model?.dispose(); textures.forEach { it.dispose() }; font?.dispose()
    }

    private fun snappedTabIndex(yaw: Float): Int {
        val raw = (-yaw / 90f).roundToInt(); return ((raw % 4) + 4) % 4
    }

    private enum class BouncePhase { Idle, Overshoot, Settle }
}
