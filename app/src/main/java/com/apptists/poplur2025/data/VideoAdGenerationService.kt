package com.apptists.poplur2025.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object VideoAdGenerationService {
    suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        val key = Secrets.replicateApiKey.ifBlank { throw VideoAdError.NoAPIKey }
        val body = JSONObject().apply {
            put("input", JSONObject().apply {
                put("prompt", prompt)
                put("num_frames", 81)
                put("size", "480*832")
            })
        }
        val id = start(body, key)
        poll(id, key)
    }

    private fun start(body: JSONObject, key: String): String {
        val conn = URL("https://api.replicate.com/v1/models/wan-ai/wan2.1-t2v-480p/predictions")
            .openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer $key")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 30_000
        conn.outputStream.bufferedWriter().use { it.write(body.toString()) }
        val data = conn.inputStream.readBytes()
        val json = JSONObject(String(data))
        return json.getString("id")
    }

    private fun poll(id: String, key: String): String {
        val conn by lazy {
            URL("https://api.replicate.com/v1/predictions/$id").openConnection() as HttpURLConnection
        }
        repeat(90) {
            Thread.sleep(3_000)
            val c = URL("https://api.replicate.com/v1/predictions/$id").openConnection() as HttpURLConnection
            c.setRequestProperty("Authorization", "Bearer $key")
            val json = JSONObject(String(c.inputStream.readBytes()))
            when (json.optString("status")) {
                "succeeded" -> {
                    val out = json.opt("output")
                    return when {
                        out is org.json.JSONArray -> out.getString(0)
                        out is String -> out
                        else -> throw VideoAdError.InvalidResponse
                    }
                }
                "failed", "canceled" -> throw VideoAdError.GenerationFailed
            }
        }
        throw VideoAdError.Timeout
    }
}

sealed class VideoAdError(message: String) : Exception(message) {
    object NoAPIKey       : VideoAdError("Replicate API key missing")
    object InvalidResponse : VideoAdError("Unexpected API response")
    object GenerationFailed : VideoAdError("Video generation failed")
    object Timeout        : VideoAdError("Generation timed out")
}
