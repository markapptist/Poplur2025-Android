package com.apptists.poplur2025.ui

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import kotlin.math.abs
import kotlin.math.sin

class CombinedGdxAdapter(private val onTabSelected: (Int) -> Unit) : ApplicationAdapter() {
    // Cube navigation variables
    private var cam: PerspectiveCamera? = null
    private var modelBatch: ModelBatch? = null
    private var model: Model? = null
    private var instance: ModelInstance? = null
    private var environment: Environment? = null
    private var targetRotation = 0f
    private var currentRotation = 0f
    private var isDragging = false
    private var lastX = 0f

    // Overlay scene variables
    private var shapes: ShapeRenderer? = null
    private var batch: SpriteBatch? = null
    private var font: BitmapFont? = null
    private var time = 0f

    override fun create() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        
        // Init Cube
        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()
        val pCam = PerspectiveCamera(67f, w, h)
        pCam.position.set(0f, 0f, 5f)
        pCam.lookAt(0f, 0f, 0f)
        pCam.near = 1f; pCam.far = 300f; pCam.update()
        cam = pCam
        modelBatch = ModelBatch()
        val modelBuilder = ModelBuilder()
        model = modelBuilder.createBox(2f, 2f, 2f, Material(ColorAttribute.createDiffuse(Color.WHITE)), (Usage.Position or Usage.Normal).toLong())
        instance = ModelInstance(model)
        val env = Environment()
        env.set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
        env.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))
        environment = env

        // Init Overlay Scene
        shapes = ShapeRenderer()
        batch = SpriteBatch()
        font = BitmapFont()

        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                isDragging = true; lastX = screenX.toFloat(); return true
            }
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                isDragging = false
                val snap = Math.round(currentRotation / 90f)
                targetRotation = (snap * 90).toFloat()
                onTabSelected((((snap % 4) + 4) % 4).toInt())
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
        if (!isDragging) currentRotation += (targetRotation - currentRotation) * 0.1f

        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        // Render Cube
        instance?.let { inst ->
            inst.transform.set(Vector3.Zero, Quaternion().setEulerAngles(currentRotation, 0f, 0f))
            modelBatch?.let { b ->
                b.begin(cam); b.render(inst, environment); b.end()
            }
        }

        // Render Overlay Scene if active
        if (GdxState.isSceneActive) {
            renderScene()
        }
    }

    private fun renderScene() {
        time += Gdx.graphics.deltaTime
        val s = shapes ?: return
        val b = batch ?: return
        val f = font ?: return

        // In Compose/Android, Y is usually from top, but libGDX is from bottom.
        // We might need to flip if needed, but for now just use the bounds.
        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()
        
        // Simple 2D drawing logic from PoplurGdxScene
        val cx = w / 2f
        val base = h * 0.22f
        s.begin(ShapeRenderer.ShapeType.Filled)
        for (i in 0 until 11) {
            val height = (h * 0.12f) + abs(sin(time * 1.6f + i * .45f)) * h * .52f
            val x = cx - 220 + i * 44
            s.color = Color(0.2f + i * .035f, 0.7f, 1f, 1f)
            s.rect(x, base, 30f, height)
        }
        s.color = Color(1f, .88f, .18f, 1f)
        s.circle(cx + sin(time) * 150f, h * .72f, 26f)
        s.end()

        b.begin()
        f.color = Color.WHITE
        f.draw(b, "LibGDX ${GdxState.currentScene.uppercase()} Scene", 28f, h - 34f)
        b.end()
    }

    override fun dispose() {
        modelBatch?.dispose(); model?.dispose(); shapes?.dispose(); batch?.dispose(); font?.dispose()
    }
}
