package com.apptists.poplur2025.gdx.fps

import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// PoplurFPS native (libGDX) — core simulation.
// Kotlin port of the TypeScript raycaster game (assets/poplur_fps/src), mirroring
// the iOS SpriteKit port. Pure simulation here; rendering lives in FpsRaycaster
// and PoplurFpsGdxGame.

object FpsK {
    const val SCREEN_W = 640
    const val SCREEN_H = 360
    const val MAP_W = 30
    const val MAP_H = 30
    const val TEX_SIZE = 64

    const val MOVE_SPEED = 3.5
    const val ROT_SPEED = 2.0
    const val LOOK_SENS = 0.0025
    const val PLANE_LEN = 0.66
    const val INTERACT_D = 1.4

    const val WALL_DOOR = 7
    const val WALL_LOCKED = 8
    const val WALL_PINK = 9

    const val SPR_SOLDIER = 100
    const val SPR_HEAVY = 101
    const val SPR_DRONE = 102
    const val SPR_TURRET = 103
    const val SPR_BOSS = 104
    const val SPR_HEALTH = 200
    const val SPR_ARMOR = 201
    const val SPR_AMMO = 202
    const val SPR_KEY_G = 203
    const val SPR_KEY_P = 204
    const val SPR_BARREL = 300
    const val SPR_CRATE = 301
    const val SPR_SIGN = 302
    const val SPR_TORCH = 303
    const val SPR_BANNER = 304
}

class FpsWeapon(
    val name: String, val damage: Double, val rate: Double, val spread: Double,
    val ammoType: Int, val ammoPerShot: Int, val projectiles: Int
)

val FPS_WEAPONS = listOf(
    FpsWeapon("PISTOL", 12.0, 0.40, 0.02, 0, 1, 1),
    FpsWeapon("SHOTGUN", 8.0, 0.85, 0.12, 1, 1, 7),
    FpsWeapon("PLASMA", 18.0, 0.22, 0.01, 2, 1, 1),
    FpsWeapon("ROCKET", 60.0, 1.10, 0.00, 3, 1, 1),
    FpsWeapon("RAILGUN", 80.0, 1.50, 0.00, 4, 1, 1),
)

val FPS_WEAPON_KEYS = listOf("pistol", "shotgun", "plasma", "rocket", "railgun")

// Input snapshot filled by the scene each frame.
class FpsInput {
    var w = false; var s = false; var a = false; var d = false
    var left = false; var right = false
    var fire = false; var interact = false
    var weapon1 = false; var weapon2 = false; var weapon3 = false; var weapon4 = false; var weapon5 = false
    var lookDX = 0.0
    var escape = false
}

class FpsDoor {
    var open = 0.0
    var opening = false
    var locked = false
    var keyType: String? = null
}

class FpsEntitySpawn(val type: Int, val x: Double, val y: Double)

class FpsLevel {
    companion object {
        val rawMap: Array<IntArray> = arrayOf(
            intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
            intArrayOf(1,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,1),
            intArrayOf(1,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,1),
            intArrayOf(1,0,0,0,0,0,0,0,7,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,1),
            intArrayOf(1,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,1),
            intArrayOf(1,1,1,7,1,1,1,1,1,1,1,1,2,2,2,2,1,0,0,0,0,0,0,0,0,0,0,0,0,1),
            intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,2,0,0,0,1,0,0,0,3,3,3,3,3,3,0,0,0,1),
            intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,2,0,0,0,7,0,0,0,3,0,0,0,0,3,0,0,0,1),
            intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,2,0,0,0,1,0,0,0,3,0,0,0,0,3,0,0,0,1),
            intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,2,2,2,7,1,0,0,0,3,3,7,3,3,3,0,0,0,1),
            intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
            intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
            intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
            intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,8,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
            intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
            intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
            intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
            intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
            intArrayOf(1,0,0,0,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,0,0,0,1),
            intArrayOf(1,0,0,0,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,4,0,0,0,1),
            intArrayOf(1,0,0,0,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,4,0,0,0,1),
            intArrayOf(1,0,0,0,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,4,0,0,0,1),
            intArrayOf(1,0,0,0,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,4,0,0,0,1),
            intArrayOf(1,0,0,0,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,4,0,0,0,1),
            intArrayOf(1,0,0,0,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,4,0,0,0,1),
            intArrayOf(1,0,0,0,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,0,0,0,1),
            intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
            intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
            intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        )

        val entitySpawns = listOf(
            FpsEntitySpawn(FpsK.SPR_SOLDIER, 11.5, 3.5),
            FpsEntitySpawn(FpsK.SPR_SOLDIER, 13.5, 7.5),
            FpsEntitySpawn(FpsK.SPR_HEAVY, 23.5, 7.5),
            FpsEntitySpawn(FpsK.SPR_DRONE, 9.5, 10.5),
            FpsEntitySpawn(FpsK.SPR_SOLDIER, 20.5, 11.5),
            FpsEntitySpawn(FpsK.SPR_HEAVY, 16.5, 16.5),
            FpsEntitySpawn(FpsK.SPR_SOLDIER, 8.5, 17.5),
            FpsEntitySpawn(FpsK.SPR_BOSS, 15.5, 22.5),
            FpsEntitySpawn(FpsK.SPR_HEALTH, 4.5, 1.5),
            FpsEntitySpawn(FpsK.SPR_AMMO, 3.5, 5.5),
            FpsEntitySpawn(FpsK.SPR_HEALTH, 14.5, 7.5),
            FpsEntitySpawn(FpsK.SPR_ARMOR, 23.5, 8.5),
            FpsEntitySpawn(FpsK.SPR_AMMO, 10.5, 11.5),
            FpsEntitySpawn(FpsK.SPR_HEALTH, 15.5, 16.5),
            FpsEntitySpawn(FpsK.SPR_AMMO, 8.5, 15.5),
            FpsEntitySpawn(FpsK.SPR_KEY_G, 24.5, 6.5),
            FpsEntitySpawn(FpsK.SPR_BARREL, 1.5, 1.5),
            FpsEntitySpawn(FpsK.SPR_BARREL, 7.5, 1.5),
            FpsEntitySpawn(FpsK.SPR_TORCH, 8.5, 6.5),
            FpsEntitySpawn(FpsK.SPR_CRATE, 10.5, 7.5),
            FpsEntitySpawn(FpsK.SPR_SIGN, 2.5, 10.5),
            FpsEntitySpawn(FpsK.SPR_TORCH, 16.5, 13.5),
            FpsEntitySpawn(FpsK.SPR_BANNER, 15.5, 20.5),
            FpsEntitySpawn(FpsK.SPR_BARREL, 5.5, 20.5),
            FpsEntitySpawn(FpsK.SPR_BARREL, 24.5, 20.5),
            FpsEntitySpawn(FpsK.SPR_CRATE, 7.5, 22.5),
        )
    }

    private val map: Array<IntArray> = Array(rawMap.size) { rawMap[it].copyOf() }
    val doors = HashMap<Int, FpsDoor>()

    init {
        for (y in map.indices) {
            for (x in map[y].indices) {
                val t = map[y][x]
                if (t == FpsK.WALL_DOOR || t == FpsK.WALL_LOCKED || t == FpsK.WALL_PINK) {
                    val d = FpsDoor()
                    d.locked = t != FpsK.WALL_DOOR
                    d.keyType = if (t == FpsK.WALL_LOCKED) "gold" else if (t == FpsK.WALL_PINK) "pink" else null
                    doors[y * FpsK.MAP_W + x] = d
                }
            }
        }
    }

    fun getCell(x: Double, y: Double): Int {
        val ix = floor(x).toInt(); val iy = floor(y).toInt()
        if (iy < 0 || iy >= map.size || ix < 0 || ix >= map[iy].size) return 1
        return map[iy][ix]
    }

    fun getDoor(x: Double, y: Double): FpsDoor? =
        doors[floor(y).toInt() * FpsK.MAP_W + floor(x).toInt()]

    fun isSolid(x: Double, y: Double): Boolean {
        val t = getCell(x, y)
        if (t == 0) return false
        if (t == FpsK.WALL_DOOR || t == FpsK.WALL_LOCKED || t == FpsK.WALL_PINK) {
            val d = getDoor(x, y) ?: return true
            return d.open < 0.85
        }
        return true
    }

    fun tryOpenDoor(x: Double, y: Double, hasGold: Boolean, hasPink: Boolean): Boolean {
        val d = getDoor(x, y) ?: return false
        if (d.locked) {
            if (d.keyType == "gold" && !hasGold) return false
            if (d.keyType == "pink" && !hasPink) return false
        }
        d.opening = true
        return true
    }

    fun update(dt: Double) {
        for (d in doors.values) {
            if (d.opening && d.open < 1) d.open = minOf(1.0, d.open + dt * 1.4)
        }
    }
}

class FpsPlayer {
    var x = 2.5; var y = 2.5
    var dirX = 1.0; var dirY = 0.0
    var planeX = 0.0; var planeY = FpsK.PLANE_LEN

    var health = 100.0; var maxHealth = 100.0
    var armor = 0.0; var maxArmor = 100.0
    var ammo = intArrayOf(100, 20, 50, 5, 10)
    var weapon = 0
    val hasKey = HashMap<String, Boolean>()

    var score = 0
    var kills = 0

    var bobPhase = 0.0
    var flashAlpha = 0.0
    var weaponKick = 0.0

    var fireCooldown = 0.0
    var interactCooldown = 0.0

    var dead = false
    var deadTimer = 0.0

    private fun tryMove(level: FpsLevel, nx: Double, ny: Double) {
        if (!level.isSolid(nx, y)) x = nx
        if (!level.isSolid(x, ny)) y = ny
    }

    fun update(dt: Double, input: FpsInput, level: FpsLevel) {
        if (dead) { deadTimer += dt; return }

        if (input.left || input.right) rotate((if (input.right) 1 else -1) * FpsK.ROT_SPEED * dt)
        if (input.lookDX != 0.0) rotate(input.lookDX * FpsK.LOOK_SENS)

        val speed = FpsK.MOVE_SPEED * dt
        val strafe = speed * 0.8
        var moving = false
        if (input.w) { tryMove(level, x + dirX * speed, y + dirY * speed); moving = true }
        if (input.s) { tryMove(level, x - dirX * speed, y - dirY * speed); moving = true }
        if (input.a) { tryMove(level, x + dirY * strafe, y - dirX * strafe); moving = true }
        if (input.d) { tryMove(level, x - dirY * strafe, y + dirX * strafe); moving = true }

        if (moving) bobPhase += dt * 6 else bobPhase *= 0.85

        fireCooldown = maxOf(0.0, fireCooldown - dt)
        interactCooldown = maxOf(0.0, interactCooldown - dt)
        flashAlpha = maxOf(0.0, flashAlpha - dt * 3)
        weaponKick = maxOf(0.0, weaponKick - dt * 8)

        if (input.weapon1) weapon = 0
        if (input.weapon2) weapon = 1
        if (input.weapon3) weapon = 2
        if (input.weapon4) weapon = 3
        if (input.weapon5) weapon = 4

        if (input.interact && interactCooldown <= 0) {
            interactCooldown = 0.4
            var step = 0.5
            while (step <= FpsK.INTERACT_D) {
                val cx = x + dirX * step; val cy = y + dirY * step
                val cell = level.getCell(cx, cy)
                if (cell == 7 || cell == 8 || cell == 9) {
                    level.tryOpenDoor(cx, cy, hasKey["gold"] == true, hasKey["pink"] == true)
                    break
                }
                step += 0.5
            }
        }
    }

    fun rotate(angle: Double) {
        val c = cos(angle); val s = sin(angle)
        val ox = dirX; val oy = dirY
        dirX = ox * c - oy * s
        dirY = ox * s + oy * c
        val px = planeX; val py = planeY
        planeX = px * c - py * s
        planeY = px * s + py * c
    }

    fun takeDamage(dmg: Double) {
        if (dead) return
        val absorb = minOf(armor, floor(dmg * 0.33))
        armor -= absorb
        health -= (dmg - absorb)
        flashAlpha = minOf(1.0, flashAlpha + 0.4)
        if (health <= 0) { health = 0.0; dead = true }
    }

    fun canFire(): Boolean {
        val wpn = FPS_WEAPONS[weapon]
        return fireCooldown <= 0 && ammo[wpn.ammoType] >= wpn.ammoPerShot
    }

    fun consumeAmmo() {
        val wpn = FPS_WEAPONS[weapon]
        ammo[wpn.ammoType] = maxOf(0, ammo[wpn.ammoType] - wpn.ammoPerShot)
        fireCooldown = wpn.rate
        weaponKick = 1.0
    }
}

class FpsEnemyDef(
    val hp: Double, val speed: Double, val damage: Double, val fireRate: Double,
    val score: Int, val sightRange: Double
)

val FPS_ENEMY_DEFS = mapOf(
    FpsK.SPR_SOLDIER to FpsEnemyDef(40.0, 2.0, 8.0, 1.5, 100, 12.0),
    FpsK.SPR_HEAVY to FpsEnemyDef(100.0, 1.0, 15.0, 2.0, 300, 10.0),
    FpsK.SPR_DRONE to FpsEnemyDef(25.0, 3.0, 5.0, 0.8, 150, 14.0),
    FpsK.SPR_TURRET to FpsEnemyDef(60.0, 0.0, 12.0, 1.2, 200, 12.0),
    FpsK.SPR_BOSS to FpsEnemyDef(600.0, 1.2, 25.0, 0.7, 2000, 20.0),
)

class FpsEnemy(val type: Int, var x: Double, var y: Double) {
    val def: FpsEnemyDef = FPS_ENEMY_DEFS[type] ?: FPS_ENEMY_DEFS[FpsK.SPR_SOLDIER]!!
    var hp = def.hp
    var angle = 0.0

    enum class State { IDLE, PATROL, CHASE, ATTACK, DEAD }
    var state = State.IDLE
    var fireCooldown = 0.0
    var idleTimer = 0.0
    var patrolAngle = Random.nextDouble(0.0, 2 * Math.PI)
    var patrolTimer = 0.0
    var animFrame = 0
    var animTimer = 0.0
    var deathTimer = 0.0
    var hitFlash = 0.0
    var dead = false

    private fun hasLineOfSight(p: FpsPlayer, level: FpsLevel): Boolean {
        val dx = p.x - x; val dy = p.y - y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist > def.sightRange) return false
        val steps = Math.ceil(dist * 4).toInt()
        var i = 1
        while (i < steps) {
            val t = i.toDouble() / steps
            if (level.isSolid(x + dx * t, y + dy * t)) return false
            i++
        }
        return true
    }

    fun update(dt: Double, p: FpsPlayer, level: FpsLevel, onDamage: () -> Unit) {
        if (dead) { deathTimer += dt; return }
        fireCooldown = maxOf(0.0, fireCooldown - dt)
        hitFlash = maxOf(0.0, hitFlash - dt * 4)
        animTimer += dt
        if (animTimer > 0.18) { animFrame = (animFrame + 1) % 4; animTimer = 0.0 }

        val canSee = hasLineOfSight(p, level)
        when (state) {
            State.IDLE -> {
                idleTimer += dt
                if (canSee) { state = State.CHASE; return }
                if (idleTimer > 2.0) { state = State.PATROL; patrolTimer = 3 + Random.nextDouble(0.0, 3.0) }
            }
            State.PATROL -> {
                if (canSee) { state = State.CHASE; return }
                patrolTimer -= dt
                if (patrolTimer <= 0) { state = State.IDLE; idleTimer = 0.0; return }
                if (def.speed > 0) {
                    val nx = x + cos(patrolAngle) * def.speed * dt
                    val ny = y + sin(patrolAngle) * def.speed * dt
                    if (!level.isSolid(nx, ny)) { x = nx; y = ny }
                    else patrolAngle += Math.PI * (0.5 + Random.nextDouble(0.0, 0.5))
                }
            }
            State.CHASE -> {
                if (!canSee) { state = State.PATROL; patrolTimer = 4.0; return }
                val dx = p.x - x; val dy = p.y - y
                val dist = sqrt(dx * dx + dy * dy)
                angle = Math.atan2(dy, dx)
                val attackDist = if (type == FpsK.SPR_BOSS) 4.0 else 2.5
                if (dist < attackDist) state = State.ATTACK
                else if (def.speed > 0) {
                    val sp = def.speed * dt
                    val nx = x + (dx / dist) * sp; val ny = y + (dy / dist) * sp
                    if (!level.isSolid(nx, y)) x = nx
                    if (!level.isSolid(x, ny)) y = ny
                }
            }
            State.ATTACK -> {
                if (!canSee) { state = State.CHASE; return }
                val dx = p.x - x; val dy = p.y - y
                val dist = sqrt(dx * dx + dy * dy)
                angle = Math.atan2(dy, dx)
                val attackDist = if (type == FpsK.SPR_BOSS) 5.0 else 3.0
                if (dist > attackDist + 1) { state = State.CHASE; return }
                if (fireCooldown <= 0) { fireCooldown = def.fireRate; onDamage() }
            }
            State.DEAD -> {}
        }
    }

    fun hit(dmg: Double) {
        if (dead) return
        hp -= dmg
        hitFlash = 1.0
        if (state == State.IDLE || state == State.PATROL) state = State.CHASE
        if (hp <= 0) { hp = 0.0; dead = true; state = State.DEAD }
    }
}

class FpsPickup(val type: Int, val x: Double, val y: Double) {
    var collected = false

    fun tryCollect(p: FpsPlayer): Boolean {
        if (collected) return false
        val dx = x - p.x; val dy = y - p.y
        if (dx * dx + dy * dy > 0.6 * 0.6) return false
        when (type) {
            FpsK.SPR_HEALTH -> {
                if (p.health >= p.maxHealth) return false
                p.health = minOf(p.maxHealth, p.health + 25)
            }
            FpsK.SPR_ARMOR -> {
                if (p.armor >= p.maxArmor) return false
                p.armor = minOf(p.maxArmor, p.armor + 50)
            }
            FpsK.SPR_AMMO -> {
                p.ammo[0] = minOf(300, p.ammo[0] + 50)
                p.ammo[1] = minOf(50, p.ammo[1] + 10)
                p.ammo[2] = minOf(150, p.ammo[2] + 30)
                p.ammo[3] = minOf(20, p.ammo[3] + 3)
                p.ammo[4] = minOf(30, p.ammo[4] + 5)
            }
            FpsK.SPR_KEY_G -> p.hasKey["gold"] = true
            FpsK.SPR_KEY_P -> p.hasKey["pink"] = true
            else -> return false
        }
        collected = true
        return true
    }
}

fun fpsIsEnemy(t: Int) = t in 100..199
fun fpsIsPickup(t: Int) = t in 200..299
fun fpsIsProp(t: Int) = t >= 300

fun fpsSpriteAssetKey(t: Int): String = when (t) {
    FpsK.SPR_HEALTH -> "health"
    FpsK.SPR_ARMOR -> "armor"
    FpsK.SPR_AMMO -> "ammo"
    FpsK.SPR_KEY_G -> "key_g"
    FpsK.SPR_KEY_P -> "key_p"
    FpsK.SPR_BARREL -> "barrel"
    FpsK.SPR_CRATE -> "crate"
    FpsK.SPR_SIGN -> "sign"
    FpsK.SPR_TORCH -> "torch"
    FpsK.SPR_BANNER -> "banner"
    else -> "barrel"
}

fun fpsEnemyKey(t: Int): String = when (t) {
    100 -> "soldier"; 101 -> "heavy"; 102 -> "drone"; 103 -> "turret"; 104 -> "boss"
    else -> "soldier"
}
