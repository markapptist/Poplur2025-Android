package com.apptists.poplur2025.gdx

import android.graphics.PixelFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidFragmentApplication

class ProfileTalkingHeadFragment : AndroidFragmentApplication() {
    private var adapter: ProfileTalkingHeadAdapter? = null
    private var pendingSpeaking = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useImmersiveMode = false
            disableAudio = true
            r = 8
            g = 8
            b = 8
            a = 8
        }
        val talkingHead = ProfileTalkingHeadAdapter().also {
            it.setSpeaking(pendingSpeaking)
        }
        adapter = talkingHead
        val gdxView = initializeForView(talkingHead, config).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            background = null
            isFocusable = false
            isFocusableInTouchMode = false
        }
        if (gdxView is SurfaceView) {
            gdxView.setZOrderMediaOverlay(true)
            gdxView.holder.setFormat(PixelFormat.TRANSLUCENT)
        }

        return FrameLayout(requireContext()).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            clipChildren = false
            clipToPadding = false
            addView(
                gdxView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
    }

    fun setSpeaking(active: Boolean) {
        pendingSpeaking = active
        adapter?.setSpeaking(active)
    }

    override fun onDestroyView() {
        adapter = null
        super.onDestroyView()
    }
}
