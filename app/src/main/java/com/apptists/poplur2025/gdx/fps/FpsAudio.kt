package com.apptists.poplur2025.gdx.fps

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// PoplurFPS procedural audio (libGDX).
// SFX are synthesised at runtime into 16-bit PCM WAVs written to local storage and
// loaded as libGDX Sounds (which support overlapping playback). Mirrors iOS FPSAudio.
class FpsAudio {
    private enum class Wave { SINE, SQUARE, SAW, TRIANGLE }

    private val sr = 22050
    private val sounds = HashMap<String, Sound>()
    var enabled = true

    fun load() {
        try {
            put("shoot0", synth(0.12, 780.0, 220.0, Wave.SQUARE, 0.15, 30.0, 0.5))            // pistol
            put("shoot1", mix(synth(0.24, 320.0, 90.0, Wave.SAW, 0.55, 14.0, 0.55),
                              synth(0.10, 180.0, 60.0, Wave.SINE, 0.7, 24.0, 0.4)))           // shotgun
            put("shoot2", mix(synth(0.18, 520.0, 940.0, Wave.SQUARE, 0.05, 15.0, 0.42),
                              synth(0.18, 260.0, 470.0, Wave.SINE, 0.0, 15.0, 0.25)))         // plasma
            put("shoot3", mix(synth(0.34, 170.0, 55.0, Wave.SAW, 0.7, 8.0, 0.6),
                              synth(0.20, 90.0, 40.0, Wave.SINE, 0.4, 10.0, 0.4)))            // rocket
            put("shoot4", mix(synth(0.34, 1600.0, 300.0, Wave.SINE, 0.08, 9.0, 0.4),
                              synth(0.30, 800.0, 150.0, Wave.SQUARE, 0.05, 11.0, 0.2)))       // railgun
            put("hit", synth(0.09, 420.0, 180.0, Wave.SQUARE, 0.4, 30.0, 0.4))
            put("die", mix(synth(0.42, 320.0, 42.0, Wave.SAW, 0.3, 6.0, 0.5),
                           synth(0.20, 160.0, 60.0, Wave.SQUARE, 0.5, 12.0, 0.3)))
            put("pickup", arp(doubleArrayOf(660.0, 990.0, 1320.0), 0.06, Wave.SQUARE, 0.4))
            put("hurt", mix(synth(0.26, 220.0, 120.0, Wave.SAW, 0.5, 10.0, 0.5),
                            synth(0.12, 140.0, 80.0, Wave.SINE, 0.6, 16.0, 0.3)))
            put("menu", synth(0.08, 900.0, 1350.0, Wave.SQUARE, 0.0, 22.0, 0.4))
            put("step", synth(0.06, 120.0, 70.0, Wave.SINE, 0.6, 40.0, 0.22))
            put("win", arp(doubleArrayOf(523.0, 659.0, 784.0, 1046.0), 0.11, Wave.SQUARE, 0.42))
            put("lose", arp(doubleArrayOf(440.0, 349.0, 262.0), 0.16, Wave.SAW, 0.45))
            put("door", mix(synth(0.5, 90.0, 135.0, Wave.SAW, 0.25, 3.0, 0.34),
                            synth(0.5, 60.0, 70.0, Wave.SINE, 0.1, 3.0, 0.2)))
        } catch (e: Exception) { /* audio is best-effort */ }
    }

    fun play(key: String) {
        if (!enabled) return
        sounds[key]?.play(0.6f)
    }

    fun shoot(weapon: Int) = play("shoot${weapon.coerceIn(0, 4)}")

    private fun put(key: String, samples: FloatArray) {
        val f = Gdx.files.local("poplur_fps_sfx/$key.wav")
        f.writeBytes(wavBytes(samples), false)
        sounds[key] = Gdx.audio.newSound(f)
    }

    // MARK: synthesis

    /** One voice: pitch glides f0→f1 over dur, exponential decay, optional noise blend. */
    private fun synth(dur: Double, f0: Double, f1: Double, wave: Wave,
                      noise: Double, decay: Double, vol: Double): FloatArray {
        val n = max(1, (dur * sr).toInt())
        val out = FloatArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / sr
            val frac = t / dur
            val f = f0 + (f1 - f0) * frac
            phase += f / sr
            val ph = phase % 1.0
            var s = when (wave) {
                Wave.SINE -> sin(2 * PI * ph)
                Wave.SQUARE -> if (ph < 0.5) 1.0 else -1.0
                Wave.SAW -> 2 * ph - 1
                Wave.TRIANGLE -> 4 * abs(ph - 0.5) - 1
            }
            if (noise > 0) s = s * (1 - noise) + Random.nextDouble(-1.0, 1.0) * noise
            val attack = min(1.0, t / 0.003)
            val amp = exp(-t * decay) * attack
            out[i] = max(-1.0, min(1.0, s * amp * vol)).toFloat()
        }
        return out
    }

    private fun arp(freqs: DoubleArray, noteDur: Double, wave: Wave, vol: Double): FloatArray {
        var out = FloatArray(0)
        for (f in freqs) out += synth(noteDur, f, f, wave, 0.0, 9.0, vol)
        return out
    }

    private fun mix(a: FloatArray, b: FloatArray): FloatArray {
        val n = max(a.size, b.size)
        val out = FloatArray(n)
        for (i in 0 until n) {
            val v = (if (i < a.size) a[i] else 0f) + (if (i < b.size) b[i] else 0f)
            out[i] = max(-1f, min(1f, v))
        }
        return out
    }

    private fun wavBytes(samples: FloatArray): ByteArray {
        val dataSize = samples.size * 2
        val buf = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray(Charsets.US_ASCII)); buf.putInt(36 + dataSize)
        buf.put("WAVE".toByteArray(Charsets.US_ASCII))
        buf.put("fmt ".toByteArray(Charsets.US_ASCII)); buf.putInt(16)
        buf.putShort(1); buf.putShort(1)                 // PCM, mono
        buf.putInt(sr); buf.putInt(sr * 2)               // sample rate, byte rate
        buf.putShort(2); buf.putShort(16)                // block align, bits/sample
        buf.put("data".toByteArray(Charsets.US_ASCII)); buf.putInt(dataSize)
        for (s in samples) {
            val v = (max(-1f, min(1f, s)) * 32767f).toInt().toShort()
            buf.putShort(v)
        }
        return buf.array()
    }

    fun dispose() {
        sounds.values.forEach { it.dispose() }
        sounds.clear()
    }
}
