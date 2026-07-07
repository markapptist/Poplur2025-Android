package com.apptists.poplur2025.gdx

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import kotlin.math.min

import java.util.Locale

class ProfileTalkingHeadAdapter : ApplicationAdapter() {
    @Volatile private var speaking: Boolean = false
    private lateinit var batch: SpriteBatch
    private var appInstance: com.badlogic.gdx.Application? = null
    private val frames = mutableListOf<Texture>()
    private var elapsed = 0f

    override fun create() {
        appInstance = Gdx.app
        Gdx.graphics.setContinuousRendering(speaking)
        batch = SpriteBatch()
        frames += (1..12).map { index ->
            val path = String.format(Locale.US, "profile_head/frame_%02d.png", index)
            Texture(Gdx.files.internal(path)).apply {
                setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            }
        }
        Gdx.graphics.requestRendering()
    }

    fun setSpeaking(active: Boolean) {
        speaking = active
        appInstance?.postRunnable {
            Gdx.graphics.setContinuousRendering(active)
            Gdx.graphics.requestRendering()
        }
    }

    override fun render() {
        elapsed += Gdx.graphics.deltaTime.coerceAtMost(0.05f)
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        if (frames.isEmpty()) return
        val texture = frames[frameIndex()]
        val maxWidth = Gdx.graphics.width * 0.96f
        val maxHeight = Gdx.graphics.height * 0.98f
        val scale = min(maxWidth / texture.width, maxHeight / texture.height)
        val drawWidth = texture.width * scale
        val drawHeight = texture.height * scale
        val x = (Gdx.graphics.width - drawWidth) / 2f
        val y = (Gdx.graphics.height - drawHeight) / 2f

        batch.begin()
        batch.draw(texture, x, y, drawWidth, drawHeight)
        batch.end()
        if (speaking) {
            Gdx.graphics.requestRendering()
        }
    }

    private fun frameIndex(): Int {
        if (speaking) {
            val talking = intArrayOf(2, 3, 11, 0, 2, 3, 9, 0)
            return talking[((elapsed / 0.085f).toInt()) % talking.size]
        }

        return when (elapsed % 7.1f) {
            in 1.12f..1.28f -> 6
            in 2.24f..2.58f -> 1
            in 3.42f..3.72f -> 4
            in 3.73f..3.92f -> 9
            in 5.12f..5.34f -> 8
            in 6.08f..6.28f -> 7
            else -> 0
        }
    }

    override fun dispose() {
        frames.forEach { it.dispose() }
        frames.clear()
        if (::batch.isInitialized) batch.dispose()
        appInstance = null
    }
}
