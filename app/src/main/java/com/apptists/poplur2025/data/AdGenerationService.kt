package com.apptists.poplur2025.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object AdGenerationService {
    suspend fun generate(prompt: String): Bitmap = withContext(Dispatchers.IO) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: throw AdGenerationError.ProviderMustRunOnBackend
        val response = PoplurAIBackendClient().generate(
            AIGenerationRequest(
                userId = uid,
                plan = PoplurPlan.Creator,
                contentType = PoplurAIContentType.Image,
                prompt = prompt,
                metadata = mapOf("surface" to "ad_generation")
            )
        )
        val outputUrl = response.outputUrl ?: throw AdGenerationError.DecodeFailed
        val conn = URL(outputUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 90_000
        conn.readTimeout = 90_000
        conn.setRequestProperty("User-Agent", "Poplur/1.0")
        try {
            val code = conn.responseCode
            if (code != 200) {
                Log.w("AdGeneration", "image failed $code: ${conn.errorBody()}")
                throw AdGenerationError.HttpError(code)
            }
            val data = conn.inputStream.readBytes()
            BitmapFactory.decodeByteArray(data, 0, data.size)
                ?: throw AdGenerationError.DecodeFailed
        } finally {
            conn.disconnect()
        }
    }

    private fun HttpURLConnection.errorBody(): String =
        try {
            errorStream?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
        } catch (_: Exception) {
            ""
        }
}

sealed class AdGenerationError(message: String) : Exception(message) {
    object ProviderMustRunOnBackend : AdGenerationError("Sign in and use Poplur AI credits to generate ads.")
    data class HttpError(val code: Int) : AdGenerationError("HTTP $code from Poplur AI")
    object DecodeFailed : AdGenerationError("Failed to decode generated image")
}
