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
    private var mediaHeld = false
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
        apply(manager.currentSnapshot(), active)
    }

    fun setVisible(visible: Boolean) {
        registration.setVisible(visible)
    }

    /**
     * "Radio outside app" (A5): while the page has a station on air for listening outside
     * Poplur, going inactive must neither pause the primary view nor cancel its loads,
     * since either can end the stream. The page is still told it is inactive and parks
     * its own drawing; releasing the hold applies the pause it skipped.
     */
    fun holdMedia(held: Boolean) {
        if (mediaHeld == held) return
        mediaHeld = held
        apply(manager.currentSnapshot(), active)
    }

    override fun onPoplurPerformanceChanged(snapshot: PoplurPerformanceSnapshot, isActive: Boolean) {
        active = isActive
        apply(snapshot, isActive)
    }

    override fun releaseInactiveResources() {
        if (!active && !mediaHeld) liveViews().forEach { it.stopLoading() }
    }

    override fun close() {
        registration.close()
        active = false
        apply(manager.currentSnapshot(), false)
        webViews.clear()
    }

    private fun apply(snapshot: PoplurPerformanceSnapshot, isActive: Boolean) {
        val action = {
            val views = liveViews()
            // Multiple WebViews in one Activity are still multiple compositor/decoder surfaces.
            // The first attached view is the primary experience; any loading/decorative siblings
            // stay paused so this controller cannot bypass the process-wide one-heavy rule.
            val activeView = if (isActive) views.firstOrNull() else null
            val heldView = if (mediaHeld) views.firstOrNull() else null
            views.forEach { view ->
                runCatching {
                    val viewActive = view === activeView
                    if (viewActive) view.onResume() else if (view !== heldView) view.onPause()
                    val detail = JSONObject().apply {
                        put("mode", snapshot.mode.name)
                        put("targetFps", snapshot.targetFps)
                        put("active", viewActive)
                        put("particles", snapshot.particlesAllowed)
                        put("particleDensity", snapshot.particleDensityMultiplier.toDouble())
                        put("postProcessing", snapshot.postProcessingAllowed)
                        put("expensiveVideo", snapshot.expensiveVideoTexturesAllowed)
                    }.toString()
                    // Re-announcing a policy the document ALREADY holds is not
                    // free. The web governor answers every `poplur-performance`
                    // by re-deciding the map's building layers, and on the
                    // native map renderer each of those decisions is a style
                    // write the SDK answers by rebuilding the layer — which
                    // blanks the 3D buildings for a frame. Measured on the
                    // iPhone's identical path: one landmark tour carried 124
                    // identical broadcasts in 20 seconds and the buildings
                    // blinked.
                    //
                    // The guard lives in the DOCUMENT, not in this controller,
                    // because a WebView navigation replaces the JavaScript
                    // world: a controller-side memo would suppress the one
                    // broadcast a fresh document has never received. Media
                    // suspension below stays unconditional — new <video>
                    // elements can appear under a policy that has not moved.
                    val policySignature = JSONObject.quote(
                        snapshot.mode.name + "|" + snapshot.thermalCondition.name + "|" + detail,
                    )
                    view.evaluateJavascript(
                        "(function(){" +
                            "var p=" + policySignature + ";" +
                            "if(window.__poplurPerformanceLast!==p){" +
                            "window.__poplurPerformanceLast=p;" +
                            "document.documentElement.dataset.poplurPerformanceMode=" +
                            JSONObject.quote(snapshot.mode.name.lowercase()) + ";" +
                            "document.documentElement.dataset.poplurThermal=" +
                            JSONObject.quote(snapshot.thermalCondition.name.lowercase()) + ";" +
                            "window.dispatchEvent(new CustomEvent('poplur-performance',{detail:$detail}));" +
                            "}" +
                            if (!viewActive || !snapshot.expensiveVideoTexturesAllowed) {
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
