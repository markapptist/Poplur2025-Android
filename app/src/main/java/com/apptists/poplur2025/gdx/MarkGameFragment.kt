package com.apptists.poplur2025.gdx

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidFragmentApplication
import com.markgame.app.MarkGame

class MarkGameFragment : PerformanceManagedGdxFragment() {
    override val performanceRendererName: String = "PoplurMarkGame"
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = true
            useCompass = false
            useImmersiveMode = false
            useGL30 = true
        }
        return initializeForView(MarkGame(), config)
    }
}
