package com.apptists.poplur2025.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.opengl.GLSurfaceView
import android.graphics.PixelFormat
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidFragmentApplication

class GdxNavigationFragment() : AndroidFragmentApplication() {
    var onTabSelected: ((Int) -> Unit)? = null

    // Secondary constructor for convenient initialization
    constructor(onTabSelected: (Int) -> Unit) : this() {
        this.onTabSelected = onTabSelected
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            r = 8; g = 8; b = 8; a = 8
        }
        
        val adapter = CombinedGdxAdapter { index ->
            onTabSelected?.invoke(index)
        }
        
        val view = initializeForView(adapter, config)
        
        view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        
        if (view is GLSurfaceView) {
            view.setZOrderOnTop(true)
            view.holder.setFormat(PixelFormat.TRANSLUCENT)
        }
        
        return view
    }
}
