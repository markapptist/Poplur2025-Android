package com.apptists.poplur2025.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PremiumAIAction(val wireValue: String, val label: String) {
    GenerateCaption("generate_caption", "Generate Caption"),
    SuggestPostIdea("suggest_post_idea", "Suggest Post Idea"),
    BusinessPromo("business_promo", "Business Promo"),
    TranslateCaption("translate_caption", "Translate Caption")
}

data class PremiumAIEntitlement(
    val isPremium: Boolean,
    val premiumTier: String?,
    val monthlyLimit: Int,
    val usedThisMonth: Int,
    val month: String
) {
    val remaining: Int get() = (monthlyLimit - usedThisMonth).coerceAtLeast(0)
}

data class PremiumAIResult(
    val action: PremiumAIAction,
    val output: Map<String, String>,
    val usage: PremiumAIEntitlement
) {
    val editableText: String
        get() = when (action) {
            PremiumAIAction.GenerateCaption -> listOfNotNull(
                output["title"],
                output["caption"],
                output["hashtags"],
                output["radiusTip"]?.let { "Radius tip: $it" }
            )
            PremiumAIAction.SuggestPostIdea -> listOfNotNull(
                output["postIdea"],
                output["suggestedCaption"],
                output["bestTimeToPost"]?.let { "Best time: $it" },
                output["radiusStrategy"]?.let { "Radius: $it" }
            )
            PremiumAIAction.BusinessPromo -> listOfNotNull(
                output["promoTitle"],
                output["promoCaption"],
                output["couponText"],
                output["callToAction"]
            )
            PremiumAIAction.TranslateCaption -> listOfNotNull(output["translatedText"])
        }.map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n\n")
}

class PremiumAIService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1")
) {
    suspend fun fetchEntitlement(): PremiumAIEntitlement {
        val uid = auth.currentUser?.uid ?: return signedOutEntitlement()
        val snapshot = firestore.collection("users").document(uid).get().await()
        val tier = snapshot.getString("premiumTier")
        val defaultLimit = if (tier.equals("business", ignoreCase = true)) 500 else 100
        val storedMonth = snapshot.getString("aiUsageMonth") ?: currentUsageMonth()
        val used = if (storedMonth == currentUsageMonth()) snapshot.getLong("aiUsedThisMonth")?.toInt() ?: 0 else 0
        val limit = snapshot.getLong("aiMonthlyLimit")?.toInt() ?: defaultLimit

        return PremiumAIEntitlement(
            isPremium = snapshot.getBoolean("isPremium") == true,
            premiumTier = tier,
            monthlyLimit = limit.coerceAtLeast(0),
            usedThisMonth = used.coerceAtLeast(0),
            month = currentUsageMonth()
        )
    }

    suspend fun run(action: PremiumAIAction, input: Map<String, Any>): PremiumAIResult {
        checkNotNull(auth.currentUser) { "Sign in to use Poplur AI." }

        val payload = mapOf(
            "action" to action.wireValue,
            "input" to input
        )
        val response = functions.getHttpsCallable("premiumAiAssist").call(payload).await()
        val data = response.getData() as? Map<*, *> ?: error("Poplur AI returned an unexpected response.")
        val output = normalizedOutput(data["output"])
        val usage = entitlementFrom(data["usage"])
        return PremiumAIResult(action = action, output = output, usage = usage)
    }

    private fun entitlementFrom(rawUsage: Any?): PremiumAIEntitlement {
        val usage = rawUsage as? Map<*, *> ?: emptyMap<Any, Any>()
        return PremiumAIEntitlement(
            isPremium = true,
            premiumTier = usage["premiumTier"] as? String,
            monthlyLimit = (usage["monthlyLimit"] as? Number)?.toInt() ?: 0,
            usedThisMonth = (usage["usedThisMonth"] as? Number)?.toInt() ?: 0,
            month = usage["month"] as? String ?: currentUsageMonth()
        )
    }

    private fun normalizedOutput(rawOutput: Any?): Map<String, String> {
        val output = rawOutput as? Map<*, *> ?: return emptyMap()
        return output.mapNotNull { (key, value) ->
            val normalizedKey = key as? String ?: return@mapNotNull null
            val normalizedValue = when (value) {
                is String -> value
                is List<*> -> value.joinToString(separator = " ") { it.toString() }
                null -> ""
                else -> value.toString()
            }
            normalizedKey to normalizedValue
        }.toMap()
    }

    private fun signedOutEntitlement(): PremiumAIEntitlement =
        PremiumAIEntitlement(
            isPremium = false,
            premiumTier = null,
            monthlyLimit = 0,
            usedThisMonth = 0,
            month = currentUsageMonth()
        )

    private fun currentUsageMonth(): String =
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
}
