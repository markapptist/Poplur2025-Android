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
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3

class CubeNavigationAdapter(private val onTabSelected: (Int) -> Unit) : ApplicationAdapter() {
    private var cam: PerspectiveCamera? = null
    private var modelBatch: ModelBatch? = null
    private var model: Model? = null
    private var instance: ModelInstance? = null
    private var environment: Environment? = null

    private var targetRotation = 0f
    var currentRotation = 0f // Make it public to allow syncing if needed
        private set
        
    private var isDragging = false
    private var lastX = 0f

    override fun create() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()
        val pCam = PerspectiveCamera(67f, w, h)
        pCam.position.set(0f, 0f, 5f)
        pCam.lookAt(0f, 0f, 0f)
        pCam.near = 1f
        pCam.far = 300f
        pCam.update()
        cam = pCam

        modelBatch = ModelBatch()

        model = ModelBuilder().createBox(
            2f,
            2f,
            2f,
            Material(ColorAttribute.createDiffuse(Color.WHITE)),
            (Usage.Position or Usage.Normal).toLong()
        )
        instance = ModelInstance(model)

        val env = Environment()
        env.set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
        env.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))
        environment = env

        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                isDragging = true
                lastX = screenX.toFloat()
                return true
            }

            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                isDragging = false
                // Snap to nearest 90 degrees
                val snap = Math.round(currentRotation / 90f)
                targetRotation = (snap * 90).toFloat()
                
                // Calculate tab index from snap (assuming 4 tabs for the cube faces)
                val tabIndex = ((snap % 4) + 4) % 4
                onTabSelected(tabIndex.toInt())
                return true
            }

            override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                if (isDragging) {
                    val deltaX = screenX - lastX
                    currentRotation += deltaX * 0.5f
                    lastX = screenX.toFloat()
                }
                return true
            }
        }
    }

    override fun render() {
        if (!isDragging) {
            currentRotation += (targetRotation - currentRotation) * 0.1f
        }

        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        instance?.let { inst ->
            inst.transform.set(Vector3.Zero, Quaternion().setEulerAngles(currentRotation, 0f, 0f))
            modelBatch?.let { batch ->
                batch.begin(cam)
                batch.render(inst, environment)
                batch.end()
            }
        }
    }

    override fun dispose() {
        modelBatch?.dispose()
        model?.dispose()
    }
}
