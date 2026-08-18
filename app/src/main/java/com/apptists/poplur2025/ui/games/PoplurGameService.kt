package com.apptists.poplur2025.ui.games

import android.util.Log
import com.apptists.poplur2025.data.PoplurAIBackendClient
import org.json.JSONObject

object PoplurGameService {

    private const val TAG = "PoplurGameService"

    /** Generate a game from the player's own description (Game Maker). */
    suspend fun generateUserGame(userPrompt: String): PoplurGameConfig {
        val trimmed = userPrompt.trim()
        return request(mode = "maker", prompt = trimmed.ifEmpty { null })
    }

    suspend fun generateConfig(userPrompt: String = ""): PoplurGameConfig =
        request(mode = "arcade", prompt = null)

    // The system prompts and Claude call live on the backend (POST /games/config);
    // the device only names the mode and forwards the player's idea.
    private suspend fun request(mode: String, prompt: String?): PoplurGameConfig =
        try {
            parseConfig(PoplurAIBackendClient().gameConfig(mode, prompt).toString())
        } catch (e: Exception) {
            Log.e(TAG, "generateConfig error: ${e.message}")
            PoplurGameConfig.Fallback
        }

    private fun parseConfig(json: String): PoplurGameConfig {
        return try {
            val obj = JSONObject(json)
            val gameKey = obj.optString("game", "pop_runner")
            val gameType = PoplurGameType.entries.firstOrNull { it.key == gameKey }
                ?: PoplurGameType.SideScroller

            val powerUpsArray = obj.optJSONArray("powerUps")
            val powerUps = buildList {
                if (powerUpsArray != null) {
                    for (i in 0 until powerUpsArray.length()) add(powerUpsArray.getString(i))
                }
                while (size < 2) add("star")
            }.take(2)

            PoplurGameConfig(
                game = gameType,
                character = obj.optString("character", "pop"),
                gameName = obj.optString("gameName", "Poplur Run"),
                theme = obj.optString("theme", "Neon City"),
                skyColor = obj.optString("skyColor", "#1A1A2E"),
                groundColor = obj.optString("groundColor", "#16213E"),
                accentColor = obj.optString("accentColor", "#E94560"),
                difficulty = obj.optString("difficulty", "medium"),
                speed = obj.optDouble("speed", 280.0).toFloat().coerceIn(200f, 400f),
                powerUps = powerUps,
                enemyType = obj.optString("enemyType", "robo_blob"),
                specialMechanic = obj.optString("specialMechanic", "double_jump")
            )
        } catch (e: Exception) {
            Log.e(TAG, "parseConfig error: ${e.message}")
            PoplurGameConfig.Fallback
        }
    }
}
