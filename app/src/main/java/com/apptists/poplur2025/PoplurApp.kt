package com.apptists.poplur2025

import android.app.Application
import com.apptists.poplur2025.ads.AdManager
import com.google.firebase.FirebaseApp

class PoplurApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        AdManager.initialize(this)
        
        // Globally initialize Maps with a stable renderer for better compatibility
        /*
        MapsInitializer.initialize(this, MapsInitializer.Renderer.LEGACY) { renderer ->
            android.util.Log.d("PoplurApp", "Maps initialized with: $renderer")
        }
        */
    }
}
