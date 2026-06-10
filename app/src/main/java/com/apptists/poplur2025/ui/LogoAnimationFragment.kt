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

        // Safety for LibGDX multi-instance managed assets
        com.badlogic.gdx.graphics.glutils.ShaderProgram.prependVertexCode = ""
        com.badlogic.gdx.graphics.glutils.ShaderProgram.prependFragmentCode = ""
        
        adapter = LogoAnimationAdapter()
        val view = initializeForView(adapter!!, config)
        
        view.setBackgroundColor(0)
        view.background = null
        view.isClickable = false
        view.isFocusable = false
        
        if (view is GLSurfaceView) {
            view.setZOrderOnTop(true)
            view.holder.setFormat(PixelFormat.TRANSLUCENT)
        }
        if (view is SurfaceView) {
            view.setZOrderOnTop(true)
            view.holder.setFormat(PixelFormat.TRANSLUCENT)
        }
        
        return view
    }
}
