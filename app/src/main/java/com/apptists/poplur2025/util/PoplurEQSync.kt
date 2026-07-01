package com.apptists.poplur2025.util

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

object PoplurEQSync {
    enum class Lane {
        MapRadio,
        FeedBumper,
        FeedVideo,
        RainAmbient,
        CityMusic,
        HoldMenu,
        TransientFx
    }

    private val _activeLanes = MutableStateFlow<Set<Lane>>(emptySet())
    val activeLanes: StateFlow<Set<Lane>> = _activeLanes

    private var lastTransientAtMs = 0L
    private var lastMenuTickAtMs = 0L

    fun begin(lane: Lane) {
        _activeLanes.update { lanes -> lanes + lane }
    }

    fun end(lane: Lane) {
        _activeLanes.update { lanes -> lanes - lane }
    }

    fun isVoiceActive(lanes: Set<Lane> = activeLanes.value): Boolean =
        Lane.MapRadio in lanes || Lane.FeedBumper in lanes

    fun rainVolumeMultiplier(lanes: Set<Lane> = activeLanes.value): Float = when {
        isVoiceActive(lanes) -> 0.22f
        Lane.FeedVideo in lanes -> 0.45f
        else -> 1f
    }

    fun cityMusicMultiplier(lanes: Set<Lane> = activeLanes.value): Float = when {
        isVoiceActive(lanes) -> 0.11f
        Lane.RainAmbient in lanes -> 0.72f
        else -> 1f
    }

    fun feedVideoVolumeMultiplier(lanes: Set<Lane> = activeLanes.value): Float =
        if (Lane.FeedBumper in lanes) 0.12f else 1f

    @Synchronized
    fun shouldPlayMenuTick(lanes: Set<Lane> = activeLanes.value): Boolean {
        if (isVoiceActive(lanes)) return false
        val now = SystemClock.elapsedRealtime()
        val minimumIntervalMs = if (Lane.HoldMenu in lanes) 60L else 75L
        if (now - lastMenuTickAtMs < minimumIntervalMs) return false
        lastMenuTickAtMs = now
        return true
    }

    @Synchronized
    fun shouldPlayTransientSound(minimumIntervalMs: Long = 120L, lanes: Set<Lane> = activeLanes.value): Boolean {
        if (isVoiceActive(lanes)) return false
        val now = SystemClock.elapsedRealtime()
        if (now - lastTransientAtMs < minimumIntervalMs) return false
        lastTransientAtMs = now
        return true
    }
}
