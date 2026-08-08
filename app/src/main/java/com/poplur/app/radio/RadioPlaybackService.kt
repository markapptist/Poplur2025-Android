package com.poplur.app.radio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.poplur.app.data.RadioPlaybackMode
import com.poplur.app.data.RadioSettings

class RadioPlaybackService : Service() {
    private var player: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopRadio()
            else -> playRadio()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopRadio()
        super.onDestroy()
    }

    private fun playRadio() {
        val mode = RadioSettings.getMode(this)
        if (mode == RadioPlaybackMode.OUTSIDE_APP) {
            startForeground(NOTIFICATION_ID, notification())
        }

        if (player == null) {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(DEFAULT_STATION_URL)
                setOnPreparedListener { it.start() }
                setOnErrorListener { mp, _, _ ->
                    mp.reset()
                    true
                }
                prepareAsync()
            }
        } else {
            player?.start()
        }
    }

    private fun stopRadio() {
        player?.runCatching {
            stop()
            reset()
            release()
        }
        player = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun notification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Poplur Radio", NotificationManager.IMPORTANCE_LOW)
            )
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Poplur Radio")
            .setContentText("CBC Radio One - Toronto")
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "poplur_radio_playback"
        private const val NOTIFICATION_ID = 9025
        private const val DEFAULT_STATION_URL = "https://playerservices.streamtheworld.com/api/livestream-redirect/CBCR1_TORONTO_HLS.m3u8"
        private const val ACTION_STOP = "com.poplur.app.radio.STOP"

        fun play(context: Context) {
            val intent = Intent(context, RadioPlaybackService::class.java)
            if (RadioSettings.getMode(context) == RadioPlaybackMode.OUTSIDE_APP) {
                androidx.core.content.ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(Intent(context, RadioPlaybackService::class.java).setAction(ACTION_STOP))
        }
    }
}
