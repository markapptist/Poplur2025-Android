package com.apptists.poplur2025

import android.app.Application
import coil.Coil
import coil.ImageLoader
import com.apptists.poplur2025.wear.WearRadiusSync
import com.apptists.poplur.performance.PoplurPerformanceManager
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient

class PoplurApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PoplurPerformanceManager.getInstance(this)
        FirebaseApp.initializeApp(this)
        // Play Integrity is the real provider and the only one release ships. It
        // needs the app distributed through Play, so a sideloaded QA handset can
        // never get a verdict — hence the qa build type, which is release in all
        // but this. Keyed off APPCHECK_DEBUG_PROVIDER rather than BuildConfig.DEBUG
        // so the bypass is a deliberate build choice, false in release, and a debug
        // build still behaves as before.
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            if (BuildConfig.APPCHECK_DEBUG_PROVIDER) {
                DebugAppCheckProviderFactory.getInstance()
            } else {
                PlayIntegrityAppCheckProviderFactory.getInstance()
            }
        )
        AppIconManager.ensureValidSelection(this)
        WearRadiusSync.start(this)
        PoplurNotifications.ensureChannel(this)
        setupImageLoader()
    }

    private fun setupImageLoader() {
        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val req = chain.request()
                val url = req.url.toString()
                val newReq = if ("cartocdn.com" in url || "openstreetmap.org" in url) {
                    req.newBuilder()
                        .header("User-Agent", "Poplur/1.0 Android (apptistinc@gmail.com)")
                        .header("Referer", "https://poplur.app")
                        .build()
                } else {
                    req
                }
                chain.proceed(newReq)
            })
            .build()

        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .okHttpClient(client)
                .build()
        )
    }
}
