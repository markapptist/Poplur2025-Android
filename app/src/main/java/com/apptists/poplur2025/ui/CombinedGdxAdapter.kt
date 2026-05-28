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

class CombinedGdxAdapter(private val onTabSelected: (Int) -> Unit) : ApplicationAdapter() {
    private var cam: PerspectiveCamera? = null
    private var modelBatch: ModelBatch? = null
    private var model: Model? = null
    private var instance: ModelInstance? = null
    private var environment: Environment? = null
    private var targetRotation = 0f
    private var currentRotation = 0f
    private var isDragging = false
    private var lastX = 0f

    override fun create() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        
        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()
        val pCam = PerspectiveCamera(67f, w, h)
        pCam.position.set(0f, 0f, 5f)
        pCam.lookAt(0f, 0f, 0f)
        pCam.near = 1f; pCam.far = 300f; pCam.update()
        cam = pCam
        
        modelBatch = ModelBatch()
        
        val modelBuilder = ModelBuilder()
        modelBuilder.begin()
        
        // Tab 0: Home (Cyan)
        val part0 = modelBuilder.part("face0", GL20.GL_TRIANGLES, (Usage.Position or Usage.Normal).toLong(), Material(ColorAttribute.createDiffuse(Color.CYAN)))
        part0.rect(-1f, -1f, 1f,  1f, -1f, 1f,  1f, 1f, 1f,  -1f, 1f, 1f,  0f, 0f, 1f)
        
        // Tab 1: Feed (Magenta)
        val part1 = modelBuilder.part("face1", GL20.GL_TRIANGLES, (Usage.Position or Usage.Normal).toLong(), Material(ColorAttribute.createDiffuse(Color.MAGENTA)))
        part1.rect(1f, -1f, 1f,  1f, -1f, -1f,  1f, 1f, -1f,  1f, 1f, 1f,  1f, 0f, 0f)
        
        // Tab 2: Messages (Yellow)
        val part2 = modelBuilder.part("face2", GL20.GL_TRIANGLES, (Usage.Position or Usage.Normal).toLong(), Material(ColorAttribute.createDiffuse(Color.YELLOW)))
        part2.rect(1f, -1f, -1f,  -1f, -1f, -1f,  -1f, 1f, -1f,  1f, 1f, -1f,  0f, 0f, -1f)
        
        // Tab 3: Profile (Lime)
        val part3 = modelBuilder.part("face3", GL20.GL_TRIANGLES, (Usage.Position or Usage.Normal).toLong(), Material(ColorAttribute.createDiffuse(Color.LIME)))
        part3.rect(-1f, -1f, -1f,  -1f, -1f, 1f,  -1f, 1f, 1f,  -1f, 1f, -1f,  -1f, 0f, 0f)
        
        model = modelBuilder.end()
        instance = ModelInstance(model)
        
        val env = Environment()
        env.set(ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.5f, 0.5f, 1f))
        env.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))
        environment = env

        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                isDragging = true; lastX = screenX.toFloat(); return true
            }
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                isDragging = false
                val snap = Math.round(currentRotation / 90f)
                targetRotation = (snap * 90).toFloat()
                
                val tabIndex = (((snap % 4) + 4) % 4).toInt()
                Gdx.app.postRunnable { onTabSelected(tabIndex) }
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
            modelBatch?.let { b ->
                b.begin(cam); b.render(inst, environment); b.end()
            }
        }
    }

    override fun dispose() {
        modelBatch?.dispose(); model?.dispose()
    }
}
