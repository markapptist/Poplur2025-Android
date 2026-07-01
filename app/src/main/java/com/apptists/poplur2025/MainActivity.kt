package com.apptists.poplur2025

import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.KeyEvent
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import com.apptists.poplur2025.call.AgoraCallEngine
import com.apptists.poplur2025.ui.CubeSkin
import com.apptists.poplur2025.ui.GdxNavigationFragment
import com.apptists.poplur2025.ui.PoplurRoot
import com.apptists.poplur2025.ui.components.PersistentCubeOverlayConfig
import com.apptists.poplur2025.ui.components.PersistentCubeOverlayHost
import com.apptists.poplur2025.wear.WearRadiusSync
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.badlogic.gdx.backends.android.AndroidFragmentApplication
import kotlin.math.roundToInt

class MainActivity : FragmentActivity(), AndroidFragmentApplication.Callbacks, PersistentCubeOverlayHost {
    private var composeView: ComposeView? = null
    private var cubeContainer: FragmentContainerView? = null
    private val cubeContainerId = R.id.cube_container
    private var lastCubeSkin: CubeSkin = CubeSkin.Gold

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Poplur)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        volumeControlStream = AudioManager.STREAM_MUSIC

        lifecycleScope.launch(Dispatchers.IO) {
            AgoraCallEngine.initialize(this@MainActivity)
        }

        val root = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
        }

        val content = ComposeView(this).apply {
            setContent {
                MaterialTheme {
                    Surface(Modifier.fillMaxSize(), color = Color(0xFF1A1A1A)) {
                        PoplurRoot()
                    }
                }
            }
        }
        composeView = content
        root.addView(
            content,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val overlay = FragmentContainerView(this).apply {
            id = cubeContainerId
            visibility = View.GONE
            isFocusable = false
            isFocusableInTouchMode = false
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            background = null
            clipChildren = false
            clipToPadding = false
        }
        cubeContainer = overlay
        root.addView(
            overlay,
            cubeOverlayLayoutParams()
        )

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        WearRadiusSync.refreshAndSendRadius()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                val direction = if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    AudioManager.ADJUST_RAISE
                } else {
                    AudioManager.ADJUST_LOWER
                }
                val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
                audioManager?.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    direction,
                    AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
                )
                PoplurVolumeEvents.notifyVolumeKeyPressed()
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun exit() {
        // The persistent LibGDX cube is an activity overlay and can be recreated locally.
        // Treat its exit callback as overlay teardown instead of closing the whole app.
    }

    override fun showPersistentCubeOverlay(config: PersistentCubeOverlayConfig) {
        runOnUiThread {
            val container = cubeContainer ?: return@runOnUiThread
            if (container.visibility != View.VISIBLE) {
                container.visibility = View.VISIBLE
                container.bringToFront()
            }
            
            val params = cubeOverlayLayoutParams()
            if (container.layoutParams.width != params.width || container.layoutParams.height != params.height) {
                container.layoutParams = params
            }

            val existing = supportFragmentManager.findFragmentById(cubeContainerId) as? GdxNavigationFragment
            if (existing == null || existing.isRemoving) {
                val fragment = createCubeFragment(config)
                lastCubeSkin = config.cubeSkin
                supportFragmentManager.beginTransaction()
                    .replace(cubeContainerId, fragment)
                    .commitNowAllowingStateLoss()
            } else {
                if (lastCubeSkin != config.cubeSkin) {
                    lastCubeSkin = config.cubeSkin
                    existing.setSkin(config.cubeSkin)
                }
                applyCubeConfig(existing, config)
            }
        }
    }

    override fun hidePersistentCubeOverlay() {
        runOnUiThread {
            cubeContainer?.visibility = View.GONE
            (supportFragmentManager.findFragmentById(cubeContainerId) as? GdxNavigationFragment)?.let { fragment ->
                supportFragmentManager.beginTransaction()
                    .remove(fragment)
                    .commitNowAllowingStateLoss()
            }
        }
    }

    private fun createCubeFragment(config: PersistentCubeOverlayConfig): GdxNavigationFragment {
        return GdxNavigationFragment(
            initialTabIndex = config.selectedTabIndex,
            onVerticalSwipe = if (config.isInteractive) config.onVerticalSwipe else null,
            onLongPress = if (config.isInteractive) config.onLongPress else null,
            onLongPressMove = if (config.isInteractive) config.onLongPressMove else null,
            onLongPressEnd = if (config.isInteractive) config.onLongPressEnd else null,
            onTap = if (config.isInteractive) config.onTap else null,
            onFirstFrameRendered = config.onFirstFrameRendered,
            onTabSelected = if (config.isInteractive) config.onTabSelected else { _: Int -> }
        ).apply {
            setFaceTitles(
                mapTitle = config.mapCubeFaceTitle,
                cameraTitle = config.cameraCubeFaceTitle,
                feedTitle = config.feedCubeFaceTitle,
                profileTitle = config.profileCubeIconTitle
            )
            setInteractionEnabled(config.isInteractive)
            setSkin(config.cubeSkin)
            showChannelObject(config.channelObjectId, config.channelObjectPopToken)
        }
    }

    private fun applyCubeConfig(fragment: GdxNavigationFragment, config: PersistentCubeOverlayConfig) {
        fragment.apply {
            syncSelectedTab(config.selectedTabIndex)
            setFaceTitles(
                mapTitle = config.mapCubeFaceTitle,
                cameraTitle = config.cameraCubeFaceTitle,
                feedTitle = config.feedCubeFaceTitle,
                profileTitle = config.profileCubeIconTitle
            )
            setInteractionEnabled(config.isInteractive)
            setSkin(config.cubeSkin)
            showChannelObject(config.channelObjectId, config.channelObjectPopToken)
            onVerticalSwipe = if (config.isInteractive) config.onVerticalSwipe else null
            onLongPress = if (config.isInteractive) config.onLongPress else null
            onLongPressMove = if (config.isInteractive) config.onLongPressMove else null
            onLongPressEnd = if (config.isInteractive) config.onLongPressEnd else null
            onTap = if (config.isInteractive) config.onTap else null
            onFirstFrameRendered = config.onFirstFrameRendered
            onTabSelected = if (config.isInteractive) config.onTabSelected else null
        }
    }

    private fun cubeOverlayLayoutParams(): FrameLayout.LayoutParams {
        val density = resources.displayMetrics.density
        val overlaySize = (CUBE_OVERLAY_SIZE_DP * density).roundToInt()
        return FrameLayout.LayoutParams(
            overlaySize,
            overlaySize
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = ((CUBE_OVERLAY_CENTER_FROM_BOTTOM_DP - CUBE_OVERLAY_SIZE_DP / 2f) * density).roundToInt()
        }
    }

    override fun onDestroy() {
        composeView?.disposeComposition()
        composeView = null
        cubeContainer = null
        super.onDestroy()
    }
}

private const val CUBE_OVERLAY_SIZE_DP = 280f
private const val CUBE_OVERLAY_CENTER_FROM_BOTTOM_DP = 174f
private const val VOLUME_EVENT_MIN_INTERVAL_MS = 180L

object PoplurVolumeEvents {
    private val listeners = mutableSetOf<() -> Unit>()
    @Volatile private var lastNotifyUptimeMs = 0L

    fun addListener(listener: () -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: () -> Unit) {
        listeners -= listener
    }

    fun notifyVolumeKeyPressed() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastNotifyUptimeMs < VOLUME_EVENT_MIN_INTERVAL_MS) return
        lastNotifyUptimeMs = now
        listeners.toList().forEach { it() }
    }
}
