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
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.glutils.FrameBuffer

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
    
    private val textures = mutableListOf<Texture>()

    override fun create() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        
        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()
        val pCam = PerspectiveCamera(67f, w, h)
        pCam.position.set(0f, 0f, 4f)
        pCam.lookAt(0f, 0f, 0f)
        pCam.near = 1f; pCam.far = 300f; pCam.update()
        cam = pCam
        
        modelBatch = ModelBatch()

        // Tab Order: 0:Feed, 1:Map, 2:Profile, 3:Messages
        // Swiping left (decreasing rotation) should move from Feed to Map.
        val labels = listOf("Feed", "Map", "Profile", "Messages")
        val size = 256
        val batch = SpriteBatch()
        val font = BitmapFont()
        font.data.setScale(3f)

        val materials = labels.map { label ->
            val fbo = FrameBuffer(Pixmap.Format.RGBA8888, size, size, false)
            fbo.begin()
            Gdx.gl.glClearColor(0.12f, 0.12f, 0.21f, 0.9f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            batch.begin()
            font.draw(batch, label, size * 0.15f, size * 0.6f)
            batch.end()
            fbo.end()
            val tex = fbo.colorBufferTexture
            textures.add(tex)
            Material(TextureAttribute.createDiffuse(tex))
        }
        
        val modelBuilder = ModelBuilder()
        modelBuilder.begin()
        
        // Face 0: Feed (Front) - at 0 degrees
        modelBuilder.part("face0", GL20.GL_TRIANGLES, (Usage.Position or Usage.Normal or Usage.TextureCoordinates).toLong(), materials[0])
            .rect(-1f, -1f, 1f,  1f, -1f, 1f,  1f, 1f, 1f,  -1f, 1f, 1f,  0f, 0f, 1f)
        
        // Face 1: Map (Right) - at +90 degrees rotation (visible when swiping left)
        modelBuilder.part("face1", GL20.GL_TRIANGLES, (Usage.Position or Usage.Normal or Usage.TextureCoordinates).toLong(), materials[1])
            .rect(1f, -1f, 1f,  1f, -1f, -1f,  1f, 1f, -1f,  1f, 1f, 1f,  1f, 0f, 0f)
        
        // Face 2: Profile (Back) - at 180 degrees
        modelBuilder.part("face2", GL20.GL_TRIANGLES, (Usage.Position or Usage.Normal or Usage.TextureCoordinates).toLong(), materials[2])
            .rect(1f, -1f, -1f,  -1f, -1f, -1f,  -1f, 1f, -1f,  1f, 1f, -1f,  0f, 0f, -1f)
        
        // Face 3: Messages (Left) - at -90 degrees
        modelBuilder.part("face3", GL20.GL_TRIANGLES, (Usage.Position or Usage.Normal or Usage.TextureCoordinates).toLong(), materials[3])
            .rect(-1f, -1f, -1f,  -1f, -1f, 1f,  -1f, 1f, 1f,  -1f, 1f, -1f,  -1f, 0f, 0f)
        
        model = modelBuilder.end()
        instance = ModelInstance(model)
        
        val env = Environment()
        env.set(ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.8f, 0.8f, 1f))
        environment = env

        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                isDragging = true; lastX = screenX.toFloat(); return true
            }
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                isDragging = false
                // Snap to nearest 90 degrees
                // Invert the calculation to match visual rotation
                // Swiping left decreases rotation, bringing Face 1 (Map) to front
                val snap = Math.round(currentRotation / 90f)
                targetRotation = (snap * 90).toFloat()
                
                // Rotation 0 -> Index 0 (Feed)
                // Rotation -90 -> snap -1 -> Index 1 (Map)
                // Rotation -180 -> snap -2 -> Index 2 (Profile)
                // Rotation -270 -> snap -3 -> Index 3 (Messages)
                val tabIndex = (((-snap % 4) + 4) % 4).toInt()
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
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
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
        textures.forEach { it.dispose() }
    }
}
