package com.apptists.poplur2025

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class NotificationSoundTest {
    private lateinit var context: Context
    private lateinit var manager: NotificationManager

    @Before fun prepare() {
        context = ApplicationProvider.getApplicationContext()
        manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.deleteNotificationChannel(PoplurNotifications.CHANNEL_ID)
        manager.deleteNotificationChannel("poplur_alerts")
    }

    @Test fun freshChannelUsesBundledPop() {
        PoplurNotifications.ensureChannel(context)
        val sound = manager.getNotificationChannel(PoplurNotifications.CHANNEL_ID).sound
        assertEquals("android.resource://${context.packageName}/raw/poplur_pop", sound.toString())
        assertTrue(context.resources.openRawResource(R.raw.poplur_pop).use { it.available() } > 0)
    }

    @Test fun migrationPreservesBlockedNotifications() {
        manager.createNotificationChannel(NotificationChannel("poplur_alerts", "Poplur", NotificationManager.IMPORTANCE_NONE))
        PoplurNotifications.ensureChannel(context)
        assertEquals(NotificationManager.IMPORTANCE_NONE, manager.getNotificationChannel(PoplurNotifications.CHANNEL_ID).importance)
    }

    @Test fun migrationPreservesSilentChoice() {
        manager.createNotificationChannel(NotificationChannel("poplur_alerts", "Poplur", NotificationManager.IMPORTANCE_HIGH).apply { setSound(null, null) })
        PoplurNotifications.ensureChannel(context)
        assertNull(manager.getNotificationChannel(PoplurNotifications.CHANNEL_ID).sound)
    }

    @Test @Config(sdk = [28]) fun migrationPreservesCustomSoundBeforeAndroid10() {
        val chosenSound = Uri.parse("content://media/internal/audio/media/12")
        manager.createNotificationChannel(NotificationChannel("poplur_alerts", "Poplur", NotificationManager.IMPORTANCE_HIGH).apply { setSound(chosenSound, null) })
        PoplurNotifications.ensureChannel(context)
        assertEquals(chosenSound, manager.getNotificationChannel(PoplurNotifications.CHANNEL_ID).sound)
    }
}
