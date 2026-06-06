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
    }
    
    private fun sin(f: Float): Float = MathUtils.sin(f)
}
