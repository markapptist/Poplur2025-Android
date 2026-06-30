package com.apptists.poplur2025.ui.games

import android.util.Log
import com.apptists.poplur2025.data.Secrets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object PoplurGameService {

    private const val TAG = "PoplurGameService"
    private const val ENDPOINT = "https://api.anthropic.com/v1/messages"
    private const val MODEL = "claude-haiku-4-5-20251001"

    private val SYSTEM_PROMPT = """
You are a game configuration generator for Poplur, a social media app with mini-games.
Return ONLY a valid JSON object (no markdown, no explanation, no code block) with exactly these fields:
{
  "game": "stella_blast",
  "character": one of: pop, zyla, blip, rex, stella, glitch,
  "gameName": short catchy game name string,
  "theme": environment theme name (e.g. "Neon City", "Jungle", "Space Station"),
  "skyColor": hex color string for sky background (e.g. "#1A1A2E"),
  "groundColor": hex color string for ground (e.g. "#16213E"),
  "accentColor": hex color string for accents (e.g. "#E94560"),
  "difficulty": one of: easy, medium, hard,
  "speed": number between 200 and 400,
  "powerUps": array of exactly 2 strings (e.g. ["star", "shield"]),
  "enemyType": string describing enemy type,
  "specialMechanic": string describing the special mechanic
}
Generate a polished arcade-shooter session that feels closer to a real console game than a toy mini-game.
Use stella_blast, choose cinematic space/neon colors, and make the mechanics sound skill-based.
""".trimIndent()

    suspend fun generateConfig(userPrompt: String = "Generate a fun random game config"): PoplurGameConfig =
        withContext(Dispatchers.IO) {
            try {
                val key = Secrets.claudeApiKey
                if (key.isEmpty()) return@withContext PoplurGameConfig.Fallback

                val body = JSONObject().apply {
                    put("model", MODEL)
                    put("max_tokens", 256)
                    put("system", SYSTEM_PROMPT)
                    put("messages", JSONArray().put(JSONObject().apply {
                        put("role", "user")
                        put("content", userPrompt)
                    }))
                }

                val url = URL(ENDPOINT)
                val conn = url.openConnection() as HttpURLConnection
                try {
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("x-api-key", key)
                    conn.setRequestProperty("anthropic-version", "2023-06-01")
                    conn.setRequestProperty("content-type", "application/json")
                    conn.connectTimeout = 15_000
                    conn.readTimeout = 15_000
                    conn.doOutput = true
                    OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }

                    if (conn.responseCode != 200) {
                        Log.w(TAG, "API error ${conn.responseCode}")
                        return@withContext PoplurGameConfig.Fallback
                    }

                    val responseText = conn.inputStream.bufferedReader().readText()
                    val content = JSONObject(responseText)
                        .getJSONArray("content")
                        .getJSONObject(0)
                        .getString("text")
                        .trim()

                    parseConfig(content)
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                Log.e(TAG, "generateConfig error: ${e.message}")
                PoplurGameConfig.Fallback
            }
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
