package com.apptists.poplur2025.gdx

import android.util.Log
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import net.mgsx.gltf.loaders.glb.GLBLoader
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneAsset
import net.mgsx.gltf.scene3d.scene.SceneManager
import kotlin.math.abs
import kotlin.math.sin

class MarkHeadAdapter : ApplicationAdapter() {
    @Volatile private var speaking: Boolean = false

    private var camera: PerspectiveCamera? = null
    private var sceneManager: SceneManager? = null
    private var scene: Scene? = null
    private var sceneAsset: SceneAsset? = null
    private var fallbackModel: Model? = null
    private var appInstance: com.badlogic.gdx.Application? = null
    private var graphicsRef: com.badlogic.gdx.Graphics? = null
    private val modelCenter = Vector3()
    private var modelScale = 1f
    private var modelYOffset = 0f
    private var elapsed = 0f
    private val fallbackTextures = mutableListOf<Texture>()

    override fun create() {
        try {
            appInstance = Gdx.app
            graphicsRef = Gdx.graphics  // capture now, on GL thread, before cube can clobber the global
            graphicsRef?.setContinuousRendering(true)  // always animate; speaking flag controls head movement
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
            Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
            Gdx.gl.glEnable(GL20.GL_BLEND)
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

            val width = Gdx.graphics.width.toFloat().coerceAtLeast(1f)
            val height = Gdx.graphics.height.toFloat().coerceAtLeast(1f)
            camera = PerspectiveCamera(34f, width, height).apply {
                position.set(0f, 0.15f, 4.2f)
                lookAt(0f, 0.10f, 0f)
                near = 0.05f
                far = 100f
                update()
            }

            sceneManager = SceneManager().apply {
                setCamera(camera)
                setAmbientLight(0.95f)
                environment.add(
                    DirectionalLightEx().set(
                        Color(1f, 1f, 1f, 1f),
                        Vector3(-0.4f, -0.6f, -0.7f),
                        3.5f
                    )
                )
            }

            val loadedScene = tryLoadGlb() ?: buildFallbackHead()
            scene = loadedScene
            fitModel(loadedScene)
            sceneManager?.addScene(loadedScene)

            graphicsRef?.requestRendering()
        } catch (error: Throwable) {
            Log.e(TAG, "3D Mark head construction failed.", error)
            runCatching { graphicsRef?.setContinuousRendering(false) }
            scene = null
        }
    }

    private fun tryLoadGlb(): Scene? {
        val glbPaths = listOf(
            "mark_head/MarkHead.glb",
            "mark_head/mark_head.glb",
            "mark_head/MarkHead_TalkingRig.glb"
        )
        for (glbPath in glbPaths) {
            val file = Gdx.files.internal(glbPath)
            if (!file.exists()) continue
            val byteSize = runCatching { file.length() }.getOrDefault(0L)
            if (byteSize > MAX_MOBILE_GLB_BYTES) {
                Log.w(TAG, "Skipping oversized Mark head GLB $glbPath (${byteSize / (1024 * 1024)} MB). Using procedural 3D fallback.")
                continue
            }
            runCatching {
                val asset = GLBLoader().load(file)
                sceneAsset = asset
                Log.i(TAG, "Loaded $glbPath - animations: ${asset.animations?.size ?: 0}")
                return Scene(asset.scene)
            }.onFailure { error ->
                Log.w(TAG, "GLB load failed for $glbPath", error)
            }
        }
        Log.w(TAG, "No usable Mark head GLB found. Using procedural 3D fallback.")
        return null
    }

    private fun buildFallbackHead(): Scene {
        listOf(
            "mark_head/MarkHead_texture_0001.jpg",
            "mark_head/MarkHead_texture_0002.jpg",
            "mark_head/MarkHead_texture_0003.jpg"
        ).forEach { path ->
            if (Gdx.files.internal(path).exists()) {
                runCatching {
                    fallbackTextures.add(Texture(Gdx.files.internal(path)).apply {
                        setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                    })
                }
            }
        }
        val attr = (Usage.Position or Usage.Normal or Usage.TextureCoordinates).toLong()

        val skin = Material(ColorAttribute.createDiffuse(Color(1f, 0.68f, 0.46f, 1f)))
        val hair = Material(ColorAttribute.createDiffuse(Color(0.23f, 0.11f, 0.04f, 1f)))
        val jacket = Material(ColorAttribute.createDiffuse(Color(0.76f, 0.04f, 0.06f, 1f)))
        val bow = Material(ColorAttribute.createDiffuse(Color(0.02f, 0.12f, 0.42f, 1f)))
        val blue = Material(ColorAttribute.createDiffuse(Color(0.02f, 0.23f, 0.62f, 1f)))
        val gold = Material(ColorAttribute.createDiffuse(Color(1f, 0.66f, 0.12f, 1f)))
        val white = Material(ColorAttribute.createDiffuse(Color.WHITE))
        val black = Material(ColorAttribute.createDiffuse(Color(0.02f, 0.02f, 0.025f, 1f)))

        val builder = ModelBuilder()
        builder.begin()
        builder.node().apply {
            id = "head"
            translation.set(0f, 0.42f, 0f)
        }
        builder.part("head", GL20.GL_TRIANGLES, attr, skin).sphere(1.12f, 1.24f, 1.02f, 40, 28)

        builder.node().apply {
            id = "hair"
            translation.set(0f, 1.05f, -0.02f)
        }
        builder.part("hair", GL20.GL_TRIANGLES, attr, hair).sphere(1.18f, 0.5f, 1.04f, 32, 14)

        builder.node().apply {
            id = "left_eye"
            translation.set(-0.25f, 0.56f, 0.51f)
        }
        builder.part("left_eye", GL20.GL_TRIANGLES, attr, white).sphere(0.2f, 0.2f, 0.07f, 20, 10)
        builder.node().apply {
            id = "left_pupil"
            translation.set(-0.25f, 0.56f, 0.56f)
        }
        builder.part("left_pupil", GL20.GL_TRIANGLES, attr, blue).sphere(0.09f, 0.09f, 0.04f, 14, 8)

        builder.node().apply {
            id = "right_eye"
            translation.set(0.25f, 0.56f, 0.51f)
        }
        builder.part("right_eye", GL20.GL_TRIANGLES, attr, white).sphere(0.2f, 0.2f, 0.07f, 20, 10)
        builder.node().apply {
            id = "right_pupil"
            translation.set(0.25f, 0.56f, 0.56f)
        }
        builder.part("right_pupil", GL20.GL_TRIANGLES, attr, blue).sphere(0.09f, 0.09f, 0.04f, 14, 8)

        builder.node().apply {
            id = "mouth"
            translation.set(0f, 0.12f, 0.56f)
        }
        builder.part("mouth", GL20.GL_TRIANGLES, attr, black).box(0.34f, 0.06f, 0.035f)

        builder.node().apply {
            id = "left_headphone"
            translation.set(-0.72f, 0.45f, 0f)
        }
        builder.part("left_headphone", GL20.GL_TRIANGLES, attr, blue).sphere(0.23f, 0.46f, 0.3f, 20, 12)
        builder.node().apply {
            id = "right_headphone"
            translation.set(0.72f, 0.45f, 0f)
        }
        builder.part("right_headphone", GL20.GL_TRIANGLES, attr, blue).sphere(0.23f, 0.46f, 0.3f, 20, 12)

        builder.node().apply {
            id = "mic"
            translation.set(0.58f, -0.02f, 0.54f)
        }
        builder.part("mic", GL20.GL_TRIANGLES, attr, blue).sphere(0.1f, 0.1f, 0.1f, 16, 8)

        builder.node().apply {
            id = "jacket"
            translation.set(0f, -0.88f, 0f)
        }
        builder.part("jacket", GL20.GL_TRIANGLES, attr, jacket).box(1.45f, 0.86f, 0.42f)

        builder.node().apply {
            id = "bow"
            translation.set(0f, -0.42f, 0.45f)
        }
        builder.part("bow", GL20.GL_TRIANGLES, attr, bow).box(0.48f, 0.24f, 0.08f)

        builder.node().apply {
            id = "left_epaulet"
            translation.set(-0.56f, -0.42f, 0.26f)
        }
        builder.part("left_epaulet", GL20.GL_TRIANGLES, attr, gold).box(0.42f, 0.1f, 0.12f)
        builder.node().apply {
            id = "right_epaulet"
            translation.set(0.56f, -0.42f, 0.26f)
        }
        builder.part("right_epaulet", GL20.GL_TRIANGLES, attr, gold).box(0.42f, 0.1f, 0.12f)

        fallbackModel = builder.end()
        return Scene(fallbackModel)
    }

    fun setSpeaking(active: Boolean) {
        speaking = active
        appInstance?.postRunnable {
            graphicsRef?.setContinuousRendering(true)  // always continuous; speaking controls animation style
            graphicsRef?.requestRendering()
        }
    }

    override fun resize(width: Int, height: Int) {
        val safeWidth = width.toFloat().coerceAtLeast(1f)
        val safeHeight = height.toFloat().coerceAtLeast(1f)
        camera?.apply {
            viewportWidth = safeWidth
            viewportHeight = safeHeight
            update()
        }
        sceneManager?.updateViewport(safeWidth, safeHeight)
    }

    override fun render() {
        elapsed += Gdx.graphics.deltaTime.coerceAtMost(0.05f)
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        val talkScale = if (speaking) 1f + abs(sin(elapsed * 18f)) * 0.02f else 1f
        val floatY = sin(elapsed * 1.25f) * 0.04f
        val yaw = sin(elapsed * 0.72f) * 5f
        val nod = if (speaking) sin(elapsed * 13f) * 2.0f else 0f

        val loadedScene = scene ?: run {
            graphicsRef?.setContinuousRendering(false)
            return
        }

        loadedScene.modelInstance.transform
            .idt()
            .translate(0f, floatY + modelYOffset, 0f)
            .rotate(Vector3.Y, yaw)
            .rotate(Vector3.X, nod)
            .scale(modelScale * talkScale, modelScale * talkScale, modelScale * talkScale)
            .translate(-modelCenter.x, -modelCenter.y, -modelCenter.z)

        sceneManager?.update(Gdx.graphics.deltaTime)
        sceneManager?.render()
    }

    override fun dispose() {
        scene?.let { sceneManager?.removeScene(it) }
        sceneManager?.dispose()
        sceneAsset?.dispose()
        fallbackModel?.dispose()
        fallbackTextures.forEach { it.dispose() }
        fallbackTextures.clear()
        scene = null
        sceneManager = null
        sceneAsset = null
        fallbackModel = null
        appInstance = null
        graphicsRef = null
    }

    private fun fitModel(loadedScene: Scene) {
        val bounds = BoundingBox()
        val dimensions = Vector3()
        loadedScene.modelInstance.calculateBoundingBox(bounds)
        bounds.getCenter(modelCenter)
        bounds.getDimensions(dimensions)
        val largestExtent = maxOf(dimensions.x, dimensions.y, dimensions.z).coerceAtLeast(0.001f)
        modelScale = 1.92f / largestExtent
        modelYOffset = -modelCenter.y * 0.05f
    }

    private companion object {
        const val TAG = "MarkHeadAdapter"
        const val MAX_MOBILE_GLB_BYTES = 24L * 1024L * 1024L
    }
}
