package com.apptists.poplur2025.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.opengl.GLSurfaceView
import android.graphics.PixelFormat
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidFragmentApplication
import com.apptists.poplur2025.gdx.weather.WeatherOverlayAdapter

class WeatherOverlayFragment : AndroidFragmentApplication() {
    private var weatherAdapter: WeatherOverlayAdapter? = null

    companion object {
        fun newInstance(adapter: WeatherOverlayAdapter): WeatherOverlayFragment {
            val fragment = WeatherOverlayFragment()
            fragment.weatherAdapter = adapter
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useImmersiveMode = false
            r = 8; g = 8; b = 8; a = 8
        }
        
        val adapter = weatherAdapter ?: WeatherOverlayAdapter()
        val view = initializeForView(adapter, config)
        
        view.setBackgroundColor(0)
        view.background = null
        
        if (view is GLSurfaceView) {
            view.setZOrderMediaOverlay(false)
            view.holder.setFormat(PixelFormat.TRANSLUCENT)
        }
        
        return view
    }
}
