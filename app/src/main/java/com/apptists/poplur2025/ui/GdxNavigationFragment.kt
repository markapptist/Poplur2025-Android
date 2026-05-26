package com.apptists.poplur2025.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidFragmentApplication

class GdxNavigationFragment(private val onTabSelected: (Int) -> Unit) : AndroidFragmentApplication() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            r = 8; g = 8; b = 8; a = 8
        }
        return initializeForView(CubeNavigationAdapter(onTabSelected), config)
    }
}
