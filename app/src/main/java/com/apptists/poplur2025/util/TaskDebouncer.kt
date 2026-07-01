package com.apptists.poplur2025.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TaskDebouncer(
    private val scope: CoroutineScope,
    private val delayMillis: Long = 350L
) {
    private var job: Job? = null

    fun schedule(block: suspend CoroutineScope.() -> Unit) {
        job?.cancel()
        job = scope.launch {
            delay(delayMillis)
            if (isActive) block()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}
