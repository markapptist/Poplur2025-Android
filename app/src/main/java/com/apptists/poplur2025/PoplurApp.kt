package com.apptists.poplur2025

import android.app.Application
import coil.Coil
import coil.ImageLoader
import com.apptists.poplur2025.ads.AdManager
import com.apptists.poplur2025.wear.WearRadiusSync
import com.google.firebase.FirebaseApp
import okhttp3.Interceptor
import okhttp3.OkHttpClient

class PoplurApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        AdManager.initialize(this)
        WearRadiusSync.start(this)
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
