package com.apptists.poplur2025.gdx.fps

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.OrthographicCamera
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// PoplurFPS native libGDX game (ApplicationAdapter).
// CPU raycaster (FpsRaycaster) renders the scene into an IntArray framebuffer that
// is uploaded to a Texture each frame and drawn through a y-down 640x360 camera;
// weapon / HUD / menu are SpriteBatch + BitmapFont overlays in the same space.
class PoplurFpsGdxGame : ApplicationAdapter() {

    private enum class GState { LOADING, MENU, ZOOM, PLAYING, PAUSED, DEAD, WIN }

    private val W = FpsK.SCREEN_W
    private val H = FpsK.SCREEN_H

    private lateinit var batch: SpriteBatch
    private lateinit var shapes: ShapeRenderer
    private lateinit var font: BitmapFont
    private lateinit var camera: OrthographicCamera
    private val layout = GlyphLayout()

    private lateinit var raycaster: FpsRaycaster
    private lateinit var fbPixmap: Pixmap
    private lateinit var fbTexture: Texture
    private lateinit var fbRegion: TextureRegion
    private var fbInts: java.nio.IntBuffer? = null

    // Screen-space presentation (orientation-aware controls / chrome)
    private lateinit var screenCam: OrthographicCamera
    private var circleTex: Texture? = null
    // Game-view band (letterboxed 640x360) in screen y-down coords, updated each frame.
    private var bandX = 0f; private var bandY = 0f; private var bandW = 0f; private var bandH = 0f
    private var isPortrait = false

    // Assets
    private var tiles = arrayOfNulls<FpsImage>(12)
    private val enemyImgs = HashMap<String, FpsImage>()
    private val pickupImgs = HashMap<String, FpsImage>()
    private val propImgs = HashMap<String, FpsImage>()
    private val weaponTex = HashMap<String, Texture>()
    private val hudTex = HashMap<String, Texture>()
    private val keyTex = HashMap<String, Texture>()
    private val ctlTex = HashMap<String, Texture>()   // arcade control-panel button sprites
    private var menuTex: Texture? = null
    private var logoTex: Texture? = null

    // Game state
    private var state = GState.LOADING
    private var level = FpsLevel()
    private var player = FpsPlayer()
    private var enemies = ArrayList<FpsEnemy>()
    private var pickups = ArrayList<FpsPickup>()
    private var props = ArrayList<FpsPickup>()
    private var score = 0
    private var kills = 0
    private var gameTime = 0.0
    private var menuTick = 0.0
    private var zoomT = 0.0
    private var muzzleFlash = 0.0
    private var torchPhase = 0.0
    private var menuFirePressed = false

    // Input tracking
    private var lookLastX = -1f
    private var lookPointer = -1

    // Audio + SFX edge tracking
    private var audio: FpsAudio? = null
    private var lastStep = 0
    private var interactPrev = false

    override fun create() {
        batch = SpriteBatch()
        shapes = ShapeRenderer()
        font = BitmapFont(true)   // built-in default font (lsans-15), flipped for the y-down camera
        camera = OrthographicCamera()
        camera.setToOrtho(true, W.toFloat(), H.toFloat())
        screenCam = OrthographicCamera()

        // Soft-edged white circle for translucent touch controls.
        val cpm = Pixmap(64, 64, Pixmap.Format.RGBA8888)
        cpm.setColor(1f, 1f, 1f, 1f); cpm.fillCircle(32, 32, 30)
        circleTex = Texture(cpm).apply { setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear) }
        cpm.dispose()

        raycaster = FpsRaycaster(W, H)
        fbPixmap = Pixmap(W, H, Pixmap.Format.RGBA8888)
        fbTexture = Texture(fbPixmap)
        fbTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        fbRegion = TextureRegion(fbTexture)
        fbInts = fbPixmap.pixels.order(ByteOrder.BIG_ENDIAN).asIntBuffer()

        loadAssets()
        audio = FpsAudio().also { it.load() }
        spawnEntities()
        state = GState.MENU
    }

    // MARK: assets

    private fun img(path: String): FpsImage? {
        return try {
            val pm = Pixmap(Gdx.files.internal("poplur_fps/assets/$path"))
            val w = pm.width; val h = pm.height
            val arr = IntArray(w * h)
            for (y in 0 until h) for (x in 0 until w) arr[y * w + x] = pm.getPixel(x, y)
            pm.dispose()
            FpsImage(w, h, arr)
        } catch (e: Exception) { null }
    }

    private fun tex(path: String): Texture? {
        return try {
            Texture(Gdx.files.internal("poplur_fps/assets/$path")).apply {
                setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
            }
        } catch (e: Exception) { null }
    }

    private fun loadAssets() {
        tiles[1] = img("08_environment_tiles/tile_01.png")
        tiles[2] = img("08_environment_tiles/tile_02.png")
        tiles[3] = img("08_environment_tiles/tile_03.png")
        tiles[4] = img("08_environment_tiles/tile_04.png")
        tiles[5] = img("08_environment_tiles/tile_05.png")
        tiles[6] = img("08_environment_tiles/tile_06.png")
        tiles[7] = img("08_environment_tiles/tile_07.png")
        tiles[8] = img("08_environment_tiles/tile_08.png")
        tiles[9] = img("09_doors_panels/door_panel_01.png")
        tiles[10] = img("09_doors_panels/door_panel_02.png")
        tiles[11] = img("09_doors_panels/door_panel_03.png")

        img("05_enemies/enemy_soldier_sheet.png")?.let { enemyImgs["soldier"] = it }
        img("05_enemies/enemy_heavy_sheet.png")?.let { enemyImgs["heavy"] = it }
        img("05_enemies/enemy_drone_sheet.png")?.let { enemyImgs["drone"] = it }
        img("05_enemies/enemy_turret_sheet.png")?.let { enemyImgs["turret"] = it }
        img("05_enemies/enemy_boss_sheet.png")?.let { enemyImgs["boss"] = it }

        img("06_pickups/pickup_health.png")?.let { pickupImgs["health"] = it }
        img("06_pickups/pickup_armor.png")?.let { pickupImgs["armor"] = it }
        img("06_pickups/pickup_ammo.png")?.let { pickupImgs["ammo"] = it }
        img("06_pickups/key_gold.png")?.let { pickupImgs["key_g"] = it }
        img("06_pickups/key_pink.png")?.let { pickupImgs["key_p"] = it }

        img("07_item_boxes/barrel_pink.png")?.let { propImgs["barrel"] = it }
        img("07_item_boxes/crate_pink.png")?.let { propImgs["crate"] = it }
        img("10_decals_signs/poplur_stay_connected_sign.png")?.let { propImgs["sign"] = it }
        img("13_misc/torch.png")?.let { propImgs["torch"] = it }
        img("13_misc/poplur_banner.png")?.let { propImgs["banner"] = it }

        tex("02_weapons/pistol.png")?.let { weaponTex["pistol"] = it }
        tex("02_weapons/shotgun.png")?.let { weaponTex["shotgun"] = it }
        tex("02_weapons/plasma.png")?.let { weaponTex["plasma"] = it }
        tex("02_weapons/rocket_launcher.png")?.let { weaponTex["rocket"] = it }
        tex("02_weapons/railgun.png")?.let { weaponTex["railgun"] = it }

        tex("11_hud_ui/hud_face.png")?.let { hudTex["face"] = it }
        tex("11_hud_ui/hud_health.png")?.let { hudTex["health"] = it }
        tex("11_hud_ui/hud_armor.png")?.let { hudTex["armor"] = it }
        tex("11_hud_ui/hud_ammo.png")?.let { hudTex["ammo"] = it }
        tex("06_pickups/key_gold.png")?.let { keyTex["gold"] = it }
        tex("06_pickups/key_pink.png")?.let { keyTex["pink"] = it }
        menuTex = tex("01_title_screen/main_menu_screen.png")
        logoTex = tex("13_misc/pixel_p_logo.png")

        // Arcade control-panel buttons (portrait): real neon panels from the spritesheet.
        tex("09_doors_panels/door_panel_04.png")?.let { ctlTex["fire"] = it }   // pink "P" panel
        tex("09_doors_panels/door_panel_05.png")?.let { ctlTex["use"] = it }    // blue "P" panel
        tex("09_doors_panels/door_panel_06.png")?.let { ctlTex["weapon"] = it } // blue panel
    }

    private fun spawnEntities() {
        enemies = ArrayList(); pickups = ArrayList(); props = ArrayList()
        for (s in FpsLevel.entitySpawns) {
            when {
                fpsIsEnemy(s.type) -> enemies.add(FpsEnemy(s.type, s.x, s.y))
                fpsIsPickup(s.type) -> pickups.add(FpsPickup(s.type, s.x, s.y))
                fpsIsProp(s.type) -> props.add(FpsPickup(s.type, s.x, s.y))
            }
        }
    }

    private fun resetGame() {
        level = FpsLevel(); player = FpsPlayer()
        score = 0; kills = 0; gameTime = 0.0
        spawnEntities()
    }

    // MARK: loop

    override fun render() {
        val dt = min(Gdx.graphics.deltaTime.toDouble(), 0.05)
        val input = readInput()
        update(dt, input)
        draw()
    }

    private fun readInput(): FpsInput {
        val inp = FpsInput()
        val sw = Gdx.graphics.width.toFloat()
        val sh = Gdx.graphics.height.toFloat()
        val portrait = sh > sw

        if (state == GState.PLAYING) {
            val lay = FpsControls.layout(sw, sh, portrait, bandX, bandY, bandW, bandH)
            for (p in 0 until 10) {
                if (!Gdx.input.isTouched(p)) continue
                val tx = Gdx.input.getX(p).toFloat(); val ty = Gdx.input.getY(p).toFloat()
                when {
                    lay.joy.contains(tx, ty) -> {
                        val dx = tx - lay.joy.cx; val dy = ty - lay.joy.cy
                        val dead = lay.joy.r * 0.28f
                        if (dy < -dead) inp.w = true
                        if (dy > dead) inp.s = true
                        if (portrait) {
                            // Arcade stick: left/right = turn
                            if (dx < -dead) inp.left = true
                            if (dx > dead) inp.right = true
                        } else {
                            // Landscape: left/right = strafe
                            if (dx < -dead) inp.a = true
                            if (dx > dead) inp.d = true
                        }
                    }
                    lay.fire.contains(tx, ty) -> inp.fire = true
                    lay.interact.contains(tx, ty) -> inp.interact = true
                    portrait && lay.weapons.getOrNull(2)?.contains(tx, ty) == true -> inp.weapon1 = true
                    portrait && lay.weapons.getOrNull(3)?.contains(tx, ty) == true -> inp.weapon2 = true
                    portrait && lay.weapons.getOrNull(4)?.contains(tx, ty) == true -> inp.weapon3 = true
                    !portrait && tx > sw * 0.42f -> {
                        // Landscape look-drag on the right side
                        if (lookPointer == -1) { lookPointer = p; lookLastX = tx }
                        if (p == lookPointer) {
                            if (lookLastX >= 0) inp.lookDX += (tx - lookLastX).toDouble() * 12.0
                            lookLastX = tx
                        }
                    }
                }
            }
            if (lookPointer >= 0 && !Gdx.input.isTouched(lookPointer)) { lookPointer = -1; lookLastX = -1f }
        } else {
            if (Gdx.input.justTouched()) { inp.fire = true; inp.interact = true }
        }
        return inp
    }

    fun screenW() = Gdx.graphics.width.toFloat()
    fun screenH() = Gdx.graphics.height.toFloat()

    private fun update(dt: Double, input: FpsInput) {
        when (state) {
            GState.LOADING -> {}
            GState.MENU -> {
                menuTick += dt
                val fire = input.fire || input.interact
                if (fire && !menuFirePressed) { state = GState.ZOOM; zoomT = 0.0; audio?.play("menu") }
                menuFirePressed = fire
            }
            GState.ZOOM -> {
                zoomT += dt * 0.85
                if (zoomT >= 1) { zoomT = 1.0; state = GState.PLAYING; resetGame() }
            }
            GState.PLAYING -> {
                gameTime += dt
                muzzleFlash = maxOf(0.0, muzzleFlash - dt * 12)
                torchPhase += dt * 3
                player.update(dt, input, level)
                level.update(dt)

                // Footstep clicks on each half-bob while moving.
                val moving = input.w || input.s || input.a || input.d || input.left || input.right
                val step = (player.bobPhase / PI).toInt()
                if (moving && step != lastStep) audio?.play("step")
                lastStep = step

                // Door "use" thunk on the rising edge of interact.
                if (input.interact && !interactPrev) audio?.play("door")
                interactPrev = input.interact

                if (input.fire && player.canFire()) doShoot()
                for (e in enemies) if (!e.dead) e.update(dt, player, level) {
                    val before = player.health
                    player.takeDamage(e.def.damage)
                    if (player.health < before) audio?.play("hurt")
                }
                for (p in pickups) if (!p.collected && p.tryCollect(player)) {
                    score += if (p.type >= 203) 500 else 50
                    audio?.play("pickup")
                }
                if (player.dead) { state = GState.DEAD; audio?.play("lose") }
                val boss = enemies.firstOrNull { it.type == FpsK.SPR_BOSS }
                if (boss != null && boss.dead && boss.deathTimer > 1.5) { state = GState.WIN; audio?.play("win") }
            }
            GState.PAUSED -> {}
            GState.DEAD -> {
                if (player.deadTimer > 2.0 && input.interact) {
                    state = GState.PLAYING; player.dead = false; player.health = 50.0
                    player.deadTimer = 0.0; player.flashAlpha = 0.0
                }
            }
            GState.WIN -> { if (input.interact) { state = GState.MENU; zoomT = 0.0 } }
        }
    }

    private fun doShoot() {
        val wpn = FPS_WEAPONS[player.weapon]
        player.consumeAmmo()
        muzzleFlash = 1.0
        audio?.shoot(player.weapon)
        for (i in 0 until wpn.projectiles) {
            val spread = (Random.nextDouble() - 0.5) * wpn.spread * 2
            val c = cos(spread); val s = sin(spread)
            val rdx = player.dirX * c - player.dirY * s
            val rdy = player.dirX * s + player.dirY * c
            var closest = Double.MAX_VALUE
            var hitEnemy: FpsEnemy? = null
            for (e in enemies) {
                if (e.dead) continue
                val ex = e.x - player.x; val ey = e.y - player.y
                if (ex * rdx + ey * rdy <= 0) continue
                val dist = Math.sqrt(ex * ex + ey * ey)
                if (abs(ex * rdy - ey * rdx) > 0.5) continue
                var wallHit = false
                val steps = Math.ceil(dist * 3).toInt()
                var k = 1
                while (k < steps) {
                    val t = k.toDouble() / steps
                    if (level.isSolid(player.x + rdx * dist * t, player.y + rdy * dist * t)) { wallHit = true; break }
                    k++
                }
                if (!wallHit && dist < closest) { closest = dist; hitEnemy = e }
            }
            hitEnemy?.let { e ->
                val variance = Random.nextDouble() * wpn.damage * 0.4 - wpn.damage * 0.2
                e.hit(wpn.damage + variance)
                if (e.dead) { kills++; score += e.def.score; player.score = score; audio?.play("die") }
                else audio?.play("hit")
            }
        }
    }

    // MARK: draw

    private fun draw() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val sw = Gdx.graphics.width.toFloat(); val sh = Gdx.graphics.height.toFloat()
        isPortrait = sh > sw
        val playing = state == GState.PLAYING || state == GState.PAUSED ||
                      state == GState.DEAD || state == GState.WIN
        val scale = min(sw / W, sh / H)
        val vw = W * scale; val vh = H * scale
        val vx = (sw - vw) / 2
        // Portrait gameplay: sit the CRT just below a marquee strip so the control
        // panel fills the space below it (arcade-cabinet layout). Otherwise centre it.
        val marqueeH = sh * 0.10f
        val vy = if (isPortrait && playing) sh - marqueeH - vh else (sh - vh) / 2
        // Band in y-down screen coords (GL viewport vy is measured from the bottom).
        bandX = vx; bandW = vw; bandH = vh; bandY = sh - vy - vh

        // Pass 1 — game / menu in the 640x360 letterbox viewport.
        Gdx.gl.glViewport(vx.toInt(), vy.toInt(), vw.toInt(), vh.toInt())
        batch.projectionMatrix = camera.combined
        batch.begin()
        when (state) {
            GState.LOADING -> drawLoading()
            GState.MENU -> FpsMenuRenderer.draw(this, 0.0)
            GState.ZOOM -> FpsMenuRenderer.draw(this, zoomT)
            else -> drawScene()
        }
        batch.end()

        // Pass 2 — screen-space controls: portrait = arcade cabinet buttons around
        // the game band; landscape = translucent overlays on top of the game.
        if (playing) {
            Gdx.gl.glViewport(0, 0, sw.toInt(), sh.toInt())
            screenCam.setToOrtho(true, sw, sh)
            batch.projectionMatrix = screenCam.combined
            batch.begin()
            val lay = FpsControls.layout(sw, sh, isPortrait, bandX, bandY, bandW, bandH)
            FpsControls.draw(this, lay, isPortrait, player.weapon)
            batch.end()
        }
    }

    /** Draw a filled circle (translucent control) in the current projection. */
    fun drawCircle(cx: Float, cy: Float, r: Float, col: Color, alpha: Float) {
        val t = circleTex ?: return
        batch.setColor(col.r, col.g, col.b, alpha)
        batch.draw(t, cx - r, cy - r, r * 2, r * 2)
        batch.color = Color.WHITE
    }

    private fun drawScene() {
        val sprites = ArrayList<FpsRenderSprite>()
        for (e in enemies) {
            if (e.dead && e.deathTimer > 2.5) continue
            val a = if (e.dead) maxOf(0.0, 1 - (e.deathTimer - 0.5) / 2) else 1.0
            sprites.add(FpsRenderSprite(e.x, e.y, enemyImgs[fpsEnemyKey(e.type)],
                if (e.type == FpsK.SPR_BOSS) 1.5 else 1.0, e.hitFlash > 0, a))
        }
        for (p in pickups) if (!p.collected)
            sprites.add(FpsRenderSprite(p.x, p.y, pickupImgs[fpsSpriteAssetKey(p.type)], 0.65))
        for (p in props) {
            val key = fpsSpriteAssetKey(p.type)
            val sc = if (key == "torch") 0.5 + sin(torchPhase) * 0.04 else 0.75
            sprites.add(FpsRenderSprite(p.x, p.y, propImgs[key], sc))
        }

        raycaster.renderScene(player, level, tiles.toList(), sprites)
        fbInts!!.clear(); fbInts!!.put(raycaster.fb)
        fbTexture.draw(fbPixmap, 0, 0)
        batch.color = Color.WHITE
        batch.draw(fbRegion, 0f, 0f, W.toFloat(), H.toFloat())

        if (muzzleFlash > 0) fillRect(0f, 0f, W.toFloat(), H.toFloat(), 1f, 0.78f, 0.31f, (muzzleFlash * 0.18).toFloat())

        // Weapon
        weaponTex[FPS_WEAPON_KEYS[player.weapon]]?.let { wt ->
            val s = (H * 0.62f) / wt.height
            val dw = wt.width * s; val dh = wt.height * s
            val bobY = (sin(player.bobPhase) * 6).toFloat()
            val bobX = (cos(player.bobPhase * 0.5) * 3).toFloat()
            val kickY = (player.weaponKick * 30).toFloat()
            val dx = W / 2f - dw / 2f + bobX
            val dy = H - dh + bobY + kickY + 8
            batch.color = Color.WHITE
            batch.draw(TextureRegion(wt).apply { flip(false, true) }, dx, dy, dw, dh)
        }

        FpsHudRenderer.draw(this)

        when (state) {
            GState.DEAD -> { fillRect(0f,0f,W.toFloat(),H.toFloat(),0.6f,0f,0f,(min(1.0, player.deadTimer/1.5)*0.65).toFloat()); text("YOU DIED", W/2f, H/2f-20, 3.4f, Color.valueOf("ff3333"), 0) }
            GState.WIN -> { fillRect(0f,0f,W.toFloat(),H.toFloat(),0f,0f,0f,0.75f); text("LEVEL COMPLETE!", W/2f, H/2f-40, 3f, Color.valueOf("ffd700"), 0) }
            else -> {}
        }
    }

    private fun drawLoading() {
        fillRect(0f, 0f, W.toFloat(), H.toFloat(), 0.04f, 0f, 0.09f, 1f)
        text("POPLUR FPS", W / 2f, H / 2f - 30, 2.2f, Color.valueOf("ffd700"), 0)
        text("LOADING...", W / 2f, H / 2f + 6, 1f, Color.valueOf("ff4db8"), 0)
    }

    // MARK: shared draw helpers (used by HUD/menu renderers)

    fun batch() = batch
    fun font() = font
    fun layout() = layout
    fun tilesList() = tiles
    fun menuTexture() = menuTex
    fun logoTexture() = logoTex
    fun hudTexture(k: String) = hudTex[k]
    fun keyTexture(k: String) = keyTex[k]
    fun ctlTexture(k: String) = ctlTex[k]

    /** Draw a texture (y-down) tinted white at the given alpha — for control-panel buttons. */
    fun drawTexA(t: Texture, x: Float, y: Float, w: Float, h: Float, alpha: Float) {
        batch.setColor(1f, 1f, 1f, alpha)
        batch.draw(TextureRegion(t).apply { flip(false, true) }, x, y, w, h)
        batch.color = Color.WHITE
    }
    fun playerRef() = player
    fun levelRef() = level
    fun scoreVal() = score
    fun killsVal() = kills
    fun gameTimeVal() = gameTime
    fun menuTickVal() = menuTick
    fun widthI() = W
    fun heightI() = H

    private var pixel: Texture? = null
    private fun whitePixel(): Texture {
        if (pixel == null) {
            val pm = Pixmap(1, 1, Pixmap.Format.RGBA8888); pm.setColor(Color.WHITE); pm.fill()
            pixel = Texture(pm); pm.dispose()
        }
        return pixel!!
    }

    fun fillRect(x: Float, y: Float, w: Float, h: Float, r: Float, g: Float, b: Float, a: Float) {
        batch.color = Color(r, g, b, a)
        batch.draw(whitePixel(), x, y, w, h)
        batch.color = Color.WHITE
    }

    /** Draw text; y is the top of the text (y-down). align: 0=center, -1=left, 1=right. */
    fun text(s: String, x: Float, y: Float, scale: Float, color: Color, align: Int) {
        font.data.setScale(scale)
        font.color = color
        layout.setText(font, s)
        var dx = x
        if (align == 0) dx = x - layout.width / 2 else if (align == 1) dx = x - layout.width
        font.draw(batch, layout, dx, y)
        font.data.setScale(1f)
    }

    fun drawTex(t: Texture, x: Float, y: Float, w: Float, h: Float) {
        batch.color = Color.WHITE
        batch.draw(TextureRegion(t).apply { flip(false, true) }, x, y, w, h)
    }

    override fun dispose() {
        batch.dispose(); shapes.dispose(); font.dispose()
        fbTexture.dispose(); fbPixmap.dispose()
        weaponTex.values.forEach { it.dispose() }
        hudTex.values.forEach { it.dispose() }
        keyTex.values.forEach { it.dispose() }
        ctlTex.values.forEach { it.dispose() }
        menuTex?.dispose(); logoTex?.dispose(); pixel?.dispose()
        audio?.dispose()
    }
}
