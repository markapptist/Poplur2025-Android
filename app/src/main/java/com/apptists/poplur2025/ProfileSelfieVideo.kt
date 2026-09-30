package com.apptists.poplur2025

import java.util.Base64

/** A page-recorded profile selfie, decoded from its data URL. */
internal class SelfieVideoData(val mimeType: String, val extension: String, val bytes: ByteArray)

internal object ProfileSelfieVideo {
    /** storage.rules caps profile_selfies below 40 MiB; a ten-second selfie is a few MB. */
    const val MAX_BYTES = 39 * 1024 * 1024

    /**
     * Finds the payload by its ";base64," marker rather than the first comma,
     * because a recorder's type can carry a codec list with a comma in it
     * ("video/webm;codecs=vp8,opus"). Anything that is not a video, not base64,
     * empty or over [maxBytes] is refused.
     */
    fun parse(dataUrl: String, maxBytes: Int = MAX_BYTES): SelfieVideoData? {
        val marker = dataUrl.indexOf(";base64,")
        if (marker <= 0) return null
        val header = dataUrl.substring(0, marker)
        if (!header.startsWith("data:video/")) return null
        val encoded = dataUrl.substring(marker + ";base64,".length)
        if (encoded.length > maxBytes.toLong() * 4 / 3 + 4) return null
        val bytes = runCatching { Base64.getMimeDecoder().decode(encoded) }.getOrNull() ?: return null
        if (bytes.isEmpty() || bytes.size > maxBytes) return null
        val mimeType = header.removePrefix("data:").substringBefore(';').lowercase()
        val extension = when {
            mimeType.endsWith("webm") -> "webm"
            mimeType.endsWith("quicktime") -> "mov"
            else -> "mp4"
        }
        return SelfieVideoData(mimeType, extension, bytes)
    }
}
