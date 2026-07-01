package com.apptists.poplur2025.gdx

import android.util.Log
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
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
    private var sceneAsset: SceneAsset? = null
    private var scene: Scene? = null
    private val modelCenter = Vector3()
    private var modelScale = 1f
    private var elapsed = 0f

    override fun create() {
        try {
            Gdx.graphics.setContinuousRendering(true)
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
            Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
            Gdx.gl.glEnable(GL20.GL_BLEND)
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

            val width = Gdx.graphics.width.toFloat().coerceAtLeast(1f)
            val height = Gdx.graphics.height.toFloat().coerceAtLeast(1f)
            camera = PerspectiveCamera(34f, width, height).apply {
                position.set(0f, 0.04f, 3.2f)
                lookAt(0f, 0.02f, 0f)
                near = 0.05f
                far = 20f
                update()
            }

            val asset = GLBLoader().load(Gdx.files.internal(MARK_HEAD_ASSET))
                ?: error("Required Mark head asset could not be loaded: $MARK_HEAD_ASSET")
            sceneAsset = asset
            val loadedScene = Scene(asset.scene)
            scene = loadedScene
            fitModel(loadedScene)

            sceneManager = SceneManager().apply {
                setCamera(camera)
                setAmbientLight(0.82f)
                environment.add(
                    DirectionalLightEx().set(
                        Color(1f, 0.96f, 0.88f, 1f),
                        Vector3(-0.42f, -0.58f, -0.70f),
                        4.0f
                    )
                )
                environment.add(
                    DirectionalLightEx().set(
                        Color(0.54f, 0.68f, 1f, 1f),
                        Vector3(0.45f, -0.18f, 0.86f),
                        1.2f
                    )
                )
                addScene(loadedScene)
            }
        } catch (error: Throwable) {
            Log.e(TAG, "3D Mark head GLB failed to load. No alternate head will be shown.", error)
            scene = null
        }
    }

    fun setSpeaking(active: Boolean) {
        speaking = active
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

        val talkScale = if (speaking) 1f + abs(sin(elapsed * 18f)) * 0.025f else 1f
        val floatY = sin(elapsed * 1.25f) * 0.035f
        val yaw = sin(elapsed * 0.72f) * 4f
        val nod = if (speaking) sin(elapsed * 13f) * 1.5f else 0f

        val loadedScene = scene ?: return

        loadedScene.modelInstance.transform
            .idt()
            .translate(0f, floatY, 0f)
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
        scene = null
        sceneManager = null
        sceneAsset = null
    }

    private fun fitModel(loadedScene: Scene) {
        val bounds = BoundingBox()
        val dimensions = Vector3()
        loadedScene.modelInstance.calculateBoundingBox(bounds)
        bounds.getCenter(modelCenter)
        bounds.getDimensions(dimensions)
        val largestExtent = maxOf(dimensions.x, dimensions.y, dimensions.z).coerceAtLeast(0.001f)
        modelScale = 1.74f / largestExtent
    }

    private companion object {
        const val TAG = "MarkHeadAdapter"
        const val MARK_HEAD_ASSET = "mark_head/mark_head.glb"
    }
}
