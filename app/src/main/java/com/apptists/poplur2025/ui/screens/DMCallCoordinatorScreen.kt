package com.apptists.poplur2025.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.apptists.poplur2025.call.CallSessionManager

/**
 * Shown when a DM phone or video call button is tapped.
 *
 * Drives [CallSessionManager.simulateLocalCall] to set local outgoing state so the
 * Poplur phone shows "CALLING [name]" on its LCD and plays the ringtone. After the
 * simulated connect (3 s), for video calls [InAppVideoCallScreen] is overlaid on top;
 * for audio calls the phone stays up showing call duration.
 */
@Composable
fun DMCallCoordinatorScreen(
    calleeName: String,
    isVideo: Boolean,
    onDismiss: () -> Unit
) {
    val callState by CallSessionManager.callState.collectAsState()
    val connected = callState is CallSessionManager.CallState.Connected

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

        if (connected && isVideo) {
            InAppVideoCallScreen(
                calleeName = calleeName,
                onEnd = {
                    CallSessionManager.cancelSimulatedCall()
                    onDismiss()
                }
            )
        }
    }
}
