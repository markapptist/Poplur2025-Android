package com.apptists.poplur2025

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.webkit.WebView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Real device location for the web surfaces. The clay map already consumes a
 * native-pushed location (window message `poplur-location`, falling back to
 * Toronto when none arrives) — this supplies it: request the OS permission,
 * read the last known fix, and post it into the home page AND the map iframe
 * (which may mount later, so the injected script polls for it briefly).
 */
object PoplurWebLocation {
    const val REQUEST_CODE = 7401

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun requestPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            REQUEST_CODE
        )
    }

    /** Push the freshest cheap fix into the web. No-op without permission. */
    fun pushLastKnown(webView: WebView) {
        val context = webView.context
        if (!hasPermission(context)) return
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = try {
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
                .filter { lm.allProviders.contains(it) }
                .mapNotNull { lm.getLastKnownLocation(it) }
                .maxByOrNull { it.time }
        } catch (_: SecurityException) { null } ?: return

        val lat = location.latitude
        val lng = location.longitude
        webView.evaluateJavascript(
            """
            (function () {
              var msg = { type: 'poplur-location', latitude: $lat, longitude: $lng };
              window.postMessage(msg, '*');
              window.dispatchEvent(new CustomEvent('poplur-location', { detail: msg }));
              var tries = 0;
              (function send() {
                var f = document.querySelector('iframe[title="Poplur clay map"]');
                if (f && f.contentWindow) { f.contentWindow.postMessage(msg, '*'); }
                if (++tries < 40) setTimeout(send, 500);
              })();
            })();
            """.trimIndent(),
            null
        )
    }
}
