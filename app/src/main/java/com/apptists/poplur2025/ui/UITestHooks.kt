package com.apptists.poplur2025.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.apptists.poplur2025.UITestFlags

/**
 * Test-only navigation scaffolding: exposes the same actions the 3D cube's long-press wheel menu
 * triggers as ordinary testTag-marked tap targets, so UI tests can drive navigation directly
 * instead of simulating gestures against a native, non-accessible OpenGL cube view. Only renders
 * when [UITestFlags.hooksEnabled] is set (via an `adb shell am start --ez uiTestHooks true`
 * intent extra); a normal launch never sees this.
 *
 * Fixed-size, non-scrollable boxes on purpose: a scrollable Row drops off-screen children from
 * the uiautomator dump entirely, making tags "randomly" unfindable depending on strip width.
 */
@Composable
fun UITestActionStrip(prefix: String, items: List<Pair<String, () -> Unit>>) {
    if (!UITestFlags.hooksEnabled) return
    Row(modifier = Modifier.padding(1.dp)) {
        items.forEach { (id, action) ->
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .testTag("${prefix}_$id")
                    .clickable { action() }
            )
        }
    }
}

/** A tiny testTag-readable marker so tests can confirm which screen/tab is actually on-screen. */
@Composable
fun UITestScreenMarker(name: String) {
    if (!UITestFlags.hooksEnabled) return
    Text(
        text = name,
        color = Color.Transparent,
        modifier = Modifier.testTag("UITestCurrentScreen_$name")
    )
}
