package com.apptists.poplur2025.data

enum class PoplurLanguage(
    val code: String,
    val displayName: String,
    val shortLabel: String,
    val flag: String
) {
    English("en", "English", "EN", "🇺🇸"),
    French("fr", "French", "FR", "🇫🇷"),
    Spanish("es", "Spanish", "ES", "🇪🇸");

    companion object {
        fun fromCode(code: String?): PoplurLanguage =
            entries.firstOrNull { it.code == code } ?: English
    }
}

data class AITranslatedCaption(
    val badge: String,
    val text: String
)

object AITranslationCaptionProvider {
    fun captionFor(
        post: Post,
        channelCategory: String,
        preferredLanguage: PoplurLanguage,
        playbackSeconds: Long = 0L
    ): AITranslatedCaption {
        val sourceLanguage = sourceLanguageFor(post, channelCategory)
        val badge = if (sourceLanguage == preferredLanguage) {
            "AI ${preferredLanguage.shortLabel} CC"
        } else {
            "AI ${sourceLanguage.shortLabel} -> ${preferredLanguage.shortLabel}"
        }

        val text = if (isDemoDramaPost(post)) {
            dramaCaption(
                sourceLanguage = sourceLanguage,
                preferredLanguage = preferredLanguage,
                playbackSeconds = playbackSeconds
            )
        } else {
            val creator = post.displayName.ifBlank { post.username.ifBlank { "this creator" } }
            val place = post.locationName.ifBlank { "the scene" }
            when (preferredLanguage) {
                PoplurLanguage.English -> "AI captions: $creator is sharing a moment from $place."
                PoplurLanguage.French -> "Sous-titres IA : $creator partage un moment depuis $place."
                PoplurLanguage.Spanish -> "Subtitulos de IA: $creator comparte un momento desde $place."
            }
        }

        return AITranslatedCaption(badge = badge, text = text)
    }

    fun sourceLanguageFor(post: Post, channelCategory: String): PoplurLanguage =
        when {
            channelCategory.equals("French", ignoreCase = true) -> PoplurLanguage.French
            channelCategory.equals("Spanish", ignoreCase = true) -> PoplurLanguage.Spanish
            else -> PoplurLanguage.fromCode(post.sourceLanguageCode)
        }

    private fun isDemoDramaPost(post: Post): Boolean {
        val lowerId = post.id.lowercase()
        val lowerMediaUrl = post.mediaUrl.orEmpty().lowercase()
        val lowerVideoUrl = post.videoUrl.orEmpty().lowercase()
        return lowerId.contains("demo") ||
            lowerMediaUrl.contains("guiding_light") ||
            lowerMediaUrl.contains("guidinglight") ||
            lowerMediaUrl.contains("love_of_life") ||
            lowerMediaUrl.contains("loveoflife") ||
            lowerMediaUrl.contains("valiant_lady") ||
            lowerMediaUrl.contains("1967_pd_soapie") ||
            lowerVideoUrl.contains("guiding_light") ||
            lowerVideoUrl.contains("guidinglight") ||
            lowerVideoUrl.contains("love_of_life") ||
            lowerVideoUrl.contains("loveoflife") ||
            lowerVideoUrl.contains("valiant_lady") ||
            lowerVideoUrl.contains("1967_pd_soapie")
    }

    private fun dramaCaption(
        sourceLanguage: PoplurLanguage,
        preferredLanguage: PoplurLanguage,
        playbackSeconds: Long
    ): String {
        val cueIndex = dramaCueStartTimes.indexOfLast { playbackSeconds >= it }.coerceAtLeast(0)
        val line = when (preferredLanguage) {
            PoplurLanguage.English -> englishDramaLines[cueIndex]
            PoplurLanguage.French -> frenchDramaLines[cueIndex]
            PoplurLanguage.Spanish -> spanishDramaLines[cueIndex]
        }

        if (sourceLanguage == preferredLanguage) return line

        return when (preferredLanguage) {
            PoplurLanguage.English -> "Translated from ${sourceLanguage.shortLabel}: $line"
            PoplurLanguage.French -> "Traduit depuis ${sourceLanguage.shortLabel} : $line"
            PoplurLanguage.Spanish -> "Traducido de ${sourceLanguage.shortLabel}: $line"
        }
    }

    private val dramaCueStartTimes = longArrayOf(0L, 5L, 11L, 16L, 22L, 29L)

    private val englishDramaLines = listOf(
        "[door opens] Margaret, tell me the truth before the whole town hears it.",
        "I tried to protect this family, but the secret was already in the room.",
        "No, Daniel. If you leave now, you may never know who signed that letter.",
        "[telephone rings] Stay quiet. That call is for me.",
        "After everything we built, you still chose the lie.",
        "Previously on Poplur Drama: one promise changed every life in town."
    )

    private val frenchDramaLines = listOf(
        "[porte qui s'ouvre] Margaret, dis-moi la verite avant que toute la ville l'apprenne.",
        "J'ai essaye de proteger cette famille, mais le secret etait deja dans la piece.",
        "Non, Daniel. Si tu pars maintenant, tu ne sauras peut-etre jamais qui a signe cette lettre.",
        "[telephone qui sonne] Reste silencieux. Cet appel est pour moi.",
        "Apres tout ce que nous avons construit, tu as quand meme choisi le mensonge.",
        "Precedemment dans Poplur Drama : une promesse a change toutes les vies en ville."
    )

    private val spanishDramaLines = listOf(
        "[se abre la puerta] Margaret, dime la verdad antes de que todo el pueblo se entere.",
        "Intente proteger a esta familia, pero el secreto ya estaba en la habitacion.",
        "No, Daniel. Si te vas ahora, tal vez nunca sepas quien firmo esa carta.",
        "[suena el telefono] Guarda silencio. Esa llamada es para mi.",
        "Despues de todo lo que construimos, aun elegiste la mentira.",
        "Anteriormente en Poplur Drama: una promesa cambio cada vida del pueblo."
    )
}
