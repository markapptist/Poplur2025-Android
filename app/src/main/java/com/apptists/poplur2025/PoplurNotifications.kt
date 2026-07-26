package com.apptists.poplur2025

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat

/**
 * Poplur system notifications. Parity with the iOS behaviour where the clay
 * app icon appears on lock-screen / shade notifications:
 *  - small (status-bar) icon: the monochrome clay-P silhouette (system-tinted),
 *  - large icon: the full-colour clay app icon,
 *  - accent colour: clay pink, tinting the small icon + app-name row.
 *
 * Android's small icon must be a flat silhouette (alpha only), so the colourful
 * clay tile can only ride along as the large icon — that's the platform rule,
 * not a Poplur choice.
 */
object PoplurNotifications {
    const val CHANNEL_ID = "poplur_alerts"
    private const val CHANNEL_NAME = "Poplur Alerts"
    private const val CHANNEL_DESC = "Posts, trends, and activity in your radius"

    /** Clay pink — matches the app-icon border and the web HUD accents. */
    private val ACCENT = 0xFFF63B9A.toInt()

    fun ensureChannel(context: Context) {
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }
            mgr.createNotificationChannel(channel)
        }
    }

    /**
     * Posts a Poplur-branded notification. No-ops (rather than crashing) when the
     * runtime POST_NOTIFICATIONS grant is missing on Android 13+.
     */
    fun post(context: Context, id: Int, title: String, body: String) {
        ensureChannel(context)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_poplur)
            .setLargeIcon(appIconBitmap(context))
            .setColor(ACCENT)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    /**
     * The clay-P app icon (`R.mipmap.ic_launcher`) rendered to a bitmap for the
     * notification large icon. `ic_launcher` is an adaptive icon (anydpi-v26 XML),
     * so BitmapFactory.decodeResource can't read it — we resolve the drawable and
     * paint it onto a bitmap, which correctly composites the adaptive layers.
     * (Note: the `AppIconTheme` clay→ic_launcher_sparkle_pink mapping is stale —
     * sparkle_pink is the OLD four-petal logo, not the clay art — so we point at
     * ic_launcher directly here.)
     */
    private fun appIconBitmap(context: Context): Bitmap? {
        val drawable = ResourcesCompat.getDrawable(
            context.resources, R.mipmap.ic_launcher, context.theme
        ) ?: return null
        if (drawable is BitmapDrawable) {
            drawable.bitmap?.let { return it }
        }
        val size = 192
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bmp
    }
}
