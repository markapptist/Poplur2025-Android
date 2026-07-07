package com.apptists.poplur2025.gdx

import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.opengl.GLSurfaceView
import com.apptists.poplur2025.ui.TransparentGLTextureView
import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.LifecycleListener
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidFragmentApplication
import com.badlogic.gdx.backends.android.AndroidGraphics
import com.badlogic.gdx.backends.android.AndroidApplicationLogger
import com.badlogic.gdx.backends.android.AndroidApplicationBase
import com.badlogic.gdx.backends.android.AndroidNet
import com.badlogic.gdx.backends.android.AndroidClipboard
import com.badlogic.gdx.backends.android.surfaceview.FillResolutionStrategy
import com.badlogic.gdx.backends.android.surfaceview.GLSurfaceView20
import com.badlogic.gdx.backends.android.surfaceview.ResolutionStrategy
import com.badlogic.gdx.utils.GdxNativesLoader

class ProfileTalkingHeadFragment : AndroidFragmentApplication() {
    private var adapter: ProfileTalkingHeadAdapter? = null
    private var pendingSpeaking = false
    private var textureView: TransparentGLTextureView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useImmersiveMode = false
            disableAudio = true
            r = 8; g = 8; b = 8; a = 8
        }
        val talkingHead = ProfileTalkingHeadAdapter().also {
            it.setSpeaking(pendingSpeaking)
        }
        adapter = talkingHead

        val gdxView = initializeTransparentForView(talkingHead, config)

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

    private fun initializeTransparentForView(
        listener: ApplicationListener,
        config: AndroidApplicationConfiguration
    ): View {
        GdxNativesLoader.load()
        setApplicationLogger(AndroidApplicationLogger())
        val graphics = TransparentAndroidGraphics(this, config, config.resolutionStrategy ?: FillResolutionStrategy())
        this.graphics = graphics
        
        val transparentView = TransparentGLTextureView(requireContext()).apply {
            setRenderer(graphics as GLSurfaceView.Renderer, if (config.useGL30) 3 else 2)
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY)
            isFocusable = false
            isFocusableInTouchMode = false
        }

        // Robustly swap fields to prevent LibGDX standard view logic from crashing
        runCatching {
            val graphicsClass = com.badlogic.gdx.backends.android.AndroidGraphics::class.java
            graphicsClass.getDeclaredField("view").apply { isAccessible = true }.set(graphics, transparentView)
            
            // Nulling out these fields ensures AndroidGraphics methods like onPause/onResume/setContinuousRendering
            // don't try to call methods on the standard GLSurfaceViews which might not be fully initialized.
            runCatching { graphicsClass.getDeclaredField("view20").apply { isAccessible = true }.set(graphics, null) }
            runCatching { graphicsClass.getDeclaredField("viewAPI18").apply { isAccessible = true }.set(graphics, null) }
        }

        textureView = transparentView
        input = createInput(this, requireActivity(), transparentView, config)
        audio = createAudio(requireActivity(), config)
        files = createFiles()
        net = AndroidNet(this, config)
        this.listener = listener
        handler = android.os.Handler(Looper.getMainLooper())
        clipboard = AndroidClipboard(requireActivity())

        addLifecycleListener(object : LifecycleListener {
            override fun resume() { audio.resume() }
            override fun pause() { audio.pause() }
            override fun dispose() { audio.dispose() }
        })

        Gdx.app = this
        Gdx.input = input
        Gdx.audio = audio
        Gdx.files = files
        Gdx.graphics = graphics
        Gdx.net = net
        
        return transparentView
    }

    private class TransparentAndroidGraphics(
        private val application: ProfileTalkingHeadFragment,
        config: AndroidApplicationConfiguration,
        resolutionStrategy: ResolutionStrategy
    ) : AndroidGraphics(application, config, resolutionStrategy, false) {
        override fun onResumeGLSurfaceView() {
            application.textureView?.onResume()
        }
        override fun onPauseGLSurfaceView() {
            application.textureView?.onPause()
        }
        override fun setContinuousRendering(isContinuous: Boolean) {
            val mode = if (isContinuous) {
                android.opengl.GLSurfaceView.RENDERMODE_CONTINUOUSLY
            } else {
                android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY
            }
            application.textureView?.setRenderMode(mode)
        }
        override fun requestRendering() {
            application.textureView?.requestRender()
        }
        override fun createGLSurfaceView(
            application: AndroidApplicationBase,
            resolutionStrategy: ResolutionStrategy
        ): GLSurfaceView20 {
            val glesVersion = if (config.useGL30) 3 else 2
            setTargetGlesVersion(glesVersion)
            // Return a dummy GLSurfaceView20 that ignores calls.
            // This prevents a background render thread from starting and avoids crashes if setRenderMode is called.
            return object : GLSurfaceView20(application.context, true, config.depth, config.stencil, resolutionStrategy) {
                override fun setRenderer(renderer: android.opengl.GLSurfaceView.Renderer?) {}
                override fun setRenderMode(renderMode: Int) {}
                override fun onPause() {}
                override fun onResume() {}
            }
        }
        private fun setTargetGlesVersion(version: Int) {
            runCatching {
                val field = GLSurfaceView20::class.java.getDeclaredField("targetGLESVersion")
                field.isAccessible = true
                field.setInt(null, version)
            }
        }
    }

    fun setSpeaking(active: Boolean) {
        pendingSpeaking = active
        adapter?.setSpeaking(active)
    }

    override fun onPause() {
        runCatching {
            super.onPause()
        }.onFailure { error ->
            markFragmentLifecycleCalled()
            if (error !is NullPointerException) throw error
            android.util.Log.w("ProfileTalkingHeadFragment", "Ignoring LibGDX GLSurfaceView pause for transparent TextureView", error)
        }
        textureView?.onPause()
    }

    override fun onResume() {
        runCatching {
            super.onResume()
        }.onFailure { error ->
            markFragmentLifecycleCalled()
            if (error !is NullPointerException) throw error
            android.util.Log.w("ProfileTalkingHeadFragment", "Ignoring LibGDX GLSurfaceView resume for transparent TextureView", error)
        }
        textureView?.onResume()
    }

    private fun markFragmentLifecycleCalled() {
        runCatching {
            val field = androidx.fragment.app.Fragment::class.java.getDeclaredField("mCalled")
            field.isAccessible = true
            field.setBoolean(this, true)
        }
    }

    override fun onDestroyView() {
        textureView?.onDestroy()
        textureView = null
        adapter = null
        super.onDestroyView()
    }
}
