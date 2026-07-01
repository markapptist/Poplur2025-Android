package com.apptists.poplur2025.ui

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Interpolation
import android.util.Log

class LogoAnimationAdapter(private val logoAssetPath: String) : ApplicationAdapter() {
    private var spriteBatch: SpriteBatch? = null
    private var logoTexture: Texture? = null
    private var logoScale = 0f
    private var logoAlpha = 0f
    private var logoAnimTime = 1.2f // Start finished
    private var isLogoAnimating = false

    override fun create() {
        try {
            Log.d("LogoAnimationAdapter", "create() started")
            spriteBatch = SpriteBatch()
            val logoFile = Gdx.files.internal(logoAssetPath)
            if (!logoFile.exists()) {
                error("Required logo asset is missing: $logoAssetPath")
            }
            logoTexture = Texture(logoFile)
            logoTexture?.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            Log.d("LogoAnimationAdapter", "create() finished successfully")
        } catch (e: Exception) {
            Log.e("LogoAnimationAdapter", "CRITICAL ERROR in create()", e)
            throw e
        }
    }

    fun triggerLogoPop() {
        isLogoAnimating = true
        logoAnimTime = 0f
    }

    override fun render() {
        if (isLogoAnimating) {
            logoAnimTime += Gdx.graphics.deltaTime
            if (logoAnimTime < 0.6f) {
                val p = (logoAnimTime / 0.6f)
                logoScale = Interpolation.swingOut.apply(0f, 1.2f, p)
                logoAlpha = Interpolation.fade.apply(0f, 1f, p)
            } else if (logoAnimTime < 1.2f) {
                val p = (logoAnimTime - 0.6f) / 0.6f
                logoScale = 1.2f - p * 0.2f
                logoAlpha = 1f - p
            } else {
                isLogoAnimating = false
                logoScale = 0f
                logoAlpha = 0f
            }
        }

        // CRITICAL: Ensure clear color is transparent EVERY frame
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        if (logoAlpha > 0f && logoTexture != null) {
            spriteBatch?.begin()
            val w = 340f * logoScale
            val h = 340f * logoScale
            spriteBatch?.setColor(1f, 1f, 1f, logoAlpha)
            spriteBatch?.draw(logoTexture, (Gdx.graphics.width - w) / 2f, (Gdx.graphics.height - h) / 2f, w, h)
            spriteBatch?.end()
        }
    }

    override fun dispose() {
        spriteBatch?.dispose()
        logoTexture?.dispose()
    }
}
