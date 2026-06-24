package com.apptists.poplur2025.ui

import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidFragmentApplication

class ConfettiAnimationFragment : AndroidFragmentApplication() {
    val adapter = ConfettiAnimationAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useImmersiveMode = false
            r = 8
            g = 8
            b = 8
            a = 8
        }

        val view = initializeForView(adapter, config)
        view.setBackgroundColor(0)
        view.background = null
        view.isClickable = false
        view.isFocusable = false
        view.isEnabled = false
        view.setOnTouchListener { _, _ -> false }

        if (view is GLSurfaceView) {
            view.setZOrderOnTop(false)
            view.setZOrderMediaOverlay(false)
            view.holder.setFormat(PixelFormat.TRANSLUCENT)
        }
        if (view is SurfaceView) {
            view.setZOrderOnTop(false)
            view.setZOrderMediaOverlay(false)
            view.holder.setFormat(PixelFormat.TRANSLUCENT)
        }

        return view
    }

    fun restartConfetti() {
        adapter.restart()
    }
}
