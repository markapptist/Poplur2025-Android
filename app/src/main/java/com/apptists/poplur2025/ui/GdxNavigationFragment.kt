package com.apptists.poplur2025.ui

import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.graphics.PixelFormat
import android.content.res.Configuration
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
import com.badlogic.gdx.backends.android.AndroidApplicationBase
import com.badlogic.gdx.backends.android.DefaultAndroidInput
import com.badlogic.gdx.backends.android.AndroidNet
import com.badlogic.gdx.backends.android.surfaceview.FillResolutionStrategy
import com.badlogic.gdx.backends.android.surfaceview.GLSurfaceView20
import com.badlogic.gdx.backends.android.surfaceview.ResolutionStrategy
import com.badlogic.gdx.utils.GdxNativesLoader

open class GdxNavigationFragment() : AndroidFragmentApplication() {
    var onTabSelected: ((Int) -> Unit)? = null
    var onVerticalSwipe: ((Boolean) -> Unit)? = null
    var onLongPress: (() -> Unit)? = null
    var onLongPressMove: ((Float, Float) -> Unit)? = null
    var onLongPressEnd: (() -> Unit)? = null
    var onTap: (() -> Unit)? = null
    var initialTabIndex: Int = 2
    private var adapter: CombinedGdxAdapter? = null
    private var pendingMapIconTitle: String = "Map"
    private var pendingCameraIconTitle: String = "Camera"
    private var pendingFeedIconTitle: String = "Feed"
    private var pendingProfileIconTitle: String = "Profile"
    private var pendingChannelObjectId: String = "star"
    private var pendingChannelObjectToken: Int = 0
    private var syncedTabIndex: Int = Int.MIN_VALUE
    private var pendingSkin: CubeSkin = CubeSkin.Gold
    private var inputView: View? = null
    private var textureView: TransparentGLTextureView? = null

    constructor(
        initialTabIndex: Int,
        onVerticalSwipe: ((Boolean) -> Unit)? = null,
        onLongPress: (() -> Unit)? = null,
        onLongPressMove: ((Float, Float) -> Unit)? = null,
        onLongPressEnd: (() -> Unit)? = null,
        onTap: (() -> Unit)? = null,
        onTabSelected: (Int) -> Unit
    ) : this() {
        this.initialTabIndex = initialTabIndex
        this.onVerticalSwipe = onVerticalSwipe
        this.onLongPress = onLongPress
        this.onLongPressMove = onLongPressMove
        this.onLongPressEnd = onLongPressEnd
        this.onTap = onTap
        this.onTabSelected = onTabSelected
    }

    fun setProfileIconTitle(title: String) {
        pendingProfileIconTitle = title
        adapter?.profileIconTitle = title
    }

    fun setFaceTitles(
        mapTitle: String,
        cameraTitle: String,
        feedTitle: String,
        profileTitle: String
    ) {
        pendingMapIconTitle = mapTitle.ifBlank { "Map" }
        pendingCameraIconTitle = cameraTitle.ifBlank { "Camera" }
        pendingFeedIconTitle = feedTitle.ifBlank { "Feed" }
        pendingProfileIconTitle = profileTitle.ifBlank { "Profile" }
        adapter?.setFaceTitles(
            mapTitle = pendingMapIconTitle,
            cameraTitle = pendingCameraIconTitle,
            feedTitle = pendingFeedIconTitle,
            profileTitle = pendingProfileIconTitle
        )
    }

    fun setSkin(skin: CubeSkin) {
        pendingSkin = skin
        adapter?.skin = skin
    }

    fun syncSelectedTab(index: Int) {
        if (syncedTabIndex == index) return
        syncedTabIndex = index
        initialTabIndex = index
        adapter?.syncSelectedTab(index)
    }

    fun showChannelObject(objectId: String, token: Int) {
        if (token <= 0 || pendingChannelObjectToken == token) return
        pendingChannelObjectId = objectId
        pendingChannelObjectToken = token
        adapter?.showChannelObject(objectId)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useImmersiveMode = false
            useGL30 = true
            disableAudio = true
            r = 8; g = 8; b = 8; a = 8
        }

        val adapter = CombinedGdxAdapter(
            initialTabIndex,
            onVerticalSwipe = { isUp -> activity?.runOnUiThread { onVerticalSwipe?.invoke(isUp) } },
            onLongPress = { activity?.runOnUiThread { onLongPress?.invoke() } },
            onLongPressMove = { deltaX, deltaY -> activity?.runOnUiThread { onLongPressMove?.invoke(deltaX, deltaY) } },
            onLongPressEnd = { activity?.runOnUiThread { onLongPressEnd?.invoke() } },
            onTap = { activity?.runOnUiThread { onTap?.invoke() } }
        ) { index ->
            activity?.runOnUiThread { onTabSelected?.invoke(index) }
        }
        this.adapter = adapter
        adapter.setFaceTitles(
            mapTitle = pendingMapIconTitle,
            cameraTitle = pendingCameraIconTitle,
            feedTitle = pendingFeedIconTitle,
            profileTitle = pendingProfileIconTitle
        )
        adapter.skin = pendingSkin
        if (pendingChannelObjectToken > 0) adapter.showChannelObject(pendingChannelObjectId)

        val gdxView = initializeTransparentForView(adapter, config)
        gdxView.isFocusable = false
        gdxView.isFocusableInTouchMode = false

        if (gdxView is SurfaceView) {
            gdxView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            gdxView.background = null
            configureTransparentSurface(gdxView)
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
            post { requestLayout() }
        }
    }

    private fun initializeTransparentForView(
        listener: ApplicationListener,
        config: AndroidApplicationConfiguration
    ): View {
        GdxNativesLoader.load()
        setApplicationLogger(AndroidApplicationLogger())
        val graphics = TransparentAndroidGraphics(
            this,
            config,
            config.resolutionStrategy ?: FillResolutionStrategy()
        )
        this.graphics = graphics

        val transparentView = TransparentGLTextureView(requireContext()).apply {
            setRenderer(graphics as GLSurfaceView.Renderer, if (config.useGL30) 3 else 2)
            setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY)
            isFocusable = false
            isFocusableInTouchMode = false
        }

        // Update the graphics object to point to our real view
        runCatching {
            val field = com.badlogic.gdx.backends.android.AndroidGraphics::class.java.getDeclaredField("view")
            field.isAccessible = true
            field.set(graphics, transparentView)
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
            override fun resume() {
                audio.resume()
            }

            override fun pause() {
                audio.pause()
            }

            override fun dispose() {
                audio.dispose()
            }
        })

        Gdx.app = this
        Gdx.input = input
        Gdx.audio = audio
        Gdx.files = files
        Gdx.graphics = graphics
        Gdx.net = net
        createWakeLock(config.useWakelock)
        useImmersiveMode(config.useImmersiveMode)
        if (resources.configuration.keyboard != Configuration.KEYBOARD_NOKEYS) {
            input.setKeyboardAvailable(true)
        }
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

    private class TransparentAndroidGraphics(
        application: GdxNavigationFragment,
        config: AndroidApplicationConfiguration,
        resolutionStrategy: ResolutionStrategy
    ) : AndroidGraphics(application, config, resolutionStrategy, false) {
        override fun createGLSurfaceView(
            application: AndroidApplicationBase,
            resolutionStrategy: ResolutionStrategy
        ): GLSurfaceView20 {
            val glesVersion = if (config.useGL30) 3 else 2
            setTargetGlesVersion(glesVersion)

            // Return a dummy GLSurfaceView20 that ignores setRenderer.
            // This prevents a background render thread from starting.
            return object : GLSurfaceView20(application.context, true, config.depth, config.stencil, resolutionStrategy) {
                override fun setRenderer(renderer: GLSurfaceView.Renderer?) {
                    // Do nothing
                }
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

    companion object {
        private fun configureTransparentSurface(surfaceView: SurfaceView) {
            surfaceView.setZOrderOnTop(true)
            surfaceView.setZOrderMediaOverlay(true)
            surfaceView.holder.setFormat(PixelFormat.TRANSLUCENT)
            surfaceView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            surfaceView.background = null
        }
    }
}
