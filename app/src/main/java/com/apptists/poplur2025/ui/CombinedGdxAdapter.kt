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
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import kotlin.math.abs
import kotlin.math.roundToInt
import android.util.Log

class CombinedGdxAdapter(
    private val initialTabIndex: Int = 1,
    private val onVerticalSwipe: ((Boolean) -> Unit)? = null,
    private val onLongPress: (() -> Unit)? = null,
    private val onLongPressEnd: (() -> Unit)? = null,
    private val onTap: (() -> Unit)? = null,
    private val onTabSelected: (Int) -> Unit
) : ApplicationAdapter() {

    private var camera: PerspectiveCamera? = null
    private var modelBatch: ModelBatch? = null
    private var model: Model? = null
    private var instance: ModelInstance? = null
    private var environment: Environment? = null

    private var textureBatch: SpriteBatch? = null
    private var shapeRenderer: ShapeRenderer? = null
    @Volatile private var texturesDirty = false
    @Volatile private var modelDirty = false
    @Volatile private var materialsDirty = false
    private var appInstance: com.badlogic.gdx.Application? = null

    var cubeScale: Float = 0.80f
        set(value) {
            if (field != value) {
                field = value
                modelDirty = true
            }
        }

    var cornerRadius: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                texturesDirty = true
                modelDirty = true
            }
        }

    var opacity: Float = 1.0f
        set(value) {
            if (field != value) {
                field = value
                materialsDirty = true
            }
        }

    var currentIconTitle: String = "Record"
        set(value) {
            if (field != value) {
                field = value
                texturesDirty = true
                modelDirty = true
            }
        }

    var profileIconTitle: String = "Profile"
        set(value) {
            val v = value.ifBlank { "Profile" }
            if (field != v) {
                field = v
                texturesDirty = true
                modelDirty = true
            }
        }

    var yawDegrees: Float = -90f * initialTabIndex
        set(value) {
            if (field != value) {
                field = value
                currentYawDegrees = value
                targetYawDegrees = value
            }
        }

    private var currentYawDegrees = yawDegrees
    private var targetYawDegrees = currentYawDegrees
    private var startYawDegrees = currentYawDegrees
    private var animationTime = 0f
    private var bouncePhase = BouncePhase.Idle

    private var dragging = false
    private var isVerticalSwipe = false
    private var isSwipeTriggered = false
    private var isRotation = false
    private var longPressSent = false
    private var longPressStarted = false
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var touchStartMillis = 0L
    private var time = 0f

    private val textures = arrayOfNulls<Texture>(4)
    private val materials = arrayOfNulls<Material>(4)
    private val currentTitles = arrayOfNulls<String>(4)
    private var font: BitmapFont? = null

    override fun create() {
        try {
            appInstance = Gdx.app
            Gdx.graphics.setContinuousRendering(true)
            // Disable LibGDX asset management to avoid multi-instance conflicts
            com.badlogic.gdx.graphics.glutils.ShaderProgram.prependVertexCode = ""
            com.badlogic.gdx.graphics.glutils.ShaderProgram.prependFragmentCode = ""

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
            textureBatch = SpriteBatch()
            shapeRenderer = ShapeRenderer().apply { setAutoShapeType(true) }
            font = BitmapFont().apply {
                data.setScale(2.5f)
                setUseIntegerPositions(false)
            }

            rebuildTextures()
            rebuildModel()

            environment = Environment().apply {
                set(ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f))
            }

            Gdx.input.inputProcessor = object : InputAdapter() {
                override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                    dragging = true; isVerticalSwipe = false; isSwipeTriggered = false; isRotation = false; longPressSent = false; longPressStarted = false; bouncePhase = BouncePhase.Idle; downX = screenX.toFloat(); downY = screenY.toFloat(); lastX = screenX.toFloat(); touchStartMillis = System.currentTimeMillis(); return true
                }
                override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                    if (!dragging) return true
                    val dx = screenX.toFloat() - downX; val dy = screenY.toFloat() - downY
                    if (abs(dx) > 10f || abs(dy) > 10f) longPressSent = true
                    if (!isVerticalSwipe && !isRotation) {
                        if (abs(dy) > 15f && abs(dy) > abs(dx) * 1.5f) isVerticalSwipe = true else if (abs(dx) > 15f) isRotation = true
                    }
                    if (isVerticalSwipe && !isSwipeTriggered && abs(dy) > 40f) {
                        isSwipeTriggered = true; appInstance?.postRunnable { onVerticalSwipe?.invoke(dy < 0) }
                    }
                    if (isRotation) {
                        val deltaX = screenX.toFloat() - lastX; currentYawDegrees += deltaX * 0.8f; targetYawDegrees = currentYawDegrees
                    }
                    lastX = screenX.toFloat(); return true
                }
                override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                    dragging = false
                    if (longPressStarted) {
                        longPressStarted = false
                        appInstance?.postRunnable { onLongPressEnd?.invoke() }
                    }
                    if (isVerticalSwipe && !isSwipeTriggered) appInstance?.postRunnable { onVerticalSwipe?.invoke((screenY - downY) < 0) }
                    val moved = abs(screenX - downX) > 15f || abs(screenY - downY) > 15f
                    if (!moved && !longPressSent) {
                        appInstance?.postRunnable { onTap?.invoke() }
                        return true
                    }
                    val snappedIndex = snappedTabIndex(currentYawDegrees)
                    startBounceTo(-90f * snappedIndex)
                    if (isRotation || abs(screenX - downX) > 15f) appInstance?.postRunnable { onTabSelected(snappedIndex) }
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e("CombinedGdxAdapter", "CRITICAL ERROR in create()", e)
        }
    }

    private fun rebuildTextures() {
        val recordTitle = if (currentIconTitle != "Record") currentIconTitle else "Record"
        val titles = listOf(recordTitle, "Feed", profileIconTitle, "Map")

        var anyChanged = false
        titles.forEachIndexed { index, title ->
            if (currentTitles[index] != title) {
                textures[index]?.dispose()
                val texture = createFaceTexture(title)
                textures[index] = texture
                materials[index] = Material(
                    TextureAttribute.createDiffuse(texture),
                    ColorAttribute.createDiffuse(Color.WHITE),
                    BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, opacity)
                )
                currentTitles[index] = title
                anyChanged = true
            }
        }
        
        if (anyChanged) {
            modelDirty = true
        }
    }

    private fun rebuildMaterialsOpacity() {
        materials.forEach { material ->
            val blending = material?.get(BlendingAttribute.Type) as? BlendingAttribute
            blending?.opacity = opacity
        }
    }

    private fun rebuildModel() {
        val oldModel = model
        val nonNullMaterials = materials.filterNotNull()
        if (nonNullMaterials.size == 4) {
            model = buildCube(nonNullMaterials)
            instance = ModelInstance(model)
            oldModel?.dispose()
        }
    }

    private fun buildCube(materials: List<Material>): Model {
        val mb = ModelBuilder()
        val attr = (Usage.Position or Usage.Normal or Usage.TextureCoordinates).toLong()
        val s = cubeScale
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
        val fbo = try {
            FrameBuffer(Pixmap.Format.RGBA8888, size, size, false)
        } catch (e: Exception) {
            // FBO creation might fail if GL context is unstable or out of memory
            return Texture(size, size, Pixmap.Format.RGBA8888) 
        }
        val projection = Matrix4().setToOrtho2D(0f, 0f, size.toFloat(), size.toFloat())
        val sr = shapeRenderer ?: ShapeRenderer().also { shapeRenderer = it; it.setAutoShapeType(true) }
        val batch = textureBatch ?: SpriteBatch().also { textureBatch = it }

        fbo.begin()
        Gdx.gl.glViewport(0, 0, size, size)
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        sr.projectionMatrix = projection
        sr.begin(ShapeRenderer.ShapeType.Filled)

        // Dark indigo background — matches iOS baseColor(white:0.2) × .indigo multiply.
        // Fill the whole texture so adjacent cube faces visually meet at every edge.
        val bgR = 0.07f; val bgG = 0.07f; val bgB = 0.18f
        val bgColor = Color(bgR, bgG, bgB, 1f)
        val pad = 0f
        val r = cornerRadius
        sr.color = bgColor
        sr.filledRoundRect(pad, pad, size - pad * 2, size - pad * 2, r)

        // Soft radial glow behind icon (simulates iOS emission mask luminosity)
        val cx = size / 2f
        val cy = size * 0.63f
        val isize = 108f
        for (i in 14 downTo 1) {
            val frac = i.toFloat() / 14f
            sr.color = Color(1f, 1f, 1f, 0.018f * frac)
            sr.circle(cx, cy, isize * 0.55f + i * 9f, 44)
        }

        // Icon: pure white — simulates iOS emission mask glow over dark face
        sr.color = Color(1f, 1f, 1f, 1f)
        when (title) {
            "Record" -> {
                // plus.circle.fill: filled circle with + cutout
                sr.circle(cx, cy, isize * 0.37f, 44)
                sr.color = bgColor
                val arm = isize * 0.065f; val len = isize * 0.22f
                sr.rect(cx - arm, cy - len, arm * 2f, len * 2f)
                sr.rect(cx - len, cy - arm, len * 2f, arm * 2f)
            }
            "Feed" -> {
                // list.dash: three rows with short dash + long line
                val lh = isize * 0.09f; val dashW = isize * 0.13f; val lineW = isize * 0.42f
                val totalW = dashW + isize * 0.07f + lineW
                val startX = cx - totalW / 2f
                for (i in 0..2) {
                    val ly = cy + (i - 1) * isize * 0.30f - lh / 2f
                    sr.rect(startX, ly, dashW, lh)
                    sr.rect(startX + dashW + isize * 0.07f, ly, lineW, lh)
                }
            }
            "Profile" -> {
                // person.circle: ring with person silhouette inside
                sr.circle(cx, cy, isize * 0.38f, 44)
                sr.color = bgColor
                sr.circle(cx, cy, isize * 0.29f, 44)
                sr.color = Color(1f, 1f, 1f, 1f)
                sr.circle(cx, cy + isize * 0.12f, isize * 0.11f, 32)
                sr.filledRoundRect(cx - isize * 0.18f, cy - isize * 0.20f, isize * 0.36f, isize * 0.20f, isize * 0.11f)
            }
            "Map" -> {
                // map: three folded panels at slightly different heights
                val pw = isize * 0.21f; val gap = isize * 0.035f
                val tallH = isize * 0.54f; val shortH = isize * 0.46f
                sr.rect(cx - pw * 1.5f - gap, cy - shortH / 2f, pw, shortH)
                sr.rect(cx - pw / 2f, cy - tallH / 2f, pw, tallH)
                sr.rect(cx + pw / 2f + gap, cy - shortH / 2f, pw, shortH)
            }
            "Mail" -> {
                val ew = isize * 0.74f; val eh = isize * 0.50f
                val el = cx - ew / 2f; val eb = cy - eh / 2f; val b = isize * 0.08f
                sr.rect(el, eb, ew, eh)
                sr.color = bgColor
                sr.rect(el + b, eb + b, ew - b * 2, eh - b * 2)
                sr.color = Color(1f, 1f, 1f, 1f)
                sr.rectLine(el + b, eb + eh - b, cx, cy + isize * 0.04f, b)
                sr.rectLine(el + ew - b, eb + eh - b, cx, cy + isize * 0.04f, b)
            }
            "Settings" -> {
                repeat(8) { i ->
                    val angle = Math.toRadians((i * 45).toDouble()).toFloat()
                    sr.rectLine(
                        cx + kotlin.math.cos(angle) * isize * 0.22f, cy + kotlin.math.sin(angle) * isize * 0.22f,
                        cx + kotlin.math.cos(angle) * isize * 0.38f, cy + kotlin.math.sin(angle) * isize * 0.38f,
                        isize * 0.10f
                    )
                }
                sr.circle(cx, cy, isize * 0.20f, 36)
                sr.color = bgColor
                sr.circle(cx, cy, isize * 0.09f, 28)
            }
            "Alerts" -> {
                sr.filledRoundRect(cx - isize * 0.24f, cy - isize * 0.10f, isize * 0.48f, isize * 0.42f, isize * 0.16f)
                sr.circle(cx, cy + isize * 0.36f, isize * 0.08f, 20)
                sr.circle(cx, cy - isize * 0.16f, isize * 0.08f, 20)
            }
            "Phone" -> {
                val ps = isize * 1.5f; val pb = ps * 0.13f
                sr.rectLine(cx - ps * 0.30f, cy + ps * 0.22f, cx - ps * 0.06f, cy - ps * 0.14f, pb)
                sr.rectLine(cx - ps * 0.06f, cy - ps * 0.14f, cx + ps * 0.32f, cy - ps * 0.25f, pb)
                sr.circle(cx - ps * 0.33f, cy + ps * 0.24f, ps * 0.13f, 28)
                sr.circle(cx + ps * 0.35f, cy - ps * 0.27f, ps * 0.13f, 28)
            }
            "Store" -> {
                val b = isize * 0.09f
                sr.rectLine(cx - isize * 0.34f, cy + isize * 0.18f, cx - isize * 0.20f, cy - isize * 0.18f, b)
                sr.rect(cx - isize * 0.16f, cy - isize * 0.16f, isize * 0.50f, isize * 0.30f)
                sr.circle(cx - isize * 0.06f, cy - isize * 0.28f, isize * 0.08f, 20)
                sr.circle(cx + isize * 0.28f, cy - isize * 0.28f, isize * 0.08f, 20)
            }
        }
        sr.end()

        batch.projectionMatrix = projection
        batch.begin()
        font?.let { f ->
            f.setColor(Color(1f, 1f, 1f, 1f))
            val l = GlyphLayout(f, title)
            f.draw(batch, title, (size - l.width) / 2f, size * 0.305f)
        }
        batch.end()

        val pixmap = Pixmap.createFromFrameBuffer(0, 0, size, size)
        fbo.end()
        val upright = Pixmap(size, size, Pixmap.Format.RGBA8888)
        for (y in 0 until size) upright.drawPixmap(pixmap, 0, size - y - 1, size, 1, 0, y, size, 1)
        val tex = Texture(upright)
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        pixmap.dispose(); upright.dispose(); fbo.dispose()
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
            longPressSent = true; longPressStarted = true; appInstance?.postRunnable { onLongPress?.invoke() }
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
        camera?.viewportWidth = width.toFloat().coerceAtLeast(1f); camera?.viewportHeight = height.toFloat().coerceAtLeast(1f); camera?.update()
    }

    override fun render() {
        try {
            if (texturesDirty) {
                rebuildTextures()
                texturesDirty = false
            }
            if (modelDirty) {
                rebuildModel()
                modelDirty = false
            }
            if (materialsDirty) {
                rebuildMaterialsOpacity()
                materialsDirty = false
            }

            val deltaTime = Gdx.graphics.deltaTime
            time += deltaTime
            if (dragging || bouncePhase != BouncePhase.Idle) updateAnimation()
            Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height); Gdx.gl.glClearColor(0f, 0f, 0f, 0f); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
            instance?.let { cube ->
                val hoverY = Math.sin(time.toDouble() * 2.0).toFloat() * 0.12f
                cube.transform.setToTranslation(0f, hoverY, 0f)
                    .rotate(Vector3.Y, currentYawDegrees)
                    .scale(cubeScale.coerceAtLeast(0.01f), cubeScale.coerceAtLeast(0.01f), cubeScale.coerceAtLeast(0.01f))
                modelBatch?.begin(camera); modelBatch?.render(cube, environment); modelBatch?.end()
            }
        } catch (e: Exception) {
            Log.e("CombinedGdxAdapter", "Error in render()", e)
        }
    }

    override fun dispose() {
        modelBatch?.dispose(); model?.dispose()
        textures.forEach { it?.dispose() }
        font?.dispose()
        textureBatch?.dispose(); shapeRenderer?.dispose()
    }

    private fun snappedTabIndex(yaw: Float): Int {
        val raw = (-yaw / 90f).roundToInt(); return ((raw % 4) + 4) % 4
    }

    private enum class BouncePhase { Idle, Overshoot, Settle }
}
