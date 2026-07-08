package com.apptists.poplur2025

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import com.apptists.poplur2025.gdx.PoplurFpsFragment
import com.badlogic.gdx.backends.android.AndroidFragmentApplication

/**
 * DEBUG-only entry point that launches straight into the native libGDX PoplurFPS
 * game, bypassing login — used to verify the game in isolation:
 *   adb shell am start -n com.apptists.poplur2025/.FpsDemoActivity
 *
 * Hosts the PoplurFpsFragment directly in the activity's content view (the
 * canonical libGDX GLSurfaceView setup) so there is no opaque overlay competing
 * with the GL surface. Implements AndroidFragmentApplication.Callbacks, the
 * contract the fragment requires (same as MainActivity for the real game path).
 */
class FpsDemoActivity : FragmentActivity(), AndroidFragmentApplication.Callbacks {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = FrameLayout(this).apply { id = View.generateViewId() }
        setContentView(container)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(container.id, PoplurFpsFragment())
                .commit()
        }
    }

    override fun exit() {}
}
