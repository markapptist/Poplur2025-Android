package com.apptists.poplur2025.ui

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils

class ConfettiAnimationAdapter : ApplicationAdapter() {
    private var camera: OrthographicCamera? = null
    private var shapeRenderer: ShapeRenderer? = null
    private val pieces = mutableListOf<ConfettiPiece>()
    private var restartPending = false

    override fun create() {
        Gdx.graphics.setContinuousRendering(true)
        shapeRenderer = ShapeRenderer()
        resetCamera()
        populatePieces()
    }

    fun restart() {
        if (shapeRenderer == null) {
            restartPending = true
            return
        }
        Gdx.graphics.setContinuousRendering(true)
        restartPending = false
        populatePieces()
        Gdx.graphics.requestRendering()
    }

    private fun populatePieces() {
        pieces.clear()
        repeat(110) { pieces += newPiece(initial = true) }
    }

    override fun resize(width: Int, height: Int) {
        resetCamera()
    }

    override fun render() {
        val width = Gdx.graphics.width.toFloat().coerceAtLeast(1f)
        val height = Gdx.graphics.height.toFloat().coerceAtLeast(1f)
        val delta = Gdx.graphics.deltaTime.coerceAtMost(1f / 30f)

        if (restartPending) restart()
        Gdx.graphics.setContinuousRendering(true)

        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        shapeRenderer?.projectionMatrix = camera?.combined
        shapeRenderer?.begin(ShapeRenderer.ShapeType.Filled)
        pieces.forEach { piece ->
            piece.y -= piece.speed * delta
            piece.x += piece.drift * delta
            piece.rotation += piece.spin * delta
            if (piece.y < -piece.size * 2f) piece.reset(width, height)

            shapeRenderer?.color = piece.color
            shapeRenderer?.identity()
            shapeRenderer?.translate(piece.x, piece.y, 0f)
            shapeRenderer?.rotate(0f, 0f, 1f, piece.rotation)
            shapeRenderer?.rect(
                -piece.size * 0.5f,
                -piece.size * 0.18f,
                piece.size,
                piece.size * 0.36f
            )
        }
        shapeRenderer?.end()
    }

    override fun dispose() {
        shapeRenderer?.dispose()
    }

    private fun resetCamera() {
        val width = Gdx.graphics.width.toFloat().coerceAtLeast(1f)
        val height = Gdx.graphics.height.toFloat().coerceAtLeast(1f)
        camera = OrthographicCamera(width, height).apply {
            setToOrtho(false, width, height)
            update()
        }
    }

    private fun newPiece(initial: Boolean): ConfettiPiece {
        val width = Gdx.graphics.width.toFloat().coerceAtLeast(1f)
        val height = Gdx.graphics.height.toFloat().coerceAtLeast(1f)
        return ConfettiPiece().apply {
            reset(width, height)
            if (initial) y = MathUtils.random(-height * 0.15f, height * 1.15f)
        }
    }

    private class ConfettiPiece {
        var x = 0f
        var y = 0f
        var size = 18f
        var speed = 160f
        var drift = 0f
        var spin = 0f
        var rotation = 0f
        var color = Color.WHITE

        fun reset(width: Float, height: Float) {
            x = MathUtils.random(-40f, width + 40f)
            y = MathUtils.random(height, height + 260f)
            size = MathUtils.random(16f, 34f)
            speed = MathUtils.random(95f, 230f)
            drift = MathUtils.random(-55f, 55f)
            spin = MathUtils.random(-180f, 180f)
            rotation = MathUtils.random(0f, 360f)
            color = CONFETTI_COLORS[MathUtils.random(CONFETTI_COLORS.lastIndex)]
        }
    }

    companion object {
        private val CONFETTI_COLORS = arrayOf(
            Color(0.96f, 0.30f, 0.34f, 0.92f),
            Color(0.98f, 0.48f, 0.23f, 0.9f),
            Color(1.0f, 0.72f, 0.22f, 0.92f),
            Color(0.93f, 0.20f, 0.40f, 0.86f)
        )
    }
}
