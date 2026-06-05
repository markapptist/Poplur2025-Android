package com.apptists.poplur2025.ui

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import kotlin.math.abs
import kotlin.math.roundToInt
import android.util.Log

/**
 * Android recreation of the iOS SceneKit CubeTabView.
 *
 * iOS behavior mirrored here:
 * - bottom floating 3D cube, transparent background
 * - 40-degree perspective camera at z = 5
 * - 1.3 scene-unit cube
 * - face order/yaw: Record 0°, Feed 90°, Profile 180°, Map 270°
 * - indigo-tinted faces with large white icon + bold title
 * - horizontal pan rotates continuously, release snaps to nearest 90° with overshoot
 */
class CombinedGdxAdapter(
    private val initialTabIndex: Int = IOS_FEED_INDEX,
    private val onVerticalSwipe: ((Boolean) -> Unit)? = null,
    private val onLongPress: (() -> Unit)? = null,
    private val onTabSelected: (Int) -> Unit
) : ApplicationAdapter() {

    private var camera: PerspectiveCamera? = null
    private var modelBatch: ModelBatch? = null
    private var model: Model? = null
    private var instance: ModelInstance? = null
    private var environment: Environment? = null

    private var currentYawDegrees = yawForTab(initialTabIndex)
    private var targetYawDegrees = currentYawDegrees
    private var startYawDegrees = currentYawDegrees
    private var animationTime = 0f
    private var bouncePhase = BouncePhase.Idle

    private var dragging = false
    private var isVerticalSwipe = false
    private var isSwipeTriggered = false // Prevent multiple triggers per swipe
    private var isRotation = false
    private var longPressSent = false
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var touchStartMillis = 0L

    private val textures = mutableListOf<Texture>()
    private var spriteBatch: SpriteBatch? = null
    private var shapeRenderer: ShapeRenderer? = null
    private var font: BitmapFont? = null

    override fun create() {
        try {
            Log.d("CombinedGdxAdapter", "create() started")
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
            Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
            Gdx.gl.glEnable(GL20.GL_BLEND)
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

            currentYawDegrees = yawForTab(initialTabIndex)
            targetYawDegrees = currentYawDegrees
            startYawDegrees = currentYawDegrees

            val w = Gdx.graphics.width.toFloat().coerceAtLeast(1f)
            val h = Gdx.graphics.height.toFloat().coerceAtLeast(1f)
            camera = PerspectiveCamera(40f, w, h).apply {
                position.set(0f, 0f, 5f)
                lookAt(0f, 0f, 0f)
                near = 0.1f
                far = 100f
                update()
            }

            modelBatch = ModelBatch()
            spriteBatch = SpriteBatch()
            shapeRenderer = ShapeRenderer()
            
            try {
                font = BitmapFont().apply {
                    data.setScale(4.2f)
                    setUseIntegerPositions(false)
                }
            } catch (e: Exception) {
                Log.e("CombinedGdxAdapter", "Failed to load default font", e)
            }

            val materials = buildFaceMaterials()
            model = buildIosOrderedCube(materials)
            instance = ModelInstance(model)

            environment = Environment().apply {
                set(ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f))
                add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))
                add(DirectionalLight().set(0.5f, 0.5f, 0.5f, 1f, 0.5f, 0.5f))
            }

            Gdx.input.inputProcessor = object : InputAdapter() {
                override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                    dragging = true
                    isVerticalSwipe = false
                    isSwipeTriggered = false
                    isRotation = false
                    longPressSent = false
                    bouncePhase = BouncePhase.Idle
                    downX = screenX.toFloat()
                    downY = screenY.toFloat()
                    lastX = screenX.toFloat()
                    touchStartMillis = System.currentTimeMillis()
                    return true
                }

                override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                    if (!dragging) return true
                    
                    val dx = screenX.toFloat() - downX
                    val dy = screenY.toFloat() - downY

                    if (abs(dx) > 10f || abs(dy) > 10f) {
                        longPressSent = true
                    }

                    if (!isVerticalSwipe && !isRotation) {
                        if (abs(dy) > 15f && abs(dy) > abs(dx) * 1.5f) {
                            isVerticalSwipe = true
                        } else if (abs(dx) > 15f) {
                            isRotation = true
                        }
                    }

                    if (isVerticalSwipe && !isSwipeTriggered) {
                        if (abs(dy) > 40f) { // Threshold for instant trigger
                            isSwipeTriggered = true
                            val isUp = dy < 0
                            Gdx.app.postRunnable { onVerticalSwipe?.invoke(isUp) }
                        }
                    }

                    if (isRotation) {
                        val deltaX = screenX.toFloat() - lastX
                        currentYawDegrees += deltaX * 0.8f
                        targetYawDegrees = currentYawDegrees
                    }
                    
                    lastX = screenX.toFloat()
                    return true
                }

                override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                    dragging = false
                    val dx = screenX.toFloat() - downX
                    val dy = screenY.toFloat() - downY

                    // Only handle vertical swipe on release if it wasn't already triggered during drag
                    if ((isVerticalSwipe || (abs(dy) > 40f && abs(dy) > abs(dx) * 2f)) && !isSwipeTriggered) {
                        val isUp = dy < 0
                        Gdx.app.postRunnable { onVerticalSwipe?.invoke(isUp) }
                    } else if (abs(dx) < 10f && abs(dy) < 10f && !longPressSent) {
                        Log.d("CombinedGdxAdapter", "Tap on ${tabForYaw(currentYawDegrees).title}")
                    }

                    val snappedIndex = snappedTabIndex(currentYawDegrees)
                    val snappedYaw = yawForTab(snappedIndex)
                    startBounceTo(snappedYaw)
                    
                    // If we rotated significantly, trigger tab selection
                    if (isRotation || abs(dx) > 15f) {
                        Gdx.app.postRunnable { onTabSelected(snappedIndex) }
                    }
                    
                    return true
                }
            }
            Log.d("CombinedGdxAdapter", "create() finished successfully")
        } catch (e: Exception) {
            Log.e("CombinedGdxAdapter", "CRITICAL ERROR in create()", e)
        }
    }

    private fun buildFaceMaterials(): List<Material> {
        return IOS_TABS.map { tab ->
            val texture = createFaceTexture(tab)
            textures += texture
            Material(
                TextureAttribute.createDiffuse(texture),
                ColorAttribute.createDiffuse(Color.WHITE),
                ColorAttribute.createEmissive(Color.WHITE.cpy().mul(0.12f)),
                ColorAttribute.createSpecular(1f, 1f, 1f, 1f),
                FloatAttribute.createShininess(12f)
            )
        }
    }

    private fun buildIosOrderedCube(materials: List<Material>): Model {
        val mb = ModelBuilder()
        val attr = (Usage.Position or Usage.Normal or Usage.TextureCoordinates).toLong()
        val s = 0.65f
        mb.begin()
        // Front: Camera (Yaw 0)
        mb.part("front", GL20.GL_TRIANGLES, attr, materials[0])
            .rect(-s, -s, s, s, -s, s, s, s, s, -s, s, s, 0f, 0f, 1f)
        // Right: Feed (Yaw 90)
        mb.part("right", GL20.GL_TRIANGLES, attr, materials[1])
            .rect(s, -s, s, s, -s, -s, s, s, -s, s, s, s, 1f, 0f, 0f)
        // Back: Profile (Yaw 180)
        mb.part("back", GL20.GL_TRIANGLES, attr, materials[2])
            .rect(s, -s, -s, -s, -s, -s, -s, s, -s, s, s, -s, 0f, 0f, -1f)
        // Left: Map (Yaw 270)
        mb.part("left", GL20.GL_TRIANGLES, attr, materials[3])
            .rect(-s, -s, -s, -s, -s, s, -s, s, s, -s, s, -s, -1f, 0f, 0f)
        // Top: Feed
        mb.part("top", GL20.GL_TRIANGLES, attr, materials[1])
            .rect(-s, s, s, s, s, s, s, s, -s, -s, s, -s, 0f, 1f, 0f)
        // Bottom: Profile
        mb.part("bottom", GL20.GL_TRIANGLES, attr, materials[2])
            .rect(-s, -s, -s, s, -s, -s, s, -s, s, -s, -s, s, 0f, -1f, 0f)
        return mb.end()
    }

    private fun createFaceTexture(tab: CubeTab): Texture {
        val size = 256 // Reduced size for stability
        try {
            val fbo = FrameBuffer(Pixmap.Format.RGBA8888, size, size, false)
            val projection = Matrix4().setToOrtho2D(0f, 0f, size.toFloat(), size.toFloat())

            // Create local temporary renderers to avoid state pollution between faces
            val localSR = ShapeRenderer()
            val localBatch = SpriteBatch()
            
            fbo.begin()
            Gdx.gl.glViewport(0, 0, size, size)
            // Dark neon purple background
            Gdx.gl.glClearColor(0.04f, 0.01f, 0.05f, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

            localSR.projectionMatrix = projection
            
            // 1. Draw Glow (Multi-layered pink blur effect)
            localSR.begin(ShapeRenderer.ShapeType.Filled)
            val cx = size / 2f
            val cy = size / 2f
            val pink = Color(1f, 0.078f, 0.576f, 1f) // Deep Pink
            
            for (i in 0 until 8) {
                val radius = 45f + i * 11f // Adjusted for smaller size
                val alpha = 0.14f / (i + 1)
                localSR.color = Color(pink.r, pink.g, pink.b, alpha)
                localSR.circle(cx, cy, radius, 32)
                // Asymmetric glows for depth
                localSR.circle(cx - 15f, cy + 12f, radius * 0.75f, 32)
                localSR.circle(cx + 15f, cy - 12f, radius * 0.75f, 32)
            }
            localSR.end()
            
            // 2. Draw Frame (Sharp pink border)
            localSR.begin(ShapeRenderer.ShapeType.Line)
            Gdx.gl.glLineWidth(8f)
            localSR.color = pink
            val margin = 14f
            localSR.rect(margin, margin, size - 2 * margin, size - 2 * margin)
            localSR.end()

            // 3. Draw Text (Bold White)
            localBatch.projectionMatrix = projection
            localBatch.begin()
            font?.let { f ->
                f.setColor(Color.WHITE)
                f.data.setScale(2.5f) // Adjusted for smaller size
                val layout = GlyphLayout(f, tab.title)
                f.draw(localBatch, tab.title, (size - layout.width) / 2f, (size + layout.height) / 2f)
            }
            localBatch.end()

            val facePixmap = Pixmap.createFromFrameBuffer(0, 0, size, size)
            fbo.end()

            val uprightPixmap = Pixmap(size, size, Pixmap.Format.RGBA8888)
            for (y in 0 until size) {
                uprightPixmap.drawPixmap(facePixmap, 0, size - y - 1, size, 1, 0, y, size, 1)
            }
            val tex = Texture(uprightPixmap)
            tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            
            // Cleanup temporary renderers
            localSR.dispose()
            localBatch.dispose()
            facePixmap.dispose()
            uprightPixmap.dispose()
            fbo.dispose()

            return tex
        } catch (e: Exception) {
            Log.e("CombinedGdxAdapter", "Failed to create face texture for ${tab.title}", e)
            val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
            pixmap.setColor(0.345f, 0.337f, 0.839f, 1f) // Indigo fallback
            pixmap.fill()
            val tex = Texture(pixmap)
            pixmap.dispose()
            return tex
        }
    }

    private fun drawIcon(tab: CubeTab, size: Float) {
        // No longer used, drawing text instead per new style
    }

    private fun ShapeRenderer.roundedLine(x1: Float, y1: Float, x2: Float, y2: Float, width: Float) {
        rectLine(x1, y1, x2, y2, width)
        circle(x1, y1, width / 2f, 16)
        circle(x2, y2, width / 2f, 16)
    }

    private fun startBounceTo(target: Float) {
        startYawDegrees = currentYawDegrees
        targetYawDegrees = target
        animationTime = 0f
        bouncePhase = BouncePhase.Overshoot
    }

    private fun updateAnimation() {
        if (dragging && !longPressSent && System.currentTimeMillis() - touchStartMillis >= 800L) {
            longPressSent = true
            Gdx.app.postRunnable { onLongPress?.invoke() }
        }

        when (bouncePhase) {
            BouncePhase.Idle -> Unit
            BouncePhase.Overshoot -> {
                animationTime += Gdx.graphics.deltaTime
                val progress = (animationTime / 0.18f).coerceIn(0f, 1f)
                val overshootTarget = targetYawDegrees + if (targetYawDegrees >= startYawDegrees) 6.9f else -6.9f
                currentYawDegrees = Interpolation.sine.apply(startYawDegrees, overshootTarget, progress)
                if (progress >= 1f) {
                    startYawDegrees = currentYawDegrees
                    animationTime = 0f
                    bouncePhase = BouncePhase.Settle
                }
            }
            BouncePhase.Settle -> {
                animationTime += Gdx.graphics.deltaTime
                val progress = (animationTime / 0.22f).coerceIn(0f, 1f)
                currentYawDegrees = Interpolation.sine.apply(startYawDegrees, targetYawDegrees, progress)
                if (progress >= 1f) {
                    currentYawDegrees = targetYawDegrees
                    bouncePhase = BouncePhase.Idle
                }
            }
        }
    }

    override fun resize(width: Int, height: Int) {
        camera?.viewportWidth = width.toFloat().coerceAtLeast(1f)
        camera?.viewportHeight = height.toFloat().coerceAtLeast(1f)
        camera?.update()
    }

    override fun render() {
        if (dragging || bouncePhase != BouncePhase.Idle) updateAnimation()

        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        // Ensure background is fully transparent every frame
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        instance?.let { cube ->
            cube.transform.set(Vector3.Zero, Quaternion().setEulerAngles(currentYawDegrees, 0f, 0f))
            modelBatch?.begin(camera)
            modelBatch?.render(cube, environment)
            modelBatch?.end()
        }
    }

    override fun dispose() {
        modelBatch?.dispose()
        model?.dispose()
        textures.forEach { it.dispose() }
        spriteBatch?.dispose()
        shapeRenderer?.dispose()
        font?.dispose()
    }

    private fun yawForTab(index: Int): Float = -90f * index

    private fun snappedTabIndex(yaw: Float): Int {
        val raw = (-yaw / 90f).roundToInt()
        return ((raw % IOS_TABS.size) + IOS_TABS.size) % IOS_TABS.size
    }

    private fun tabForYaw(yaw: Float): CubeTab = IOS_TABS[snappedTabIndex(yaw)]

    private enum class BouncePhase { Idle, Overshoot, Settle }

    private enum class CubeTab(val title: String) { Camera("Camera"), Feed("Feed"), Profile("Profile"), Map("Map") }

    companion object {
        const val IOS_CAMERA_INDEX = 0
        const val IOS_FEED_INDEX = 1
        const val IOS_PROFILE_INDEX = 2
        const val IOS_MAP_INDEX = 3
        private val IOS_TABS = listOf(CubeTab.Camera, CubeTab.Feed, CubeTab.Profile, CubeTab.Map)
    }
}
