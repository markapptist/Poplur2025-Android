package com.apptists.poplur2025.ui.components

import android.content.Context
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.apptists.poplur.performance.PoplurManagedRenderer
import com.apptists.poplur.performance.PoplurPerformanceManager
import com.apptists.poplur.performance.PoplurPerformanceMode
import com.apptists.poplur.performance.PoplurPerformanceSnapshot
import com.google.android.filament.View.AntiAliasing
import com.google.android.filament.View.Dithering
import io.github.sceneview.SceneView

/** SceneView whose Choreographer callback is removed while inactive and whose actual GPU draws
 * are capped to the shared 30/24/15/10 FPS policy while active. */
class PerformanceSceneView(context: Context, isOpaque: Boolean = true) : SceneView(context, isOpaque = isOpaque), PoplurManagedRenderer {
    private val managedLifecycleOwner = object : LifecycleOwner {
        lateinit var registry: LifecycleRegistry
        override val lifecycle: Lifecycle get() = registry
    }
    private val managedLifecycle = LifecycleRegistry(managedLifecycleOwner).also {
        managedLifecycleOwner.registry = it
        it.currentState = Lifecycle.State.CREATED
    }
    private val performanceManager = PoplurPerformanceManager.getInstance(context)
    private var registration: PoplurPerformanceManager.RendererRegistration? = null
    private var active = false
    private var renderingRequested = true

    /** Park an unchanged native scene without retaining a frame callback. */
    fun setRenderingRequested(value: Boolean) {
        if (renderingRequested == value) return
        renderingRequested = value
        syncVisibility()
        if (!value) setPerformanceActive(false)
    }
    private var destroyed = false
    private var targetFrameNanos = performanceManager.currentSnapshot().frameIntervalNanos
    private var lastRenderedFrameNanos = Long.MIN_VALUE

    override val performanceRendererName: String = "PoplurMarkHeadFilament"

    init {
        lifecycle = managedLifecycle
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (registration == null) registration = performanceManager.registerRenderer(this, heavy = true)
        syncVisibility()
    }

    override fun onDetachedFromWindow() {
        registration?.close()
        registration = null
        setPerformanceActive(false)
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        syncVisibility()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        syncVisibility()
    }

    override fun onFrame(frameTimeNanos: Long) {
        if (!active || destroyed) return
        if (lastRenderedFrameNanos != Long.MIN_VALUE &&
            frameTimeNanos - lastRenderedFrameNanos < targetFrameNanos
        ) return
        lastRenderedFrameNanos = frameTimeNanos
        super.onFrame(frameTimeNanos)
    }

    override fun onPoplurPerformanceChanged(snapshot: PoplurPerformanceSnapshot, isActive: Boolean) {
        targetFrameNanos = snapshot.frameIntervalNanos
        runCatching {
            view.isPostProcessingEnabled = snapshot.postProcessingAllowed
            view.setShadowingEnabled(snapshot.dynamicShadowsAllowed)
            view.antiAliasing = if (snapshot.mode == PoplurPerformanceMode.FULL) {
                AntiAliasing.FXAA
            } else {
                AntiAliasing.NONE
            }
            view.dithering = if (snapshot.mode <= PoplurPerformanceMode.REDUCED) {
                Dithering.TEMPORAL
            } else {
                Dithering.NONE
            }
        }
        setPerformanceActive(isActive && renderingRequested)
    }

    override fun destroy() {
        if (destroyed) return
        destroyed = true
        registration?.close()
        registration = null
        managedLifecycle.currentState = Lifecycle.State.DESTROYED
        super.destroy()
    }

    private fun syncVisibility() {
        registration?.setVisible(
            isAttachedToWindow && isShown && windowVisibility == View.VISIBLE && !destroyed && renderingRequested
        )
    }

    private fun setPerformanceActive(value: Boolean) {
        if (destroyed || active == value) return
        active = value
        lastRenderedFrameNanos = Long.MIN_VALUE
        managedLifecycle.currentState = if (value) Lifecycle.State.RESUMED else Lifecycle.State.CREATED
    }
}
