package com.apptists.poplur2025.gdx

import android.os.Bundle
import android.view.View
import com.apptists.poplur.performance.PoplurManagedRenderer
import com.apptists.poplur.performance.PoplurPerformanceManager
import com.apptists.poplur.performance.PoplurPerformanceSnapshot
import com.badlogic.gdx.backends.android.AndroidFragmentApplication

/** Lifecycle and one-heavy-renderer enforcement for canonical LibGDX SurfaceView games. */
abstract class PerformanceManagedGdxFragment : AndroidFragmentApplication(), PoplurManagedRenderer {
    private var registration: PoplurPerformanceManager.RendererRegistration? = null
    private var performanceActive = false
    private lateinit var performanceManager: PoplurPerformanceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        performanceManager = PoplurPerformanceManager.getInstance(requireContext())
        registration = performanceManager.registerRenderer(this, heavy = true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyPerformance(performanceManager.currentSnapshot(), performanceActive)
    }

    override fun onResume() {
        super.onResume()
        registration?.setVisible(view?.isShown == true)
    }

    override fun onPause() {
        registration?.setVisible(false)
        super.onPause()
    }

    override fun onDestroy() {
        registration?.close()
        registration = null
        super.onDestroy()
    }

    override fun onPoplurPerformanceChanged(snapshot: PoplurPerformanceSnapshot, isActive: Boolean) {
        performanceActive = isActive
        applyPerformance(snapshot, isActive)
    }

    private fun applyPerformance(snapshot: PoplurPerformanceSnapshot, active: Boolean) {
        runCatching {
            graphics.setForegroundFPS(snapshot.targetFps)
            graphics.setContinuousRendering(active)
            if (active) graphics.requestRendering()
        }
    }
}

