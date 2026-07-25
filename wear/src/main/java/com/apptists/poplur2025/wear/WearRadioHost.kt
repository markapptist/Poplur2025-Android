package com.apptists.poplur2025.wear

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.Random

/**
 * The watch's own radio — the Wear twin of the iOS `PoplurWatch` WatchAudio.
 *
 * It does not follow the phone: on start it takes the watch's own location,
 * reverse-geocodes to the local area, and tunes a rotation of local channels,
 * each introduced by the Poplur AI host voice (a British TextToSpeech voice,
 * matching the phone's on-device radio voice) over a ducking radio_bed loop.
 * A watchdog keeps the broadcast moving if a speech segment never reports back.
 * Fully on-device — no network and no companion app required.
 */
class WearRadioHost(private val context: Context) : TextToSpeech.OnInitListener {

    private val main = Handler(Looper.getMainLooper())
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var bed: MediaPlayer? = null

    private var running = false
    private var isStatic = false
    private var didWelcome = false
    private var areaName = "your area"
    private var channels = defaultChannels("your area")
    private var channelIndex = 0

    private var tuneRunnable: Runnable? = null
    private var watchdog: Runnable? = null

    // TV-static noise (press-and-hold easter egg)
    @Volatile private var noisePlaying = false
    private var noiseThread: Thread? = null
    private var noiseTrack: AudioTrack? = null

    private val bedFull = 0.5f
    private val bedDuck = 0.14f

    // MARK: Start / stop

    fun start() {
        if (running) { bed?.start(); return }
        running = true
        if (tts == null) tts = TextToSpeech(context, this)
        startBed()
        requestLocationFix()
        scheduleTuneIn(1400)
    }

    fun stop() {
        running = false
        cancel(tuneRunnable); tuneRunnable = null
        cancel(watchdog); watchdog = null
        runCatching { tts?.stop() }
        runCatching { bed?.pause() }
    }

    fun release() {
        stop()
        stopNoise()
        runCatching { bed?.release() }; bed = null
        runCatching { tts?.shutdown() }; tts = null
    }

    /** Re-attempt a location fix — called once the user grants the permission. */
    fun refreshLocation() { if (running) requestLocationFix() }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val engine = tts ?: return
        val res = engine.setLanguage(Locale.UK)
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.ENGLISH)
        }
        // Prefer a non-network British voice, highest quality available.
        runCatching {
            engine.voices
                ?.filter { it.locale?.language == "en" && it.locale?.country == "GB" && !it.isNetworkConnectionRequired }
                ?.maxByOrNull { it.quality }
                ?.let { engine.voice = it }
        }
        engine.setPitch(0.98f)
        engine.setSpeechRate(0.92f)   // unhurried radio cadence
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { main.post { resumeAfterSegment() } }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { main.post { resumeAfterSegment() } }
            override fun onError(utteranceId: String?, errorCode: Int) { main.post { resumeAfterSegment() } }
        })
        ttsReady = true
    }

    private fun startBed() {
        if (bed == null) {
            bed = runCatching { MediaPlayer.create(context, R.raw.radio_bed) }.getOrNull()?.apply {
                isLooping = true
            }
        }
        bed?.setVolume(bedFull, bedFull)
        runCatching { bed?.start() }
    }

    // MARK: Location → local area

    private fun requestLocationFix() {
        val lm = locationManager ?: return
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return   // denied → keep broadcasting "your area"

        val provider = when {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> LocationManager.PASSIVE_PROVIDER
        }
        try {
            lm.getLastKnownLocation(provider)?.let { reverseGeocode(it) }
            lm.getCurrentLocation(provider, null, context.mainExecutor) { loc ->
                if (loc != null) reverseGeocode(loc)
            }
        } catch (_: SecurityException) {
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun reverseGeocode(loc: Location) {
        val geocoder = runCatching { Geocoder(context, Locale.getDefault()) }.getOrNull() ?: return
        if (Build.VERSION.SDK_INT >= 33) {
            runCatching {
                geocoder.getFromLocation(loc.latitude, loc.longitude, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        addresses.firstOrNull()?.let { onArea(it) }
                    }
                    override fun onError(errorMessage: String?) {}
                })
            }
        } else {
            Thread {
                @Suppress("DEPRECATION")
                val results = runCatching { geocoder.getFromLocation(loc.latitude, loc.longitude, 1) }.getOrNull()
                results?.firstOrNull()?.let { addr -> main.post { onArea(addr) } }
            }.start()
        }
    }

    private fun onArea(addr: Address) {
        val name = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: addr.featureName
        if (!name.isNullOrEmpty()) {
            areaName = name
            channels = defaultChannels(name)
        }
    }

    // MARK: The host cycle

    private fun scheduleTuneIn(delayMs: Long) {
        cancel(tuneRunnable)
        val r = Runnable { tuneIn() }
        tuneRunnable = r
        main.postDelayed(r, delayMs)
    }

    private fun tuneIn() {
        if (!running || isStatic) return
        val channel = channels.getOrElse(channelIndex % channels.size) { "Poplur Radio" }
        val script = if (!didWelcome) {
            didWelcome = true
            "You're locked in to Poplur Radio, live and direct from $areaName. " +
                "Let's hear what's on the air near you."
        } else {
            channelLine(channel, areaName, channelIndex)
        }
        channelIndex++
        speak(script)
    }

    private fun speak(text: String) {
        bed?.setVolume(bedDuck, bedDuck)   // duck the bed under the voice
        if (ttsReady) {
            runCatching { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "poplur-$channelIndex") }
        }
        // Safety net: if the engine never reports back, don't let the broadcast stall.
        cancel(watchdog)
        val estimate = (3500L + text.length * 90L).coerceAtMost(30_000L)
        val wd = Runnable { resumeAfterSegment() }
        watchdog = wd
        main.postDelayed(wd, estimate)
    }

    /** Bring the bed back up and queue the next tune-in — whichever of the engine
     *  callback or the watchdog lands first. */
    private fun resumeAfterSegment() {
        cancel(watchdog); watchdog = null
        if (!running || isStatic) return
        bed?.setVolume(bedFull, bedFull)
        scheduleTuneIn(6500)   // musical breather between segments
    }

    // MARK: Easter egg — press-and-hold TV static (picture + sound)

    fun startStatic() {
        isStatic = true
        runCatching { tts?.stop() }
        cancel(tuneRunnable); tuneRunnable = null
        cancel(watchdog); watchdog = null
        bed?.setVolume(0.05f, 0.05f)
        startNoise()
    }

    fun stopStatic() {
        isStatic = false
        stopNoise()
        bed?.setVolume(bedFull, bedFull)
        if (running) scheduleTuneIn(2000)
    }

    private fun startNoise() {
        if (noisePlaying) return
        val rate = 22_050
        val minBuf = AudioTrack.getMinBufferSize(
            rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(rate)
        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(rate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.getOrNull() ?: return

        track.setVolume(0.75f)
        runCatching { track.play() }
        noiseTrack = track
        noisePlaying = true
        noiseThread = Thread {
            val chunk = ShortArray(1024)
            val rnd = Random()
            while (noisePlaying) {
                for (i in chunk.indices) {
                    chunk[i] = ((rnd.nextInt(65_536) - 32_768) * 0.6).toInt().toShort()
                }
                if (track.write(chunk, 0, chunk.size) < 0) break
            }
        }.also { it.start() }
    }

    private fun stopNoise() {
        noisePlaying = false
        runCatching { noiseThread?.join(200) }
        noiseThread = null
        runCatching { noiseTrack?.stop() }
        runCatching { noiseTrack?.release() }
        noiseTrack = null
    }

    // MARK: Helpers

    private fun cancel(r: Runnable?) { if (r != null) main.removeCallbacks(r) }

    private fun defaultChannels(area: String) = listOf(
        "Poplur $area",
        "$area Live",
        "$area After Dark",
        "The $area Underground",
        "Poplur Radio One"
    )

    private fun channelLine(channel: String, area: String, index: Int): String {
        val lines = listOf(
            "You're tuned to $channel. The signal's strong here in $area tonight.",
            "This is $channel — every post around $area, straight to your wrist.",
            "Picking up $channel now. $area is buzzing on the Poplur dial.",
            "Coming through on $channel, live from the streets of $area.",
            "Stay locked to $channel. More from $area right after this.",
            "$channel has the local frequency — $area, you're on the air.",
            "That's the sound of $channel, broadcasting across $area.",
            "Poplur's got $area covered. You're listening to $channel."
        )
        return lines[((index % lines.size) + lines.size) % lines.size]
    }
}
