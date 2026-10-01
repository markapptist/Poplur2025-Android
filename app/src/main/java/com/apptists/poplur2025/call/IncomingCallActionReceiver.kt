package com.apptists.poplur2025.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Decline on the ringing notification, without opening Poplur. The call is
 * declined only while it is still ringing, so a late tap cannot end a call that
 * was answered on another device or has already been hung up.
 */
class IncomingCallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != IncomingCallNotifier.ACTION_DECLINE) return
        val call = IncomingCallNotifier.IncomingCall.fromIntent(intent) ?: return
        IncomingCallNotifier.cancel(context, call.callId)
        val pending = goAsync()
        PoplurWebCallSignalling.declineIfRinging(call.callId) { pending.finish() }
    }
}
