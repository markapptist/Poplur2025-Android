package com.apptists.poplur2025.gdx

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidFragmentApplication

class LibGdxFragment(private val scene: String) : AndroidFragmentApplication() {
    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, saved: android.os.Bundle?): android.view.View? {
        val config = AndroidApplicationConfiguration().apply {
            r = 8; g = 8; b = 8; a = 8
        }
        return initializeForView(PoplurGdxScene(scene), config)
    }
}

@Composable
fun LibGdxSceneView(scene: String, modifier: Modifier = Modifier) {
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { context ->
            val fragment = LibGdxFragment(scene)
            val container = androidx.fragment.app.FragmentContainerView(context).apply {
                id = android.view.View.generateViewId()
            }
            val activity = context as androidx.fragment.app.FragmentActivity
            activity.supportFragmentManager.beginTransaction().replace(container.id, fragment).commit()
            container
        },
        modifier = modifier
    )
}
