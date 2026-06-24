package com.apptists.poplur2025.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.apptists.poplur2025.call.CallSessionManager

/**
 * Shown when a DM phone or video call button is tapped.
 *
 * Video calls open directly into the in-app FaceTime-style camera surface.
 * Audio calls keep the Poplur phone UI and simulated call state.
 */
@Composable
fun DMCallCoordinatorScreen(
    calleeName: String,
    isVideo: Boolean,
    onDismiss: () -> Unit
) {
    if (isVideo) {
        InAppVideoCallScreen(
            calleeName = calleeName,
            onEnd = onDismiss
        )
        return
    }

    LaunchedEffect(Unit) {
        CallSessionManager.simulateLocalCall(calleeName)
    }

    DisposableEffect(Unit) {
        onDispose {
            CallSessionManager.cancelSimulatedCall()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PopularPhoneCallScreen(
            userName = calleeName,
            onHangUp = {
                CallSessionManager.cancelSimulatedCall()
                onDismiss()
            }
        )
    }
}
