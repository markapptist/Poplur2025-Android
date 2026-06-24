package com.apptists.poplur2025.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// MARK: - Data model

data class TVStyleMetadata(
    val channel: String,       // food | fitness | sports | beauty | fashion | music | tech | news | city | entertainment
    val tvFormat: String,      // commercial | highlight_reel | cooking_show | documentary | talk_show | music_video | news_segment
    val adConcept: String,     // e.g. "protein supplement commercial"
    val caption: String,       // punchy ALL-CAPS headline, ≤5 words
    val tagline: String,       // one motivational brand sentence
    val broadcastName: String  // e.g. "POPLUR FITNESS"
)

// MARK: - Errors

sealed class TVAnalyzerError(message: String) : Exception(message) {
    object NoAPIKey : TVAnalyzerError("No Claude API key configured")
    object ParseError : TVAnalyzerError("Failed to parse Claude response as TVStyleMetadata")
}

// MARK: - Analyzer

object TVStyleAnalyzer {

    private const val ENDPOINT = "https://api.anthropic.com/v1/messages"
    private const val MODEL = "claude-haiku-4-5-20251001"
    private const val MAX_DIMENSION = 512

    suspend fun analyze(context: Context, uri: Uri, isVideo: Boolean): TVStyleMetadata =
        withContext(Dispatchers.IO) {
            val bitmap = if (isVideo) firstFrame(context, uri) else decodeBitmap(context, uri)
            val jpeg = resized(bitmap).toJpegBase64()
            callClaude(jpeg)
        }

    // MARK: - Private helpers

    private fun firstFrame(context: Context, uri: Uri): Bitmap {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: BitmapFactory.decodeResource(context.resources, android.R.drawable.ic_menu_camera)
        } finally {
            retriever.release()
        }
    }

    private fun decodeBitmap(context: Context, uri: Uri): Bitmap {
        return context.contentResolver.openInputStream(uri).use { stream ->
            BitmapFactory.decodeStream(stream)
                ?: BitmapFactory.decodeResource(context.resources, android.R.drawable.ic_menu_camera)
        }
    }

    private fun resized(bitmap: Bitmap): Bitmap {
        val w = bitmap.width; val h = bitmap.height
        val max = maxOf(w, h)
        if (max <= MAX_DIMENSION) return bitmap
        val scale = MAX_DIMENSION.toFloat() / max
        return Bitmap.createScaledBitmap(bitmap, (w * scale).toInt(), (h * scale).toInt(), true)
    }

    private fun Bitmap.toJpegBase64(): String {
        val out = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 72, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun callClaude(imageBase64: String): TVStyleMetadata {
        val key = Secrets.claudeApiKey
        if (key.isEmpty()) throw TVAnalyzerError.NoAPIKey

        val prompt = """
You are a TV broadcast classifier for Poplur TV, a social platform where every post is a TV broadcast.
Analyze this image and respond ONLY with a JSON object — no extra text, no markdown:
{"channel":"<food|fitness|sports|beauty|fashion|music|tech|news|city|entertainment>","tvFormat":"<commercial|highlight_reel|cooking_show|documentary|talk_show|music_video|news_segment>","adConcept":"<what this would be as a TV ad or show in under 8 words>","caption":"<PUNCHY ALL-CAPS HEADLINE MAX 5 WORDS>","tagline":"<one compelling motivational sentence>","broadcastName":"<POPLUR CHANNEL e.g. POPLUR FITNESS>"}
""".trimIndent()

        val body = JSONObject().apply {
            put("model", MODEL)
            put("max_tokens", 300)
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "image")
                        put("source", JSONObject().apply {
                            put("type", "base64")
                            put("media_type", "image/jpeg")
                            put("data", imageBase64)
                        })
                    })
                    put(JSONObject().apply {
                        put("type", "text")
                        put("text", prompt)
                    })
                })
            }))
        }

        val url = URL(ENDPOINT)
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("x-api-key", key)
            conn.setRequestProperty("anthropic-version", "2023-06-01")
            conn.setRequestProperty("content-type", "application/json")
            conn.connectTimeout = 20_000
            conn.readTimeout = 20_000
            conn.doOutput = true

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }

            val response = conn.inputStream.bufferedReader().readText()
            val outer = JSONObject(response)
            val text = outer.getJSONArray("content").getJSONObject(0).getString("text").trim()
            val json = JSONObject(text)
            return TVStyleMetadata(
                channel       = json.optString("channel", "entertainment"),
                tvFormat      = json.optString("tvFormat", "commercial"),
                adConcept     = json.optString("adConcept", ""),
                caption       = json.optString("caption", ""),
                tagline       = json.optString("tagline", ""),
                broadcastName = json.optString("broadcastName", "POPLUR TV")
            )
        } catch (e: Exception) {
            throw TVAnalyzerError.ParseError
        } finally {
            conn.disconnect()
        }
    }
}
