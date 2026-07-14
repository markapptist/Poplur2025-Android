package com.apptists.poplur2025.ui.components

import android.content.Context
import com.apptists.poplur2025.data.Secrets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Funny "what's loading" lines for the launch splash.
 *
 * Shows a curated pool instantly (offline, no launch delay) and refreshes the pool from
 * Claude in the background so the next launch gets fresh AI-written lines. Mirrors iOS.
 */
object SplashLoadingLines {

    val pool: List<String> = listOf(
        "Warming up the pinetrees", "Rolling out the clay roads", "Inflating the palm trees",
        "Teaching the pin to drop", "Buffing the city lights", "Sprinkling pink stars",
        "Kneading the little mountains", "Waking up the tugboat", "Charging the flip phone",
        "Tuning the clay TV", "Squishing fresh hearts", "Painting the dotted lines",
        "Polishing the big P", "Fluffing the clouds", "Wiring the tiny streetlamps",
        "Planting roadside bushes", "Smoothing the ocean", "Stacking the skyscrapers",
        "Greasing the map roads", "Feeding the stars some glitter", "Rounding the mountain edges",
        "Tucking rivers into valleys", "Handing out heart pins", "Booting the clay servers",
        "Shaping the coastline", "Coloring in the grass", "Warming the neon signs",
        "Shooing the thunderclouds away", "Counting nearby posts", "Measuring your radius",
        "Dusting off the globe", "Pinning the hotspots", "Untangling the roads",
        "Cranking the pink to eleven", "Baking a fresh batch of pins", "Aligning the constellations",
        "Reticulating clay splines", "Molding the mountains", "Whisking the clouds",
        "Rounding up the local legends", "Sanding the sidewalks", "Topping off the pink paint",
    )

    private const val PREFS = "poplur_settings"
    private const val CACHE_KEY = "poplur_splash_lines_ai"

    /** Cached AI lines (if any) merged with the pool, shuffled — different each launch. */
    fun sessionLines(context: Context): List<String> {
        val cached = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(CACHE_KEY, null)
        val ai = cached?.let {
            runCatching {
                val arr = JSONArray(it)
                (0 until arr.length()).map { i -> arr.getString(i) }
            }.getOrNull()
        }.orEmpty()
        return (ai + pool).distinct().shuffled()
    }

    fun destinationLine(loggedIn: Boolean): String =
        if (loggedIn) "Opening your feed" else "Rolling out the welcome mat"

    /** Fire-and-forget: fetch fresh lines from Claude and cache for next launch. No-ops on failure. */
    fun refreshFromClaudeInBackground(context: Context) {
        val key = Secrets.claudeApiKey
        if (key.isBlank()) return
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val prompt = """
                    Poplur is a playful social-map app with a claymation look: a pink clay world with roads,
                    a map pin, palm trees, a little city, a tugboat, hearts, a retro TV and flip phone, and pink stars.
                    Write 14 very short, funny app "loading" status lines in that world's voice, like
                    "Warming up the pinetrees" or "Rolling out the clay roads".
                    Each 2-5 words, present-continuous, whimsical, no ending punctuation, no emoji, no quotes.
                    Return ONLY a JSON array of strings.
                """.trimIndent()

                val body = JSONObject()
                    .put("model", "claude-haiku-4-5-20251001")
                    .put("max_tokens", 400)
                    .put(
                        "messages",
                        JSONArray().put(JSONObject().put("role", "user").put("content", prompt))
                    ).toString()

                val conn = (URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 8_000
                    readTimeout = 12_000
                    setRequestProperty("x-api-key", key)
                    setRequestProperty("anthropic-version", "2023-06-01")
                    setRequestProperty("content-type", "application/json")
                }
                conn.outputStream.use { it.write(body.toByteArray()) }

                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                    val content = JSONObject(text).getJSONArray("content").getJSONObject(0).getString("text")
                    val start = content.indexOf('[')
                    val end = content.lastIndexOf(']')
                    if (start in 0 until end) {
                        val arr = JSONArray(content.substring(start, end + 1))
                        val cleaned = (0 until arr.length())
                            .map { arr.getString(it).trim() }
                            .filter { it.isNotEmpty() && it.length <= 34 }
                        if (cleaned.size >= 6) {
                            appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                                .putString(CACHE_KEY, JSONArray(cleaned.take(20)).toString())
                                .apply()
                        }
                    }
                }
                conn.disconnect()
            }
        }
    }
}
