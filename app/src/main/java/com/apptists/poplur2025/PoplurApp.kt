package com.apptists.poplur2025

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.android.gms.maps.MapsInitializer

class PoplurApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        
        // Globally initialize Maps with the latest renderer
        MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST) { renderer ->
            android.util.Log.d("PoplurApp", "Maps initialized with: $renderer")
        }
    }
}
