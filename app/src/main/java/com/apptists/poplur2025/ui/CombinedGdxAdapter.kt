package com.apptists.poplur2025.ui

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import net.mgsx.gltf.loaders.glb.GLBLoader
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneAsset
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.PI
import android.util.Log

class CombinedGdxAdapter(
    private val initialTabIndex: Int = 2,
    private val onVerticalSwipe: ((Boolean) -> Unit)? = null,
    private val onLongPress: (() -> Unit)? = null,
    private val onLongPressMove: ((Float, Float) -> Unit)? = null,
    private val onLongPressEnd: (() -> Unit)? = null,
    private val onTap: (() -> Unit)? = null,
    private val onFirstFrameRendered: (() -> Unit)? = null,
    private val onTabSelected: (Int) -> Unit
) : ApplicationAdapter() {

    @Volatile
    var isInteractionEnabled: Boolean = true

    private var camera: PerspectiveCamera? = null
    private var modelBatch: ModelBatch? = null
    private var channelObjectModel: Model? = null
    private var channelObjectInstance: ModelInstance? = null
    private var channelObjectSceneAsset: SceneAsset? = null
    private var channelObjectScene: Scene? = null
    private val channelObjectCenter = Vector3()
    private var channelObjectBaseScale = 1f
    private var channelObjectEndMillis: Long = 0L
    private var channelObjectStartedMillis: Long = 0L
    private var channelObjectId: String = "star"
    private var environment: Environment? = null
    private var cubeSceneAsset: SceneAsset? = null
    private var cubeScene: Scene? = null
    private var cubeSceneAssetPath: String? = null
    private val cubeModelCenter = Vector3()
    private var cubeModelBaseScale = 1f
    private val cubeRestingPitchDegrees = -10f

    var skin: CubeSkin = CubeSkin.Gold
        set(value) {
            if (field != value) {
                field = value
                modelDirty = true
                appInstance?.postRunnable { Gdx.graphics.requestRendering() }
            }
        }

    var modelAssetPathOverride: String? = null
        set(value) {
            if (field != value) {
                field = value
                firstFrameReported = false
                modelDirty = true
                appInstance?.postRunnable { Gdx.graphics.requestRendering() }
            }
        }

    @Volatile private var modelDirty = false
    private var appInstance: com.badlogic.gdx.Application? = null

    var cubeScale: Float = 0.65f
        set(value) {
            if (field != value) {
                field = value
            }
        }

    var cornerRadius: Float = 92f

    var opacity: Float = 1.0f

    var mapIconTitle: String = "Map"
        set(value) {
            field = value.ifBlank { "Map" }
        }

    var currentIconTitle: String = "Camera"
        set(value) {
            field = normalizeCameraTitle(value)
        }

    var feedIconTitle: String = "Feed"
        set(value) {
            field = value.ifBlank { "Feed" }
        }

    var profileIconTitle: String = "Profile"
        set(value) {
            field = value.ifBlank { "Profile" }
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

    var yawDegrees: Float = yawForPhysicalIndex(physicalIndexForTab(initialTabIndex))
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
            startBounceTo(continuousYawForTab(targetIndex))
            Gdx.graphics.requestRendering()
        }
    }

    fun showChannelObject(objectId: String) {
        appInstance?.postRunnable {
            channelObjectId = objectId
            channelObjectStartedMillis = System.currentTimeMillis()
            channelObjectEndMillis = channelObjectStartedMillis + 2_000L
            disposeChannelObject()
            PopularObjectCatalog.assetPath(objectId)?.let { loadChannelObjectScene(it) }
            if (channelObjectScene == null) {
                channelObjectModel = buildChannelObject(objectId)
                channelObjectInstance = channelObjectModel?.let { ModelInstance(it) }
            }
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

    private var lastWidth = 0
    private var lastHeight = 0
    @Volatile private var firstFrameReported = false
    private var firstFrameTime = 0L

    override fun create() {
        try {
            appInstance = Gdx.app
            Gdx.graphics.setContinuousRendering(false)
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

            environment = Environment().apply {
                set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.45f, 1f))
                add(DirectionalLight().set(0.95f, 0.90f, 1.00f, -0.45f, -0.62f, -0.78f))
                add(DirectionalLight().set(0.35f, 0.30f, 0.40f, 0.45f, 0.62f, 0.78f))
            }

            rebuildModel()

            Gdx.input.inputProcessor = object : InputAdapter() {
                override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                    if (!isInteractionEnabled) return false
                    if (pointer != 0) return false  // single-touch only — prevents multi-finger twists
                    if (!isInsideCubeTouchTarget(screenX.toFloat(), screenY.toFloat())) return false
                    dragging = true; isVerticalSwipe = false; isSwipeTriggered = false; isRotation = false; longPressSent = false; longPressStarted = false; bouncePhase = BouncePhase.Idle; downX = screenX.toFloat(); downY = screenY.toFloat(); lastX = screenX.toFloat(); lastY = screenY.toFloat(); touchStartMillis = System.currentTimeMillis()
                    if (peekPhase != PeekPhase.Idle) { peekPitchDegrees = 0f; peekPhase = PeekPhase.Idle; nextPeekSeconds = peekIntervalSeconds }
                    Gdx.graphics.requestRendering()
                    return true
                }
                override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                    if (!isInteractionEnabled) return false
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
                    Gdx.graphics.requestRendering()
                    lastX = screenX.toFloat(); lastY = screenY.toFloat(); return true
                }
                override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                    if (pointer != 0) return false  // single-touch only
                    dragging = false
                    Gdx.graphics.requestRendering()
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
                    val snappedPhysicalIndex = snappedPhysicalIndex(currentYawDegrees)
                    val snappedIndex = tabIndexForPhysicalIndex(snappedPhysicalIndex)
                    val canonicalYaw = yawForPhysicalIndex(snappedPhysicalIndex)
                    val nearestYaw = canonicalYaw + ((currentYawDegrees - canonicalYaw) / 360f).roundToInt() * 360f
                    startBounceTo(nearestYaw)
                    if (isRotation || abs(screenX - downX) > 15f) appInstance?.postRunnable { onTabSelected(snappedIndex) }
                    return true
                }
            }
            Gdx.graphics.requestRendering()
        } catch (e: Exception) {
            Log.e("CombinedGdxAdapter", "CRITICAL ERROR in create()", e)
            throw e
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

    private fun rebuildModel() {
        val modelPath = modelAssetPathOverride ?: skin.modelAssetPath

        if (cubeScene == null || cubeSceneAssetPath != modelPath) {
            loadCubeScene(modelPath)
        }
    }

    private fun loadCubeScene(modelPath: String) {
        try {
            cubeScene = null
            cubeSceneAsset?.dispose()
            cubeSceneAsset = null

            val loadedAsset = GLBLoader().load(requiredCubeGlbBytes(modelPath))
                ?: error("Required cube model could not be loaded: $modelPath")
            val loadedScene = Scene(loadedAsset.scene)
            cubeSceneAsset = loadedAsset
            cubeScene = loadedScene
            cubeSceneAssetPath = modelPath
            prepareCubeMaterials(loadedScene)
            fitCubeModel(loadedScene)
            Log.d("CombinedGdxAdapter", "Loaded required cube GLB: $modelPath")
        } catch (error: Throwable) {
            Log.e("CombinedGdxAdapter", "Required cube model failed to load: $modelPath", error)
            throw error
        }
    }

    private fun requiredCubeGlbBytes(modelPath: String): ByteArray {
        val file = Gdx.files.internal(modelPath)
        if (!file.exists()) {
            error("Required cube model asset is missing: $modelPath")
        }
        return stripNormalTexturesFromGlb(file.readBytes())
    }

    private fun stripNormalTexturesFromGlb(glbBytes: ByteArray): ByteArray {
        if (glbBytes.size < GLB_HEADER_BYTES) error("Invalid cube GLB: file is too small.")

        val source = ByteBuffer.wrap(glbBytes).order(ByteOrder.LITTLE_ENDIAN)
        val magic = source.int
        val version = source.int
        source.int // declared length
        if (magic != GLB_MAGIC || version != GLB_VERSION) {
            error("Invalid cube GLB header.")
        }

        val chunks = mutableListOf<GlbChunk>()
        var jsonChunkIndex = -1
        while (source.position() < glbBytes.size) {
            if (source.remaining() < GLB_CHUNK_HEADER_BYTES) error("Invalid cube GLB chunk header.")
            val chunkLength = source.int
            val chunkType = source.int
            if (chunkLength < 0 || chunkLength > source.remaining()) {
                error("Invalid cube GLB chunk length.")
            }
            val chunkBytes = ByteArray(chunkLength)
            source.get(chunkBytes)
            if (chunkType == GLB_CHUNK_TYPE_JSON) jsonChunkIndex = chunks.size
            chunks += GlbChunk(chunkType, chunkBytes)
        }
        if (jsonChunkIndex < 0) error("Required cube GLB is missing a JSON chunk.")

        val originalJson = chunks[jsonChunkIndex].bytes
            .toString(StandardCharsets.UTF_8)
            .trimEnd(' ', '\u0000', '\n', '\r', '\t')
        val json = JSONObject(originalJson)
        var removedNormalMaps = 0
        json.optJSONArray("materials")?.let { materials ->
            for (index in 0 until materials.length()) {
                val material = materials.optJSONObject(index) ?: continue
                if (material.has("normalTexture")) {
                    material.remove("normalTexture")
                    removedNormalMaps += 1
                }
            }
        }
        val sanitizedJson = json.toString().toByteArray(StandardCharsets.UTF_8)
        chunks[jsonChunkIndex] = GlbChunk(
            GLB_CHUNK_TYPE_JSON,
            paddedChunk(sanitizedJson, GLB_JSON_PADDING_BYTE)
        )

        val totalLength = GLB_HEADER_BYTES + chunks.sumOf { GLB_CHUNK_HEADER_BYTES + it.bytes.size }
        val output = ByteBuffer.allocate(totalLength).order(ByteOrder.LITTLE_ENDIAN)
        output.putInt(GLB_MAGIC)
        output.putInt(GLB_VERSION)
        output.putInt(totalLength)
        chunks.forEach { chunk ->
            output.putInt(chunk.bytes.size)
            output.putInt(chunk.type)
            output.put(chunk.bytes)
        }
        if (removedNormalMaps > 0) {
            Log.d("CombinedGdxAdapter", "Removed $removedNormalMaps cube normal map declaration(s) to avoid GLB tangent generation.")
        }
        return output.array()
    }

    private fun paddedChunk(bytes: ByteArray, paddingByte: Byte): ByteArray {
        val paddedLength = (bytes.size + 3) and -4
        if (paddedLength == bytes.size) return bytes
        return bytes.copyOf(paddedLength).also { padded ->
            for (index in bytes.size until paddedLength) padded[index] = paddingByte
        }
    }

    private fun prepareCubeMaterials(loadedScene: Scene) {
        loadedScene.modelInstance.materials.forEach { material ->
            val baseColor = material.get(
                PBRTextureAttribute::class.java,
                PBRTextureAttribute.BaseColorTexture
            )
            val emissive = material.get(
                PBRTextureAttribute::class.java,
                PBRTextureAttribute.EmissiveTexture
            )
            material.clear()
            material.set(ColorAttribute.createDiffuse(Color.WHITE))
            baseColor?.let {
                material.set(TextureAttribute(TextureAttribute.Diffuse, it.textureDescription))
            }
            emissive?.let {
                material.set(TextureAttribute(TextureAttribute.Emissive, it.textureDescription))
            }
            material.set(IntAttribute.createCullFace(GL20.GL_NONE))
            material.set(FloatAttribute.createAlphaTest(0.01f))
        }
    }

    private fun fitCubeModel(loadedScene: Scene) {
        val bounds = BoundingBox()
        val dimensions = Vector3()
        loadedScene.modelInstance.calculateTransforms()
        loadedScene.modelInstance.calculateBoundingBox(bounds)
        bounds.getCenter(cubeModelCenter)
        bounds.getDimensions(dimensions)
        val largestExtent = maxOf(dimensions.x, dimensions.y, dimensions.z).coerceAtLeast(0.001f)
        cubeModelBaseScale = 2f / largestExtent
        Log.d(
            "CombinedGdxAdapter",
            "Cube GLB bounds center=$cubeModelCenter dimensions=$dimensions baseScale=$cubeModelBaseScale"
        )
    }

    private fun loadChannelObjectScene(assetPath: String) {
        try {
            val file = Gdx.files.internal(assetPath)
            if (!file.exists()) {
                Log.e("CombinedGdxAdapter", "Popular object GLB missing: $assetPath")
                return
            }
            val loadedAsset = GLBLoader().load(stripNormalTexturesFromGlb(file.readBytes())) ?: return
            val loadedScene = Scene(loadedAsset.scene)
            channelObjectSceneAsset = loadedAsset
            channelObjectScene = loadedScene
            prepareCubeMaterials(loadedScene)
            fitChannelObjectScene(loadedScene)
            Log.d("CombinedGdxAdapter", "Loaded popular object GLB: $assetPath")
        } catch (error: Throwable) {
            Log.e("CombinedGdxAdapter", "Popular object GLB failed to load: $assetPath", error)
            channelObjectScene = null
            channelObjectSceneAsset?.dispose()
            channelObjectSceneAsset = null
        }
    }

    private fun fitChannelObjectScene(loadedScene: Scene) {
        val bounds = BoundingBox()
        val dimensions = Vector3()
        loadedScene.modelInstance.calculateTransforms()
        loadedScene.modelInstance.calculateBoundingBox(bounds)
        bounds.getCenter(channelObjectCenter)
        bounds.getDimensions(dimensions)
        val largestExtent = maxOf(dimensions.x, dimensions.y, dimensions.z).coerceAtLeast(0.001f)
        channelObjectBaseScale = 2f / largestExtent
    }

    private fun renderCubeScene(now: Long, popScale: Float) {
        val loadedScene = cubeScene ?: return

        val scale = cubeTransformScale() * cubeModelBaseScale * popScale
        loadedScene.modelInstance.transform
            .idt()
            .translate(
                cubeSceneX(),
                cubeSceneY() + kotlin.math.sin(time * 3.9f) * 0.045f, // Increased hover amount
                0f
            )
            .rotate(Vector3.X, cubeRestingPitchDegrees)
            .rotate(Vector3.Y, currentYawDegrees + GdxState.tiltYawOffset)
            .scale(scale, scale, scale)
            .translate(-cubeModelCenter.x, -cubeModelCenter.y, -cubeModelCenter.z)
        loadedScene.modelInstance.calculateTransforms()
        Gdx.gl.glDisable(GL20.GL_CULL_FACE)
        modelBatch?.begin(camera)
        modelBatch?.render(loadedScene.modelInstance, environment)
        modelBatch?.end()
        reportFirstFrameRendered()
        if (dragging || now < channelObjectEndMillis || bouncePhase != BouncePhase.Idle || GdxState.tiltYawOffset != 0f) {
            Gdx.graphics.requestRendering()
        }
    }

    private fun reportFirstFrameRendered() {
        if (firstFrameReported) return
        firstFrameReported = true
        firstFrameTime = System.currentTimeMillis()
        Log.d("CombinedGdxAdapter", "Cube first frame rendered")
        onFirstFrameRendered?.invoke()
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

    private fun startBounceTo(target: Float) {
        Gdx.graphics.requestRendering()
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
            if (modelDirty) {
                rebuildModel()
                modelDirty = false
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
                channelObjectScene?.let { scene ->
                    val scale = cubeTransformScale() * channelObjectBaseScale * popScale.coerceIn(0.08f, 1.18f)
                    scene.modelInstance.transform
                        .idt()
                        .translate(cubeSceneX(), cubeSceneY() + kotlin.math.sin(time * 3.9f) * 0.035f, 0f)
                        .rotate(Vector3.X, cubeRestingPitchDegrees)
                        .rotate(Vector3.Y, currentYawDegrees + time * 28f)
                        .scale(scale, scale, scale)
                        .translate(-channelObjectCenter.x, -channelObjectCenter.y, -channelObjectCenter.z)
                    scene.modelInstance.calculateTransforms()
                    Gdx.gl.glDisable(GL20.GL_CULL_FACE)
                    modelBatch?.begin(camera); modelBatch?.render(scene.modelInstance, environment); modelBatch?.end()
                } ?: channelObjectInstance?.let { obj ->
                    val scale = cubeTransformScale() * popScale.coerceIn(0.08f, 1.18f)
                    obj.transform.setToTranslation(cubeSceneX(), cubeSceneY() + kotlin.math.sin(time * 3.9f) * 0.035f, 0f)
                        .rotate(Vector3.Y, currentYawDegrees + time * 28f)
                        .rotate(Vector3.X, if (channelObjectId == "banana" || channelObjectId == "shoe" || channelObjectId == "guitar") 18f else 0f)
                        .scale(scale, scale, scale)
                    modelBatch?.begin(camera); modelBatch?.render(obj, environment); modelBatch?.end()
                }
                Gdx.graphics.requestRendering()
            } else {
                renderCubeScene(now, 1f)
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
        cubeSceneAsset?.dispose()
        modelBatch?.dispose()
        disposeChannelObject()
    }

    private fun disposeChannelObject() {
        channelObjectModel?.dispose()
        channelObjectModel = null
        channelObjectInstance = null
        channelObjectScene = null
        channelObjectSceneAsset?.dispose()
        channelObjectSceneAsset = null
    }

    private fun snappedPhysicalIndex(yaw: Float): Int {
        val raw = (-yaw / 90f).roundToInt(); return ((raw % 4) + 4) % 4
    }

    private fun physicalIndexForTab(tabIndex: Int): Int =
        (tabIndex.coerceIn(0, 3) + CUBE_GLB_FACE_OFFSET) % 4

    private fun tabIndexForPhysicalIndex(physicalIndex: Int): Int =
        ((physicalIndex - CUBE_GLB_FACE_OFFSET) % 4 + 4) % 4

    private fun yawForPhysicalIndex(physicalIndex: Int): Float = -90f * physicalIndex.coerceIn(0, 3)

    private fun continuousYawForTab(targetIndex: Int): Float {
        val target = physicalIndexForTab(targetIndex)
        val currentIndex = snappedPhysicalIndex(currentYawDegrees)
        val forwardSteps = (target - currentIndex + 4) % 4
        val backwardSteps = (currentIndex - target + 4) % 4
        val signedSteps = when {
            forwardSteps == 0 -> 0
            forwardSteps <= backwardSteps -> forwardSteps
            else -> -backwardSteps
        }
        val currentFaceYaw = yawForPhysicalIndex(currentIndex)
        val currentRevolution = ((currentYawDegrees - currentFaceYaw) / 360f).roundToInt() * 360f
        return currentFaceYaw + currentRevolution - (90f * signedSteps)
    }

    private enum class BouncePhase { Idle, Overshoot, Settle }

    private data class GlbChunk(val type: Int, val bytes: ByteArray)

    private fun cameraDistance(): Float {
        return 6.0f
    }

    private companion object {
        const val GLB_MAGIC = 0x46546C67
        const val GLB_VERSION = 2
        const val GLB_HEADER_BYTES = 12
        const val GLB_CHUNK_HEADER_BYTES = 8
        const val GLB_CHUNK_TYPE_JSON = 0x4E4F534A
        const val CUBE_GLB_FACE_OFFSET = 1
        val GLB_JSON_PADDING_BYTE: Byte = 0x20
    }
}
