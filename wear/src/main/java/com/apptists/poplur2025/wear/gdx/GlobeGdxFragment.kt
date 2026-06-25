package com.apptists.poplur2025.wear.gdx

import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.LifecycleListener
import com.badlogic.gdx.backends.android.AndroidApplicationLogger
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidClipboard
import com.badlogic.gdx.backends.android.AndroidFragmentApplication
import com.badlogic.gdx.backends.android.AndroidGraphics
import com.badlogic.gdx.backends.android.AndroidInput
import com.badlogic.gdx.backends.android.DefaultAndroidInput
import com.badlogic.gdx.backends.android.AndroidNet
import com.badlogic.gdx.backends.android.surfaceview.FillResolutionStrategy
import com.badlogic.gdx.utils.GdxNativesLoader
import com.apptists.poplur2025.ui.TransparentGLTextureView

class GlobeGdxFragment : AndroidFragmentApplication() {
    private var textureView: TransparentGLTextureView? = null
    private var inputView: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useImmersiveMode = false
            useGL30 = true
            disableAudio = true
            r = 8; g = 8; b = 8; a = 8
        }

        val adapter = GlobeGdxAdapter()
        val gdxView = initializeTransparentForView(adapter, config)

        return FrameLayout(requireContext()).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
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
        graphics = AndroidGraphics(
            this,
            config,
            config.resolutionStrategy ?: FillResolutionStrategy()
        )
        val transparentView = TransparentGLTextureView(requireContext()).apply {
            setRenderer(graphics as GLSurfaceView.Renderer)
            setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY)
        }
        textureView = transparentView
        inputView = transparentView
        input = createInput(this, requireActivity(), transparentView, config)
        audio = createAudio(requireActivity(), config)
        files = createFiles()
        net = AndroidNet(this, config)
        this.listener = listener
        handler = Handler()
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

    override fun createInput(
        activity: Application?,
        context: Context?,
        view: Any?,
        config: AndroidApplicationConfiguration?
    ): AndroidInput {
        return DefaultAndroidInput(this, requireActivity(), inputView ?: view, config)
    }

    override fun onPause() {
        textureView?.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        textureView?.onResume()
    }

    override fun onDestroyView() {
        textureView?.onDestroy()
        textureView = null
        inputView = null
        super.onDestroyView()
    }
}
