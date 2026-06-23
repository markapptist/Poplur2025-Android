package com.apptists.poplur2025.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.opengl.GLSurfaceView
import android.graphics.PixelFormat
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidFragmentApplication

class LogoAnimationFragment : AndroidFragmentApplication() {
    private var adapter: LogoAnimationAdapter? = null

    fun triggerLogoPop() {
        adapter?.triggerLogoPop()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useImmersiveMode = false
            r = 8; g = 8; b = 8; a = 8
        }
        
        adapter = LogoAnimationAdapter()
        val view = initializeForView(adapter!!, config)
        
        view.setBackgroundColor(0)
        view.background = null
        
        if (view is GLSurfaceView) {
            view.setZOrderMediaOverlay(true)
            view.holder.setFormat(PixelFormat.TRANSLUCENT)
        }
        
        return view
    }
}
