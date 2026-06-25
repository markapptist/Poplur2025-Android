package com.apptists.poplur2025.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.random.Random

object AdGenerationService {
    suspend fun generate(prompt: String): Bitmap = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(prompt, "UTF-8").replace("+", "%20")
        val seed = Random.nextInt(0, 99999)
        val url = URL(
            "https://image.pollinations.ai/prompt/$encoded" +
                "?width=1080&height=1920&model=flux&nologo=true&enhance=false&seed=$seed"
        )
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 90_000
        conn.readTimeout = 90_000
        conn.setRequestProperty("User-Agent", "Poplur/1.0")
        try {
            val code = conn.responseCode
            if (code != 200) throw AdGenerationError.HttpError(code)
            val data = conn.inputStream.readBytes()
            BitmapFactory.decodeByteArray(data, 0, data.size)
                ?: throw AdGenerationError.DecodeFailed
        } finally {
            conn.disconnect()
        }
    }
}

sealed class AdGenerationError(message: String) : Exception(message) {
    data class HttpError(val code: Int) : AdGenerationError("HTTP $code from Pollinations")
    object DecodeFailed : AdGenerationError("Failed to decode generated image")
}
