package com.apptists.poplur2025

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.FrameDropEffect
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FilterInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Bounded, silent animation for the 58-point map pin. The profile original is untouched. */
@UnstableApi
class MapSelfieVideo(private val context: Context, private val scope: CoroutineScope) {
    private val directory = File(context.cacheDir, "map-selfie-v1")
    private var requestId: String? = null
    private var job: Job? = null

    fun cancel(id: String? = null) {
        if (id != null && requestId != id) return
        job?.cancel()
        job = null
        requestId = null
    }

    fun prepare(source: String, id: String, capture: Uri?, completion: (String?) -> Unit) {
        cancel()
        requestId = id
        job = scope.launch {
            var input: File? = null
            var staging: File? = null
            try {
                withTimeout(60_000) {
                    directory.mkdirs()
                    input = File(directory, "${UUID.randomUUID()}.input")
                    val key = withContext(Dispatchers.IO) {
                        val destination = requireNotNull(input)
                        val connection = if (capture == null && source.startsWith("https://")) {
                            (URL(source).openConnection() as HttpURLConnection).apply {
                                connectTimeout = 15_000
                                readTimeout = 15_000
                                require(responseCode in 200..299 && contentLengthLong <= MAX_BYTES) { "Invalid selfie response" }
                            }
                        } else null
                        try {
                            val stream = when {
                                capture != null -> requireNotNull(context.contentResolver.openInputStream(capture))
                                connection != null -> connection.inputStream
                                source.startsWith("data:video/") && source.length <= MAX_BYTES * 2 -> {
                                    require(source.substringBefore(',').endsWith(";base64"))
                                    ByteArrayInputStream(Base64.decode(source.substringAfter(','), Base64.DEFAULT))
                                }
                                else -> error("Unsupported selfie source")
                            }
                            val digest = MessageDigest.getInstance("SHA-256")
                            stream.use { from -> destination.outputStream().use { to ->
                                val buffer = ByteArray(64 * 1024)
                                var total = 0
                                while (true) {
                                    ensureActive()
                                    val count = from.read(buffer)
                                    if (count < 0) break
                                    total += count
                                    require(total <= MAX_BYTES) { "Selfie is too large" }
                                    digest.update(buffer, 0, count)
                                    to.write(buffer, 0, count)
                                }
                                require(total > 0)
                            } }
                            digest.digest().joinToString("") { "%02x".format(it) }
                        } finally { connection?.disconnect() }
                    }
                    val output = File(directory, "$key.mp4")
                    if (!output.isFile) {
                        staging = File(directory, "${UUID.randomUUID()}.mp4")
                        export(requireNotNull(input), requireNotNull(staging))
                        ensureActive()
                        check(requireNotNull(staging).renameTo(output)) { "Cannot cache selfie" }
                        Log.i("MapSelfie", "Cached 320x320 silent clip (${output.length()} bytes)")
                    }
                    directory.listFiles()?.filter { it != output && it.name.matches(Regex("[a-f0-9]{64}\\.mp4")) }
                        ?.sortedByDescending { it.lastModified() }?.drop(1)?.forEach { it.delete() }
                    completion("https://appassets.androidplatform.net/assets/map-selfie/$key.mp4")
                }
            } catch (error: Exception) {
                if (error !is CancellationException) {
                    Log.w("MapSelfie", "Derivative unavailable: ${error.javaClass.simpleName}")
                    completion(null)
                }
            } finally {
                input?.delete()
                staging?.delete()
                if (requestId == id) { requestId = null; job = null }
            }
        }
    }

    private suspend fun export(input: File, output: File) = suspendCancellableCoroutine<Unit> { continuation ->
        val media = MediaItem.Builder().setUri(Uri.fromFile(input))
            .setClippingConfiguration(MediaItem.ClippingConfiguration.Builder().setEndPositionMs(10_000).build()).build()
        val edited = EditedMediaItem.Builder(media).setRemoveAudio(true)
            .setEffects(Effects(emptyList(), listOf(
                Presentation.createForWidthAndHeight(320, 320, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP),
                FrameDropEffect.createDefaultFrameDropEffect(30f),
            ))).build()
        val transformer = Transformer.Builder(context).setVideoMimeType(MimeTypes.VIDEO_H264)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    if (continuation.isActive) continuation.resume(Unit)
                }
                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    if (continuation.isActive) continuation.resumeWithException(exportException)
                }
            }).build()
        // All callers and lifecycle cancellation run on the activity's main dispatcher.
        continuation.invokeOnCancellation {
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) transformer.cancel()
            else android.os.Handler(android.os.Looper.getMainLooper()).post { transformer.cancel() }
        }
        try { transformer.start(edited, output.path) }
        catch (error: Exception) { if (continuation.isActive) continuation.resumeWithException(error) }
    }

    fun response(request: WebResourceRequest): WebResourceResponse {
        val name = request.url.lastPathSegment.orEmpty()
        val file = File(directory, name)
        if (!name.matches(Regex("[a-f0-9]{64}\\.mp4")) || !file.isFile) {
            return WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", emptyMap(), ByteArrayInputStream(byteArrayOf()))
        }
        val size = file.length()
        val range = request.requestHeaders.entries.firstOrNull { it.key.equals("Range", true) }?.value
        val match = range?.let { Regex("bytes=(\\d*)-(\\d*)").matchEntire(it) }
        val start = if (match?.groupValues?.get(1)?.isEmpty() == true) maxOf(0, size - (match.groupValues[2].toLongOrNull() ?: size))
            else match?.groupValues?.get(1)?.toLongOrNull() ?: 0
        val end = if (match?.groupValues?.get(1)?.isEmpty() == true) size - 1
            else minOf(size - 1, match?.groupValues?.get(2)?.toLongOrNull() ?: (size - 1))
        if (start > end || start >= size) return WebResourceResponse("video/mp4", null, 416, "Range Not Satisfiable",
            mapOf("Content-Range" to "bytes */$size"), ByteArrayInputStream(byteArrayOf()))
        val stream = file.inputStream()
        stream.channel.position(start)
        var remaining = end - start + 1
        val limited = object : FilterInputStream(stream) {
            override fun read(): Int { if (remaining <= 0) return -1; return super.read().also { if (it >= 0) remaining-- } }
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (remaining <= 0) return -1
                return `in`.read(buffer, offset, minOf(length.toLong(), remaining).toInt()).also { if (it > 0) remaining -= it }
            }
        }
        val headers = mutableMapOf("Accept-Ranges" to "bytes", "Content-Length" to remaining.toString(), "Cache-Control" to "no-store")
        if (match != null) headers["Content-Range"] = "bytes $start-$end/$size"
        return WebResourceResponse("video/mp4", null, if (match != null) 206 else 200,
            if (match != null) "Partial Content" else "OK", headers, limited)
    }

    companion object { private const val MAX_BYTES = 12_000_000 }
}
