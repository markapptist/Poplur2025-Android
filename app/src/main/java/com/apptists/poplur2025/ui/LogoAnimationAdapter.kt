package com.apptists.poplur2025.ui

import android.util.Log
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttributes.Usage
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
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import kotlin.math.sin

class LogoAnimationAdapter : ApplicationAdapter() {
    private var camera: PerspectiveCamera? = null
    private var modelBatch: ModelBatch? = null
    private var logoTexture: Texture? = null
    private var logoModel: Model? = null
    private var logoInstance: ModelInstance? = null
    private var environment: Environment? = null
    private var popSound: Sound? = null
    private var animTime = LOGO_POP_TOTAL_SECONDS
    private var animating = false
    private var pendingTrigger = true
    private var hasPlayedSound = false

    override fun create() {
        try {
            // Disable LibGDX asset management to avoid multi-instance conflicts
            com.badlogic.gdx.graphics.glutils.ShaderProgram.prependVertexCode = ""
            com.badlogic.gdx.graphics.glutils.ShaderProgram.prependFragmentCode = ""

            Gdx.graphics.setContinuousRendering(true)
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
            Gdx.gl.glEnable(GL20.GL_BLEND)
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

            val width = Gdx.graphics.width.toFloat().coerceAtLeast(1f)
            val height = Gdx.graphics.height.toFloat().coerceAtLeast(1f)
            camera = PerspectiveCamera(38f, width, height).apply {
                position.set(0f, 0f, 5.2f)
                lookAt(0f, 0f, 0f)
                near = 0.1f
                far = 100f
                update()
            }
            modelBatch = ModelBatch()
            logoTexture = Texture(Gdx.files.internal("logo.png")).apply {
                setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            }

            val material = Material(
                TextureAttribute.createDiffuse(logoTexture),
                ColorAttribute.createDiffuse(1f, 1f, 1f, 1f),
                BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 1f)
            )
            logoModel = ModelBuilder().createBox(
                2.2f,
                2.2f,
                0.18f,
                material,
                (Usage.Position or Usage.Normal or Usage.TextureCoordinates).toLong()
            )
            logoInstance = ModelInstance(logoModel)
            environment = Environment().apply {
                set(ColorAttribute(ColorAttribute.AmbientLight, 0.88f, 0.88f, 0.92f, 1f))
                add(DirectionalLight().set(0.75f, 0.72f, 0.82f, -0.2f, -0.5f, -1f))
            }
            popSound = Gdx.audio.newSound(Gdx.files.internal("pop.mp3"))
            if (pendingTrigger) triggerLogoPop()
        } catch (e: Exception) {
            Log.e("LogoAnimationAdapter", "Failed to create 3D logo animation", e)
        }
    }

    fun triggerLogoPop() {
        pendingTrigger = true
        animating = true
        animTime = 0f
        hasPlayedSound = false
        Gdx.graphics.requestRendering()
    }

    override fun resize(width: Int, height: Int) {
        camera?.viewportWidth = width.toFloat().coerceAtLeast(1f)
        camera?.viewportHeight = height.toFloat().coerceAtLeast(1f)
        camera?.update()
    }

    override fun render() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        if (!animating) return

        if (!hasPlayedSound && animTime >= 0f) {
            try {
                if (popSound?.play() != -1L) {
                    hasPlayedSound = true
                }
            } catch (e: Exception) {
                // Silently ignore audio issues to prevent crash
                hasPlayedSound = true
            }
        }

        animTime += Gdx.graphics.deltaTime
        val alpha: Float
        val scale: Float
        when {
            animTime < 0.42f -> {
                val progress = (animTime / 0.42f).coerceIn(0f, 1f)
                scale = Interpolation.swingOut.apply(0.15f, 1.08f, progress)
                alpha = Interpolation.fade.apply(0f, 1f, progress)
            }
            animTime < 2.35f -> {
                val breathe = sin((animTime - 0.42f) * 5.4f) * 0.025f
                scale = 1.03f + breathe
                alpha = 1f
            }
            animTime < LOGO_POP_TOTAL_SECONDS -> {
                val progress = ((animTime - 2.35f) / (LOGO_POP_TOTAL_SECONDS - 2.35f)).coerceIn(0f, 1f)
                scale = Interpolation.fade.apply(1.03f, 0.72f, progress)
                alpha = 1f - progress
            }
            else -> {
                animating = false
                pendingTrigger = false
                return
            }
        }

        logoModel?.materials?.firstOrNull()?.set(
            BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, alpha)
        )
        val yaw = sin(animTime * 2.2f) * 8f
        val pitch = sin(animTime * 1.8f) * 4f
        val s = scale.coerceAtLeast(0.01f)
        logoInstance?.transform?.setToTranslation(0f, 0f, 0f)
            ?.rotate(Vector3.Y, yaw)
            ?.rotate(Vector3.X, pitch)
            ?.rotate(Vector3.Z, 90f)
            ?.scale(s, s, s)

        val batch = modelBatch ?: return
        val cam = camera ?: return
        try {
            logoInstance?.let {
                batch.begin(cam)
                batch.render(it, environment)
                batch.end()
            }
        } catch (e: Exception) {
            Log.e("LogoAnimationAdapter", "Render error", e)
        }
    }

    override fun dispose() {
        modelBatch?.dispose()
        logoModel?.dispose()
        logoTexture?.dispose()
        popSound?.dispose()
    }

    companion object {
        private const val LOGO_POP_TOTAL_SECONDS = 3f
    }
}
