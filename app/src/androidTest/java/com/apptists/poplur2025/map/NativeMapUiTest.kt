package com.apptists.poplur2025.map

import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.apptists.poplur2025.PostLoginWebActivity
import com.mapbox.maps.MapView
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Checks the shipping map and original web controls together on a real GPU.
 * Launching the home activity preserves the existing account and post data. */
@RunWith(AndroidJUnit4::class)
class NativeMapUiTest {
    @Test fun nativePixelsKeepProductionRailAndTorontoLandmarks() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val evidence = File(context.getExternalFilesDir(null), "native-map-test-evidence").apply { mkdirs() }
        val intent = Intent(context, PostLoginWebActivity::class.java).putExtra("fromAuth", true)
        val scenario = ActivityScenario.launch<PostLoginWebActivity>(intent)
        try {
            lateinit var web: WebView
            scenario.onActivity { activity ->
                web = descendants(activity.window.decorView).filterIsInstance<WebView>().first()
                // Inspector access belongs to the test process, never release startup.
                WebView.setWebContentsDebuggingEnabled(true)
                // Home intentionally opens Feed. Exercise the shipping Map
                // route in the same activity/bridge, without relying on a
                // remembered tab or an advertising interstitial dismissing.
                web.loadUrl(requireNotNull(web.url).replace("view=home", "view=map"))
            }
            fun js(expression: String): String {
                val latch = CountDownLatch(1)
                var result = "null"
                scenario.onActivity { web.evaluateJavascript(expression) { result = it; latch.countDown() } }
                // A navigation may discard evaluations queued for the old
                // document. The bounded readiness loop retries the new one.
                return if (latch.await(3, TimeUnit.SECONDS)) result else "null"
            }
            fun waitFor(label: String, condition: () -> Boolean) {
                val deadline = SystemClock.uptimeMillis() + 90_000
                while (!condition()) {
                    assertTrue("Timed out: $label", SystemClock.uptimeMillis() < deadline)
                    SystemClock.sleep(500)
                }
            }
            val mapWindow = "document.querySelector('iframe[data-poplur-screen=map]')?.contentWindow"
            waitFor("native map loaded") { js("(()=>{const w=$mapWindow;return !!(w?.__poplurMap?.nativeRenderer && w.__poplurMap.loaded())})()") == "true" }
            scenario.onActivity { activity ->
                val maps = descendants(activity.window.decorView).filterIsInstance<MapView>()
                assertEquals("Exactly one native map renderer", 1, maps.size)
                assertTrue("Native map is visible", maps.single().isShown)
                val style = JSONObject(maps.single().mapboxMap.getStyle()!!.styleJSON)
                assertTrue("The clay map disables atmosphere instead of enabling white fog",
                    !style.has("fog") || style.isNull("fog"))
            }
            assertEquals("true", js("(()=>{const w=$mapWindow;return !!w.document.querySelector('[data-map-zoom-rail] [data-zoom-thumb]') && w.document.querySelector('canvas.mapboxgl-canvas').dataset.poplurNativeMap==='true'})()"))
            val initial = js("(()=>{const w=$mapWindow;return JSON.stringify({url:location.href,frameURL:w.location.href,zoom:w.__poplurMap.getZoom(),pitch:w.__poplurMap.getPitch()})})()")
            File(evidence, "initial.json").writeText(JSONTokener(initial).nextValue().toString())
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(File(evidence, "android-production-map.png"))

            // Exercise the production rail's own handler, not a native preview slider.
            js("(()=>{const w=$mapWindow;w.document.querySelector('[data-map-zoom-rail]').dispatchEvent(new w.KeyboardEvent('keydown',{key:'ArrowUp',bubbles:true}));return true})()")
            val initialZoom = JSONObject(JSONTokener(initial).nextValue().toString()).getDouble("zoom")
            waitFor("production zoom rail updates native camera") {
                var zoom = initialZoom
                scenario.onActivity { activity -> zoom = descendants(activity.window.decorView).filterIsInstance<MapView>().single().mapboxMap.cameraState.zoom }
                kotlin.math.abs(zoom - initialZoom) > .05
            }

            js("(()=>{const w=$mapWindow;w.__poplurMap.jumpTo({center:[-79.389,43.6418],zoom:16,pitch:55,bearing:25});return true})()")
            waitFor("CN Tower and SkyDome model sources") {
                var present = false
                scenario.onActivity { activity ->
                    val map = descendants(activity.window.decorView).filterIsInstance<MapView>().single().mapboxMap
                    present = map.styleSourceExists("native-model-source-cn-tower") && map.styleSourceExists("native-model-source-rogers-centre")
                }
                present
            }
            SystemClock.sleep(10_000)
            scenario.onActivity { activity ->
                val map = descendants(activity.window.decorView).filterIsInstance<MapView>().single().mapboxMap
                File(evidence, "toronto-style.json").writeText(map.getStyle()!!.styleJSON)
            }
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(File(evidence, "android-toronto-landmarks.png"))
            val hold = InstrumentationRegistry.getArguments().getString("mapInspectorHoldSeconds")?.toLongOrNull()?.coerceIn(0, 300) ?: 0
            if (hold > 0) SystemClock.sleep(hold * 1000)
        } catch (failure: Throwable) {
            File(evidence, "failure.txt").writeText(failure.stackTraceToString())
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(File(evidence, "android-map-failure.png"))
            val hold = InstrumentationRegistry.getArguments().getString("mapInspectorHoldSeconds")?.toLongOrNull()?.coerceIn(0, 300) ?: 0
            if (hold > 0) SystemClock.sleep(hold * 1000)
            throw failure
        } finally {
            scenario.onActivity { WebView.setWebContentsDebuggingEnabled(false) }
            scenario.close()
        }
    }

    private fun descendants(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()
}
