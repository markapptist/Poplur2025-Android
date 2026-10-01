package com.apptists.poplur2025.call

import android.app.Application
import com.google.firebase.Timestamp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

/**
 * The incoming-call listener's query matches every ringing call an account is
 * IN. From 4 Sep the caller's own page received its outgoing call as incoming
 * and declined it as busy, so only the callee may ring.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class IncomingCallsTest {
    private val call = mapOf<String, Any?>(
        "participants" to listOf("callee", "caller"),
        "callerId" to "caller",
        "calleeId" to "callee",
        "status" to "ringing",
        "provider" to "webrtc",
    )

    @Test fun theCalleeRings() {
        assertTrue(IncomingCalls.isIncomingFor(call, "callee"))
    }

    @Test fun theCallerNeverReceivesItsOwnCallAsIncoming() {
        assertFalse(IncomingCalls.isIncomingFor(call, "caller"))
    }

    @Test fun legacyAgoraCallsAreLeftToTheirOwnListener() {
        assertFalse(IncomingCalls.isIncomingFor(call + ("provider" to "agora"), "callee"))
        assertFalse(IncomingCalls.isIncomingFor(call - "provider", "callee"))
    }

    @Test fun aSignedOutListenerMatchesNothing() {
        assertFalse(IncomingCalls.isIncomingFor(call, ""))
    }

    @Test fun aCallFromYourselfIsNeverIncoming() {
        assertFalse(IncomingCalls.isIncomingFor(call + ("callerId" to "callee"), "callee"))
    }

    @Test fun aStaleRingingDocumentDoesNotRingAgain() {
        val now = 1_790_000_000_000L
        assertTrue(IncomingCalls.isFresh(call + ("createdAt" to Timestamp(Date(now - 20_000))), now))
        assertTrue(IncomingCalls.isFresh(call + ("createdAt" to Timestamp(Date(now - IncomingCalls.MAX_RING_AGE_MS))), now))
        assertFalse(IncomingCalls.isFresh(call + ("createdAt" to Timestamp(Date(now - IncomingCalls.MAX_RING_AGE_MS - 1))), now))
        // Still waiting for its server timestamp: brand new.
        assertTrue(IncomingCalls.isFresh(call, now))
    }
}
