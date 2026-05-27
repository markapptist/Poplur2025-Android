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
    private var cam: PerspectiveCamera? = null
    private var modelBatch: ModelBatch? = null
    private var model: Model? = null
    private var instance: ModelInstance? = null
    private var environment: Environment? = null
    private var targetRotation = 0f
    private var currentRotation = 0f
    private var isDragging = false
    private var lastX = 0f

    private var shapes: ShapeRenderer? = null
    private var batch: SpriteBatch? = null
    private var font: BitmapFont? = null
    private var time = 0f

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
        
        // Create a cube with different colored faces
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

        shapes = ShapeRenderer()
        batch = SpriteBatch()
        font = BitmapFont()

        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                isDragging = true; lastX = screenX.toFloat(); return true
            }
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                isDragging = false
                // Snap to nearest 90 degrees
                val snap = Math.round(currentRotation / 90f)
                targetRotation = (snap * 90).toFloat()
                
                // Trigger tab change in Compose
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
            // Rotate the cube around the Y axis based on drag
            inst.transform.set(Vector3.Zero, Quaternion().setEulerAngles(currentRotation, 0f, 0f))
            modelBatch?.let { b ->
                b.begin(cam); b.render(inst, environment); b.end()
            }
        }

        if (GdxState.isSceneActive) {
            renderScene()
        }
    }

    private fun renderScene() {
        time += Gdx.graphics.deltaTime
        val s = shapes ?: return
        val b = batch ?: return
        val f = font ?: return
        val h = Gdx.graphics.height.toFloat()
        val w = Gdx.graphics.width.toFloat()
        val cx = w / 2f
        val base = h * 0.22f
        
        s.begin(ShapeRenderer.ShapeType.Filled)
        for (i in 0 until 11) {
            val height = (h * 0.12f) + abs(sin(time * 1.6f + i * .45f)) * h * .52f
            val x = cx - 220 + i * 44
            s.color = Color(0.2f + i * .035f, 0.7f, 1f, 0.8f) // Slight transparency
            s.rect(x, base, 30f, height)
        }
        s.color = Color(1f, .88f, .18f, 0.9f)
        s.circle(cx + sin(time) * 150f, h * .72f, 26f)
        s.end()

        b.begin()
        f.color = Color.WHITE
        f.draw(b, "Scene: ${GdxState.currentScene.uppercase()}", 40f, h - 40f)
        b.end()
    }

    override fun dispose() {
        modelBatch?.dispose(); model?.dispose(); shapes?.dispose(); batch?.dispose(); font?.dispose()
    }
}
