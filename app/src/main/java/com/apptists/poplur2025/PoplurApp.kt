package com.apptists.poplur2025

import android.app.Application

class PoplurApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // FirebaseApp.initializeApp(this) 
        
        // Globally initialize Maps with a stable renderer for better compatibility
        /*
        MapsInitializer.initialize(this, MapsInitializer.Renderer.LEGACY) { renderer ->
            android.util.Log.d("PoplurApp", "Maps initialized with: $renderer")
        }
        */
    }
}
