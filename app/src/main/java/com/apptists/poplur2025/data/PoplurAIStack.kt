package com.apptists.poplur2025.data

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import java.util.UUID
import java.net.HttpURLConnection
import java.net.URL

enum class PoplurAIContentType(val wireValue: String, val label: String, val systemIconLabel: String) {
    Image("image", "Images", "photo"),
    Video("video", "Videos", "video"),
    Edit("edit", "AI Edits", "wand"),
    Vfx("vfx", "VFX", "sparkles"),
    VfxVideo("vfx_video", "VFX", "sparkles"),
    ThreeDModel("3d_model", "3D Characters", "cube"),
    Voice("voice", "Voice", "waveform"),
    Music("music", "Music", "music"),
    Game("game", "Games", "gamepad"),
    Coupon("coupon", "Coupons", "ticket"),
    Campaign("campaign", "Campaigns", "megaphone");

    val defaultProvider: PoplurAIProvider
        get() = when (this) {
            Image, Video, Edit, Coupon, Campaign -> PoplurAIProvider.Google
            ThreeDModel -> PoplurAIProvider.Meshy
            Voice -> PoplurAIProvider.Voice
            Music -> PoplurAIProvider.Music
            Game -> PoplurAIProvider.Game
            Vfx, VfxVideo -> PoplurAIProvider.Vfx
        }

    val providerLabel: String get() = "Powered by ${defaultProvider.label}"
}

enum class PoplurAIProvider(val wireValue: String, val label: String, val envVar: String) {
    Google("google", "Google AI Studio", "GOOGLE_AI_API_KEY"),
    Meshy("meshy", "Meshy", "MESHY_API_KEY"),
    Voice("voice", "ElevenLabs", "ELEVENLABS_API_KEY"),
    Music("music", "Suno", "SUNO_API_KEY"),
    Game("game", "Rosebud AI", "GAME_PROVIDER_API_KEY"),
    Vfx("vfx", "Poplur VFX", "VFX_PROVIDER_API_KEY")
}

enum class PoplurAIGenerationStatus(val wireValue: String) {
    Queued("queued"),
    Processing("processing"),
    Completed("completed"),
    Failed("failed"),
    Cancelled("cancelled")
}

enum class CreatorCreditTransactionType(val wireValue: String) {
    SubscriptionGrant("subscription_grant"),
    PurchasedCredits("purchased_credits"),
    PromotionalGrant("promotional_grant"),
    ReferralReward("referral_reward"),
    AdReward("ad_reward"),
    AiGenerationSpend("ai_generation_spend"),
    RadiusBoostSpend("radius_boost_spend"),
    Refund("refund"),
    AdminAdjustment("admin_adjustment")
}

enum class PoplurPlan {
    Free,
    Creator,
    Business;

    fun canGenerateAI(credits: Int): Boolean = when (this) {
        Free -> credits > 0
        Creator, Business -> true
    }

    val canUseBusinessCampaignTools: Boolean get() = this == Business
}

enum class RadiusBoostTier(val wireValue: String, val label: String, val additionalRadiusMeters: Int) {
    Local("local", "Local Boost", 1_000),
    Neighborhood("neighborhood", "Neighborhood Boost", 5_000),
    City("city", "City Boost", 25_000),
    Regional("regional", "Regional Boost", 100_000);

    val durationMillis: Long get() = 24L * 60L * 60L * 1_000L
}

data class PoplurAIStackConfig(
    val providerEnabled: Map<PoplurAIProvider, Boolean> = PoplurAIProvider.entries.associateWith { true },
    val creditCostByContentType: Map<PoplurAIContentType, Int> = mapOf(
        PoplurAIContentType.Image to 5,
        PoplurAIContentType.Voice to 10,
        PoplurAIContentType.Music to 25,
        PoplurAIContentType.ThreeDModel to 50,
        PoplurAIContentType.Game to 75,
        PoplurAIContentType.Video to 100,
        PoplurAIContentType.Edit to 100,
        PoplurAIContentType.Vfx to 125,
        PoplurAIContentType.VfxVideo to 125,
        PoplurAIContentType.Coupon to 15,
        PoplurAIContentType.Campaign to 0
    ),
    val boostCostByTier: Map<RadiusBoostTier, Int> = mapOf(
        RadiusBoostTier.Local to 10,
        RadiusBoostTier.Neighborhood to 25,
        RadiusBoostTier.City to 80,
        RadiusBoostTier.Regional to 250
    ),
    val maxPromptLength: Int = 1_000,
    val maxVideoDurationSeconds: Int = 15,
    val maxGenerationsPerUserPerDay: Int = 50,
    val freePromotionalCreditAmount: Int = 25,
    val watermarkEnabledForFreeUsers: Boolean = true,
    val providerTimeoutMillis: Long = 120_000L
) {
    fun estimatedCost(contentType: PoplurAIContentType, campaignAssets: List<PoplurAIContentType> = emptyList()): Int =
        if (contentType == PoplurAIContentType.Campaign) {
            campaignAssets.sumOf { estimatedCost(it) }
        } else {
            creditCostByContentType[contentType] ?: 0
        }
}

data class CreatorCreditBalance(
    val userId: String,
    val availableCredits: Int,
    val reservedCredits: Int = 0,
    val updatedAt: Date = Date()
) {
    val totalCredits: Int get() = availableCredits + reservedCredits
}

data class CreatorCreditTransaction(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val amount: Int,
    val type: CreatorCreditTransactionType,
    val relatedJobId: String? = null,
    val relatedPostId: String? = null,
    val provider: PoplurAIProvider? = null,
    val contentType: PoplurAIContentType? = null,
    val createdAt: Date = Date(),
    val metadata: Map<String, String> = emptyMap()
)

data class AIGenerationJob(
    val id: String,
    val userId: String,
    val provider: PoplurAIProvider,
    val contentType: PoplurAIContentType,
    val prompt: String,
    val inputAssetIds: List<String>,
    val status: PoplurAIGenerationStatus,
    val creditsReserved: Int,
    val creditsCharged: Int,
    val outputAssetIds: List<String>,
    val errorMessage: String?,
    val createdAt: Date,
    val updatedAt: Date,
    val completedAt: Date?,
    val metadata: Map<String, String>
)

data class RadiusBoostTransaction(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val postId: String,
    val tier: RadiusBoostTier,
    val creditsCharged: Int,
    val boostedRadiusMeters: Int,
    val boostExpiresAt: Date,
    val createdAt: Date = Date(),
    val metadata: Map<String, String> = emptyMap()
)

data class AIMapPublishMetadata(
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val address: String? = null,
    val radiusMeters: Int,
    val contentType: PoplurAIContentType,
    val businessId: String? = null,
    val campaignId: String? = null,
    val expirationDate: Date? = null,
    val tags: List<String> = emptyList(),
    val category: String? = null
)

data class AIMapPost(
    val id: String,
    val userId: String,
    val assetIds: List<String>,
    val publishMetadata: AIMapPublishMetadata,
    val organicRadiusMeters: Int,
    val boostedRadiusMeters: Int = 0,
    val boostExpiresAt: Date? = null,
    val engagementScore: Double = 0.0,
    val visibilityStatus: String = "visible"
)

data class AICampaign(
    val id: String,
    val businessId: String,
    val userId: String,
    val prompt: String,
    val generatedAssetIds: List<String>,
    val status: PoplurAIGenerationStatus,
    val scheduledPublishAt: Date?,
    val location: AIMapPublishMetadata,
    val radiusMeters: Int,
    val createdAt: Date,
    val updatedAt: Date
)

data class AIGenerationRequest(
    val userId: String,
    val plan: PoplurPlan,
    val contentType: PoplurAIContentType,
    val prompt: String,
    val inputAssetIds: List<String> = emptyList(),
    val publishMetadata: AIMapPublishMetadata? = null,
    val campaignAssetTypes: List<PoplurAIContentType> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

data class AIProviderResponse(
    val jobId: String,
    val provider: PoplurAIProvider,
    val contentType: PoplurAIContentType,
    val status: PoplurAIGenerationStatus,
    val estimatedCostCredits: Int,
    val outputUrl: String? = null,
    val outputAssetId: String? = null,
    val providerRawResponse: Map<String, String> = emptyMap()
)

data class PoplurRadioHostBackendResponse(
    val script: String,
    val audioData: ByteArray,
    val mimeType: String,
    val stationName: String?
)

interface AIProviderAdapter {
    val provider: PoplurAIProvider
    suspend fun generateImage(request: AIGenerationRequest): AIProviderResponse = unsupported(request)
    suspend fun generateVideo(request: AIGenerationRequest): AIProviderResponse = unsupported(request)
    suspend fun editVideo(request: AIGenerationRequest): AIProviderResponse = unsupported(request)
    suspend fun generate3DModel(request: AIGenerationRequest): AIProviderResponse = unsupported(request)
    suspend fun generateVoice(request: AIGenerationRequest): AIProviderResponse = unsupported(request)
    suspend fun generateMusic(request: AIGenerationRequest): AIProviderResponse = unsupported(request)
    suspend fun generateGame(request: AIGenerationRequest): AIProviderResponse = unsupported(request)
    suspend fun generateVFX(request: AIGenerationRequest): AIProviderResponse = unsupported(request)

    private fun unsupported(request: AIGenerationRequest): Nothing =
        throw AIStackException.UnsupportedProviderMethod(provider, request.contentType)
}

class BackendOnlyAIProvider(override val provider: PoplurAIProvider) : AIProviderAdapter {
    private fun backendOnly(): Nothing {
        // TODO: Implement provider calls in Poplur backend using environment variables, never in Android.
        throw AIStackException.ProviderMustRunOnBackend(provider)
    }

    override suspend fun generateImage(request: AIGenerationRequest): AIProviderResponse = backendOnly()
    override suspend fun generateVideo(request: AIGenerationRequest): AIProviderResponse = backendOnly()
    override suspend fun editVideo(request: AIGenerationRequest): AIProviderResponse = backendOnly()
    override suspend fun generate3DModel(request: AIGenerationRequest): AIProviderResponse = backendOnly()
    override suspend fun generateVoice(request: AIGenerationRequest): AIProviderResponse = backendOnly()
    override suspend fun generateMusic(request: AIGenerationRequest): AIProviderResponse = backendOnly()
    override suspend fun generateGame(request: AIGenerationRequest): AIProviderResponse = backendOnly()
    override suspend fun generateVFX(request: AIGenerationRequest): AIProviderResponse = backendOnly()
}

typealias GoogleAIProvider = BackendOnlyAIProvider
typealias MeshyProvider = BackendOnlyAIProvider
typealias VoiceProvider = BackendOnlyAIProvider
typealias AIMusicProvider = BackendOnlyAIProvider
typealias GameProvider = BackendOnlyAIProvider
typealias VFXProvider = BackendOnlyAIProvider

sealed class AIStackException(message: String) : Exception(message) {
    object BackendBaseUrlMissing : AIStackException("Poplur AI backend is not configured.")
    data class ProviderDisabled(val provider: PoplurAIProvider) : AIStackException("${provider.label} is currently disabled.")
    object PromptTooLong : AIStackException("Prompt is too long.")
    object AiLockedForPlan : AIStackException("Use Creator Credits to generate AI content.")
    object InsufficientCredits : AIStackException("Not enough Creator Credits.")
    object NegativeBalanceRejected : AIStackException("Creator Credit balance cannot go negative.")
    data class UnsupportedProviderMethod(val provider: PoplurAIProvider, val contentType: PoplurAIContentType) :
        AIStackException("${provider.label} does not support ${contentType.label}.")
    data class ProviderMustRunOnBackend(val provider: PoplurAIProvider) :
        AIStackException("${provider.label} must be called through Poplur's backend.")
    object InvalidLocation : AIStackException("Choose a real map location before publishing.")
    data class BackendError(val statusCode: Int) : AIStackException("Poplur AI backend returned HTTP $statusCode.")
}

object PoplurAIBackendEndpoint {
    const val GenerateImage = "/ai/generate/image"
    const val GenerateVideo = "/ai/generate/video"
    const val GenerateEdit = "/ai/generate/edit"
    const val GenerateVfx = "/ai/generate/vfx"
    const val Generate3D = "/ai/generate/3d"
    const val GenerateVoice = "/ai/generate/voice"
    const val GenerateMusic = "/ai/generate/music"
    const val GenerateGame = "/ai/generate/game"
    const val Jobs = "/ai/jobs"
    const val RadioHost = "/radio/host"
    const val AssistantGuestChat = "/assistant/guest-chat"
    const val TvStyle = "/tv/style"
    const val BumperScript = "/bumper/script"
    const val GameConfig = "/games/config"
    const val VideoStoryboard = "/video/storyboard"
    const val RadiusBoost = "/posts/{id}/radius-boost"
    const val BusinessCampaign = "/businesses/{businessId}/ai-campaigns"
}

class PoplurAIBackendClient(
    private val baseUrl: String = System.getenv("POPLUR_AI_API_BASE_URL")
        ?: "https://us-central1-poplur2025.cloudfunctions.net/poplurAiApi"
) {
    fun endpointPath(contentType: PoplurAIContentType): String = when (contentType) {
        PoplurAIContentType.Image -> PoplurAIBackendEndpoint.GenerateImage
        PoplurAIContentType.Video -> PoplurAIBackendEndpoint.GenerateVideo
        PoplurAIContentType.Edit -> PoplurAIBackendEndpoint.GenerateEdit
        PoplurAIContentType.Vfx, PoplurAIContentType.VfxVideo -> PoplurAIBackendEndpoint.GenerateVfx
        PoplurAIContentType.ThreeDModel -> PoplurAIBackendEndpoint.Generate3D
        PoplurAIContentType.Voice -> PoplurAIBackendEndpoint.GenerateVoice
        PoplurAIContentType.Music -> PoplurAIBackendEndpoint.GenerateMusic
        PoplurAIContentType.Game -> PoplurAIBackendEndpoint.GenerateGame
        PoplurAIContentType.Coupon, PoplurAIContentType.Campaign -> PoplurAIBackendEndpoint.BusinessCampaign
    }

    suspend fun generate(request: AIGenerationRequest): AIProviderResponse {
        if (baseUrl.isBlank()) throw AIStackException.BackendBaseUrlMissing
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            ?: throw AIStackException.AiLockedForPlan
        val token = user.getIdToken(false).await().token
            ?: throw AIStackException.AiLockedForPlan
        val endpoint = endpointPath(request.contentType)
        val connection = URL("${baseUrl.trimEnd('/')}/${endpoint.trimStart('/')}").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 30_000
            connection.readTimeout = 300_000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { stream ->
                stream.write(httpBody(request).toString().toByteArray(Charsets.UTF_8))
            }
            val statusCode = connection.responseCode
            val body = if (statusCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }
            if (statusCode !in 200..299) {
                throw AIStackException.ProviderMustRunOnBackend(request.contentType.defaultProvider)
            }
            val payload = JSONObject(body)
            val job = payload.optJSONObject("job")
                ?: throw AIStackException.ProviderMustRunOnBackend(request.contentType.defaultProvider)
            return responseFrom(job, request)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Shared POST helper for the small JSON-in/JSON-out Claude endpoints.
     * [authenticated] = false only for the pre-login guest routes.
     */
    private suspend fun postJson(
        endpoint: String,
        body: JSONObject,
        authenticated: Boolean = true
    ): JSONObject = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) throw AIStackException.BackendBaseUrlMissing
        val token = if (authenticated) {
            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                ?: throw AIStackException.AiLockedForPlan
            user.getIdToken(false).await().token
                ?: throw AIStackException.AiLockedForPlan
        } else null
        val connection = URL("${baseUrl.trimEnd('/')}/${endpoint.trimStart('/')}").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            connection.doOutput = true
            token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val statusCode = connection.responseCode
            val responseBody = if (statusCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }
            if (statusCode !in 200..299) {
                throw AIStackException.BackendError(statusCode)
            }
            JSONObject(responseBody)
        } finally {
            connection.disconnect()
        }
    }

    /** Poplur TV broadcast metadata for one post image (POST /tv/style). */
    suspend fun tvStyleMetadata(imageBase64: String): JSONObject =
        postJson(PoplurAIBackendEndpoint.TvStyle, JSONObject().put("imageBase64", imageBase64))

    /** One spoken TV-host sentence introducing the next feed video. */
    suspend fun bumperScript(context: String, language: String = "english"): String {
        val payload = postJson(
            PoplurAIBackendEndpoint.BumperScript,
            JSONObject().put("context", context).put("language", language)
        )
        val script = payload.optString("script")
        if (script.isBlank()) throw AIStackException.BackendError(0)
        return script
    }

    /** A generated mini-game session config, as the raw config JSONObject. */
    suspend fun gameConfig(mode: String, prompt: String?): JSONObject {
        val body = JSONObject().put("mode", mode)
        if (!prompt.isNullOrBlank()) body.put("prompt", prompt)
        return postJson(PoplurAIBackendEndpoint.GameConfig, body).optJSONObject("config")
            ?: throw AIStackException.BackendError(0)
    }

    /** Three-shot storyboard for the AI video studio (POST /video/storyboard). */
    suspend fun videoStoryboard(
        prompt: String,
        styleLabel: String,
        styleSuffix: String,
        locationName: String
    ): JSONObject = postJson(
        PoplurAIBackendEndpoint.VideoStoryboard,
        JSONObject()
            .put("prompt", prompt)
            .put("styleLabel", styleLabel)
            .put("styleSuffix", styleSuffix)
            .put("locationName", locationName)
    )

    /**
     * Pre-login auth-screen guide answer. No Firebase user exists yet, so this
     * hits the unauthenticated, rate-limited guest route.
     */
    suspend fun authGuideReply(screen: String, question: String): String {
        val payload = postJson(
            PoplurAIBackendEndpoint.AssistantGuestChat,
            JSONObject().put("screen", screen).put("question", question),
            authenticated = false
        )
        val reply = payload.optString("reply")
        if (reply.isBlank()) throw AIStackException.BackendError(0)
        return reply
    }

    suspend fun radioHostAudio(
        script: String,
        cityName: String,
        stopTitle: String,
        stationName: String?,
        style: String = "warm local radio presenter"
    ): PoplurRadioHostBackendResponse = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) throw AIStackException.BackendBaseUrlMissing
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            ?: throw AIStackException.AiLockedForPlan
        val token = user.getIdToken(false).await().token
            ?: throw AIStackException.AiLockedForPlan
        val connection = URL("${baseUrl.trimEnd('/')}/${PoplurAIBackendEndpoint.RadioHost.trimStart('/')}").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 30_000
            connection.readTimeout = 300_000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json")
            val body = JSONObject().apply {
                put("script", script)
                put("cityName", cityName)
                put("stopTitle", stopTitle)
                put("stationName", stationName.orEmpty())
                put("style", style)
            }
            connection.outputStream.use { stream ->
                stream.write(body.toString().toByteArray(Charsets.UTF_8))
            }
            val statusCode = connection.responseCode
            val responseBody = if (statusCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }
            if (statusCode !in 200..299) {
                throw AIStackException.ProviderMustRunOnBackend(PoplurAIProvider.Google)
            }
            val payload = JSONObject(responseBody)
            val audioBase64 = payload.optString("audioBase64")
            if (audioBase64.isBlank()) {
                throw AIStackException.ProviderMustRunOnBackend(PoplurAIProvider.Google)
            }
            PoplurRadioHostBackendResponse(
                script = payload.optString("script", script).ifBlank { script },
                audioData = Base64.decode(audioBase64, Base64.DEFAULT),
                mimeType = payload.optString("mimeType", "audio/wav"),
                stationName = payload.optString("stationName")
                    .ifBlank { stationName.orEmpty() }
                    .ifBlank { null }
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun httpBody(request: AIGenerationRequest): JSONObject =
        JSONObject().apply {
            put("prompt", request.prompt)
            put("sourceAssetIds", JSONArray(request.inputAssetIds))
            put("metadata", JSONObject(request.metadata + mapOf(
                "client" to "android",
                "requestedContentType" to request.contentType.wireValue
            )))
            request.publishMetadata?.let { metadata ->
                put("location", JSONObject().apply {
                    put("latitude", metadata.latitude)
                    put("longitude", metadata.longitude)
                    put("locationName", metadata.locationName)
                    put("address", metadata.address.orEmpty())
                    put("radiusMeters", metadata.radiusMeters)
                    put("category", metadata.category.orEmpty())
                })
                put("channelId", metadata.category ?: metadata.contentType.wireValue)
            }
        }

    private fun responseFrom(job: JSONObject, fallbackRequest: AIGenerationRequest): AIProviderResponse {
        val outputAssetIds = job.optJSONArray("outputAssetIds").toStringList()
        val outputUrls = job.optJSONArray("outputUrls").toStringList()
        val outputUrl = job.optString("outputUrl").ifBlank { outputUrls.firstOrNull().orEmpty() }
        return AIProviderResponse(
            jobId = job.optString("id", UUID.randomUUID().toString()),
            provider = providerFrom(job.optString("provider"), fallbackRequest.contentType.defaultProvider),
            contentType = contentTypeFrom(job.optString("contentType"), fallbackRequest.contentType),
            status = statusFrom(job.optString("status")),
            estimatedCostCredits = job.optInt("creditsCharged", 0) + job.optInt("creditsReserved", 0),
            outputUrl = outputUrl.ifBlank { null },
            outputAssetId = outputAssetIds.firstOrNull(),
            providerRawResponse = mapOf(
                "providerJobId" to job.optString("providerJobId"),
                "outputText" to job.optString("outputText")
            ).filterValues { it.isNotBlank() }
        )
    }

    private fun providerFrom(rawValue: String, fallback: PoplurAIProvider): PoplurAIProvider =
        when (rawValue) {
            "google_ai_studio" -> PoplurAIProvider.Google
            else -> PoplurAIProvider.entries.firstOrNull { it.wireValue == rawValue } ?: fallback
        }

    private fun contentTypeFrom(rawValue: String, fallback: PoplurAIContentType): PoplurAIContentType =
        when (rawValue) {
            "3d" -> PoplurAIContentType.ThreeDModel
            else -> PoplurAIContentType.entries.firstOrNull { it.wireValue == rawValue } ?: fallback
        }

    private fun statusFrom(rawValue: String): PoplurAIGenerationStatus =
        when (rawValue) {
            "completed" -> PoplurAIGenerationStatus.Completed
            "failed", "refunded" -> PoplurAIGenerationStatus.Failed
            "cancelled" -> PoplurAIGenerationStatus.Cancelled
            "queued" -> PoplurAIGenerationStatus.Queued
            else -> PoplurAIGenerationStatus.Processing
        }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { index -> optString(index).takeIf { it.isNotBlank() } }
    }
}

class CreatorCreditLedger(
    initialBalances: Map<String, CreatorCreditBalance> = emptyMap()
) {
    private val mutex = Mutex()
    private val balances = initialBalances.toMutableMap()
    val transactions: MutableList<CreatorCreditTransaction> = mutableListOf()

    suspend fun balance(userId: String): CreatorCreditBalance = mutex.withLock {
        balances[userId] ?: CreatorCreditBalance(userId = userId, availableCredits = 0)
    }

    suspend fun grant(userId: String, amount: Int, type: CreatorCreditTransactionType) = mutex.withLock {
        if (amount < 0) throw AIStackException.NegativeBalanceRejected
        val current = balances[userId] ?: CreatorCreditBalance(userId, 0)
        balances[userId] = current.copy(availableCredits = current.availableCredits + amount, updatedAt = Date())
        transactions += CreatorCreditTransaction(userId = userId, amount = amount, type = type)
    }

    suspend fun reserve(userId: String, amount: Int, jobId: String, provider: PoplurAIProvider, contentType: PoplurAIContentType) = mutex.withLock {
        if (amount < 0) throw AIStackException.NegativeBalanceRejected
        val current = balances[userId] ?: CreatorCreditBalance(userId, 0)
        if (current.availableCredits < amount) throw AIStackException.InsufficientCredits
        balances[userId] = current.copy(
            availableCredits = current.availableCredits - amount,
            reservedCredits = current.reservedCredits + amount,
            updatedAt = Date()
        )
        transactions += CreatorCreditTransaction(
            userId = userId,
            amount = -amount,
            type = CreatorCreditTransactionType.AiGenerationSpend,
            relatedJobId = jobId,
            provider = provider,
            contentType = contentType
        )
    }

    suspend fun completeReservation(userId: String, amount: Int) = mutex.withLock {
        val current = balances[userId] ?: CreatorCreditBalance(userId, 0)
        if (current.reservedCredits < amount) throw AIStackException.NegativeBalanceRejected
        balances[userId] = current.copy(reservedCredits = current.reservedCredits - amount, updatedAt = Date())
    }

    suspend fun refund(userId: String, amount: Int, relatedJobId: String? = null) = mutex.withLock {
        val current = balances[userId] ?: CreatorCreditBalance(userId, 0)
        if (current.reservedCredits < amount) throw AIStackException.NegativeBalanceRejected
        balances[userId] = current.copy(
            availableCredits = current.availableCredits + amount,
            reservedCredits = current.reservedCredits - amount,
            updatedAt = Date()
        )
        transactions += CreatorCreditTransaction(
            userId = userId,
            amount = amount,
            type = CreatorCreditTransactionType.Refund,
            relatedJobId = relatedJobId
        )
    }

    suspend fun spendRadiusBoost(userId: String, postId: String, amount: Int, tier: RadiusBoostTier) = mutex.withLock {
        if (amount < 0) throw AIStackException.NegativeBalanceRejected
        val current = balances[userId] ?: CreatorCreditBalance(userId, 0)
        if (current.availableCredits < amount) throw AIStackException.InsufficientCredits
        balances[userId] = current.copy(availableCredits = current.availableCredits - amount, updatedAt = Date())
        transactions += CreatorCreditTransaction(
            userId = userId,
            amount = -amount,
            type = CreatorCreditTransactionType.RadiusBoostSpend,
            relatedPostId = postId,
            metadata = mapOf("tier" to tier.wireValue)
        )
    }
}

class PoplurAIStackService(
    private val config: PoplurAIStackConfig = PoplurAIStackConfig(),
    private val ledger: CreatorCreditLedger = CreatorCreditLedger(),
    private val backend: PoplurAIBackendClient = PoplurAIBackendClient()
) {
    fun validateGenerationRequest(request: AIGenerationRequest, balance: CreatorCreditBalance): Int {
        if (request.prompt.length > config.maxPromptLength) throw AIStackException.PromptTooLong
        if (!request.plan.canGenerateAI(balance.availableCredits)) throw AIStackException.AiLockedForPlan
        val provider = request.contentType.defaultProvider
        if (config.providerEnabled[provider] != true) throw AIStackException.ProviderDisabled(provider)
        val cost = config.estimatedCost(request.contentType, request.campaignAssetTypes)
        if (balance.availableCredits < cost) throw AIStackException.InsufficientCredits
        return cost
    }

    suspend fun startGeneration(request: AIGenerationRequest): AIProviderResponse {
        val balance = ledger.balance(request.userId)
        val cost = validateGenerationRequest(request, balance)
        val jobId = UUID.randomUUID().toString()
        val provider = request.contentType.defaultProvider
        ledger.reserve(request.userId, cost, jobId, provider, request.contentType)
        return try {
            val response = backend.generate(request)
            when (response.status) {
                PoplurAIGenerationStatus.Failed, PoplurAIGenerationStatus.Cancelled -> ledger.refund(request.userId, cost, jobId)
                PoplurAIGenerationStatus.Completed -> ledger.completeReservation(request.userId, cost)
                else -> Unit
            }
            response
        } catch (e: Exception) {
            ledger.refund(request.userId, cost, jobId)
            throw e
        }
    }

    fun validatePublishMetadata(metadata: AIMapPublishMetadata) {
        if (metadata.latitude !in -90.0..90.0 ||
            metadata.longitude !in -180.0..180.0 ||
            metadata.radiusMeters <= 0
        ) {
            throw AIStackException.InvalidLocation
        }
    }

    suspend fun applyRadiusBoost(post: AIMapPost, userId: String, tier: RadiusBoostTier): AIMapPost {
        val cost = config.boostCostByTier[tier] ?: 0
        ledger.spendRadiusBoost(userId, post.id, cost, tier)
        return post.copy(
            boostedRadiusMeters = minOf(post.organicRadiusMeters + tier.additionalRadiusMeters, 100_000),
            boostExpiresAt = Date(System.currentTimeMillis() + tier.durationMillis)
        )
    }
}
