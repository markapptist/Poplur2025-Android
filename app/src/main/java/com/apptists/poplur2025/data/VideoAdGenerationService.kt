package com.apptists.poplur2025.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import org.json.JSONArray
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
        val conn = (URL("https://api.replicate.com/v1/models/wan-ai/wan2.1-t2v-480p/predictions")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $key")
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 30_000
            readTimeout = 60_000
            doOutput = true
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            if (code !in 200..201) {
                Log.w("VideoAdGeneration", "start failed $code: ${conn.errorBody()}")
                throw VideoAdError.GenerationFailed
            }
            val json = JSONObject(conn.inputStream.use { it.readBytes() }.toString(Charsets.UTF_8))
            return json.getString("id")
        } finally {
            conn.disconnect()
        }
    }

    private fun poll(id: String, key: String): String {
        val url = URL("https://api.replicate.com/v1/predictions/$id")
        repeat(90) {
            Thread.sleep(3_000)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("Authorization", "Bearer $key")
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            try {
                if (conn.responseCode != 200) {
                    Log.w("VideoAdGeneration", "poll failed ${conn.responseCode}: ${conn.errorBody()}")
                    return@repeat
                }
                val json = JSONObject(conn.inputStream.use { it.readBytes() }.toString(Charsets.UTF_8))
                when (json.optString("status")) {
                    "succeeded" -> {
                        val out = json.get("output")
                        if (out is JSONArray && out.length() > 0) return out.getString(0)
                        if (out is String) return out
                        if (out is JSONObject) {
                            out.optString("video").takeIf { it.isNotBlank() }?.let { return it }
                            out.optString("url").takeIf { it.isNotBlank() }?.let { return it }
                        }
                        throw VideoAdError.InvalidResponse
                    }
                    "failed", "canceled" -> {
                        Log.w("VideoAdGeneration", "generation failed: ${json.opt("error")}")
                        throw VideoAdError.GenerationFailed
                    }
                }
            } finally {
                conn.disconnect()
            }
        }
        throw VideoAdError.Timeout
    }

    private fun HttpURLConnection.errorBody(): String =
        try {
            errorStream?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
        } catch (_: Exception) {
            ""
        }
}

sealed class VideoAdError(message: String) : Exception(message) {
    object NoAPIKey        : VideoAdError("Replicate API key missing")
    object InvalidResponse : VideoAdError("Unexpected API response")
    object GenerationFailed : VideoAdError("Video generation failed")
    object Timeout         : VideoAdError("Generation timed out")
}
