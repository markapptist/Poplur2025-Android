package com.apptists.poplur2025.call

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import androidx.test.core.app.ApplicationProvider
import com.apptists.poplur2025.PostLoginWebActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class IncomingCallNotifierTest {
    private lateinit var context: Context
    private lateinit var manager: NotificationManager
    private val call = IncomingCallNotifier.IncomingCall(
        callId = "call-123",
        callerId = "caller",
        callerName = "Sam",
        video = false,
        context = "default",
    )

    @Before fun prepare() {
        context = ApplicationProvider.getApplicationContext()
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        manager = context.getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel(IncomingCallNotifier.CHANNEL_ID)
        manager.cancelAll()
        IncomingCallNotifier.onCallStopped = null
    }

    private fun posted(): Notification? = shadowOf(manager)
        .getNotification(IncomingCallNotifier.notificationId(call.callId))

    @Test fun theChannelRingsWithTheRingtone() {
        IncomingCallNotifier.ensureChannel(context)
        val channel = manager.getNotificationChannel(IncomingCallNotifier.CHANNEL_ID)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        assertEquals(AudioAttributes.USAGE_NOTIFICATION_RINGTONE, channel.audioAttributes.usage)
        assertTrue(channel.shouldVibrate())
    }

    @Test fun aCallRingsAsACallUntilSomeoneActs() {
        assertTrue(IncomingCallNotifier.show(context, call))
        val notification = posted()
        assertNotNull("a call notification is posted", notification)
        notification!!
        assertEquals(Notification.CATEGORY_CALL, notification.category)
        assertNotNull("rings over the lock screen", notification.fullScreenIntent)
        assertTrue("loops until answered", notification.flags and Notification.FLAG_INSISTENT != 0)
        assertTrue("cannot be swiped away mid-ring", notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertEquals(IncomingCallNotifier.RING_TIMEOUT_MS, notification.timeoutAfter)
        assertEquals("Sam", notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())
        assertEquals("Poplur call", notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString())
        assertEquals("decline and answer", 2, notification.actions?.size ?: 0)
    }

    @Test fun withoutNotificationPermissionNothingRings() {
        shadowOf(context as Application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertEquals(false, IncomingCallNotifier.show(context, call))
        assertNull(posted())
    }

    @Test fun aStoppedCallTakesTheRingDown() {
        IncomingCallNotifier.show(context, call)
        var stopped: String? = null
        IncomingCallNotifier.onCallStopped = { stopped = it }
        IncomingCallNotifier.cancel(context, call.callId)
        assertNull(posted())
        assertEquals(call.callId, stopped)
    }

    @Test fun answerOpensPoplurWithTheCall() {
        val intent = IncomingCallNotifier.answerIntent(context, call)
        assertEquals(IncomingCallNotifier.ACTION_ANSWER, intent.action)
        assertEquals(PostLoginWebActivity::class.java.name, intent.component?.className)
        assertEquals(call, IncomingCallNotifier.IncomingCall.fromIntent(intent))
    }

    @Test fun theServersMessageIsReadDefensively() {
        assertNull(IncomingCallNotifier.IncomingCall.fromData(mapOf("callerName" to "Sam")))
        assertNull(IncomingCallNotifier.IncomingCall.fromData(mapOf("callId" to "x".repeat(201))))
        val parsed = IncomingCallNotifier.IncomingCall.fromData(
            mapOf("callId" to "call-9", "callerId" to "caller", "callerName" to "  ", "video" to "true")
        )
        assertEquals("Someone", parsed?.callerName)
        assertEquals(true, parsed?.video)
        assertEquals("default", parsed?.context)
    }
}
