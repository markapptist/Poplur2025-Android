package com.apptists.poplur2025.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.media3.common.Player
import com.apptists.poplur.performance.PoplurPerformanceMode

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
    LaunchedEffect(
        player,
        performance.active,
        performance.snapshot.mode,
        performance.snapshot.expensiveVideoTexturesAllowed,
        visible,
    ) {
        val emergency = performance.snapshot.mode == PoplurPerformanceMode.EMERGENCY
        val shouldPlay = visible && performance.active && !emergency &&
            (essential || performance.snapshot.expensiveVideoTexturesAllowed)
        player.playWhenReady = shouldPlay
        if (shouldPlay) player.play() else player.pause()
    }
}
