package com.apptists.poplur2025.ui

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
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
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute

import com.badlogic.gdx.math.Matrix4

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
        pCam.position.set(0f, 0f, 2.1f) // Move camera very close so cube fills the area entirely
        pCam.lookAt(0f, 0f, 0f)
        pCam.near = 0.1f; pCam.far = 300f; pCam.update()
        cam = pCam
        
        modelBatch = ModelBatch()

        // Tab Order: 0:Feed, 1:Profile, 2:Map, 3:Camera
        val labels = listOf("Feed", "Profile", "Map", "Camera")
        val size = 256
        val batch = SpriteBatch()
        val textProjection = Matrix4().setToOrtho2D(0f, size.toFloat(), size.toFloat(), -size.toFloat())
        batch.projectionMatrix = textProjection
        
        val font = BitmapFont()
        font.data.setScale(3f)

        val materials = labels.map { label ->
            val fbo = FrameBuffer(Pixmap.Format.RGBA8888, size, size, false)
            fbo.begin()
            // Shiny purple background for each face - slightly lighter and more vibrant
            Gdx.gl.glClearColor(0.38f, 0.22f, 0.75f, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

            batch.begin()
            
            val layout = com.badlogic.gdx.graphics.g2d.GlyphLayout()
            if (label == "Camera") {
                // Special style for camera tab as per image (+ icon and Camera text)
                font.data.setScale(5f)
                layout.setText(font, "+")
                font.draw(batch, "+", (size - layout.width) / 2f, size * 0.35f)
                
                font.data.setScale(2f)
                layout.setText(font, "Camera")
                font.draw(batch, "Camera", (size - layout.width) / 2f, size * 0.7f)
                font.data.setScale(3f) // Reset
            } else if (label == "Profile") {
                // Special style for profile tab (User icon and Profile text)
                font.data.setScale(5f)
                layout.setText(font, "O")
                font.draw(batch, "O", (size - layout.width) / 2f, size * 0.35f)
                
                font.data.setScale(2f)
                layout.setText(font, "Profile")
                font.draw(batch, "Profile", (size - layout.width) / 2f, size * 0.7f)
                font.data.setScale(3f) // Reset
            } else {
                layout.setText(font, label)
                font.draw(batch, label, (size - layout.width) / 2f, (size + layout.height) / 2f)
            }
            
            batch.end()
            fbo.end()
            val tex = fbo.colorBufferTexture
            textures.add(tex)
            Material(
                TextureAttribute.createDiffuse(tex),
                ColorAttribute.createSpecular(1f, 1f, 1f, 1f), // Bright white highlight
                BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA),
                com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute.createShininess(64f)
            )
        }
        
        val modelBuilder = ModelBuilder()
        modelBuilder.begin()
        
        // Face 0: Feed (Front)
        modelBuilder.part("face0", GL20.GL_TRIANGLES, (Usage.Position or Usage.Normal or Usage.TextureCoordinates).toLong(), materials[0])
            .rect(-1f, -1f, 1f,  1f, -1f, 1f,  1f, 1f, 1f,  -1f, 1f, 1f,  0f, 0f, 1f)
        
        // Face 1: Profile (Right)
        modelBuilder.part("face1", GL20.GL_TRIANGLES, (Usage.Position or Usage.Normal or Usage.TextureCoordinates).toLong(), materials[1])
            .rect(1f, -1f, 1f,  1f, -1f, -1f,  1f, 1f, -1f,  1f, 1f, 1f,  1f, 0f, 0f)
        
        // Face 2: Map (Back)
        modelBuilder.part("face2", GL20.GL_TRIANGLES, (Usage.Position or Usage.Normal or Usage.TextureCoordinates).toLong(), materials[2])
            .rect(1f, -1f, -1f,  -1f, -1f, -1f,  -1f, 1f, -1f,  1f, 1f, -1f,  0f, 0f, -1f)
        
        // Face 3: Camera (Left)
        modelBuilder.part("face3", GL20.GL_TRIANGLES, (Usage.Position or Usage.Normal or Usage.TextureCoordinates).toLong(), materials[3])
            .rect(-1f, -1f, -1f,  -1f, -1f, 1f,  -1f, 1f, 1f,  -1f, 1f, -1f,  -1f, 0f, 0f)
        
        model = modelBuilder.end()
        instance = ModelInstance(model)
        
        val env = Environment()
        env.set(ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.5f, 0.5f, 1f))
        env.add(DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f))
        env.add(DirectionalLight().set(0.5f, 0.5f, 0.5f, 1f, 0.8f, 0.2f))
        environment = env

        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                isDragging = true; lastX = screenX.toFloat(); return true
            }
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                isDragging = false
                val snap = Math.round(currentRotation / 90f)
                targetRotation = (snap * 90).toFloat()
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

        // Force absolute transparency in every frame
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
