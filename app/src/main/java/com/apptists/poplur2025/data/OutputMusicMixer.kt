package com.apptists.poplur2025.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.util.UUID

object OutputMusicMixer {

    // Mux audio from `audioUri` into `videoFile`. If the source is a video file the audio
    // track is extracted automatically. Trims the output to `trimSeconds` when provided.
    suspend fun mixAudio(
        context: Context,
        audioUri: Uri,
        videoFile: File,
        trimSeconds: Double? = null
    ): File? = withContext(Dispatchers.IO) {
        val outFile = File(context.cacheDir, "poplur-mixed-${UUID.randomUUID()}.mp4")
        val videoExtractor = MediaExtractor()
        val audioExtractor = MediaExtractor()
        try {
            videoExtractor.setDataSource(context, Uri.fromFile(videoFile), null)
            audioExtractor.setDataSource(context, audioUri, null)

            val videoTrackIdx = (0 until videoExtractor.trackCount).firstOrNull {
                videoExtractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME, "").startsWith("video/")
            } ?: return@withContext null
            val audioTrackIdx = (0 until audioExtractor.trackCount).firstOrNull {
                audioExtractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME, "").startsWith("audio/")
            } ?: return@withContext null

            val videoFormat = videoExtractor.getTrackFormat(videoTrackIdx)
            val audioFormat = audioExtractor.getTrackFormat(audioTrackIdx)
            val videoDurUs = videoFormat.getLong(MediaFormat.KEY_DURATION)
            val trimUs = trimSeconds?.let { (it * 1_000_000).toLong().coerceAtMost(videoDurUs) } ?: videoDurUs

            videoExtractor.selectTrack(videoTrackIdx)
            audioExtractor.selectTrack(audioTrackIdx)

            val muxer = MediaMuxer(outFile.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxVideo = muxer.addTrack(videoFormat)
            val muxAudio = muxer.addTrack(audioFormat)
            muxer.start()

            val buf = ByteBuffer.allocate(1 shl 20)
            val info = MediaCodec.BufferInfo()

            fun copyTrack(extractor: MediaExtractor, track: Int, limit: Long) {
                while (true) {
                    info.size = extractor.readSampleData(buf, 0)
                    if (info.size < 0) break
                    val ts = extractor.sampleTime
                    if (ts > limit) break
                    info.presentationTimeUs = ts
                    info.flags = extractor.sampleFlags
                    muxer.writeSampleData(track, buf, info)
                    extractor.advance()
                }
            }
            copyTrack(videoExtractor, muxVideo, trimUs)
            copyTrack(audioExtractor, muxAudio, trimUs)

            muxer.stop(); muxer.release()
            outFile
        } catch (e: Exception) {
            outFile.delete(); null
        } finally {
            videoExtractor.release(); audioExtractor.release()
        }
    }

    // Trim a video file to `trimSeconds` without re-encoding.
    suspend fun trimVideo(context: Context, input: File, trimSeconds: Double): File? = withContext(Dispatchers.IO) {
        val out = File(context.cacheDir, "poplur-trim-${UUID.randomUUID()}.mp4")
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(input.path)
            val muxer = MediaMuxer(out.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val trackMap = (0 until extractor.trackCount).associateWith { i ->
                extractor.selectTrack(i)
                muxer.addTrack(extractor.getTrackFormat(i))
            }
            muxer.start()
            val trimUs = (trimSeconds * 1_000_000).toLong()
            val buf = ByteBuffer.allocate(1 shl 20)
            val info = MediaCodec.BufferInfo()
            while (true) {
                info.size = extractor.readSampleData(buf, 0)
                if (info.size < 0) break
                val ts = extractor.sampleTime
                if (ts > trimUs) break
                info.presentationTimeUs = ts
                info.flags = extractor.sampleFlags
                muxer.writeSampleData(trackMap[extractor.sampleTrackIndex]!!, buf, info)
                extractor.advance()
            }
            muxer.stop(); muxer.release()
            out
        } catch (e: Exception) {
            out.delete(); null
        } finally {
            extractor.release()
        }
    }

    // Encode a Bitmap as a silent video then mux in an audio track.
    suspend fun imageWithAudio(
        context: Context,
        bitmap: Bitmap,
        audioUri: Uri,
        duration: Double = 10.0,
        filterId: String = "original"
    ): File? = withContext(Dispatchers.IO) {
        val filtered = applyFilter(bitmap, filterId)
        val silent = encodeImageToVideo(context, filtered, duration) ?: return@withContext null
        val result = mixAudio(context, audioUri, silent, duration)
        silent.delete()
        result
    }

    // Download a remote video URL to cache and return the local File.
    suspend fun downloadVideo(context: Context, url: String): File? = withContext(Dispatchers.IO) {
        val out = File(context.cacheDir, "poplur-dl-${UUID.randomUUID()}.mp4")
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 30_000; conn.readTimeout = 120_000
            conn.inputStream.use { out.outputStream().use { os -> it.copyTo(os) } }
            conn.disconnect()
            out
        } catch (e: Exception) {
            out.delete(); null
        }
    }

    // Apply a color-grade filter to a Bitmap. Returns the same Bitmap if filterId == "original".
    fun applyFilter(bitmap: Bitmap, filterId: String): Bitmap {
        val cm = when (filterId) {
            "pop" -> ColorMatrix().also { it.setSaturation(1.8f) }.let { sat ->
                ColorMatrix(floatArrayOf(
                    1.1f, 0f, 0f, 0f, 20f,
                    0f, 1.0f, 0f, 0f, 0f,
                    0f, 0f, 0.9f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )).also { it.preConcat(sat) }
            }
            "noir" -> ColorMatrix().also { it.setSaturation(0f) }
            "chrome" -> ColorMatrix().also { it.setSaturation(1.4f) }.let { sat ->
                ColorMatrix(floatArrayOf(
                    1.0f, 0f, 0.1f, 0f, 0f,
                    0f, 1.0f, 0.05f, 0f, 0f,
                    0f, 0.05f, 1.1f, 0f, 10f,
                    0f, 0f, 0f, 1f, 0f
                )).also { it.preConcat(sat) }
            }
            "warm" -> ColorMatrix(floatArrayOf(
                1.2f, 0f, 0f, 0f, 20f,
                0f, 1.0f, 0f, 0f, 10f,
                0f, 0f, 0.85f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            ))
            "cool" -> ColorMatrix(floatArrayOf(
                0.85f, 0f, 0f, 0f, -10f,
                0f, 1.0f, 0f, 0f, 5f,
                0f, 0f, 1.2f, 0f, 20f,
                0f, 0f, 0f, 1f, 0f
            ))
            else -> return bitmap
        }
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Canvas(result).drawBitmap(bitmap, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(cm) })
        return result
    }

    // MARK: - Image → Video encoder

    private fun encodeImageToVideo(context: Context, bitmap: Bitmap, durationSecs: Double): File? {
        // Ensure even dimensions (H.264 requirement)
        val w = (bitmap.width.coerceAtMost(1080) / 2) * 2
        val h = (bitmap.height.coerceAtMost(1920) / 2) * 2
        val src = if (w != bitmap.width || h != bitmap.height)
            Bitmap.createScaledBitmap(bitmap, w, h, true) else bitmap

        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        val out = File(context.cacheDir, "poplur-silent-${UUID.randomUUID()}.mp4")
        val fps = 30
        val frameCount = (durationSecs * fps).toInt()

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, w, h).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, 8_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        val muxer = MediaMuxer(out.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxTrack = -1
        var muxStarted = false
        val bufInfo = MediaCodec.BufferInfo()
        var frameIdx = 0
        var inputDone = false
        var outputDone = false

        try {
            while (!outputDone) {
                if (!inputDone) {
                    val idx = encoder.dequeueInputBuffer(10_000)
                    if (idx >= 0) {
                        if (frameIdx >= frameCount) {
                            encoder.queueInputBuffer(idx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            val img = encoder.getInputImage(idx)!!
                            fillYUV(img, pixels, w, h)
                            val pts = frameIdx.toLong() * 1_000_000L / fps
                            encoder.queueInputBuffer(idx, 0, 0, pts, 0)
                            frameIdx++
                        }
                    }
                }

                val outIdx = encoder.dequeueOutputBuffer(bufInfo, 10_000)
                when {
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        muxTrack = muxer.addTrack(encoder.outputFormat)
                        muxer.start(); muxStarted = true
                    }
                    outIdx >= 0 -> {
                        if (muxStarted && bufInfo.size > 0 &&
                            bufInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0
                        ) {
                            muxer.writeSampleData(muxTrack, encoder.getOutputBuffer(outIdx)!!, bufInfo)
                        }
                        encoder.releaseOutputBuffer(outIdx, false)
                        if (bufInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    }
                }
            }
            encoder.stop(); encoder.release()
            muxer.stop(); muxer.release()
            return out
        } catch (e: Exception) {
            try { encoder.stop(); encoder.release() } catch (_: Exception) {}
            try { muxer.stop(); muxer.release() } catch (_: Exception) {}
            out.delete()
            return null
        }
    }

    private fun fillYUV(image: Image, pixels: IntArray, w: Int, h: Int) {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val yStride = yPlane.rowStride
        val uvStride = uPlane.rowStride
        val uvPixel = uPlane.pixelStride
        for (row in 0 until h) {
            for (col in 0 until w) {
                val p = pixels[row * w + col]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                yPlane.buffer.put(row * yStride + col, y.coerceIn(16, 235).toByte())
                if (row % 2 == 0 && col % 2 == 0) {
                    val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    val uvIdx = (row / 2) * uvStride + (col / 2) * uvPixel
                    uPlane.buffer.put(uvIdx, u.coerceIn(16, 240).toByte())
                    vPlane.buffer.put(uvIdx, v.coerceIn(16, 240).toByte())
                }
            }
        }
    }
}
