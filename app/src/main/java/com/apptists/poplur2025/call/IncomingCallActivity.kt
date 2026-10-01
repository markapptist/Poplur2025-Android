package com.apptists.poplur2025.call

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The full-screen ringing screen Android shows for a Poplur call when the phone
 * is locked or the screen is off (the call notification's full-screen intent).
 * The notification keeps ringing underneath; this screen closes when the call
 * stops ringing, is answered or is declined.
 */
class IncomingCallActivity : ComponentActivity() {
    private var stopListener: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val incoming = IncomingCallNotifier.IncomingCall.fromIntent(intent)
        if (incoming == null) {
            finish()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        val listener: (String) -> Unit = { stoppedId ->
            if (stoppedId == incoming.callId) runOnUiThread { if (!isFinishing) finish() }
        }
        stopListener = listener
        IncomingCallNotifier.onCallStopped = listener
        setContent {
            IncomingCallScreen(
                callerName = incoming.callerName,
                video = incoming.video,
                onDecline = { decline(incoming) },
                onAnswer = { answer(incoming) },
            )
        }
    }

    override fun onDestroy() {
        if (IncomingCallNotifier.onCallStopped === stopListener) IncomingCallNotifier.onCallStopped = null
        super.onDestroy()
    }

    private fun decline(incoming: IncomingCallNotifier.IncomingCall) {
        IncomingCallNotifier.cancel(this, incoming.callId)
        PoplurWebCallSignalling.declineIfRinging(incoming.callId)
        finish()
    }

    /** The call lives in Poplur's page, which needs the phone unlocked; until the
     *  unlock succeeds the screen and the ring stay. */
    private fun answer(incoming: IncomingCallNotifier.IncomingCall) {
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && keyguard?.isKeyguardLocked == true) {
            keyguard.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() = openCall(incoming)
            })
        } else {
            openCall(incoming)
        }
    }

    private fun openCall(incoming: IncomingCallNotifier.IncomingCall) {
        IncomingCallNotifier.cancel(this, incoming.callId)
        startActivity(IncomingCallNotifier.answerIntent(this, incoming))
        finish()
    }
}

private val Ink = Color(0xFFFFF8FB)
private val Label = Color(0xFF71DFFF)
private val DeclineRed = Color(0xFFD03B3B)
private val AnswerGreen = Color(0xFF0CA30C)

@Composable
internal fun IncomingCallScreen(
    callerName: String,
    video: Boolean,
    onDecline: () -> Unit,
    onAnswer: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1D0818), Color(0xFF08070D)))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 120.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2A1023)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = callerName.trim().firstOrNull()?.uppercase() ?: "?",
                    color = Ink,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = callerName,
                color = Ink,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (video) "POPLUR VIDEO CALL" else "POPLUR CALL",
                color = Label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            CallButton("Decline", Icons.Filled.CallEnd, DeclineRed, onDecline)
            CallButton("Answer", Icons.Filled.Call, AnswerGreen, onAnswer)
        }
    }
}

@Composable
private fun CallButton(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(76.dp)
                .semantics { contentDescription = label },
            colors = IconButtonDefaults.iconButtonColors(containerColor = color, contentColor = Color.White),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(label, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}
