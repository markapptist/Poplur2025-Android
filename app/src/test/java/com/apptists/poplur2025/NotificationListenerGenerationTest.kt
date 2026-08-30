package com.apptists.poplur2025

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationListenerGenerationTest {
    @Test
    fun `only the newest listener may deliver a snapshot`() {
        val generation = NotificationListenerGeneration()
        val first = generation.replace()
        assertTrue(generation.isCurrent(first))

        val reconnect = generation.replace()
        assertFalse(generation.isCurrent(first))
        assertTrue(generation.isCurrent(reconnect))

        generation.replace() // stop/unmount invalidates the active callback
        assertFalse(generation.isCurrent(reconnect))
    }
}
