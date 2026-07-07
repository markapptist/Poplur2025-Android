package com.apptists.poplur2025.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object FaceAdGenerationService {
    suspend fun generateImage(faceBitmap: Bitmap, prompt: String): Bitmap = withContext(Dispatchers.IO) {
        val response = PoplurAIBackendClient().generate(
            request(PoplurAIContentType.Edit, prompt, mapOf("hasFaceImage" to "true"))
        )
        val url = response.outputUrl ?: throw FaceAdError.InvalidResponse
        downloadBitmap(url)
    }

    suspend fun generateVideo(faceBitmap: Bitmap, prompt: String): String = withContext(Dispatchers.IO) {
        val response = PoplurAIBackendClient().generate(
            request(PoplurAIContentType.Video, prompt, mapOf("hasFaceImage" to "true"))
        )
        response.outputUrl ?: throw FaceAdError.InvalidResponse
    }

    private fun request(
        contentType: PoplurAIContentType,
        prompt: String,
        metadata: Map<String, String>
    ): AIGenerationRequest {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: throw FaceAdError.ProviderMustRunOnBackend
        return AIGenerationRequest(
            userId = uid,
            plan = PoplurPlan.Creator,
            contentType = contentType,
            prompt = prompt,
            metadata = metadata
        )
    }

    private fun downloadBitmap(url: String): Bitmap {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 30_000
            connection.readTimeout = 90_000
            val code = connection.responseCode
            if (code !in 200..299) throw FaceAdError.DownloadFailed
            BitmapFactory.decodeStream(connection.inputStream) ?: throw FaceAdError.DownloadFailed
        } finally {
            connection.disconnect()
        }
    }
}

sealed class FaceAdError(message: String) : Exception(message) {
    object ProviderMustRunOnBackend : FaceAdError("Poplur premium AI must run through the backend using Google AI Studio.")
    object InvalidResponse : FaceAdError("Unexpected API response")
    object GenerationFailed : FaceAdError("AI generation failed")
    object DownloadFailed : FaceAdError("Could not download result")
    object Timeout : FaceAdError("Generation timed out")
}
