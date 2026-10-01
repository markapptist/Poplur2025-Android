package com.apptists.poplur2025.call

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import com.apptists.poplur2025.PostLoginWebActivity
import com.apptists.poplur2025.R

/**
 * Rings for a Poplur call while Poplur is closed or in the background (A7).
 *
 * The web page owns the call itself (webCall.ts): with Poplur on screen, its
 * own listener rings with the in-app popup. When Poplur is not on screen the
 * server sends a high-priority data message (functions/src/callNotifications.ts)
 * and this draws the phone's incoming-call notification: the system ringtone
 * on loop, Answer and Decline, and a full-screen ringing screen over the lock
 * screen (IncomingCallActivity). Answer opens Poplur straight into the call;
 * Decline declines it without opening the app.
 */
object IncomingCallNotifier {
    const val CHANNEL_ID = "poplur_incoming_calls_v1"
    const val ACTION_ANSWER = "com.apptists.poplur2025.ANSWER_CALL"
    const val ACTION_DECLINE = "com.apptists.poplur2025.DECLINE_CALL"

    /** Matches the caller's no-answer window (webCall.ts OUTGOING_NO_ANSWER_MS). */
    const val RING_TIMEOUT_MS = 45_000L

    private const val EXTRA_CALL_ID = "callId"
    private const val EXTRA_CALLER_ID = "callerId"
    private const val EXTRA_CALLER_NAME = "callerName"
    private const val EXTRA_VIDEO = "video"
    private const val EXTRA_CONTEXT = "context"

    data class IncomingCall(
        val callId: String,
        val callerId: String,
        val callerName: String,
        val video: Boolean,
        val context: String,
    ) {
        fun putInto(intent: Intent): Intent = intent
            .putExtra(EXTRA_CALL_ID, callId)
            .putExtra(EXTRA_CALLER_ID, callerId)
            .putExtra(EXTRA_CALLER_NAME, callerName)
            .putExtra(EXTRA_VIDEO, video)
            .putExtra(EXTRA_CONTEXT, context)

        companion object {
            /** The server's data message, read defensively. */
            fun fromData(data: Map<String, String>): IncomingCall? {
                val callId = data[EXTRA_CALL_ID].orEmpty().trim()
                if (callId.isEmpty() || callId.length > 200) return null
                return IncomingCall(
                    callId = callId,
                    callerId = data[EXTRA_CALLER_ID].orEmpty().take(128),
                    callerName = data[EXTRA_CALLER_NAME].orEmpty().trim().take(80).ifEmpty { "Someone" },
                    video = data[EXTRA_VIDEO] == "true",
                    context = data[EXTRA_CONTEXT].orEmpty().take(40).ifEmpty { "default" },
                )
            }

            fun fromIntent(intent: Intent?): IncomingCall? {
                val callId = intent?.getStringExtra(EXTRA_CALL_ID)?.trim().orEmpty()
                if (callId.isEmpty() || callId.length > 200) return null
                return IncomingCall(
                    callId = callId,
                    callerId = intent?.getStringExtra(EXTRA_CALLER_ID).orEmpty().take(128),
                    callerName = intent?.getStringExtra(EXTRA_CALLER_NAME).orEmpty().take(80).ifEmpty { "Someone" },
                    video = intent?.getBooleanExtra(EXTRA_VIDEO, false) == true,
                    context = intent?.getStringExtra(EXTRA_CONTEXT).orEmpty().take(40).ifEmpty { "default" },
                )
            }
        }
    }

    /** One notification per call, so a cancel for call A never takes down call B. */
    fun notificationId(callId: String): Int = "poplur-call:$callId".hashCode()

    /** Told when a call stops ringing, so an open ringing screen can close. */
    @Volatile
    var onCallStopped: ((String) -> Unit)? = null

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val ringtone = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Incoming calls", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Rings when someone calls you on Poplur"
                // The phone's own ringtone: a call should sound like a call.
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE), ringtone)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 900, 700, 900, 700)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
    }

    /** Draws the ringing notification. False when notifications are not allowed. */
    fun show(context: Context, call: IncomingCall): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        ensureChannel(context)
        val id = notificationId(call.callId)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val answer = PendingIntent.getActivity(context, id + 1, answerIntent(context, call), flags)
        val decline = PendingIntent.getBroadcast(
            context, id + 2,
            call.putInto(Intent(context, IncomingCallActionReceiver::class.java).setAction(ACTION_DECLINE)),
            flags,
        )
        val ringingScreen = PendingIntent.getActivity(
            context, id + 3,
            call.putInto(
                Intent(context, IncomingCallActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            ),
            flags,
        )
        val caller = Person.Builder().setName(call.callerName).setImportant(true).build()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_poplur)
            .setContentTitle(call.callerName)
            .setContentText(if (call.video) "Poplur video call" else "Poplur call")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setTimeoutAfter(RING_TIMEOUT_MS)
            // A call style needs a full-screen intent or a foreground service.
            .setFullScreenIntent(ringingScreen, true)
            .setContentIntent(ringingScreen)
            .setStyle(NotificationCompat.CallStyle.forIncomingCall(caller, decline, answer))
            .build()
        // Keep ringing until it is answered, declined, cancelled or times out.
        notification.flags = notification.flags or Notification.FLAG_INSISTENT
        return runCatching { NotificationManagerCompat.from(context).notify(id, notification) }.isSuccess
    }

    /** Opens Poplur and joins the call (PostLoginWebActivity reads ACTION_ANSWER). */
    fun answerIntent(context: Context, call: IncomingCall): Intent = call.putInto(
        Intent(context, PostLoginWebActivity::class.java)
            .setAction(ACTION_ANSWER)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    )

    /** Takes the ringing notification down and tells an open ringing screen. */
    fun cancel(context: Context, callId: String) {
        if (callId.isBlank()) return
        NotificationManagerCompat.from(context).cancel(notificationId(callId))
        onCallStopped?.invoke(callId)
    }
}
