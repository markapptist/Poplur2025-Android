package com.apptists.poplur2025.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.media3.common.Player
import com.apptists.poplur.performance.PoplurPerformanceMode

private class PolicyPauseState {
    var resumeWhenAllowed = false
    var stoppedForPolicy = false
}

/**
 * Gives a video decoder/surface a heavy-renderer slot and stops it immediately while hidden,
 * backgrounded, pre-empted, or under a policy that disables optional video textures.
 */
@Composable
fun ManagePoplurPlayerPerformance(
    player: Player,
    name: String,
    visible: Boolean = true,
    essential: Boolean = false,
) {
    val performance = rememberPoplurRendererState(name, visible = visible, heavy = true)
    val policyPauseState = remember(player) { PolicyPauseState() }
    LaunchedEffect(
        player,
        performance.active,
        performance.snapshot.mode,
        performance.snapshot.expensiveVideoTexturesAllowed,
        visible,
    ) {
        val emergency = performance.snapshot.mode == PoplurPerformanceMode.EMERGENCY
        val playbackAllowed = visible && performance.active && !emergency &&
            (essential || performance.snapshot.expensiveVideoTexturesAllowed)
        if (!playbackAllowed) {
            // Preserve the owning screen's playback intent. The performance layer may suspend a
            // playing decoder, but it must not turn an intentionally paused player into autoplay
            // merely because a renderer slot becomes available again.
            if (player.playWhenReady || player.isPlaying) {
                policyPauseState.resumeWhenAllowed = true
            }
            player.pause()
            // pause() alone keeps codecs, frame queues, and buffered media allocated. stop()
            // releases those heavy resources while preserving the media items and position.
            player.stop()
            policyPauseState.stoppedForPolicy = true
        } else if (policyPauseState.stoppedForPolicy) {
            val resume = policyPauseState.resumeWhenAllowed
            policyPauseState.stoppedForPolicy = false
            policyPauseState.resumeWhenAllowed = false
            player.prepare()
            if (resume) player.play()
        }
    }
}
