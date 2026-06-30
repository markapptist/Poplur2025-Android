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
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import android.util.Log

class CombinedGdxAdapter(
    private val initialTabIndex: Int = 2,
    private val onVerticalSwipe: ((Boolean) -> Unit)? = null,
    private val onLongPress: (() -> Unit)? = null,
    private val onLongPressMove: ((Float, Float) -> Unit)? = null,
    private val onLongPressEnd: (() -> Unit)? = null,
    private val onTap: (() -> Unit)? = null,
    private val onTabSelected: (Int) -> Unit
) : ApplicationAdapter() {

    private var camera: PerspectiveCamera? = null
    private var modelBatch: ModelBatch? = null
    private var model: Model? = null
    private var instance: ModelInstance? = null
    private var channelObjectModel: Model? = null
    private var channelObjectInstance: ModelInstance? = null
    private var channelObjectEndMillis: Long = 0L
    private var channelObjectStartedMillis: Long = 0L
    private var channelObjectId: String = "star"
    private var environment: Environment? = null

    private val skinFaceTextures = arrayOfNulls<Texture>(6)
    private val skinMaterials = arrayOfNulls<Material>(6)

    var skin: CubeSkin = CubeSkin.Gold
        set(value) {
            if (field != value) {
                field = value
                texturesDirty = true
                modelDirty = true
            }
        }

    private var textureBatch: SpriteBatch? = null
    private var shapeRenderer: ShapeRenderer? = null
    @Volatile private var texturesDirty = false
    @Volatile private var modelDirty = false
    @Volatile private var materialsDirty = false
    private var appInstance: com.badlogic.gdx.Application? = null

    // 0.65 = iOS SCNBox half-size (width 1.3 / 2). Geometry is unit size; transform
    // scale is the only multiplier, so cubeScale == world half-extent directly.
    var cubeScale: Float = 0.65f
        set(value) {
            if (field != value) {
                field = value
                modelDirty = true
            }
        }

    var cornerRadius: Float = 92f
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
                modelDirty = true
            }
        }

    var mapIconTitle: String = "Map"
        set(value) {
            val v = value.ifBlank { "Map" }
            if (field != v) {
                field = v
                texturesDirty = true
                modelDirty = true
            }
        }

    var currentIconTitle: String = "Camera"
        set(value) {
            val v = normalizeCameraTitle(value)
            if (field != v) {
                field = v
                texturesDirty = true
                modelDirty = true
            }
        }

    var feedIconTitle: String = "Feed"
        set(value) {
            val v = value.ifBlank { "Feed" }
            if (field != v) {
                field = v
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

    fun setFaceTitles(
        mapTitle: String,
        cameraTitle: String,
        feedTitle: String,
        profileTitle: String
    ) {
        mapIconTitle = mapTitle
        currentIconTitle = cameraTitle
        feedIconTitle = feedTitle
        profileIconTitle = profileTitle
    }

    private fun normalizeCameraTitle(title: String): String {
        return when (title.ifBlank { "Camera" }) {
            "Record" -> "Camera"
            else -> title.ifBlank { "Camera" }
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

    fun syncSelectedTab(index: Int) {
        val targetIndex = index.coerceIn(0, 3)
        appInstance?.postRunnable {
            dragging = false
            isVerticalSwipe = false
            isSwipeTriggered = false
            isRotation = false
            longPressSent = false
            longPressStarted = false
            Gdx.graphics.setContinuousRendering(true)
            startBounceTo(continuousYawForTab(targetIndex))
            Gdx.graphics.requestRendering()
        }
    }

    fun showChannelObject(objectId: String) {
        appInstance?.postRunnable {
            channelObjectId = objectId
            channelObjectStartedMillis = System.currentTimeMillis()
            channelObjectEndMillis = channelObjectStartedMillis + 2_000L
            channelObjectModel?.dispose()
            channelObjectModel = buildChannelObject(objectId)
            channelObjectInstance = channelObjectModel?.let { ModelInstance(it) }
            Gdx.graphics.setContinuousRendering(true)
            Gdx.graphics.requestRendering()
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
    private var lastY = 0f
    private var touchStartMillis = 0L
    private var time = 0f
    private val cubeTouchRadiusPx = 150f

    // P-logo periodic peek (tilts bottom face toward camera at -90° X pitch)
    private enum class PeekPhase { Idle, TiltIn, Hold, TiltOut }
    private var peekPhase = PeekPhase.Idle
    private var peekPhaseTime = 0f
    private var peekPitchDegrees = 0f
    private val peekIntervalSeconds = 25f
    private var nextPeekSeconds = peekIntervalSeconds

    // Face texture cache — all possible titles are rendered once at create() time so
    // runtime icon changes (e.g. Profile wheel cycling every 620 ms) are just HashMap
    // lookups instead of FrameBuffer + glReadPixels GPU stalls on the render thread.
    private val textureCache = HashMap<String, Texture>()
    private var lastCachedCornerRadius = -1f  // forces initial cache build

    private val textures = arrayOfNulls<Texture>(6)
    private val materials = arrayOfNulls<Material>(6)
    private val currentTitles = arrayOfNulls<String>(6)
    private var font: BitmapFont? = null

    private var lastWidth = 0
    private var lastHeight = 0

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
            lastWidth = w.toInt()
            lastHeight = h.toInt()
            camera = PerspectiveCamera(40f, w, h).apply {
                position.set(0f, 0f, cameraDistance())
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
                add(DirectionalLight().set(0.95f, 0.90f, 0.82f, -0.45f, -0.62f, -0.78f))
            }

            Gdx.input.inputProcessor = object : InputAdapter() {
                override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                    if (pointer != 0) return false  // single-touch only — prevents multi-finger twists
                    if (!isInsideCubeTouchTarget(screenX.toFloat(), screenY.toFloat())) return false
                    dragging = true; isVerticalSwipe = false; isSwipeTriggered = false; isRotation = false; longPressSent = false; longPressStarted = false; bouncePhase = BouncePhase.Idle; downX = screenX.toFloat(); downY = screenY.toFloat(); lastX = screenX.toFloat(); lastY = screenY.toFloat(); touchStartMillis = System.currentTimeMillis()
                    if (peekPhase != PeekPhase.Idle) { peekPitchDegrees = 0f; peekPhase = PeekPhase.Idle; nextPeekSeconds = peekIntervalSeconds }
                    return true
                }
                override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                    if (pointer != 0) return false  // single-touch only
                    if (!dragging) return true
                    val dx = screenX.toFloat() - downX; val dy = screenY.toFloat() - downY
                    val deltaX = screenX.toFloat() - lastX
                    val deltaY = screenY.toFloat() - lastY
                    if (longPressStarted) {
                        appInstance?.postRunnable { onLongPressMove?.invoke(deltaX, deltaY) }
                        lastX = screenX.toFloat()
                        lastY = screenY.toFloat()
                        return true
                    }
                    if (abs(dx) > 48f || abs(dy) > 48f) longPressSent = true
                    if (!isVerticalSwipe && !isRotation) {
                        if (abs(dy) > 15f && abs(dy) > abs(dx) * 1.5f) isVerticalSwipe = true else if (abs(dx) > 15f) isRotation = true
                    }
                    if (isVerticalSwipe && !isSwipeTriggered && abs(dy) > 40f) {
                        isSwipeTriggered = true; appInstance?.postRunnable { onVerticalSwipe?.invoke(dy < 0) }
                    }
                    if (isRotation) {
                        currentYawDegrees += deltaX * 0.5f; targetYawDegrees = currentYawDegrees
                    }
                    lastX = screenX.toFloat(); lastY = screenY.toFloat(); return true
                }
                override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                    if (pointer != 0) return false  // single-touch only
                    dragging = false
                    if (longPressStarted) {
                        longPressStarted = false
                        appInstance?.postRunnable { onLongPressEnd?.invoke() }
                        return true
                    }
                    if (isVerticalSwipe && !isSwipeTriggered) appInstance?.postRunnable { onVerticalSwipe?.invoke((screenY - downY) < 0) }
                    val moved = abs(screenX - downX) > 15f || abs(screenY - downY) > 15f
                    if (!moved && !longPressSent) {
                        appInstance?.postRunnable { onTap?.invoke() }
                        return true
                    }
                    val snappedIndex = snappedTabIndex(currentYawDegrees)
                    val canonicalYaw = -90f * snappedIndex
                    val nearestYaw = canonicalYaw + ((currentYawDegrees - canonicalYaw) / 360f).roundToInt() * 360f
                    startBounceTo(nearestYaw)
                    if (isRotation || abs(screenX - downX) > 15f) appInstance?.postRunnable { onTabSelected(snappedIndex) }
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e("CombinedGdxAdapter", "CRITICAL ERROR in create()", e)
        }
    }

    private fun cubeSceneY(): Float {
        return 0f
    }

    private fun cubeSceneX(): Float {
        return 0f
    }

    private fun cubeTouchCenterY(): Float {
        val h = lastHeight.coerceAtLeast(1).toFloat()
        return h / 2f
    }

    private fun isInsideCubeTouchTarget(x: Float, y: Float): Boolean {
        val cx = lastWidth.coerceAtLeast(1).toFloat() / 2f
        val cy = cubeTouchCenterY()
        return abs(x - cx) <= cubeTouchRadiusPx && abs(y - cy) <= cubeTouchRadiusPx
    }

    private fun cubeTransformScale(): Float {
        return cubeScale.coerceAtLeast(0.01f)
    }

    private fun clearTextureCacheIfNeeded() {
        // If cornerRadius changed since the last cache build, discard stale textures.
        if (lastCachedCornerRadius != cornerRadius) {
            textureCache.values.forEach { it.dispose() }
            textureCache.clear()
            currentTitles.fill(null)
            lastCachedCornerRadius = cornerRadius
        }
    }

    private fun rebuildTextures() {
        if (skin != CubeSkin.Default) {
            rebuildSkinTextures()
            return
        }
        clearTextureCacheIfNeeded()

        // Face order matches iOS SceneKit: +Z Map (0°), +X Camera (90°), -Z Feed (180°), -X Profile (270°).
        val titles = listOf(
            mapIconTitle.ifBlank { "Map" },
            normalizeCameraTitle(currentIconTitle),
            feedIconTitle.ifBlank { "Feed" },
            profileIconTitle.ifBlank { "Profile" },
            "TopFork",
            "BottomP"
        )

        titles.forEachIndexed { index, title ->
            if (currentTitles[index] != title) {
                // Cache hit is the common path — no FBO, no glReadPixels, no GPU stall.
                val texture = textureCache.getOrPut(title) { createFaceTexture(title) }
                textures[index] = texture
                val existing = materials[index]
                if (existing == null) {
                    // First time — create the Material; model must be built around it.
                    materials[index] = Material(
                        TextureAttribute.createDiffuse(texture),
                        ColorAttribute.createDiffuse(Color.WHITE),
                        FloatAttribute.createAlphaTest(0.04f)
                    )
                    modelDirty = true
                } else {
                    // Update the texture attribute in-place on the object the model
                    // already references — the model picks up the change automatically
                    // on the next render without a rebuild.
                    existing.set(TextureAttribute.createDiffuse(texture))
                }
                currentTitles[index] = title
            }
        }
    }

    private fun rebuildSkinTextures() {
        rebuildSkinTexturesWithDynamicIcons()
    }

    private fun rebuildSkinTexturesWithDynamicIcons() {
        val titles = listOf(
            mapIconTitle.ifBlank { "Map" },
            normalizeCameraTitle(currentIconTitle),
            feedIconTitle.ifBlank { "Feed" },
            profileIconTitle.ifBlank { "Profile" },
            "TopFork",
            "BottomP"
        )
        val newTextures = arrayOfNulls<Texture>(6)
        val newMaterials = arrayOfNulls<Material>(6)

        titles.forEachIndexed { index, title ->
            val tex = createFaceTexture(title, skin, index)
            newTextures[index] = tex
            newMaterials[index] = Material(
                TextureAttribute.createDiffuse(tex),
                ColorAttribute.createDiffuse(Color.WHITE),
                FloatAttribute.createAlphaTest(0.04f)
            )
            currentTitles[index] = "${skin.id}:$title"
        }

        skinFaceTextures.forEach { it?.dispose() }
        repeat(6) { i ->
            skinFaceTextures[i] = newTextures[i]
            skinMaterials[i] = newMaterials[i]
        }
        modelDirty = true
    }

    private fun rebuildSkinTexturesFromFile(path: String) {
        val fullPixmap = Pixmap(Gdx.files.internal(path))
        val tileW = fullPixmap.width / 4
        val tileH = fullPixmap.height / 3
        // Cubemap cross layout (col, row) per face index — matches iOS exactly.
        // Face order: 0=front(+Z)=Map, 1=right(+X)=Camera, 2=back(-Z)=Feed, 3=left(-X)=Profile, 4=top, 5=bottom.
        // Cross layout: (0,1)=Feed, (1,1)=Profile, (2,1)=Map, (3,1)=Camera, (1,0)=top, (1,2)=bottom.
        val coords = listOf(2 to 1, 3 to 1, 0 to 1, 1 to 1, 1 to 0, 1 to 2)
        
        val newTextures = arrayOfNulls<Texture>(6)
        val newMaterials = arrayOfNulls<Material>(6)

        coords.forEachIndexed { i, (col, row) ->
            val p = Pixmap(tileW, tileH, Pixmap.Format.RGBA8888)
            p.drawPixmap(fullPixmap, 0, 0, col * tileW, row * tileH, tileW, tileH)

            makeDrawnPixelsOpaque(p)
            val tex = Texture(p)
            tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            tex.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
            newTextures[i] = tex
            p.dispose()
            newMaterials[i] = Material(
                TextureAttribute.createDiffuse(tex),
                ColorAttribute.createDiffuse(Color.WHITE),
                FloatAttribute.createAlphaTest(0.04f)
            )
        }
        
        skinFaceTextures.forEach { it?.dispose() }
        repeat(6) { i ->
            skinFaceTextures[i] = newTextures[i]
            skinMaterials[i] = newMaterials[i]
        }
        fullPixmap.dispose()
        modelDirty = true
    }

    private fun rebuildSkinTexturesFromColor(baseColor: Color) {
        val size = 256
        val newTextures = arrayOfNulls<Texture>(6)
        val newMaterials = arrayOfNulls<Material>(6)
        
        repeat(6) { i ->
            val p = Pixmap(size, size, Pixmap.Format.RGBA8888)
            // Gradient from lighter top-left to darker bottom-right
            for (y in 0 until size) {
                for (x in 0 until size) {
                    val t = (x + y).toFloat() / (size * 2)
                    val r = (baseColor.r * (1.4f - t * 0.8f)).coerceIn(0f, 1f)
                    val g = (baseColor.g * (1.4f - t * 0.8f)).coerceIn(0f, 1f)
                    val b = (baseColor.b * (1.4f - t * 0.8f)).coerceIn(0f, 1f)
                    p.setColor(r, g, b, 1f)
                    p.drawPixel(x, y)
                }
            }
            val tex = Texture(p)
            tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            tex.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
            newTextures[i] = tex
            p.dispose()
            newMaterials[i] = Material(
                TextureAttribute.createDiffuse(tex),
                ColorAttribute.createDiffuse(Color.WHITE),
                FloatAttribute.createAlphaTest(0.04f)
            )
        }
        
        // Update arrays and dispose old textures
        skinFaceTextures.forEach { it?.dispose() }
        repeat(6) { i ->
            skinFaceTextures[i] = newTextures[i]
            skinMaterials[i] = newMaterials[i]
        }
        modelDirty = true
    }

    private fun rebuildMaterialsOpacity() {
        // Cube faces are intentionally rendered as alpha-tested opaque surfaces.
        // Blended cube quads self-compose through depth testing on Android and make
        // the cube look washed out or partially transparent.
    }

    private fun rebuildModel() {
        val oldModel = model

        if (skin != CubeSkin.Default) {
            val nonNullSkinMaterials = skinMaterials.filterNotNull()
            if (nonNullSkinMaterials.size == 6) {
                model = buildSkinCube(nonNullSkinMaterials)
                instance = ModelInstance(model)
                oldModel?.dispose()
            }
            return
        }
        val nonNullMaterials = materials.filterNotNull()
        if (nonNullMaterials.size == 6) {
            model = buildCube(nonNullMaterials)
            instance = ModelInstance(model)
            oldModel?.dispose()
        }
    }

    private fun buildCube(materials: List<Material>): Model {
        return buildRoundedCube(materials)
    }

    private fun buildSkinCube(materials: List<Material>): Model {
        return buildRoundedCube(materials)
    }

    private fun buildRoundedCube(faceMaterials: List<Material>): Model {
        val mb = ModelBuilder()
        val faceAttr = (Usage.Position or Usage.Normal or Usage.TextureCoordinates).toLong()
        // Geometry is always unit half-size (1.0). Visual size is controlled exclusively
        // by the transform .scale(cubeScale) in render(), matching the iOS pattern where
        // SCNBox(width:1.3) is scaled by the transform rather than baked into geometry.
        val s = 1.0f
        mb.begin()
        // Face order: front +Z, right +X, back -Z, left -X, top +Y, bottom -Y.
        mb.part("front",  GL20.GL_TRIANGLES, faceAttr, faceMaterials[0]).rect(-s, -s, s,  s, -s, s,  s, s, s,  -s, s, s,  0f, 0f, 1f)
        mb.part("right",  GL20.GL_TRIANGLES, faceAttr, faceMaterials[1]).rect( s, -s, s,  s, -s, -s, s, s, -s,  s, s, s,  1f, 0f, 0f)
        mb.part("back",   GL20.GL_TRIANGLES, faceAttr, faceMaterials[2]).rect( s, -s, -s, -s, -s, -s, -s, s, -s, s, s, -s, 0f, 0f, -1f)
        mb.part("left",   GL20.GL_TRIANGLES, faceAttr, faceMaterials[3]).rect(-s, -s, -s, -s, -s, s, -s, s, s, -s, s, -s, -1f, 0f, 0f)
        mb.part("top",    GL20.GL_TRIANGLES, faceAttr, faceMaterials[4]).rect(-s, s, s,  s, s, s,  s, s, -s, -s, s, -s, 0f, 1f, 0f)
        mb.part("bottom", GL20.GL_TRIANGLES, faceAttr, faceMaterials[5]).rect(-s, -s, -s, s, -s, -s, s, -s, s, -s, -s, s, 0f, -1f, 0f)
        return mb.end()
    }

    private fun buildChannelObject(objectId: String): Model {
        val style = channelObjectStyle(objectId)
        val mb = ModelBuilder()
        val attr = (Usage.Position or Usage.Normal).toLong()
        mb.begin()
        when (style.kind) {
            ChannelObjectKind.Curved -> buildCurvedObject(mb, attr, style)
            ChannelObjectKind.Orb -> buildOrbObject(mb, attr, style)
            ChannelObjectKind.Cylinder -> buildCylinderObject(mb, attr, style)
            ChannelObjectKind.Boxy -> buildPropObject(mb, attr, style)
            ChannelObjectKind.Flat -> buildFlatObject(mb, attr, style)
            ChannelObjectKind.Trophy -> buildTrophyObject(mb, attr, style)
            ChannelObjectKind.Star -> buildStarObject(mb, attr, style)
        }
        return mb.end()
    }

    private fun channelObjectStyle(objectId: String): ChannelObjectStyle = when (objectId) {
        "banana" -> ChannelObjectStyle(ChannelObjectKind.Curved, Color(1f, 0.82f, 0.14f, 1f), Color(0.90f, 0.48f, 0.08f, 1f), Color(0.36f, 0.20f, 0.07f, 1f), objectId)
        "cake" -> ChannelObjectStyle(ChannelObjectKind.Cylinder, Color(1f, 0.48f, 0.72f, 1f), Color(1f, 0.84f, 0.91f, 1f), Color(0.48f, 0.25f, 0.13f, 1f), objectId)
        "fruit" -> ChannelObjectStyle(ChannelObjectKind.Orb, Color(1f, 0.28f, 0.16f, 1f), Color(1f, 0.64f, 0.22f, 1f), Color(0.20f, 0.83f, 0.43f, 1f), objectId)
        "lipstick" -> ChannelObjectStyle(ChannelObjectKind.Cylinder, Color(1f, 0.08f, 0.32f, 1f), Color(0.08f, 0.09f, 0.16f, 1f), Color(1f, 0.72f, 0.83f, 1f), objectId)
        "compact" -> ChannelObjectStyle(ChannelObjectKind.Flat, Color(1f, 0.70f, 0.90f, 1f), Color(0.32f, 0.25f, 0.44f, 1f), Color(1f, 0.94f, 0.98f, 1f), objectId)
        "sparkle", "star" -> ChannelObjectStyle(ChannelObjectKind.Star, Color(1f, 0.91f, 0.42f, 1f), Color(1f, 0.32f, 0.85f, 1f), Color.WHITE, objectId)
        "ball" -> ChannelObjectStyle(ChannelObjectKind.Orb, Color(0.15f, 0.72f, 1f, 1f), Color(0.08f, 0.28f, 0.78f, 1f), Color.WHITE, objectId)
        "trophy" -> ChannelObjectStyle(ChannelObjectKind.Trophy, Color(1f, 0.76f, 0.18f, 1f), Color(0.83f, 0.48f, 0.08f, 1f), Color(1f, 0.95f, 0.65f, 1f), objectId)
        "dumbbell" -> ChannelObjectStyle(ChannelObjectKind.Cylinder, Color(0.36f, 1f, 0.48f, 1f), Color(0.12f, 0.49f, 0.29f, 1f), Color(0.78f, 1f, 0.82f, 1f), objectId)
        "camera" -> ChannelObjectStyle(ChannelObjectKind.Boxy, Color(0.52f, 0.62f, 0.95f, 1f), Color(0.09f, 0.13f, 0.29f, 1f), Color(0.38f, 0.97f, 1f, 1f), objectId)
        "spotlight" -> ChannelObjectStyle(ChannelObjectKind.Trophy, Color(1f, 0.86f, 0.32f, 1f), Color(0.44f, 0.28f, 0.06f, 1f), Color(1f, 0.96f, 0.70f, 1f), objectId)
        "dress" -> ChannelObjectStyle(ChannelObjectKind.Trophy, Color(0.72f, 0.34f, 1f, 1f), Color(0.29f, 0.11f, 0.52f, 1f), Color(1f, 0.66f, 0.95f, 1f), objectId)
        "shoe" -> ChannelObjectStyle(ChannelObjectKind.Curved, Color(1f, 0.52f, 0.18f, 1f), Color(0.35f, 0.15f, 0.08f, 1f), Color(1f, 0.88f, 0.65f, 1f), objectId)
        "bag" -> ChannelObjectStyle(ChannelObjectKind.Boxy, Color(1f, 0.28f, 0.72f, 1f), Color(0.45f, 0.15f, 0.39f, 1f), Color(1f, 0.84f, 0.96f, 1f), objectId)
        "microphone" -> ChannelObjectStyle(ChannelObjectKind.Cylinder, Color(0.20f, 0.88f, 1f, 1f), Color(0.12f, 0.14f, 0.23f, 1f), Color.WHITE, objectId)
        "note" -> ChannelObjectStyle(ChannelObjectKind.Flat, Color(0.48f, 1f, 1f, 1f), Color(0.13f, 0.38f, 1f, 1f), Color.WHITE, objectId)
        "speaker" -> ChannelObjectStyle(ChannelObjectKind.Boxy, Color(0.40f, 0.48f, 1f, 1f), Color(0.09f, 0.10f, 0.22f, 1f), Color(0.38f, 0.97f, 1f, 1f), objectId)
        "laptop" -> ChannelObjectStyle(ChannelObjectKind.Flat, Color(0.16f, 0.55f, 1f, 1f), Color(0.07f, 0.09f, 0.18f, 1f), Color(0.30f, 1f, 0.75f, 1f), objectId)
        "chip" -> ChannelObjectStyle(ChannelObjectKind.Boxy, Color(0.20f, 1f, 0.75f, 1f), Color(0.08f, 0.22f, 0.18f, 1f), Color(1f, 0.94f, 0.46f, 1f), objectId)
        "controller" -> ChannelObjectStyle(ChannelObjectKind.Boxy, Color(0.52f, 0.40f, 1f, 1f), Color(0.14f, 0.10f, 0.28f, 1f), Color(1f, 0.40f, 0.84f, 1f), objectId)
        "newspaper" -> ChannelObjectStyle(ChannelObjectKind.Flat, Color(0.90f, 0.92f, 0.96f, 1f), Color(0.54f, 0.57f, 0.65f, 1f), Color(0.18f, 0.42f, 1f, 1f), objectId)
        "globe" -> ChannelObjectStyle(ChannelObjectKind.Orb, Color(0.24f, 0.64f, 1f, 1f), Color(0.06f, 0.23f, 0.60f, 1f), Color(0.35f, 1f, 0.68f, 1f), objectId)
        "eiffel" -> ChannelObjectStyle(ChannelObjectKind.Trophy, Color(0.22f, 0.36f, 1f, 1f), Color(0.09f, 0.13f, 0.37f, 1f), Color(0.69f, 0.76f, 1f, 1f), objectId)
        "croissant" -> ChannelObjectStyle(ChannelObjectKind.Curved, Color(1f, 0.66f, 0.24f, 1f), Color(0.72f, 0.39f, 0.11f, 1f), Color(1f, 0.88f, 0.60f, 1f), objectId)
        "beret" -> ChannelObjectStyle(ChannelObjectKind.Orb, Color(0.10f, 0.16f, 0.82f, 1f), Color(0.05f, 0.07f, 0.24f, 1f), Color(0.68f, 0.73f, 1f, 1f), objectId)
        "guitar" -> ChannelObjectStyle(ChannelObjectKind.Curved, Color(0.94f, 0.45f, 0.15f, 1f), Color(0.48f, 0.20f, 0.08f, 1f), Color(1f, 0.83f, 0.64f, 1f), objectId)
        "sun" -> ChannelObjectStyle(ChannelObjectKind.Orb, Color(1f, 0.82f, 0.20f, 1f), Color(1f, 0.48f, 0.10f, 1f), Color(1f, 0.95f, 0.65f, 1f), objectId)
        "fan" -> ChannelObjectStyle(ChannelObjectKind.Star, Color(1f, 0.22f, 0.30f, 1f), Color(0.38f, 0.10f, 0.16f, 1f), Color(1f, 0.70f, 0.73f, 1f), objectId)
        else -> ChannelObjectStyle(ChannelObjectKind.Star, Color.WHITE, Color(0.49f, 0.54f, 1f, 1f), Color(1f, 0.91f, 0.42f, 1f), objectId)
    }

    private fun buildCurvedObject(mb: ModelBuilder, attr: Long, style: ChannelObjectStyle) {
        val primary = objectMaterial(style.primary)
        val secondary = objectMaterial(style.secondary)
        val accent = objectMaterial(style.accent)

        when (style.variant) {
            "shoe" -> {
                addBox(mb, attr, "shoe-sole", secondary, 1.55f, 0.20f, 0.46f, objectTransform(y = -0.28f, rotateZ = -7f))
                addSphere(mb, attr, "shoe-upper", primary, 0.95f, 0.38f, 0.54f, objectTransform(x = -0.18f, y = -0.04f, rotateZ = -10f))
                addSphere(mb, attr, "shoe-toe", accent, 0.55f, 0.30f, 0.50f, objectTransform(x = 0.54f, y = -0.12f, rotateZ = -8f))
                repeat(3) { index ->
                    addCylinder(mb, attr, "shoe-lace-$index", accent, 0.03f, 0.52f, 0.03f, objectTransform(x = -0.14f + index * 0.15f, y = 0.11f, z = 0.03f, rotateZ = 68f))
                }
            }
            "guitar" -> {
                addSphere(mb, attr, "guitar-body-low", primary, 0.72f, 0.85f, 0.35f, objectTransform(x = -0.28f, y = -0.12f, rotateZ = -8f))
                addSphere(mb, attr, "guitar-body-high", primary, 0.54f, 0.62f, 0.32f, objectTransform(x = 0.08f, y = 0.18f, rotateZ = -8f))
                addCylinder(mb, attr, "guitar-neck", secondary, 0.12f, 1.12f, 0.12f, objectTransform(x = 0.56f, y = 0.28f, rotateZ = -54f))
                addBox(mb, attr, "guitar-head", accent, 0.32f, 0.18f, 0.16f, objectTransform(x = 0.96f, y = 0.66f, rotateZ = -54f))
                addCylinder(mb, attr, "guitar-hole", secondary, 0.23f, 0.05f, 0.23f, objectTransform(x = -0.15f, y = 0.02f, rotateX = 90f))
            }
            else -> {
                val isCroissant = style.variant == "croissant"
                val count = if (isCroissant) 7 else 6
                repeat(count) { index ->
                    val t = (index - (count - 1) / 2f) / ((count - 1) / 2f)
                    val x = t * 0.52f
                    val y = if (isCroissant) -0.14f * (1f - t * t) else -0.22f * t * t + 0.08f
                    val width = if (isCroissant) 0.42f else 0.50f
                    val height = if (isCroissant) 0.34f + (1f - kotlin.math.abs(t)) * 0.13f else 0.30f
                    val material = if (index % 2 == 0) primary else accent
                    addSphere(mb, attr, "curve-segment-$index", material, width, height, 0.34f, objectTransform(x = x, y = y, rotateZ = t * 24f))
                }
                addSphere(mb, attr, "curve-tip-left", secondary, 0.18f, 0.16f, 0.18f, objectTransform(x = -0.72f, y = -0.18f, rotateZ = -32f))
                addSphere(mb, attr, "curve-tip-right", secondary, 0.18f, 0.16f, 0.18f, objectTransform(x = 0.72f, y = -0.18f, rotateZ = 32f))
            }
        }
    }

    private fun buildOrbObject(mb: ModelBuilder, attr: Long, style: ChannelObjectStyle) {
        val primary = objectMaterial(style.primary)
        val secondary = objectMaterial(style.secondary)
        val accent = objectMaterial(style.accent)

        when (style.variant) {
            "sun" -> {
                addSphere(mb, attr, "sun-core", primary, 1.05f, 1.05f, 1.05f)
                repeat(12) { index ->
                    val angle = Math.toRadians((index * 30).toDouble())
                    val x = cos(angle).toFloat() * 0.78f
                    val y = sin(angle).toFloat() * 0.78f
                    addCone(mb, attr, "sun-ray-$index", secondary, 0.20f, 0.52f, 0.20f, objectTransform(x = x, y = y, rotateZ = index * 30f - 90f))
                }
            }
            "globe" -> {
                addSphere(mb, attr, "globe-core", primary, 1.25f, 1.25f, 1.25f)
                addSphere(mb, attr, "globe-land-a", accent, 0.42f, 0.18f, 0.08f, objectTransform(x = -0.28f, y = 0.22f, z = 0.54f, rotateZ = -18f))
                addSphere(mb, attr, "globe-land-b", accent, 0.34f, 0.16f, 0.08f, objectTransform(x = 0.32f, y = -0.12f, z = 0.55f, rotateZ = 24f))
                addCylinder(mb, attr, "globe-equator", objectMaterial(Color(1f, 1f, 1f, 0.78f)), 1.35f, 0.035f, 1.35f, objectTransform(rotateX = 90f))
            }
            "beret" -> {
                addSphere(mb, attr, "beret-crown", primary, 1.30f, 0.38f, 0.90f, objectTransform(y = -0.04f))
                addSphere(mb, attr, "beret-brim", secondary, 0.62f, 0.14f, 0.82f, objectTransform(y = -0.22f, rotateZ = 3f))
                addCylinder(mb, attr, "beret-stem", accent, 0.08f, 0.18f, 0.08f, objectTransform(y = 0.23f))
            }
            "fruit" -> {
                addSphere(mb, attr, "fruit-body", primary, 1.18f, 1.18f, 1.12f)
                addCylinder(mb, attr, "fruit-stem", secondary, 0.08f, 0.32f, 0.08f, objectTransform(y = 0.72f, rotateZ = -12f))
                addSphere(mb, attr, "fruit-leaf", accent, 0.34f, 0.14f, 0.20f, objectTransform(x = 0.24f, y = 0.77f, rotateZ = 24f))
            }
            "ball" -> {
                addSphere(mb, attr, "ball-core", primary, 1.18f, 1.18f, 1.18f)
                addCylinder(mb, attr, "ball-band-x", accent, 1.20f, 0.035f, 1.20f, objectTransform(rotateX = 90f))
                addCylinder(mb, attr, "ball-band-y", secondary, 1.20f, 0.035f, 1.20f, objectTransform(rotateZ = 90f))
            }
            else -> addSphere(mb, attr, "orb-core", primary, 1.20f, 1.20f, 1.20f)
        }
    }

    private fun buildCylinderObject(mb: ModelBuilder, attr: Long, style: ChannelObjectStyle) {
        val primary = objectMaterial(style.primary)
        val secondary = objectMaterial(style.secondary)
        val accent = objectMaterial(style.accent)

        when (style.variant) {
            "cake" -> {
                addCylinder(mb, attr, "cake-base", primary, 1.18f, 0.58f, 1.18f, objectTransform(y = -0.12f))
                addCylinder(mb, attr, "cake-frosting", secondary, 1.24f, 0.18f, 1.24f, objectTransform(y = 0.25f))
                addCylinder(mb, attr, "cake-candle", accent, 0.08f, 0.46f, 0.08f, objectTransform(y = 0.62f))
                addCone(mb, attr, "cake-flame", objectMaterial(Color(1f, 0.84f, 0.12f, 1f)), 0.17f, 0.28f, 0.17f, objectTransform(y = 0.98f))
            }
            "lipstick" -> {
                addCylinder(mb, attr, "lipstick-case", secondary, 0.44f, 0.82f, 0.44f, objectTransform(y = -0.22f))
                addCylinder(mb, attr, "lipstick-color", primary, 0.34f, 0.55f, 0.34f, objectTransform(y = 0.38f))
                addCone(mb, attr, "lipstick-tip", primary, 0.34f, 0.32f, 0.34f, objectTransform(y = 0.82f))
                addCylinder(mb, attr, "lipstick-ring", accent, 0.48f, 0.08f, 0.48f, objectTransform(y = 0.04f))
            }
            "microphone" -> {
                addCapsule(mb, attr, "mic-head", primary, 0.28f, 0.72f, objectTransform(y = 0.34f))
                addCylinder(mb, attr, "mic-grille-a", secondary, 0.62f, 0.035f, 0.62f, objectTransform(y = 0.48f, rotateX = 90f))
                addCylinder(mb, attr, "mic-grille-b", secondary, 0.55f, 0.035f, 0.55f, objectTransform(y = 0.24f, rotateX = 90f))
                addCylinder(mb, attr, "mic-stem", secondary, 0.12f, 0.58f, 0.12f, objectTransform(y = -0.35f))
                addCylinder(mb, attr, "mic-base", accent, 0.82f, 0.10f, 0.82f, objectTransform(y = -0.72f))
            }
            "dumbbell" -> {
                addCylinder(mb, attr, "dumbbell-bar", secondary, 0.12f, 1.38f, 0.12f, objectTransform(rotateZ = 90f))
                listOf(-0.66f, -0.48f, 0.48f, 0.66f).forEachIndexed { index, x ->
                    addCylinder(mb, attr, "dumbbell-plate-$index", primary, 0.42f, 0.16f, 0.42f, objectTransform(x = x, rotateZ = 90f))
                }
                addSphere(mb, attr, "dumbbell-center", accent, 0.22f, 0.22f, 0.22f)
            }
            else -> {
                addCylinder(mb, attr, "cylinder-core", primary, 0.94f, 1.18f, 0.94f)
                addCylinder(mb, attr, "cylinder-top", accent, 0.98f, 0.08f, 0.98f, objectTransform(y = 0.64f))
            }
        }
    }

    private fun buildPropObject(mb: ModelBuilder, attr: Long, style: ChannelObjectStyle) {
        val primary = objectMaterial(style.primary)
        val secondary = objectMaterial(style.secondary)
        val accent = objectMaterial(style.accent)

        when (style.variant) {
            "camera" -> {
                addBox(mb, attr, "camera-body", primary, 1.24f, 0.72f, 0.42f, objectTransform(y = -0.02f))
                addBox(mb, attr, "camera-bump", secondary, 0.42f, 0.22f, 0.34f, objectTransform(x = -0.32f, y = 0.46f))
                addCylinder(mb, attr, "camera-lens", accent, 0.56f, 0.32f, 0.56f, objectTransform(z = 0.28f, rotateX = 90f))
                addCylinder(mb, attr, "camera-lens-core", secondary, 0.34f, 0.36f, 0.34f, objectTransform(z = 0.38f, rotateX = 90f))
                addSphere(mb, attr, "camera-flash", objectMaterial(Color.WHITE), 0.14f, 0.14f, 0.06f, objectTransform(x = 0.42f, y = 0.18f, z = 0.24f))
            }
            "bag" -> {
                addBox(mb, attr, "bag-body", primary, 1.02f, 0.82f, 0.40f, objectTransform(y = -0.14f))
                addCylinder(mb, attr, "bag-handle-left", accent, 0.08f, 0.54f, 0.08f, objectTransform(x = -0.28f, y = 0.43f, rotateZ = -24f))
                addCylinder(mb, attr, "bag-handle-right", accent, 0.08f, 0.54f, 0.08f, objectTransform(x = 0.28f, y = 0.43f, rotateZ = 24f))
                addSphere(mb, attr, "bag-shine", accent, 0.20f, 0.20f, 0.06f, objectTransform(x = -0.24f, y = 0.04f, z = 0.24f))
            }
            "speaker" -> {
                addBox(mb, attr, "speaker-body", secondary, 0.82f, 1.25f, 0.46f)
                addCylinder(mb, attr, "speaker-woofer", primary, 0.52f, 0.14f, 0.52f, objectTransform(y = -0.26f, z = 0.30f, rotateX = 90f))
                addCylinder(mb, attr, "speaker-tweeter", accent, 0.32f, 0.12f, 0.32f, objectTransform(y = 0.36f, z = 0.30f, rotateX = 90f))
            }
            "chip" -> {
                addBox(mb, attr, "chip-board", primary, 0.92f, 0.92f, 0.12f, objectTransform(rotateZ = 5f))
                addBox(mb, attr, "chip-core", secondary, 0.44f, 0.44f, 0.16f, objectTransform(z = 0.06f, rotateZ = 5f))
                repeat(6) { index ->
                    val p = -0.42f + index * 0.17f
                    addBox(mb, attr, "chip-pin-top-$index", accent, 0.035f, 0.18f, 0.035f, objectTransform(x = p, y = 0.56f))
                    addBox(mb, attr, "chip-pin-bottom-$index", accent, 0.035f, 0.18f, 0.035f, objectTransform(x = p, y = -0.56f))
                    addBox(mb, attr, "chip-pin-left-$index", accent, 0.18f, 0.035f, 0.035f, objectTransform(x = -0.56f, y = p))
                    addBox(mb, attr, "chip-pin-right-$index", accent, 0.18f, 0.035f, 0.035f, objectTransform(x = 0.56f, y = p))
                }
            }
            "controller" -> {
                addSphere(mb, attr, "controller-body", primary, 1.28f, 0.58f, 0.36f, objectTransform(y = -0.02f))
                addSphere(mb, attr, "controller-left-grip", secondary, 0.48f, 0.52f, 0.34f, objectTransform(x = -0.52f, y = -0.16f, rotateZ = -18f))
                addSphere(mb, attr, "controller-right-grip", secondary, 0.48f, 0.52f, 0.34f, objectTransform(x = 0.52f, y = -0.16f, rotateZ = 18f))
                addCylinder(mb, attr, "controller-button-a", accent, 0.16f, 0.07f, 0.16f, objectTransform(x = 0.32f, y = 0.10f, z = 0.24f, rotateX = 90f))
                addCylinder(mb, attr, "controller-button-b", objectMaterial(Color.WHITE), 0.13f, 0.07f, 0.13f, objectTransform(x = 0.52f, y = -0.03f, z = 0.24f, rotateX = 90f))
                addBox(mb, attr, "controller-dpad-h", objectMaterial(Color.WHITE), 0.34f, 0.08f, 0.05f, objectTransform(x = -0.34f, y = 0.02f, z = 0.24f))
                addBox(mb, attr, "controller-dpad-v", objectMaterial(Color.WHITE), 0.08f, 0.34f, 0.05f, objectTransform(x = -0.34f, y = 0.02f, z = 0.25f))
            }
            else -> {
                addBox(mb, attr, "prop-body", primary, 1.10f, 0.70f, 0.40f)
                addSphere(mb, attr, "prop-detail", accent, 0.34f, 0.34f, 0.12f, objectTransform(z = 0.28f))
            }
        }
    }

    private fun buildFlatObject(mb: ModelBuilder, attr: Long, style: ChannelObjectStyle) {
        val primary = objectMaterial(style.primary)
        val secondary = objectMaterial(style.secondary)
        val accent = objectMaterial(style.accent)

        when (style.variant) {
            "laptop" -> {
                addBox(mb, attr, "laptop-screen", primary, 1.24f, 0.76f, 0.08f, objectTransform(y = 0.22f, rotateX = -10f))
                addBox(mb, attr, "laptop-keyboard", secondary, 1.36f, 0.10f, 0.72f, objectTransform(y = -0.38f, z = 0.20f, rotateX = 74f))
                addBox(mb, attr, "laptop-trackpad", accent, 0.34f, 0.04f, 0.18f, objectTransform(y = -0.46f, z = 0.55f, rotateX = 74f))
            }
            "note" -> {
                addCylinder(mb, attr, "note-stem", secondary, 0.08f, 1.04f, 0.08f, objectTransform(x = 0.25f, y = 0.12f, rotateZ = -8f))
                addBox(mb, attr, "note-flag", primary, 0.58f, 0.16f, 0.08f, objectTransform(x = 0.02f, y = 0.58f, rotateZ = -8f))
                addSphere(mb, attr, "note-head", primary, 0.52f, 0.34f, 0.18f, objectTransform(x = -0.28f, y = -0.46f, rotateZ = -8f))
            }
            "newspaper" -> {
                addBox(mb, attr, "paper-body", primary, 1.28f, 0.90f, 0.08f, objectTransform(rotateZ = -7f))
                repeat(4) { index ->
                    addBox(mb, attr, "paper-line-$index", secondary, 0.78f, 0.035f, 0.04f, objectTransform(x = 0.08f, y = 0.26f - index * 0.16f, z = 0.08f, rotateZ = -7f))
                }
                addBox(mb, attr, "paper-photo", accent, 0.30f, 0.22f, 0.05f, objectTransform(x = -0.38f, y = -0.18f, z = 0.08f, rotateZ = -7f))
            }
            else -> {
                addCylinder(mb, attr, "compact-base", primary, 1.10f, 0.14f, 1.10f, objectTransform(rotateX = 90f))
                addCylinder(mb, attr, "compact-lid", secondary, 0.88f, 0.08f, 0.88f, objectTransform(y = 0.12f, rotateX = 90f))
                addCylinder(mb, attr, "compact-shine", accent, 0.40f, 0.04f, 0.40f, objectTransform(y = 0.18f, z = 0.10f, rotateX = 90f))
            }
        }
    }

    private fun buildTrophyObject(mb: ModelBuilder, attr: Long, style: ChannelObjectStyle) {
        val primary = objectMaterial(style.primary)
        val secondary = objectMaterial(style.secondary)
        val accent = objectMaterial(style.accent)

        when (style.variant) {
            "dress" -> {
                addCone(mb, attr, "dress-skirt", primary, 1.12f, 0.92f, 1.12f, objectTransform(y = -0.16f))
                addSphere(mb, attr, "dress-bodice", accent, 0.46f, 0.38f, 0.34f, objectTransform(y = 0.44f))
                addCylinder(mb, attr, "dress-belt", secondary, 0.50f, 0.08f, 0.50f, objectTransform(y = 0.14f))
            }
            "spotlight" -> {
                addCylinder(mb, attr, "spotlight-body", primary, 0.70f, 0.90f, 0.70f, objectTransform(x = -0.20f, rotateZ = -22f))
                addCone(mb, attr, "spotlight-beam", accent, 1.08f, 0.92f, 1.08f, objectTransform(x = 0.55f, y = 0.04f, rotateZ = -90f))
                addCylinder(mb, attr, "spotlight-stand", secondary, 0.10f, 0.80f, 0.10f, objectTransform(y = -0.50f))
            }
            "eiffel" -> {
                addCone(mb, attr, "eiffel-body", primary, 0.82f, 1.42f, 0.82f, objectTransform(y = 0.02f))
                addBox(mb, attr, "eiffel-base", secondary, 1.02f, 0.12f, 0.42f, objectTransform(y = -0.74f))
                addBox(mb, attr, "eiffel-deck-a", accent, 0.64f, 0.08f, 0.34f, objectTransform(y = -0.28f))
                addBox(mb, attr, "eiffel-deck-b", accent, 0.42f, 0.06f, 0.26f, objectTransform(y = 0.22f))
            }
            else -> {
                addCone(mb, attr, "trophy-cup", primary, 0.98f, 0.82f, 0.98f, objectTransform(y = 0.18f, rotateX = 180f))
                addCylinder(mb, attr, "trophy-stem", secondary, 0.18f, 0.45f, 0.18f, objectTransform(y = -0.38f))
                addCylinder(mb, attr, "trophy-base", primary, 0.78f, 0.14f, 0.78f, objectTransform(y = -0.70f))
                addSphere(mb, attr, "trophy-left-handle", accent, 0.24f, 0.46f, 0.12f, objectTransform(x = -0.56f, y = 0.16f))
                addSphere(mb, attr, "trophy-right-handle", accent, 0.24f, 0.46f, 0.12f, objectTransform(x = 0.56f, y = 0.16f))
            }
        }
    }

    private fun buildStarObject(mb: ModelBuilder, attr: Long, style: ChannelObjectStyle) {
        val primary = objectMaterial(style.primary)
        val secondary = objectMaterial(style.secondary)
        val accent = objectMaterial(style.accent)

        if (style.variant == "fan") {
            repeat(3) { index ->
                val degrees = index * 120f
                val radians = Math.toRadians(degrees.toDouble())
                addSphere(
                    mb,
                    attr,
                    "fan-blade-$index",
                    primary,
                    0.78f,
                    0.22f,
                    0.18f,
                    objectTransform(x = cos(radians).toFloat() * 0.34f, y = sin(radians).toFloat() * 0.34f, rotateZ = degrees)
                )
            }
            addSphere(mb, attr, "fan-hub", accent, 0.34f, 0.34f, 0.24f)
            return
        }

        addSphere(mb, attr, "star-core", accent, 0.42f, 0.42f, 0.36f)
        repeat(5) { index ->
            val degrees = index * 72f - 90f
            val radians = Math.toRadians(degrees.toDouble())
            addCone(
                mb,
                attr,
                "star-point-$index",
                if (index % 2 == 0) primary else secondary,
                0.34f,
                0.74f,
                0.34f,
                objectTransform(x = cos(radians).toFloat() * 0.42f, y = sin(radians).toFloat() * 0.42f, rotateZ = degrees - 90f)
            )
        }
    }

    private fun objectMaterial(color: Color): Material = Material(
        ColorAttribute.createDiffuse(Color(color))
    )

    private fun addSphere(
        mb: ModelBuilder,
        attr: Long,
        id: String,
        material: Material,
        width: Float,
        height: Float,
        depth: Float,
        transform: Matrix4 = Matrix4()
    ) {
        mb.objectPart(id, attr, material, transform).sphere(width, height, depth, 32, 18)
    }

    private fun addCylinder(
        mb: ModelBuilder,
        attr: Long,
        id: String,
        material: Material,
        width: Float,
        height: Float,
        depth: Float,
        transform: Matrix4 = Matrix4()
    ) {
        mb.objectPart(id, attr, material, transform).cylinder(width, height, depth, 32)
    }

    private fun addCone(
        mb: ModelBuilder,
        attr: Long,
        id: String,
        material: Material,
        width: Float,
        height: Float,
        depth: Float,
        transform: Matrix4 = Matrix4()
    ) {
        mb.objectPart(id, attr, material, transform).cone(width, height, depth, 32)
    }

    private fun addBox(
        mb: ModelBuilder,
        attr: Long,
        id: String,
        material: Material,
        width: Float,
        height: Float,
        depth: Float,
        transform: Matrix4 = Matrix4()
    ) {
        mb.objectPart(id, attr, material, transform).box(width, height, depth)
    }

    private fun addCapsule(
        mb: ModelBuilder,
        attr: Long,
        id: String,
        material: Material,
        radius: Float,
        height: Float,
        transform: Matrix4 = Matrix4()
    ) {
        mb.objectPart(id, attr, material, transform).capsule(radius, height.coerceAtLeast(radius * 2.1f), 32)
    }

    private fun ModelBuilder.objectPart(
        id: String,
        attr: Long,
        material: Material,
        transform: Matrix4
    ): MeshPartBuilder {
        val part = part(id, GL20.GL_TRIANGLES, attr, material)
        part.setVertexTransform(transform)
        return part
    }

    private fun objectTransform(
        x: Float = 0f,
        y: Float = 0f,
        z: Float = 0f,
        rotateX: Float = 0f,
        rotateY: Float = 0f,
        rotateZ: Float = 0f
    ): Matrix4 = Matrix4().apply {
        idt()
        translate(x, y, z)
        if (rotateX != 0f) rotate(Vector3.X, rotateX)
        if (rotateY != 0f) rotate(Vector3.Y, rotateY)
        if (rotateZ != 0f) rotate(Vector3.Z, rotateZ)
    }

    private data class ChannelObjectStyle(
        val kind: ChannelObjectKind,
        val primary: Color,
        val secondary: Color,
        val accent: Color,
        val variant: String
    )

    private enum class ChannelObjectKind {
        Curved,
        Orb,
        Cylinder,
        Boxy,
        Flat,
        Trophy,
        Star
    }

    private fun skinBaseColor(skinOverride: CubeSkin?): Color {
        return skinOverride?.gdxColor?.let { Color(it) } ?: Color(0.04f, 0.03f, 0.10f, 1f)
    }

    private fun skinBorderColor(skinOverride: CubeSkin?): Color {
        return skinOverride?.gdxColor?.let {
            Color(
                (it.r + 0.18f).coerceAtMost(1f),
                (it.g + 0.18f).coerceAtMost(1f),
                (it.b + 0.18f).coerceAtMost(1f),
                1f
            )
        } ?: Color(0.55f, 0.30f, 1.0f, 1f)
    }

    private fun skinIconColor(skinOverride: CubeSkin?): Color {
        return when (skinOverride) {
            CubeSkin.Gold -> Color(1.00f, 0.95f, 0.60f, 1f)
            CubeSkin.Chocolate -> Color(0.92f, 0.65f, 0.25f, 1f)
            CubeSkin.Camo -> Color(0.82f, 0.84f, 0.56f, 1f)
            CubeSkin.Ice -> Color(0.82f, 0.96f, 1.00f, 1f)
            else -> Color.WHITE
        }
    }

    private fun drawSkinFaceBackground(
        sr: ShapeRenderer,
        size: Float,
        cornerRadius: Float,
        baseColor: Color,
        skinOverride: CubeSkin?
    ) {
        sr.color = if (skinOverride == null) {
            baseColor
        } else {
            Color(
                (baseColor.r * 0.48f).coerceIn(0f, 1f),
                (baseColor.g * 0.48f).coerceIn(0f, 1f),
                (baseColor.b * 0.48f).coerceIn(0f, 1f),
                1f
            )
        }
        sr.filledRoundRect(0f, 0f, size, size, cornerRadius)

        if (skinOverride == null) return

        val pad = size * 0.045f
        sr.color = Color(
            (baseColor.r * 1.32f).coerceIn(0f, 1f),
            (baseColor.g * 1.32f).coerceIn(0f, 1f),
            (baseColor.b * 1.32f).coerceIn(0f, 1f),
            0.92f
        )
        sr.filledRoundRect(pad, pad, size - pad * 2f, size - pad * 2f, (cornerRadius - pad).coerceAtLeast(8f))

        sr.color = Color(1f, 1f, 1f, if (skinOverride == CubeSkin.Ice) 0.30f else 0.14f)
        sr.rectLine(size * 0.18f, size * 0.82f, size * 0.78f, size * 0.20f, size * 0.018f)

        sr.color = Color(0f, 0f, 0f, if (skinOverride == CubeSkin.Toy || skinOverride == CubeSkin.Ice) 0.10f else 0.24f)
        sr.filledRoundRect(size * 0.08f, size * 0.08f, size * 0.84f, size * 0.22f, cornerRadius * 0.45f)
    }

    private fun createFaceTexture(title: String, skinOverride: CubeSkin? = null, faceIndex: Int? = null): Texture {
        val size = 384
        val displayTitle = when (title) {
            "Record" -> "Camera"
            "TopFork", "BottomP" -> ""
            else -> title
        }
        val fbo = try {
            FrameBuffer(Pixmap.Format.RGBA8888, size, size, false)
        } catch (e: Exception) {
            // FBO creation might fail if GL context is unstable or out of memory
            return Texture(size, size, Pixmap.Format.RGBA8888) 
        }
        val projection = Matrix4().setToOrtho2D(0f, 0f, size.toFloat(), size.toFloat())
        val sr = shapeRenderer ?: ShapeRenderer().also { shapeRenderer = it; it.setAutoShapeType(true) }
        val batch = textureBatch ?: SpriteBatch().also { textureBatch = it }
        val skinBackground = createSkinFaceBackgroundTexture(skinOverride, faceIndex, size)

        fbo.begin()
        Gdx.gl.glViewport(0, 0, size, size)
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        batch.projectionMatrix = projection
        skinBackground?.let { background ->
            batch.begin()
            batch.draw(background, 0f, 0f, size.toFloat(), size.toFloat())
            batch.end()
        }
        sr.projectionMatrix = projection
        sr.begin(ShapeRenderer.ShapeType.Filled)

        val bgColor = skinBaseColor(skinOverride)
        val borderColor = skinBorderColor(skinOverride)
        val iconColor = skinIconColor(skinOverride)
        val pad = 0f
        val r = cornerRadius
        if (skinBackground == null) {
            drawSkinFaceBackground(sr, size.toFloat(), r, bgColor, skinOverride)
        }

        if (skinBackground == null) {
            // Skin-matched neon border glow (18 passes, inward from edge).
            for (i in 18 downTo 1) {
                val inset = i.toFloat() * 1.8f
                val alpha = (1f - i.toFloat() / 18f) * 0.55f
                sr.color = Color(borderColor.r, borderColor.g, borderColor.b, alpha)
                sr.filledRoundRect(inset, inset, size - inset * 2f, size - inset * 2f, (r - inset).coerceAtLeast(4f))
            }
            // Crisp inner border
            val crisp = 5f
            sr.color = Color(
                (borderColor.r + 0.22f).coerceAtMost(1f),
                (borderColor.g + 0.22f).coerceAtMost(1f),
                (borderColor.b + 0.22f).coerceAtMost(1f),
                0.92f
            )
            sr.filledRoundRect(crisp, crisp, size - crisp * 2f, size - crisp * 2f, (r - crisp).coerceAtLeast(4f))
            // Re-fill interior with background so only border is left
            val innerPad = crisp + 2.5f
            sr.color = bgColor
            sr.filledRoundRect(innerPad, innerPad, size - innerPad * 2f, size - innerPad * 2f, (r - innerPad).coerceAtLeast(4f))
        }

        // Soft radial glow behind icon — cy chosen so content (icon+gap+label) is vertically centered
        val cx = size / 2f
        val cy = size * 0.605f
        val isize = 108f
        for (i in 14 downTo 1) {
            val frac = i.toFloat() / 14f
            sr.color = Color(iconColor.r, iconColor.g, iconColor.b, 0.018f * frac)
            sr.circle(cx, cy, isize * 0.55f + i * 9f, 44)
        }

        // Icon: dynamically drawn so skin cubemap artwork never locks in stale symbols.
        sr.color = iconColor
        when (title) {
            "Camera", "Record", "Create", "Photo", "Post Here" -> {
                // camera.fill: rounded-rect body + viewfinder bump + lens circle
                val bodyW = isize * 0.74f; val bodyH = isize * 0.48f
                val bodyX = cx - bodyW / 2f; val bodyY = cy - bodyH / 2f - isize * 0.04f
                sr.filledRoundRect(bodyX, bodyY, bodyW, bodyH, isize * 0.10f)
                // Viewfinder bump on top-center
                val bumpW = isize * 0.22f; val bumpH = isize * 0.13f
                sr.rect(cx - bumpW / 2f, bodyY + bodyH - isize * 0.01f, bumpW, bumpH)
                // Lens: cutout (dark) then lens ring
                sr.color = bgColor
                sr.circle(cx, cy - isize * 0.04f, isize * 0.20f, 36)
                sr.color = iconColor
                sr.circle(cx, cy - isize * 0.04f, isize * 0.16f, 36)
                sr.color = bgColor
                sr.circle(cx, cy - isize * 0.04f, isize * 0.08f, 28)
                if (title == "Create" || title == "Post Here") {
                    sr.color = iconColor
                    sr.rectLine(cx + isize * 0.24f, cy + isize * 0.18f, cx + isize * 0.24f, cy + isize * 0.38f, isize * 0.055f)
                    sr.rectLine(cx + isize * 0.14f, cy + isize * 0.28f, cx + isize * 0.34f, cy + isize * 0.28f, isize * 0.055f)
                }
            }
            "Feed", "TV Guide" -> {
                // tv.fill: TV screen rectangle + antenna/stand
                val tvW = isize * 0.68f; val tvH = isize * 0.50f
                val tvX = cx - tvW / 2f; val tvY = cy - tvH / 2f
                sr.filledRoundRect(tvX, tvY, tvW, tvH, isize * 0.09f)
                // Screen cutout (bezel → inner screen)
                val bezel = isize * 0.075f
                sr.color = bgColor
                sr.filledRoundRect(tvX + bezel, tvY + bezel, tvW - bezel * 2f, tvH - bezel * 2f, isize * 0.04f)
                sr.color = iconColor
                // Stand base under TV
                val standW = isize * 0.24f; val standH = isize * 0.07f
                sr.rect(cx - standW / 2f, tvY - standH, standW, standH)
                // Feet
                sr.rect(cx - standW / 2f, tvY - standH - isize * 0.04f, isize * 0.06f, isize * 0.04f)
                sr.rect(cx + standW / 2f - isize * 0.06f, tvY - standH - isize * 0.04f, isize * 0.06f, isize * 0.04f)
                if (title == "TV Guide") {
                    repeat(3) { row ->
                        val y = tvY + tvH - bezel * 1.8f - row * isize * 0.11f
                        sr.rect(tvX + bezel * 1.55f, y, tvW - bezel * 3.1f, isize * 0.035f)
                    }
                }
            }
            "Profile" -> {
                // person.circle: ring with person silhouette inside
                sr.circle(cx, cy, isize * 0.38f, 44)
                sr.color = bgColor
                sr.circle(cx, cy, isize * 0.29f, 44)
                sr.color = iconColor
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
            "2D Map", "3D Map" -> {
                val pw = isize * 0.20f; val gap = isize * 0.035f
                sr.rect(cx - pw * 1.5f - gap, cy - isize * 0.22f, pw, isize * 0.44f)
                sr.rect(cx - pw / 2f, cy - isize * 0.30f, pw, isize * 0.60f)
                sr.rect(cx + pw / 2f + gap, cy - isize * 0.22f, pw, isize * 0.44f)
                if (title == "3D Map") {
                    sr.rectLine(cx - isize * 0.26f, cy + isize * 0.34f, cx + isize * 0.22f, cy + isize * 0.34f, isize * 0.045f)
                    sr.rectLine(cx + isize * 0.22f, cy + isize * 0.34f, cx + isize * 0.36f, cy + isize * 0.20f, isize * 0.045f)
                }
            }
            "Search", "Detect On", "Detect Off" -> {
                val ring = isize * 0.26f
                sr.circle(cx - isize * 0.06f, cy + isize * 0.05f, ring, 44)
                sr.color = bgColor
                sr.circle(cx - isize * 0.06f, cy + isize * 0.05f, ring * 0.70f, 44)
                sr.color = iconColor
                sr.rectLine(cx + isize * 0.14f, cy - isize * 0.15f, cx + isize * 0.36f, cy - isize * 0.36f, isize * 0.10f)
                if (title.startsWith("Detect")) {
                    sr.circle(cx - isize * 0.06f, cy + isize * 0.05f, isize * 0.065f, 20)
                }
            }
            "Location" -> {
                sr.circle(cx, cy + isize * 0.12f, isize * 0.27f, 44)
                sr.triangle(cx - isize * 0.20f, cy - isize * 0.02f, cx + isize * 0.20f, cy - isize * 0.02f, cx, cy - isize * 0.40f)
                sr.color = bgColor
                sr.circle(cx, cy + isize * 0.12f, isize * 0.11f, 24)
            }
            "Start Tour", "Stop Tour" -> {
                val routeStroke = isize * 0.07f
                sr.rectLine(cx - isize * 0.34f, cy - isize * 0.20f, cx - isize * 0.08f, cy + isize * 0.16f, routeStroke)
                sr.rectLine(cx - isize * 0.08f, cy + isize * 0.16f, cx + isize * 0.30f, cy - isize * 0.06f, routeStroke)
                sr.circle(cx - isize * 0.34f, cy - isize * 0.20f, isize * 0.08f, 20)
                sr.circle(cx + isize * 0.30f, cy - isize * 0.06f, isize * 0.08f, 20)
                if (title == "Start Tour") {
                    sr.triangle(cx - isize * 0.03f, cy - isize * 0.40f, cx - isize * 0.03f, cy - isize * 0.18f, cx + isize * 0.16f, cy - isize * 0.29f)
                } else {
                    sr.rect(cx - isize * 0.10f, cy - isize * 0.39f, isize * 0.20f, isize * 0.20f)
                }
            }
            "Video" -> {
                val bodyW = isize * 0.54f; val bodyH = isize * 0.42f
                sr.filledRoundRect(cx - bodyW * 0.58f, cy - bodyH / 2f, bodyW, bodyH, isize * 0.09f)
                sr.triangle(cx + bodyW * 0.06f, cy + bodyH * 0.24f, cx + bodyW * 0.06f, cy - bodyH * 0.24f, cx + bodyW * 0.44f, cy - bodyH * 0.02f)
            }
            "Live" -> {
                sr.circle(cx, cy, isize * 0.13f, 28)
                repeat(3) { ring ->
                    val radius = isize * (0.23f + ring * 0.11f)
                    sr.rectLine(cx - radius, cy, cx - radius * 0.64f, cy + radius * 0.28f, isize * 0.035f)
                    sr.rectLine(cx - radius, cy, cx - radius * 0.64f, cy - radius * 0.28f, isize * 0.035f)
                    sr.rectLine(cx + radius, cy, cx + radius * 0.64f, cy + radius * 0.28f, isize * 0.035f)
                    sr.rectLine(cx + radius, cy, cx + radius * 0.64f, cy - radius * 0.28f, isize * 0.035f)
                }
            }
            "AR", "Poplur AI", "Generating" -> {
                fun sparkle(x: Float, y: Float, s: Float) {
                    sr.rectLine(x, y - s, x, y + s, isize * 0.045f)
                    sr.rectLine(x - s, y, x + s, y, isize * 0.045f)
                }
                sparkle(cx, cy + isize * 0.10f, isize * 0.28f)
                sparkle(cx - isize * 0.30f, cy - isize * 0.18f, isize * 0.14f)
                sparkle(cx + isize * 0.28f, cy - isize * 0.24f, isize * 0.12f)
            }
            "Games" -> {
                val bodyW = isize * 0.72f; val bodyH = isize * 0.36f
                sr.filledRoundRect(cx - bodyW / 2f, cy - bodyH / 2f, bodyW, bodyH, isize * 0.18f)
                sr.color = bgColor
                sr.rect(cx - isize * 0.24f, cy - isize * 0.03f, isize * 0.18f, isize * 0.06f)
                sr.rect(cx - isize * 0.18f, cy - isize * 0.09f, isize * 0.06f, isize * 0.18f)
                sr.circle(cx + isize * 0.18f, cy + isize * 0.05f, isize * 0.045f, 18)
                sr.circle(cx + isize * 0.28f, cy - isize * 0.05f, isize * 0.045f, 18)
            }
            "Preview" -> {
                sr.triangle(cx - isize * 0.22f, cy + isize * 0.32f, cx - isize * 0.22f, cy - isize * 0.32f, cx + isize * 0.34f, cy)
            }
            "Like" -> {
                sr.circle(cx - isize * 0.14f, cy + isize * 0.13f, isize * 0.18f, 32)
                sr.circle(cx + isize * 0.14f, cy + isize * 0.13f, isize * 0.18f, 32)
                sr.triangle(cx - isize * 0.31f, cy + isize * 0.08f, cx + isize * 0.31f, cy + isize * 0.08f, cx, cy - isize * 0.34f)
            }
            "Comment", "Message", "Add Message" -> {
                val bw = isize * 0.72f; val bh = isize * 0.46f
                val bx = cx - bw / 2f; val by = cy - bh / 2f + isize * 0.04f
                sr.filledRoundRect(bx, by, bw, bh, isize * 0.12f)
                sr.triangle(cx - isize * 0.16f, by + isize * 0.02f, cx + isize * 0.02f, by + isize * 0.02f, cx - isize * 0.14f, by - isize * 0.18f)
                sr.color = bgColor
                repeat(3) { dot ->
                    sr.circle(cx - isize * 0.18f + dot * isize * 0.18f, cy + isize * 0.06f, isize * 0.04f, 16)
                }
            }
            "News" -> {
                val nw = isize * 0.68f; val nh = isize * 0.56f
                val nx = cx - nw / 2f; val ny = cy - nh / 2f
                sr.filledRoundRect(nx, ny, nw, nh, isize * 0.04f)
                sr.color = bgColor
                sr.rect(nx + isize * 0.08f, ny + nh - isize * 0.18f, nw - isize * 0.16f, isize * 0.055f)
                repeat(3) { row ->
                    sr.rect(nx + isize * 0.08f, ny + nh - isize * 0.30f - row * isize * 0.10f, nw - isize * 0.16f, isize * 0.035f)
                }
            }
            "CC On", "CC Off" -> {
                val bw = isize * 0.70f; val bh = isize * 0.42f
                sr.filledRoundRect(cx - bw / 2f, cy - bh / 2f, bw, bh, isize * 0.10f)
                sr.color = bgColor
                sr.rectLine(cx - isize * 0.22f, cy, cx - isize * 0.05f, cy, isize * 0.06f)
                sr.rectLine(cx + isize * 0.05f, cy, cx + isize * 0.22f, cy, isize * 0.06f)
            }
            "Back" -> {
                sr.rectLine(cx + isize * 0.28f, cy, cx - isize * 0.28f, cy, isize * 0.10f)
                sr.triangle(cx - isize * 0.35f, cy, cx - isize * 0.08f, cy + isize * 0.24f, cx - isize * 0.08f, cy - isize * 0.24f)
            }
            "TopFork" -> {
                val stroke = isize * 0.07f
                val forkX = cx - isize * 0.16f
                sr.rectLine(forkX, cy - isize * 0.34f, forkX, cy + isize * 0.28f, stroke)
                repeat(3) { prong ->
                    val px = forkX - isize * 0.10f + prong * isize * 0.10f
                    sr.rectLine(px, cy + isize * 0.18f, px, cy + isize * 0.38f, stroke * 0.62f)
                }
                val knifeX = cx + isize * 0.18f
                sr.rectLine(knifeX, cy - isize * 0.34f, knifeX, cy + isize * 0.34f, stroke)
                sr.triangle(
                    knifeX - stroke * 0.38f, cy + isize * 0.08f,
                    knifeX + isize * 0.18f, cy + isize * 0.36f,
                    knifeX + stroke * 0.44f, cy + isize * 0.03f
                )
            }
            "BottomP" -> {
                sr.circle(cx, cy, isize * 0.39f, 48)
                sr.color = bgColor
                sr.circle(cx, cy, isize * 0.29f, 48)
                sr.color = iconColor
            }
            "Mail" -> {
                val ew = isize * 0.74f; val eh = isize * 0.50f
                val el = cx - ew / 2f; val eb = cy - eh / 2f; val b = isize * 0.08f
                sr.rect(el, eb, ew, eh)
                sr.color = bgColor
                sr.rect(el + b, eb + b, ew - b * 2, eh - b * 2)
                sr.color = iconColor
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
            "Stats" -> {
                val stroke = isize * 0.12f
                sr.circle(cx, cy, isize * 0.35f, 44)
                sr.color = bgColor
                sr.circle(cx, cy, isize * 0.35f - stroke, 44)
                sr.color = iconColor
                sr.rect(cx - isize * 0.03f, cy, isize * 0.06f, isize * 0.34f)
                sr.rect(cx - isize * 0.03f, cy - isize * 0.03f, isize * 0.34f, isize * 0.06f)
                sr.circle(cx + isize * 0.31f, cy - isize * 0.03f, isize * 0.07f, 18)
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
            f.setColor(iconColor)
            if (title == "BottomP") {
                val pLayout = GlyphLayout(f, "P")
                f.draw(batch, "P", (size - pLayout.width) / 2f, cy + pLayout.height / 2f)
            }
            if (displayTitle.isNotEmpty()) {
                val l = GlyphLayout(f, displayTitle)
                f.draw(batch, displayTitle, (size - l.width) / 2f, size * 0.280f)
            }
        }
        batch.end()

            val pixmap = Pixmap.createFromFrameBuffer(0, 0, size, size)
            fbo.end()
            makeDrawnPixelsOpaque(pixmap)
            val tex = Texture(pixmap)
            tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
            tex.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
            pixmap.dispose()
            skinBackground?.dispose()
            fbo.dispose()
            return tex
        }

    private fun createSkinFaceBackgroundTexture(skinOverride: CubeSkin?, faceIndex: Int?, size: Int): Texture? {
        val path = skinOverride?.assetPath ?: return null
        val index = faceIndex ?: return null
        return runCatching {
            val source = Pixmap(Gdx.files.internal(path))
            val tileW = source.width / 4
            val tileH = source.height / 3
            val coords = listOf(2 to 1, 3 to 1, 0 to 1, 1 to 1, 1 to 0, 1 to 2)
            val (col, row) = coords[index.coerceIn(0, coords.lastIndex)]
            val tile = Pixmap(tileW, tileH, Pixmap.Format.RGBA8888)
            tile.drawPixmap(source, 0, 0, col * tileW, row * tileH, tileW, tileH)
            val content = tileContentBounds(tile, threshold = 20)
            val result = Pixmap(size, size, Pixmap.Format.RGBA8888)
            val edgeColor = skinOverride.gdxColor ?: Color(0.04f, 0.03f, 0.10f, 1f)
            result.setColor(edgeColor)
            result.fill()
            result.drawPixmap(
                tile,
                0,
                0,
                size,
                size,
                content.x,
                content.y,
                content.width,
                content.height
            )
            makeDrawnPixelsOpaque(result)
            val texture = Texture(result)
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
            texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
            source.dispose()
            tile.dispose()
            result.dispose()
            texture
        }.getOrNull()
    }

    private data class PixelBounds(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int
    )

    private fun tileContentBounds(pixmap: Pixmap, threshold: Int): PixelBounds {
        var minX = pixmap.width
        var minY = pixmap.height
        var maxX = -1
        var maxY = -1
        for (y in 0 until pixmap.height) {
            for (x in 0 until pixmap.width) {
                val pixel = pixmap.getPixel(x, y)
                val r = (pixel ushr 24) and 0xFF
                val g = (pixel ushr 16) and 0xFF
                val b = (pixel ushr 8) and 0xFF
                if (r > threshold || g > threshold || b > threshold) {
                    if (x < minX) minX = x
                    if (y < minY) minY = y
                    if (x > maxX) maxX = x
                    if (y > maxY) maxY = y
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            return PixelBounds(0, 0, pixmap.width, pixmap.height)
        }
        return PixelBounds(minX, minY, maxX - minX + 1, maxY - minY + 1)
    }

    private fun makeDrawnPixelsOpaque(pixmap: Pixmap) {
        for (y in 0 until pixmap.height) {
            for (x in 0 until pixmap.width) {
                val pixel = pixmap.getPixel(x, y)
                val alpha = pixel and 0xFF
                if (alpha > 2) {
                    pixmap.drawPixel(x, y, pixel or 0xFF)
                }
            }
        }
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
            longPressSent = true; longPressStarted = true; lastX = Gdx.input.x.toFloat(); lastY = Gdx.input.y.toFloat(); appInstance?.postRunnable { onLongPress?.invoke() }
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
        lastWidth = width
        lastHeight = height
        camera?.apply {
            viewportWidth = width.toFloat().coerceAtLeast(1f)
            viewportHeight = height.toFloat().coerceAtLeast(1f)
            position.set(0f, 0f, cameraDistance())
            lookAt(0f, 0f, 0f)
            update()
        }
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
            updatePeekAnimation(deltaTime)
            
            val w = lastWidth.coerceAtLeast(1)
            val h = lastHeight.coerceAtLeast(1)
            Gdx.gl.glViewport(0, 0, w, h)
            Gdx.gl.glColorMask(true, true, true, true)
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

            val now = System.currentTimeMillis()
            val replacementActive = now < channelObjectEndMillis
            val popAge = (now - channelObjectStartedMillis).coerceAtLeast(0L)
            val popScale = when {
                !replacementActive -> 1f
                popAge < 180L -> Interpolation.swingOut.apply(popAge / 180f)
                else -> 1f
            }

            if (replacementActive) {
                channelObjectInstance?.let { obj ->
                    val scale = cubeTransformScale() * popScale.coerceIn(0.08f, 1.18f)
                    obj.transform.setToTranslation(cubeSceneX(), cubeSceneY() + kotlin.math.sin(time * 3.9f) * 0.035f, 0f)
                        .rotate(Vector3.Y, currentYawDegrees + time * 28f)
                        .rotate(Vector3.X, if (channelObjectId == "banana" || channelObjectId == "shoe" || channelObjectId == "guitar") 18f else 0f)
                        .scale(scale, scale, scale)
                    modelBatch?.begin(camera); modelBatch?.render(obj, environment); modelBatch?.end()
                }
                Gdx.graphics.requestRendering()
            } else {
                instance?.let { cube ->
                val scale = cubeTransformScale()
                cube.transform.setToTranslation(cubeSceneX(), cubeSceneY() + kotlin.math.sin(time * 3.9f) * 0.035f, 0f)
                    .rotate(Vector3.Y, currentYawDegrees + GdxState.tiltYawOffset)
                    .scale(scale, scale, scale)
                modelBatch?.begin(camera); modelBatch?.render(cube, environment); modelBatch?.end()
                }
            }
        } catch (e: Exception) {
            Log.e("CombinedGdxAdapter", "Error in render()", e)
        }
    }

    private fun updatePeekAnimation(delta: Float) {
        // Peek animation disabled — cube only rotates on Y axis
        peekPitchDegrees = 0f
        peekPhase = PeekPhase.Idle
    }

    override fun dispose() {
        modelBatch?.dispose()
        model?.dispose()
        channelObjectModel?.dispose()
        // textureCache owns all Texture objects; textures[] just holds references into it.
        textureCache.values.forEach { it.dispose() }
        textureCache.clear()
        skinFaceTextures.forEach { it?.dispose() }
        font?.dispose()
        textureBatch?.dispose(); shapeRenderer?.dispose()
    }

    private fun snappedTabIndex(yaw: Float): Int {
        val raw = (-yaw / 90f).roundToInt(); return ((raw % 4) + 4) % 4
    }

    private fun continuousYawForTab(targetIndex: Int): Float {
        val target = targetIndex.coerceIn(0, 3)
        val currentIndex = snappedTabIndex(currentYawDegrees)
        val forwardSteps = (target - currentIndex + 4) % 4
        val backwardSteps = (currentIndex - target + 4) % 4
        val signedSteps = when {
            forwardSteps == 0 -> 0
            forwardSteps <= backwardSteps -> forwardSteps
            else -> -backwardSteps
        }
        val currentFaceYaw = -90f * currentIndex
        val currentRevolution = ((currentYawDegrees - currentFaceYaw) / 360f).roundToInt() * 360f
        return currentFaceYaw + currentRevolution - (90f * signedSteps)
    }

    private enum class BouncePhase { Idle, Overshoot, Settle }

    private fun cameraDistance(): Float {
        return 3.0f
    }
}
