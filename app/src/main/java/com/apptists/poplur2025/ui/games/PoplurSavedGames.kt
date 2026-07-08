package com.apptists.poplur2025.ui.games

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// User-created AI games (saved list + map placement). Mirrors iOS PoplurSavedGames.

data class PoplurSavedGame(
    val id: String,
    val config: PoplurGameConfig,
    val latitude: Double?,
    val longitude: Double?,
    val createdAt: Long
) {
    val isPlaced: Boolean get() = latitude != null && longitude != null
}

object PoplurSavedGamesStore {
    private const val PREFS = "poplur_saved_games"
    private const val KEY = "games_v1"

    /** All AI-made games, newest first. Compose-observable. */
    val games = mutableStateListOf<PoplurSavedGame>()
    /** When non-null the map is waiting for a tap to place this game. */
    var pendingPlacement by mutableStateOf<PoplurSavedGame?>(null)
    /** When non-null a placed game's pin was tapped and should be launched full-screen. */
    var launchRequest by mutableStateOf<PoplurGameConfig?>(null)

    private var appContext: Context? = null

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        load()
    }

    val placedGames: List<PoplurSavedGame> get() = games.filter { it.isPlaced }

    fun add(config: PoplurGameConfig): PoplurSavedGame {
        val g = PoplurSavedGame(UUID.randomUUID().toString(), config, null, null, System.currentTimeMillis())
        games.add(0, g)
        save()
        return g
    }

    fun remove(game: PoplurSavedGame) {
        games.removeAll { it.id == game.id }
        if (pendingPlacement?.id == game.id) pendingPlacement = null
        save()
    }

    fun place(game: PoplurSavedGame, latitude: Double, longitude: Double) {
        val i = games.indexOfFirst { it.id == game.id }
        if (i < 0) return
        games[i] = games[i].copy(latitude = latitude, longitude = longitude)
        save()
    }

    fun unplace(game: PoplurSavedGame) {
        val i = games.indexOfFirst { it.id == game.id }
        if (i < 0) return
        games[i] = games[i].copy(latitude = null, longitude = null)
        save()
    }

    // MARK: persistence (org.json → SharedPreferences)

    private fun load() {
        val ctx = appContext ?: return
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return
        try {
            val arr = JSONArray(raw)
            games.clear()
            for (i in 0 until arr.length()) games.add(fromJson(arr.getJSONObject(i)))
        } catch (_: Exception) { }
    }

    private fun save() {
        val ctx = appContext ?: return
        val arr = JSONArray()
        for (g in games) arr.put(toJson(g))
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, arr.toString()).apply()
    }

    private fun toJson(g: PoplurSavedGame): JSONObject {
        val c = g.config
        val cfg = JSONObject().apply {
            put("game", c.game.key)
            put("character", c.character)
            put("gameName", c.gameName)
            put("theme", c.theme)
            put("skyColor", c.skyColor)
            put("groundColor", c.groundColor)
            put("accentColor", c.accentColor)
            put("difficulty", c.difficulty)
            put("speed", c.speed.toDouble())
            put("powerUps", JSONArray(c.powerUps))
            put("enemyType", c.enemyType)
            put("specialMechanic", c.specialMechanic)
        }
        return JSONObject().apply {
            put("id", g.id)
            put("createdAt", g.createdAt)
            g.latitude?.let { put("latitude", it) }
            g.longitude?.let { put("longitude", it) }
            put("config", cfg)
        }
    }

    private fun fromJson(o: JSONObject): PoplurSavedGame {
        val c = o.getJSONObject("config")
        val powerUps = c.optJSONArray("powerUps")?.let { pa ->
            (0 until pa.length()).map { pa.getString(it) }
        } ?: listOf("star", "shield")
        val gameType = PoplurGameType.entries.firstOrNull { it.key == c.optString("game") }
            ?: PoplurGameType.ArcadeShooter
        val config = PoplurGameConfig(
            game = gameType,
            character = c.optString("character", "pop"),
            gameName = c.optString("gameName", "Poplur Game"),
            theme = c.optString("theme", "Neon"),
            skyColor = c.optString("skyColor", "#1A1A2E"),
            groundColor = c.optString("groundColor", "#16213E"),
            accentColor = c.optString("accentColor", "#E94560"),
            difficulty = c.optString("difficulty", "medium"),
            speed = c.optDouble("speed", 280.0).toFloat(),
            powerUps = powerUps,
            enemyType = c.optString("enemyType", "robo_blob"),
            specialMechanic = c.optString("specialMechanic", "double_jump")
        )
        return PoplurSavedGame(
            id = o.optString("id", UUID.randomUUID().toString()),
            config = config,
            latitude = if (o.has("latitude")) o.getDouble("latitude") else null,
            longitude = if (o.has("longitude")) o.getDouble("longitude") else null,
            createdAt = o.optLong("createdAt", System.currentTimeMillis())
        )
    }
}
