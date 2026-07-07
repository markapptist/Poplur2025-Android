package com.apptists.poplur2025.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object VideoAdGenerationService {
    suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: throw VideoAdError.ProviderMustRunOnBackend
        val response = PoplurAIBackendClient().generate(
            AIGenerationRequest(
                userId = uid,
                plan = PoplurPlan.Creator,
                contentType = PoplurAIContentType.Video,
                prompt = prompt,
                metadata = mapOf("surface" to "capture_preview")
            )
        )
        response.outputUrl ?: throw VideoAdError.InvalidResponse
    }
}

sealed class VideoAdError(message: String) : Exception(message) {
    object ProviderMustRunOnBackend : VideoAdError("Poplur AI generation must run through the backend.")
    object InvalidResponse : VideoAdError("Unexpected API response")
    object GenerationFailed : VideoAdError("Video generation failed")
    object Timeout : VideoAdError("Generation timed out")
}
