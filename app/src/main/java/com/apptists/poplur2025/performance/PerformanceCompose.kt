package com.apptists.poplur2025.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.apptists.poplur.performance.PoplurManagedRenderer
import com.apptists.poplur.performance.PoplurPerformanceManager
import com.apptists.poplur.performance.PoplurPerformanceSnapshot

@Immutable
data class PoplurRendererState(
    val active: Boolean,
    val snapshot: PoplurPerformanceSnapshot,
)

/** Claims a renderer slot for a Composable and releases it deterministically on disposal. */
@Composable
fun rememberPoplurRendererState(
    name: String,
    visible: Boolean = true,
    heavy: Boolean = true,
): PoplurRendererState {
    val context = LocalContext.current
    val manager = remember(context) { PoplurPerformanceManager.getInstance(context) }
    val processSnapshot by manager.state.collectAsState()
    var active by remember(name) { mutableStateOf(false) }
    val renderer = remember(name) {
        object : PoplurManagedRenderer {
            override val performanceRendererName: String = name

            override fun onPoplurPerformanceChanged(
                snapshot: PoplurPerformanceSnapshot,
                isActive: Boolean,
            ) {
                active = isActive
            }
        }
    }
    var registration by remember(manager, renderer, heavy) {
        mutableStateOf<PoplurPerformanceManager.RendererRegistration?>(null)
    }

    SideEffect { registration?.setVisible(visible) }
    DisposableEffect(manager, renderer, heavy) {
        val created = manager.registerRenderer(renderer, heavy)
        registration = created
        created.setVisible(visible)
        onDispose {
            created.close()
            if (registration === created) registration = null
        }
    }
    return PoplurRendererState(active = active, snapshot = processSnapshot)
}
