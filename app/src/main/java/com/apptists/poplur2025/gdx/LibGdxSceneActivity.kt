package com.apptists.poplur2025.gdx

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

class LibGdxSceneActivity : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scene = intent.getStringExtra("scene") ?: "radius"
        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = true
            useCompass = false
            useImmersiveMode = true
        }
        initialize(PoplurGdxScene(scene), config)
    }
}
