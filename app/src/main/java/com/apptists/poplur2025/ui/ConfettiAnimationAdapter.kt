package com.apptists.poplur2025.ui

import com.apptists.poplur.performance.PoplurPerformanceManager
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.Vector2

class ConfettiAnimationAdapter : ApplicationAdapter() {
    private var camera: OrthographicCamera? = null
    private var modelBatch: ModelBatch? = null
    private var environment: Environment? = null
    private var confettiModel: Model? = null
    private val pieces = mutableListOf<ConfettiPiece>()
    private val fireworks = mutableListOf<Firework>()
    private var lastWidth = 0
    private var lastHeight = 0
    private var fireworkTimer = 0f
    private var spawnTimer = 0f
    private var crackleSound: Sound? = null
    private var performanceManager: PoplurPerformanceManager? = null
    
    @Volatile
    var isMuted: Boolean = false

    override fun create() {
        performanceManager = PoplurPerformanceManager.getInitializedInstance()
        Gdx.graphics.setContinuousRendering(true)
        modelBatch = ModelBatch()
        
        environment = Environment().apply {
            set(ColorAttribute(ColorAttribute.AmbientLight, 0.7f, 0.7f, 0.7f, 1f))
            add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -0.5f, -0.5f, -0.5f))
        }

        val modelBuilder = ModelBuilder()
        confettiModel = modelBuilder.createRect(
            -0.5f, -0.5f, 0f,
            0.5f, -0.5f, 0f,
            0.5f, 0.5f, 0f,
            -0.5f, 0.5f, 0f,
            0f, 0f, 1f,
            Material(ColorAttribute.createDiffuse(Color.WHITE)),
            (Usage.Position or Usage.Normal).toLong()
        )

        runCatching {
            // Explicitly logging or attempting alternative load if this fails
            val file = Gdx.files.internal("Firework-crackling-noise.mp3")
            if (file.exists()) {
                crackleSound = Gdx.audio.newSound(file)
            } else {
                android.util.Log.e("ConfettiAdapter", "FIREWORK SOUND FILE MISSING AT: Firework-crackling-noise.mp3")
            }
        }.onFailure { 
            android.util.Log.e("ConfettiAdapter", "FAILED TO LOAD FIREWORK SOUND", it)
        }

        resetCamera()
        populateInitialPieces()
    }

    private fun populateInitialPieces() {
        pieces.clear()
        // Dense sprinkle to match the prototype/iOS confetti. iOS emits ~189 particles (birthRate 45
        // x lifetime 4.2) across logical points; this GL surface renders in physical pixels (~3.5x the
        // area on the A03s), so parity needs a proportionally higher count. The old lag came from
        // per-frame ModelInstance allocation (now cached below), so a higher count is safe.
        val density = performanceManager?.state?.value?.particleDensityMultiplier ?: 1f
        repeat((520 * density).toInt().coerceAtLeast(0)) {
            pieces += ConfettiPiece().apply {
                reset(fullScreen = true)
            }
        }
    }

    override fun resize(width: Int, height: Int) {
        lastWidth = width
        lastHeight = height
        resetCamera()
    }

    private fun resetCamera() {
        val w = Gdx.graphics.width.toFloat().coerceAtLeast(1f)
        val h = Gdx.graphics.height.toFloat().coerceAtLeast(1f)
        camera = OrthographicCamera(w, h).apply {
            position.set(w / 2f, h / 2f, 0f)
            update()
        }
    }

    override fun render() {
        val policy = performanceManager?.state?.value
        val density = policy?.particleDensityMultiplier ?: 1f
        if (policy?.particlesAllowed == false) {
            pieces.clear()
            fireworks.clear()
        }
        val delta = Gdx.graphics.deltaTime.coerceAtMost(1f / 30f)
        val w = lastWidth.toFloat()
        val h = lastHeight.toFloat()

        if (w < 1f || h < 1f) return

        // Light continuous sprinkle — was 1000/sec up to a cap of 2500, which (combined with
        // per-frame ModelInstance allocation) is what made this screen lag.
        spawnTimer += delta
        val particleCap = (700 * density).toInt().coerceAtLeast(0)
        while (pieces.size > particleCap) pieces.removeAt(pieces.lastIndex)
        val spawnRate = 180f * density
        val spawnInterval = if (spawnRate > 0f) 1f / spawnRate else Float.MAX_VALUE
        while (spawnTimer > spawnInterval) {
            if (pieces.size < particleCap) { // cached ModelInstances make this cheap
                pieces += ConfettiPiece().apply { reset(fullScreen = false) }
            }
            spawnTimer -= spawnInterval
        }

        // Occasional firework rather than a constant barrage.
        fireworkTimer += delta
        if (density >= 0.45f && fireworkTimer > 3.5f / density.coerceAtLeast(0.1f)) {
            fireworks += Firework()
            fireworkTimer = 0f
        }

        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        modelBatch?.begin(camera)
        
        val iterator = pieces.iterator()
        while (iterator.hasNext()) {
            val piece = iterator.next()
            piece.update(delta)
            if (piece.isDead(h)) {
                iterator.remove()
                continue
            }

            // Reuse the piece's own cached ModelInstance instead of allocating a new one
            // every frame — with up to 150 pieces at 60fps that was ~9,000 allocations/sec
            // and the main source of GC-driven lag on this screen.
            val instance = piece.modelInstance
            instance.transform.setToTranslation(piece.position)
            instance.transform.rotate(Vector3.X, piece.rotation.x)
            instance.transform.rotate(Vector3.Y, piece.rotation.y)
            instance.transform.rotate(Vector3.Z, piece.rotation.z)
            // Delicate scaling
            val densityScale = w / 640f
            instance.transform.scale(piece.size.x * densityScale, piece.size.y * densityScale, 1f)

            modelBatch?.render(instance, environment)
        }

        val fwIterator = fireworks.iterator()
        while (fwIterator.hasNext()) {
            val fw = fwIterator.next()
            fw.update(delta)
            if (fw.isDead()) {
                fwIterator.remove()
                continue
            }

            val densityScale = w / 480f
            if (fw.state == 0) {
                val instance = fw.modelInstance
                instance.transform.setToTranslation(fw.position)
                instance.transform.scale(2.5f * densityScale, 2.5f * densityScale, 1f)
                modelBatch?.render(instance, environment)
            } else {
                fw.sparks.forEach { spark ->
                    val instance = spark.modelInstance
                    instance.transform.setToTranslation(spark.position)
                    instance.transform.scale(1.8f * densityScale, 1.8f * densityScale, 1f)
                    modelBatch?.render(instance, environment)
                }
            }
        }

        modelBatch?.end()
    }

    override fun dispose() {
        modelBatch?.dispose()
        confettiModel?.dispose()
        crackleSound?.dispose()
    }

    fun restart() {
        populateInitialPieces()
        fireworks.clear()
        fireworkTimer = 0f
    }

    private inner class ConfettiPiece {
        val position = Vector3()
        val velocity = Vector3()
        val acceleration = Vector3(25f, -220f, 0f)
        val rotation = Vector3()
        val spin = Vector3()
        var color = Color.WHITE
        var lifetime = 0f
        val maxLifetime = 5.5f
        val size = Vector2(3f, 7f) // Thinner, more elegant
        // Created once per piece instead of once per frame — see render().
        val modelInstance = ModelInstance(confettiModel!!)

        fun reset(fullScreen: Boolean) {
            val w = lastWidth.toFloat()
            val h = lastHeight.toFloat()
            
            position.x = MathUtils.random(0f, w)
            if (fullScreen) {
                position.y = MathUtils.random(0f, h)
                lifetime = MathUtils.random(0f, maxLifetime)
            } else {
                position.y = h + 20f
                lifetime = 0f
            }
            position.z = MathUtils.random(-5f, 5f)

            val baseSpeed = h * 0.18f + MathUtils.random(-h * 0.05f, h * 0.05f)
            val angle = MathUtils.random(-25f, 25f) * MathUtils.degreesToRadians
            velocity.set(MathUtils.sin(angle) * baseSpeed, -MathUtils.cos(angle) * baseSpeed, 0f)

            spin.set(
                MathUtils.random(-4f, 4f) * 180f,
                MathUtils.random(-4f, 4f) * 180f,
                MathUtils.random(-4f, 4f) * 180f
            )
            
            rotation.set(MathUtils.random(360f), MathUtils.random(360f), MathUtils.random(360f))
            
            color = CONFETTI_PALETTE[MathUtils.random(CONFETTI_PALETTE.size - 1)]
            modelInstance.materials.first().set(ColorAttribute.createDiffuse(color))
        }

        fun update(delta: Float) {
            velocity.add(acceleration.x * delta, acceleration.y * delta, acceleration.z * delta)
            position.add(velocity.x * delta, velocity.y * delta, velocity.z * delta)
            rotation.add(spin.x * delta, spin.y * delta, spin.z * delta)
            lifetime += delta
        }

        fun isDead(screenHeight: Float) = lifetime > maxLifetime || position.y < -100f
    }

    private inner class Firework {
        val sparks = mutableListOf<Spark>()
        var state = 0
        val position = Vector3()
        val velocity = Vector3()
        val burstHeight: Float
        // Created once instead of once per frame — see render().
        val modelInstance = ModelInstance(confettiModel!!).apply {
            materials.first().set(ColorAttribute.createDiffuse(Color.WHITE))
        }

        init {
            val w = lastWidth.toFloat()
            val h = lastHeight.toFloat()
            position.set(MathUtils.random(w * 0.25f, w * 0.75f), -20f, 0f)
            burstHeight = h * MathUtils.random(0.65f, 0.9f)
            velocity.set(0f, h * 0.55f, 0f)
        }

        fun update(delta: Float) {
            if (state == 0) {
                position.add(0f, velocity.y * delta, 0f)
                if (position.y >= burstHeight) {
                    burst()
                }
            } else {
                val iterator = sparks.iterator()
                while (iterator.hasNext()) {
                    val spark = iterator.next()
                    spark.update(delta)
                    if (spark.lifetime > 1.4f) {
                        iterator.remove()
                    }
                }
            }
        }

        private fun burst() {
            state = 1
            if (!isMuted) {
                // High volume for explosion
                crackleSound?.play(1.0f, 1.0f, (position.x / lastWidth) * 2f - 1f)
            }
            repeat(20) {
                val spark = Spark(position)
                val angle = MathUtils.random(0f, 360f) * MathUtils.degreesToRadians
                val speed = lastHeight * 0.12f * MathUtils.random(0.5f, 1.4f)
                spark.velocity.set(
                    MathUtils.cos(angle) * speed * 0.7f,
                    MathUtils.sin(angle) * speed,
                    MathUtils.random(-8f, 8f)
                )
                spark.color = CONFETTI_PALETTE[MathUtils.random(CONFETTI_PALETTE.size - 1)]
                spark.modelInstance.materials.first().set(ColorAttribute.createDiffuse(spark.color))
                sparks += spark
            }
        }

        fun isDead() = state == 1 && sparks.isEmpty()
    }

    private inner class Spark(startPos: Vector3) {
        val position = Vector3().set(startPos)
        val velocity = Vector3()
        var lifetime = 0f
        var color = Color.WHITE
        // Created once instead of once per frame — see render().
        val modelInstance = ModelInstance(confettiModel!!)

        fun update(delta: Float) {
            position.add(velocity.x * delta, velocity.y * delta, velocity.z * delta)
            velocity.y -= Gdx.graphics.height * 0.12f * delta
            lifetime += delta
        }
    }

    companion object {
        private val CONFETTI_PALETTE = arrayOf(
            Color.valueOf("FF2D55"),
            Color.valueOf("FF9500"),
            Color.valueOf("FFCC00"),
            Color.valueOf("34C759"),
            Color.valueOf("007AFF"),
            Color.valueOf("AF52DE")
        )
    }
}
