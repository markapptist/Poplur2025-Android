package com.apptists.poplur2025.performance

import android.content.Context
import android.os.Looper
import android.webkit.WebView
import com.apptists.poplur.performance.PoplurManagedRenderer
import com.apptists.poplur.performance.PoplurPerformanceManager
import com.apptists.poplur.performance.PoplurPerformanceSnapshot
import org.json.JSONObject
import java.lang.ref.WeakReference

/** Treats one Activity's related WebViews as one heavy renderer and pauses decoding/JS when hidden. */
class PoplurWebViewPerformanceController(
    context: Context,
    override val performanceRendererName: String,
) : PoplurManagedRenderer, AutoCloseable {
    private val manager = PoplurPerformanceManager.getInstance(context)
    private val webViews = mutableListOf<WeakReference<WebView>>()
    private var active = false
    private val registration = manager.registerRenderer(this, heavy = true)

    fun attach(vararg views: WebView) {
        webViews.removeAll { it.get() == null }
        views.forEach { view ->
            if (webViews.none { it.get() === view }) webViews += WeakReference(view)
        }
        apply(manager.currentSnapshot(), active)
    }

    fun detach(view: WebView) {
        webViews.removeAll { it.get() == null || it.get() === view }
    }

    fun setVisible(visible: Boolean) {
        registration.setVisible(visible)
    }

    override fun onPoplurPerformanceChanged(snapshot: PoplurPerformanceSnapshot, isActive: Boolean) {
        active = isActive
        apply(snapshot, isActive)
    }

    override fun releaseInactiveResources() {
        if (!active) liveViews().forEach { it.stopLoading() }
    }

    override fun close() {
        registration.close()
        active = false
        apply(manager.currentSnapshot(), false)
        webViews.clear()
    }

    private fun apply(snapshot: PoplurPerformanceSnapshot, isActive: Boolean) {
        val action = {
            liveViews().forEach { view ->
                runCatching {
                    if (isActive) view.onResume() else view.onPause()
                    val detail = JSONObject().apply {
                        put("mode", snapshot.mode.name)
                        put("targetFps", snapshot.targetFps)
                        put("active", isActive)
                        put("particles", snapshot.particlesAllowed)
                        put("particleDensity", snapshot.particleDensityMultiplier.toDouble())
                        put("postProcessing", snapshot.postProcessingAllowed)
                        put("expensiveVideo", snapshot.expensiveVideoTexturesAllowed)
                    }.toString()
                    view.evaluateJavascript(
                        "(function(){" +
                            "document.documentElement.dataset.poplurPerformanceMode=" +
                            JSONObject.quote(snapshot.mode.name.lowercase()) + ";" +
                            "document.documentElement.dataset.poplurThermal=" +
                            JSONObject.quote(snapshot.thermalCondition.name.lowercase()) + ";" +
                            "window.dispatchEvent(new CustomEvent('poplur-performance',{detail:$detail}));" +
                            if (!isActive || !snapshot.expensiveVideoTexturesAllowed) {
                                "document.querySelectorAll('video').forEach(function(v){v.pause();});" +
                                    "window.speechSynthesis&&window.speechSynthesis.cancel();"
                            } else {
                                ""
                            } +
                            "})();true;",
                        null,
                    )
                }
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) action() else liveViews().firstOrNull()?.post(action)
    }

    private fun liveViews(): List<WebView> {
        webViews.removeAll { it.get() == null }
        return webViews.mapNotNull { it.get() }
    }
}
