package com.apptists.poplur2025.wear.gdx

import android.content.Context
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Cubemap
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import net.mgsx.gltf.loaders.glb.GLBLoader
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneAsset
import net.mgsx.gltf.scene3d.scene.SceneManager
import net.mgsx.gltf.scene3d.utils.IBLBuilder
import com.apptists.poplur.performance.PoplurPerformanceManager
import com.apptists.poplur.performance.PoplurPerformanceMode

/**
 * Renders the real Poplur clay globe (poplur_clay_globe.glb) with gdx-gltf's PBR
 * pipeline over a transparent framebuffer, so the Compose blue clay-dot background
 * shows through. Matches the phone loading-screen clay aesthetic.
 */
class GlobeGdxAdapter(context: Context) : ApplicationAdapter() {
    private val performanceManager = PoplurPerformanceManager.getInstance(context)
    private var camera: PerspectiveCamera? = null
    private var sceneManager: SceneManager? = null
    private var sceneAsset: SceneAsset? = null
    private var scene: Scene? = null
    private var wordAsset: SceneAsset? = null
    private var wordScene: Scene? = null
    private var brdfLUT: Texture? = null
    private var envCubemap: Cubemap? = null
    private var diffuseCubemap: Cubemap? = null
    private var specularCubemap: Cubemap? = null
    private var yRotation = 0f

    override fun create() {
        camera = PerspectiveCamera(32f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()).apply {
            position.set(0f, 0.05f, 3.0f)
            lookAt(0f, 0f, 0f)
            near = 0.1f
            far = 60f
            update()
        }

        val manager = SceneManager()
        sceneManager = manager
        manager.setCamera(camera)

        // Warm key light, roughly matching the iOS SceneKit sun direction.
        val light = DirectionalLightEx().apply {
            set(Color(1.0f, 0.97f, 0.90f, 1.0f), Vector3(-0.5f, -0.5f, -0.7f), 3.4f)
        }
        manager.environment.add(light)

        // Image-based lighting so the clay PBR materials read correctly.
        val iblBuilder = IBLBuilder.createOutdoor(light)
        val iblSize = when (performanceManager.state.value.mode) {
            PoplurPerformanceMode.FULL -> 256
            PoplurPerformanceMode.REDUCED -> 128
            PoplurPerformanceMode.LOW_POWER -> 64
            PoplurPerformanceMode.EMERGENCY -> 32
        }
        envCubemap = iblBuilder.buildEnvMap(iblSize)
        diffuseCubemap = iblBuilder.buildIrradianceMap((iblSize / 4).coerceAtLeast(16))
        specularCubemap = iblBuilder.buildRadianceMap(8)
        iblBuilder.dispose()

        manager.setAmbientLight(0.9f)
        manager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap))
        manager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap))
        brdfLUT = Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"))
        manager.environment.set(PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT))

        try {
            val asset = GLBLoader().load(Gdx.files.internal("poplur_clay_globe.glb"))
            sceneAsset = asset
            val s = Scene(asset.scene)
            scene = s
            manager.addScene(s)
        } catch (e: Exception) {
            Gdx.app.error("GlobeGdxAdapter", "Failed to load poplur_clay_globe.glb", e)
        }

        // The 3D "poplur" wordmark, placed in FRONT of the globe (toward the camera),
        // centred and held still while the globe spins behind it.
        try {
            val wAsset = GLBLoader().load(Gdx.files.internal("poplur_wordmark.glb"))
            wordAsset = wAsset
            val ws = Scene(wAsset.scene)
            wordScene = ws
            manager.addScene(ws)
            val bbox = BoundingBox()
            ws.modelInstance.model.calculateBoundingBox(bbox)
            val center = bbox.getCenter(Vector3())
            val scale = 1.5f / maxOf(bbox.width, 0.001f)
            ws.modelInstance.transform.idt()
                .translate(0f, 0.15f, 1.0f)
                .scale(scale, scale, scale)
                .translate(-center.x, -center.y, -center.z)
        } catch (e: Exception) {
            Gdx.app.error("GlobeGdxAdapter", "Failed to load poplur_wordmark.glb", e)
        }
    }

    override fun render() {
        val delta = Gdx.graphics.deltaTime
        val performance = performanceManager.state.value
        if (performance.decorativeRenderingAllowed) {
            yRotation += 30f * delta // 12-second full rotation, matching the phone
        }

        scene?.modelInstance?.transform?.apply {
            idt()
            rotate(Vector3.Z, -15f)     // gentle clay tilt
            rotate(Vector3.Y, yRotation)
        }

        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)   // transparent — Compose bg shows through
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        val manager = sceneManager ?: return
        manager.update(delta)
        manager.render()
    }

    override fun resize(width: Int, height: Int) {
        camera?.apply {
            viewportWidth = width.toFloat()
            viewportHeight = height.toFloat()
            update()
        }
    }

    override fun dispose() {
        sceneManager?.dispose()
        sceneAsset?.dispose()
        wordAsset?.dispose()
        brdfLUT?.dispose()
        envCubemap?.dispose()
        diffuseCubemap?.dispose()
        specularCubemap?.dispose()
    }
}
