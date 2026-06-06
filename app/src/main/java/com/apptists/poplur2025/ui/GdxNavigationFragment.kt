package com.apptists.poplur2025.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.opengl.GLSurfaceView
import android.graphics.PixelFormat
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidFragmentApplication

class GdxNavigationFragment() : AndroidFragmentApplication() {
    var onTabSelected: ((Int) -> Unit)? = null
    var onVerticalSwipe: ((Boolean) -> Unit)? = null
    var onLongPress: (() -> Unit)? = null
    var initialTabIndex: Int = 1

    constructor(initialTabIndex: Int, onVerticalSwipe: ((Boolean) -> Unit)? = null, onLongPress: (() -> Unit)? = null, onTabSelected: (Int) -> Unit) : this() {
        this.initialTabIndex = initialTabIndex
        this.onVerticalSwipe = onVerticalSwipe
        this.onLongPress = onLongPress
        this.onTabSelected = onTabSelected
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useImmersiveMode = false
            useGL30 = true
            r = 8; g = 8; b = 8; a = 8
        }
        
        val adapter = CombinedGdxAdapter(initialTabIndex, 
            onVerticalSwipe = { isUp -> activity?.runOnUiThread { onVerticalSwipe?.invoke(isUp) } },
            onLongPress = { activity?.runOnUiThread { onLongPress?.invoke() } }
        ) { index ->
            activity?.runOnUiThread { onTabSelected?.invoke(index) }
        }
        
        val view = initializeForView(adapter, config)
        
        view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        view.background = null
        
        if (view is GLSurfaceView) {
            view.setZOrderMediaOverlay(true)
            view.holder.setFormat(PixelFormat.TRANSLUCENT)
        }
        if (view is SurfaceView) {
            view.setZOrderMediaOverlay(true)
            view.holder.setFormat(PixelFormat.TRANSLUCENT)
        }
        
        return view
    }
}
