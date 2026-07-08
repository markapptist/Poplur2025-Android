package com.apptists.poplur2025.gdx

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.apptists.poplur2025.gdx.fps.PoplurFpsGdxGame
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidFragmentApplication

/**
 * Hosts the native libGDX PoplurFPS game in its own GL surface (mirrors
 * ShowtimeDashFragment). Runs as an AndroidFragmentApplication so it coexists
 * with the persistent cube overlay's libGDX lifecycle managed by MainActivity.
 */
class PoplurFpsFragment : AndroidFragmentApplication() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useImmersiveMode = true
            useGyroscope = false
        }
        return initializeForView(PoplurFpsGdxGame(), config)
    }
}
