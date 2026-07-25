package com.apptists.poplur2025

import android.app.ActivityManager
import android.content.Context
import android.webkit.WebView

/**
 * Auto low-end visual tier (measured on the A03s / Mali-G52 MP1: the approved
 * effect stack — full-screen blur, filter chains — runs ~5fps in any build).
 * On 3GB-class devices the web surfaces keep their exact layout and icons but
 * flatten the expensive effects. The home bundle self-detects via
 * navigator.deviceMemory; the pre-login pages (own inline CSS) get the same
 * rules injected here by the host.
 */
object PoplurLiteTier {
    private const val LOW_TIER_MAX_BYTES = 4_200L * 1024L * 1024L

    fun isLowTier(context: Context): Boolean {
        val info = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(info)
        return info.totalMem <= LOW_TIER_MAX_BYTES
    }

    /**
     * Pre-login pages only: throttle requestAnimationFrame to ~15fps. Their
     * full-screen decorative loops (pulsing dotmap canvas, confetti) saturate a
     * Mali-G52 at every attempted frame — throttling turns 250ms chaos into a
     * consistent gentle cadence and frees the main thread for input. The home
     * is NOT throttled (Mapbox needs real frames).
     */
    fun injectRafThrottle(webView: WebView) {
        webView.evaluateJavascript(
            """
            (function () {
              if (window.__poplurRafThrottled) return;
              window.__poplurRafThrottled = true;
              var native = window.requestAnimationFrame.bind(window);
              var last = 0;
              window.requestAnimationFrame = function (cb) {
                return native(function (t) {
                  if (t - last >= 62) { last = t; cb(t); }
                  else window.requestAnimationFrame(cb);
                });
              };
            })();
            """.trimIndent(),
            null
        )
    }

    /** Idempotent: safe to call on every page finish. */
    fun injectLiteCss(webView: WebView) {
        webView.evaluateJavascript(
            """
            (function () {
              if (document.getElementById('poplur-lite-tier')) return;
              var s = document.createElement('style');
              s.id = 'poplur-lite-tier';
              s.textContent = '*,*::before,*::after{backdrop-filter:none!important;-webkit-backdrop-filter:none!important;filter:none!important;text-shadow:none!important}' +
                '[class*="confetti"] > *:nth-child(odd){display:none!important}';
              document.documentElement.appendChild(s);
              document.documentElement.classList.add('tier-low');
            })();
            """.trimIndent(),
            null
        )
    }
}
