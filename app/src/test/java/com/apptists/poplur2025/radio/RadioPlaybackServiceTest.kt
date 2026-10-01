package com.apptists.poplur2025.radio

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class RadioPlaybackServiceTest {
    private lateinit var context: Context

    @Before fun prepare() {
        context = ApplicationProvider.getApplicationContext()
        context.getSystemService(NotificationManager::class.java)
            .deleteNotificationChannel(RadioPlaybackService.CHANNEL_ID)
    }

    @After fun clear() {
        RadioPlaybackService.stop(context)
        RadioPlaybackService.onStopRequested = null
    }

    @Test fun keepsOnlyAnOutsideStationThatIsOnAir() {
        assertTrue(RadioPlaybackService.keepsPlayingOutside("live", "outside"))
        assertTrue(RadioPlaybackService.keepsPlayingOutside("fallback", "outside"))
        assertFalse(RadioPlaybackService.keepsPlayingOutside("muted", "outside"))
        assertFalse(RadioPlaybackService.keepsPlayingOutside("off", "outside"))
        assertFalse(RadioPlaybackService.keepsPlayingOutside("live", "app"))
        assertFalse(RadioPlaybackService.keepsPlayingOutside("live", "screen"))
        assertFalse(RadioPlaybackService.keepsPlayingOutside("", ""))
    }

    @Test fun holdsTheForegroundAsMediaPlaybackWithAStopButton() {
        assertTrue(RadioPlaybackService.start(context))
        val started = shadowOf(context as Application).nextStartedService
        assertEquals(RadioPlaybackService::class.java.name, started.component?.className)

        val controller = Robolectric.buildService(RadioPlaybackService::class.java, started).create().startCommand(0, 1)
        val shadow = shadowOf(controller.get())
        val notification = shadow.lastForegroundNotification
        assertNotNull(notification)
        assertTrue(RadioPlaybackService.running)
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK, controller.get().foregroundServiceType)
        assertEquals(RadioPlaybackService.CHANNEL_ID, notification.channelId)
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertEquals(listOf("Stop"), notification.actions.map { it.title.toString() })
        val channel = context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(RadioPlaybackService.CHANNEL_ID)
        assertEquals(NotificationManager.IMPORTANCE_LOW, channel.importance)
        assertEquals(null, channel.sound)

        controller.destroy()
        assertFalse(RadioPlaybackService.running)
    }

    @Test fun stopAsksThePageAndLeavesAtOnce() {
        var asked = 0
        RadioPlaybackService.onStopRequested = { asked += 1 }
        RadioPlaybackService.start(context)
        val controller = Robolectric.buildService(RadioPlaybackService::class.java).create().startCommand(0, 1)
        assertTrue(RadioPlaybackService.running)

        controller.withIntent(Intent(context, RadioPlaybackService::class.java).setAction(RadioPlaybackService.ACTION_STOP))
            .startCommand(0, 2)
        val shadow = shadowOf(controller.get())
        assertEquals(1, asked)
        assertTrue(shadow.isForegroundStopped)
        assertTrue(shadow.isStoppedBySelf)
        assertFalse(RadioPlaybackService.running)
    }

    @Test fun aStartTheRadioOutranStillRaisesThenDropsTheNotification() {
        RadioPlaybackService.start(context)
        // The page said off before the service got its first command.
        RadioPlaybackService.stop(context)
        val controller = Robolectric.buildService(RadioPlaybackService::class.java).create().startCommand(0, 1)
        val shadow = shadowOf(controller.get())
        // Raised (Android requires it after startForegroundService) and removed again.
        assertEquals(RadioPlaybackService.NOTIFICATION_ID, shadow.lastForegroundNotificationId)
        assertTrue(shadow.isForegroundStopped)
        assertTrue(shadow.isStoppedBySelf)
        assertFalse(RadioPlaybackService.running)
    }

    @Test fun swipingPoplurAwayEndsTheService() {
        RadioPlaybackService.start(context)
        val controller = Robolectric.buildService(RadioPlaybackService::class.java).create().startCommand(0, 1)
        controller.get().onTaskRemoved(Intent())
        assertTrue(shadowOf(controller.get()).isStoppedBySelf)
        assertFalse(RadioPlaybackService.running)
    }
}
