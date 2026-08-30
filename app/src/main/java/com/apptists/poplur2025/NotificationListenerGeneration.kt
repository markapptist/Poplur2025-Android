package com.apptists.poplur2025

import java.util.concurrent.atomic.AtomicLong

/** Rejects callbacks already queued by a replaced notification listener. */
internal class NotificationListenerGeneration {
    private val value = AtomicLong(0)

    fun replace(): Long = value.incrementAndGet()

    fun isCurrent(candidate: Long): Boolean = value.get() == candidate
}
