package com.apptists.poplur2025.gdx

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import kotlin.math.abs
import kotlin.math.sin

class PoplurGdxScene(private val scene: String) : ApplicationAdapter() {
    private lateinit var shapes: ShapeRenderer
    private lateinit var batch: SpriteBatch
    private lateinit var font: BitmapFont
    private var time = 0f
    override fun create() { shapes = ShapeRenderer(); batch = SpriteBatch(); font = BitmapFont() }
    override fun render() {
        time += Gdx.graphics.deltaTime
        Gdx.gl.glClearColor(0.03f, 0.03f, 0.05f, 1f); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        val w = Gdx.graphics.width.toFloat(); val h = Gdx.graphics.height.toFloat(); val cx = w/2f; val base = h*0.22f
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (i in 0 until 11) {
            val height = (h*0.12f) + abs(sin(time*1.6f + i*.45f))*h*.52f
            val x = cx - 220 + i*44
            shapes.color = Color(0.2f + i*.035f, 0.7f, 1f, 1f)
            shapes.rect(x, base, 30f, height)
        }
        shapes.color = Color(1f, .88f, .18f, 1f); shapes.circle(cx + sin(time)*150f, h*.72f, 26f)
        shapes.end()
        batch.begin(); font.color = Color.WHITE; font.draw(batch, "LibGDX ${scene.replaceFirstChar { it.uppercase() }} Scene", 28f, h - 34f); font.draw(batch, "SceneKit/SpriteKit replacement layer", 28f, h - 62f); batch.end()
    }
    override fun dispose() { shapes.dispose(); batch.dispose(); font.dispose() }
}
