package com.apptists.poplur2025.gdx.weather

import com.apptists.poplur2025.data.weather.WeatherBadge
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import java.util.Random

class WeatherOverlayAdapter : ApplicationAdapter() {
    private lateinit var shapes: ShapeRenderer
    private var weatherBadge: WeatherBadge? = null
    private var time = 0f
    
    // Simple particle data
    private val random = Random()
    private val particles = List(100) { Particle() }
    private var lightningAlpha = 0f

    override fun create() {
        shapes = ShapeRenderer()
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    }

    fun setWeather(newWeather: WeatherBadge) {
        this.weatherBadge = newWeather
        // Reset particles on significant weather change if needed
    }

    override fun render() {
        time += Gdx.graphics.deltaTime
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val currentLabel = weatherBadge?.label ?: return
        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()

        shapes.begin(ShapeRenderer.ShapeType.Filled)
        
        when {
            currentLabel.contains("Rain", ignoreCase = true) || currentLabel.contains("Drizzle", ignoreCase = true) -> {
                renderRain(w, h)
            }
            currentLabel.contains("Snow", ignoreCase = true) -> {
                renderSnow(w, h)
            }
            currentLabel.contains("Storm", ignoreCase = true) -> {
                renderStorm(w, h)
            }
            currentLabel.contains("Fog", ignoreCase = true) || currentLabel.contains("Cloudy", ignoreCase = true) -> {
                renderMist(w, h)
            }
        }
        
        shapes.end()
        
        if (lightningAlpha > 0) {
            renderLightningOverlay(w, h)
            lightningAlpha -= Gdx.graphics.deltaTime * 2f
        }
    }

    private fun renderRain(w: Float, h: Float) {
        shapes.color = Color(0.6f, 0.7f, 1.0f, 0.6f)
        for (p in particles) {
            p.update(h, 600f + random.nextFloat() * 400f)
            shapes.rect(p.x * w, p.y * h, 2f, 20f)
        }
    }

    private fun renderSnow(w: Float, h: Float) {
        shapes.color = Color(1f, 1f, 1f, 0.8f)
        for (p in particles) {
            p.update(h, 100f + random.nextFloat() * 50f)
            val oscillation = sin(time * 2f + p.x * 10f) * 10f
            shapes.circle(p.x * w + oscillation, p.y * h, 4f)
        }
    }

    private fun renderStorm(w: Float, h: Float) {
        renderRain(w, h)
        if (random.nextFloat() < 0.01f) {
            lightningAlpha = 0.8f
        }
    }

    private fun renderMist(w: Float, h: Float) {
        particles.forEachIndexed { index, p ->
            p.drift(w, 22f + (index % 7) * 4f)
            val wave = sin(time * 0.45f + index * 0.31f) * 18f
            val x = p.x * w + wave
            val y = p.y * h
            val width = 64f + (index % 5) * 18f
            val alpha = 0.035f + (index % 4) * 0.008f

            shapes.color = Color(0.78f, 0.86f, 0.94f, alpha)
            shapes.rect(x, y, width, 1.6f)

            if (index % 3 == 0) {
                shapes.color = Color(0.9f, 0.95f, 1f, alpha * 1.35f)
                shapes.circle(x + width * 0.5f, y, 2.2f)
            }
        }
    }

    private fun renderLightningOverlay(w: Float, h: Float) {
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(1f, 1f, 1f, lightningAlpha)
        shapes.rect(0f, 0f, w, h)
        shapes.end()
    }

    override fun dispose() {
        shapes.dispose()
    }

    private inner class Particle {
        var x = random.nextFloat()
        var y = random.nextFloat()
        
        fun update(h: Float, speed: Float) {
            y -= (speed * Gdx.graphics.deltaTime) / h
            if (y < -0.1f) {
                y = 1.1f
                x = random.nextFloat()
            }
        }

        fun drift(w: Float, speed: Float) {
            x += (speed * Gdx.graphics.deltaTime) / w
            y += sin(time * 0.18f + x * 8f) * Gdx.graphics.deltaTime * 0.003f
            if (x > 1.12f) {
                x = -0.12f
                y = random.nextFloat()
            }
        }
    }
    
    private fun sin(f: Float): Float = MathUtils.sin(f)
}
