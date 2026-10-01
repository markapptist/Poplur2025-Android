package com.apptists.poplur2025.radio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.apptists.poplur2025.PostLoginWebActivity
import com.apptists.poplur2025.R

/**
 * "Radio outside app" on Android (A5). The station plays in Home's WebView, as on iOS, and
 * this service plays nothing. It exists because Android freezes or reclaims an app soon
 * after its last screen leaves unless a foreground service says the user is listening, and
 * only a mediaPlayback service may say that. Its ongoing notification is the price, and
 * also the remote: Stop asks the page to stop the station.
 */
class RadioPlaybackService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            onStopRequested?.invoke()
            leave()
            return START_NOT_STICKY
        }
        ensureChannel(this)
        // startForegroundService() obliges startForeground() even when the radio stopped
        // in between, so the notification goes up before a late start is undone.
        val shown = runCatching {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(this), mediaPlaybackType())
        }
        if (shown.isFailure || !wanted) {
            shown.exceptionOrNull()?.let { Log.w(TAG, "Radio could not hold the foreground", it) }
            leave()
            return START_NOT_STICKY
        }
        running = true
        return START_NOT_STICKY
    }

    // Swiping Poplur away closes Home and its WebView, and the station with them.
    override fun onTaskRemoved(rootIntent: Intent?) {
        leave()
    }

    override fun onDestroy() {
        running = false
        super.onDestroy()
    }

    private fun leave() {
        running = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        private const val TAG = "PoplurRadio"
        const val CHANNEL_ID = "poplur_radio_v1"
        const val NOTIFICATION_ID = 4_107
        const val ACTION_STOP = "com.apptists.poplur2025.STOP_RADIO"
        const val ACTION_OPEN = "com.apptists.poplur2025.OPEN_FROM_RADIO"

        /** True once the notification is up, until the service ends. */
        @Volatile
        var running = false
            private set

        @Volatile
        private var wanted = false

        /** Set by the open Home: the notification's Stop asks the page through it. */
        @Volatile
        var onStopRequested: (() -> Unit)? = null

        /** The page's `radioPlayback` edge. iOS keeps a station through backgrounding on
         *  the same rule (PoplurBackgroundResourceFence.preservesOutsideRadio). */
        fun keepsPlayingOutside(state: String, scope: String): Boolean =
            scope == "outside" && (state == "live" || state == "fallback")

        /** Call while Poplur is on screen: Android refuses a foreground start from the
         *  background, so false means the radio will stop when Poplur is left. */
        fun start(context: Context): Boolean {
            wanted = true
            return runCatching {
                ContextCompat.startForegroundService(context, Intent(context, RadioPlaybackService::class.java))
            }.onFailure { Log.w(TAG, "Radio service was not started", it) }.isSuccess
        }

        fun stop(context: Context) {
            wanted = false
            if (running) context.stopService(Intent(context, RadioPlaybackService::class.java))
        }

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Radio", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Shows while the radio plays outside Poplur"
                    setShowBadge(false)
                    setSound(null, null)
                }
            )
        }

        fun notification(context: Context): Notification {
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            // Its own action, so FLAG_UPDATE_CURRENT can never rewrite the extras of a
            // DM or pop notification's PendingIntent into the same activity.
            val open = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                Intent(context, PostLoginWebActivity::class.java)
                    .setAction(ACTION_OPEN)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                flags,
            )
            val stop = PendingIntent.getService(
                context,
                NOTIFICATION_ID + 1,
                Intent(context, RadioPlaybackService::class.java).setAction(ACTION_STOP),
                flags,
            )
            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_poplur)
                .setContentTitle("Poplur radio")
                .setContentText("Playing outside the app")
                .setContentIntent(open)
                .addAction(0, "Stop", stop)
                .setOngoing(true)
                .setSilent(true)
                .setShowWhen(false)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()
        }

        private fun mediaPlaybackType(): Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
    }
}
