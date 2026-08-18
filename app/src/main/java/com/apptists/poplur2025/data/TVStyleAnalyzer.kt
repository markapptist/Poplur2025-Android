package com.apptists.poplur2025.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

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
    object ParseError : TVAnalyzerError("Failed to parse Claude response as TVStyleMetadata")
    data class HttpError(val code: Int) : TVAnalyzerError("HTTP $code from Poplur AI backend")
}

// MARK: - Analyzer

object TVStyleAnalyzer {

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

    // The Claude classification runs on Poplur's backend (POST /tv/style);
    // the device only ships the reduced JPEG and reads the JSON reply.
    private suspend fun callClaude(imageBase64: String): TVStyleMetadata {
        val json = try {
            PoplurAIBackendClient().tvStyleMetadata(imageBase64)
        } catch (e: AIStackException.BackendError) {
            println("🎬 [TVStyleAnalyzer] backend HTTP ${e.statusCode}")
            throw TVAnalyzerError.HttpError(e.statusCode)
        } catch (e: TVAnalyzerError) {
            throw e
        } catch (e: Exception) {
            throw TVAnalyzerError.ParseError
        }
        return TVStyleMetadata(
            channel       = json.optString("channel", "entertainment"),
            tvFormat      = json.optString("tvFormat", "commercial"),
            adConcept     = json.optString("adConcept", ""),
            caption       = json.optString("caption", ""),
            tagline       = json.optString("tagline", ""),
            broadcastName = json.optString("broadcastName", "POPLUR TV")
        )
    }
}
