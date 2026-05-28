package com.apptists.poplur2025

import android.app.Application
import com.google.firebase.FirebaseApp

class PoplurApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        // MobileAds.initialize(this) {}
    }
}
