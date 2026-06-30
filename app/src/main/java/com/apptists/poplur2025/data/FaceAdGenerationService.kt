package com.apptists.poplur2025.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

// Face-preserving AI generation via Replicate.
// Image: zsxkib/instant-id  — styled portrait that looks like the user.
// Video: wan-ai/wan2.1-i2v-480p — animates the face image into a video.
object FaceAdGenerationService {

    suspend fun generateImage(faceBitmap: Bitmap, prompt: String): Bitmap = withContext(Dispatchers.IO) {
        val key = apiKey()
        val dataUri = encode(faceBitmap)
        val body = JSONObject().apply {
            put("input", JSONObject().apply {
                put("image", dataUri)
                put("prompt", "$prompt, cinematic TV advertisement, broadcast quality, vivid professional lighting")
                put("negative_prompt", "low quality, blurry, bad anatomy, multiple people, text, watermark, cartoon")
                put("style", "None")
                put("guidance_scale", 7.5)
                put("num_inference_steps", 30)
                put("disable_safety_checker", true)
            })
        }
        val id = start("https://api.replicate.com/v1/models/fofr/face-to-many/predictions", body, key)
        val url = poll(id, key)
        download(url)
    }

    suspend fun generateVideo(faceBitmap: Bitmap, prompt: String): String = withContext(Dispatchers.IO) {
        val key = apiKey()
        val dataUri = encode(faceBitmap)
        val body = JSONObject().apply {
            put("input", JSONObject().apply {
                put("image", dataUri)
                put("prompt", prompt)
                put("num_frames", 81)
                put("size", "480*832")
            })
        }
        val id = start("https://api.replicate.com/v1/models/wan-ai/wan2.1-i2v-480p/predictions", body, key)
        poll(id, key)
    }

    // MARK: - Private helpers

    private fun apiKey(): String =
        Secrets.replicateApiKey.ifBlank { throw FaceAdError.NoAPIKey }

    private fun encode(bitmap: Bitmap): String {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 82, out)
        val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        return "data:image/jpeg;base64,$b64"
    }

    private fun start(endpoint: String, body: JSONObject, key: String): String {
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
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
                Log.w("FaceAdGeneration", "start failed $code: ${conn.errorBody()}")
                throw FaceAdError.GenerationFailed
            }
            val response = conn.inputStream.use { it.readBytes() }.toString(Charsets.UTF_8)
            return JSONObject(response).getString("id")
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
                    Log.w("FaceAdGeneration", "poll failed ${conn.responseCode}: ${conn.errorBody()}")
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
                            out.optString("image").takeIf { it.isNotBlank() }?.let { return it }
                            out.optString("url").takeIf { it.isNotBlank() }?.let { return it }
                        }
                        throw FaceAdError.InvalidResponse
                    }
                    "failed", "canceled" -> {
                        Log.w("FaceAdGeneration", "generation failed: ${json.opt("error")}")
                        throw FaceAdError.GenerationFailed
                    }
                }
            } finally {
                conn.disconnect()
            }
        }
        throw FaceAdError.Timeout
    }

    private fun download(url: String): Bitmap {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000; readTimeout = 60_000
        }
        if (conn.responseCode !in 200..299) {
            Log.w("FaceAdGeneration", "download failed ${conn.responseCode}: ${conn.errorBody()}")
            conn.disconnect()
            throw FaceAdError.DownloadFailed
        }
        val data = conn.inputStream.use { it.readBytes() }
        conn.disconnect()
        return BitmapFactory.decodeByteArray(data, 0, data.size) ?: throw FaceAdError.DownloadFailed
    }

    private fun HttpURLConnection.errorBody(): String =
        try {
            errorStream?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
        } catch (_: Exception) {
            ""
        }
}

sealed class FaceAdError(msg: String) : Exception(msg) {
    object NoAPIKey       : FaceAdError("Replicate API key missing")
    object InvalidResponse: FaceAdError("Unexpected API response")
    object GenerationFailed: FaceAdError("AI generation failed")
    object DownloadFailed : FaceAdError("Could not download result")
    object Timeout        : FaceAdError("Generation timed out after 4.5 min")
}
