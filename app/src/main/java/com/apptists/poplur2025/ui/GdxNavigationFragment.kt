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

open class GdxNavigationFragment() : AndroidFragmentApplication() {
    var onTabSelected: ((Int) -> Unit)? = null
    var onVerticalSwipe: ((Boolean) -> Unit)? = null
    var onLongPress: (() -> Unit)? = null
    var onLongPressEnd: (() -> Unit)? = null
    var onTap: (() -> Unit)? = null
    var initialTabIndex: Int = 1
    private var adapter: CombinedGdxAdapter? = null
    private var pendingProfileIconTitle: String = "Profile"

    constructor(
        initialTabIndex: Int,
        onVerticalSwipe: ((Boolean) -> Unit)? = null,
        onLongPress: (() -> Unit)? = null,
        onLongPressEnd: (() -> Unit)? = null,
        onTap: (() -> Unit)? = null,
        onTabSelected: (Int) -> Unit
    ) : this() {
        this.initialTabIndex = initialTabIndex
        this.onVerticalSwipe = onVerticalSwipe
        this.onLongPress = onLongPress
        this.onLongPressEnd = onLongPressEnd
        this.onTap = onTap
        this.onTabSelected = onTabSelected
    }

    fun setProfileIconTitle(title: String) {
        pendingProfileIconTitle = title
        adapter?.profileIconTitle = title
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useImmersiveMode = false
            useGL30 = true
            disableAudio = false // Navigation cube uses audio? Actually, let's keep it consistent
            r = 8; g = 8; b = 8; a = 8
        }
        
        // Safety for LibGDX multi-instance managed assets
        com.badlogic.gdx.graphics.glutils.ShaderProgram.prependVertexCode = ""
        com.badlogic.gdx.graphics.glutils.ShaderProgram.prependFragmentCode = ""

        
        val adapter = CombinedGdxAdapter(initialTabIndex, 
            onVerticalSwipe = { isUp -> activity?.runOnUiThread { onVerticalSwipe?.invoke(isUp) } },
            onLongPress = { activity?.runOnUiThread { onLongPress?.invoke() } },
            onLongPressEnd = { activity?.runOnUiThread { onLongPressEnd?.invoke() } },
            onTap = { activity?.runOnUiThread { onTap?.invoke() } }
        ) { index ->
            activity?.runOnUiThread { onTabSelected?.invoke(index) }
        }
        this.adapter = adapter
        adapter.profileIconTitle = pendingProfileIconTitle
        
        val gdxView = initializeForView(adapter, config)
        
        gdxView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        gdxView.background = null
        
        if (gdxView is GLSurfaceView) {
            gdxView.setZOrderOnTop(true)
            gdxView.holder.setFormat(PixelFormat.TRANSLUCENT)
        }
        if (gdxView is SurfaceView) {
            gdxView.setZOrderOnTop(true)
            gdxView.holder.setFormat(PixelFormat.TRANSLUCENT)
        }
        
        return gdxView
    }
}
