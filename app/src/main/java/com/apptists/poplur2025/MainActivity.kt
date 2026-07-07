package com.apptists.poplur2025

import android.annotation.SuppressLint
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.ComposeView
import com.apptists.poplur2025.call.AgoraCallEngine
import com.apptists.poplur2025.ui.CubeSkin
import com.apptists.poplur2025.ui.GdxNavigationFragment
import com.apptists.poplur2025.ui.PoplurRoot
import com.apptists.poplur2025.ui.ConfettiAnimationFragment
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
    private var confettiContainer: FragmentContainerView? = null
    private val cubeContainerId = R.id.cube_container
    private val confettiContainerId = View.generateViewId()
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
            setBackgroundColor(android.graphics.Color.WHITE)
        }

        // 1. Confetti at the back
        confettiContainer = FragmentContainerView(this).apply {
            id = confettiContainerId
            visibility = View.GONE
            isClickable = false
            isFocusable = false
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
        root.addView(confettiContainer, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // 2. Cube in the middle
        cubeContainer = FragmentContainerView(this).apply {
            id = cubeContainerId
            visibility = View.GONE
            isFocusable = false
            isFocusableInTouchMode = false
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            background = null
            clipChildren = false
            clipToPadding = false
        }
        root.addView(cubeContainer, cubeOverlayLayoutParams())

        // 3. Compose on top
        composeView = ComposeView(this).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setContent {
                MaterialTheme(
                    shapes = Shapes(
                        extraSmall = RoundedCornerShape(10.dp),
                        small = RoundedCornerShape(14.dp),
                        medium = RoundedCornerShape(18.dp),
                        large = RoundedCornerShape(22.dp),
                        extraLarge = RoundedCornerShape(28.dp)
                    ),
                    typography = Typography(
                        bodyLarge = TextStyle(fontWeight = FontWeight.SemiBold),
                        bodyMedium = TextStyle(fontWeight = FontWeight.SemiBold),
                        bodySmall = TextStyle(fontWeight = FontWeight.Medium),
                        titleLarge = TextStyle(fontWeight = FontWeight.Bold),
                        titleMedium = TextStyle(fontWeight = FontWeight.Bold),
                        titleSmall = TextStyle(fontWeight = FontWeight.SemiBold),
                        labelLarge = TextStyle(fontWeight = FontWeight.SemiBold),
                        labelMedium = TextStyle(fontWeight = FontWeight.Medium),
                        labelSmall = TextStyle(fontWeight = FontWeight.Medium),
                        headlineLarge = TextStyle(fontWeight = FontWeight.ExtraBold),
                        headlineMedium = TextStyle(fontWeight = FontWeight.Bold),
                        headlineSmall = TextStyle(fontWeight = FontWeight.Bold),
                    )
                ) {
                    Surface(Modifier.fillMaxSize(), color = Color.Transparent) {
                        PoplurRoot()
                    }
                }
            }
        }
        root.addView(composeView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // Set the Z-order explicitly and DON'T change it dynamically in show/hide
        confettiContainer?.translationZ = 12f // Fireworks/Confetti in front of everything including Cube
        cubeContainer?.translationZ = 10f    // Cube in front of UI
        composeView?.translationZ = 2f       // UI at the base layer

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        WearRadiusSync.refreshAndSendRadius()
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if ((event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) || (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            if ((event.action == KeyEvent.ACTION_DOWN) && (event.repeatCount == 0)) {
                val direction = if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    AudioManager.ADJUST_RAISE
                } else {
                    AudioManager.ADJUST_LOWER
                }
                val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
                audioManager?.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    direction,
                    AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE,
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
            }
            
            // Manage interactivity and Z-order
            container.isClickable = config.isInteractive
            container.isFocusable = config.isInteractive
            
            // Interactive cube must be ABOVE Compose to catch touches (translationZ 10 vs 2)
            // Decorative cube must be BELOW Compose to let buttons work (translationZ 1 vs 2)
            container.translationZ = if (config.isInteractive) 10f else 1f
            
            val params = cubeOverlayLayoutParams()
            if (container.layoutParams.width != params.width || container.layoutParams.height != params.height) {
                container.layoutParams = params
            }

            val manager = supportFragmentManager
            val existing = manager.findFragmentById(cubeContainerId) as? GdxNavigationFragment
            
            if (existing == null || existing.isRemoving) {
                if (!manager.isStateSaved) {
                    val fragment = createCubeFragment(config)
                    lastCubeSkin = config.cubeSkin
                    manager.beginTransaction()
                        .replace(cubeContainerId, fragment)
                        .commitAllowingStateLoss()
                }
            } else {
                if (lastCubeSkin != config.cubeSkin) {
                    lastCubeSkin = config.cubeSkin
                    existing.setSkin(config.cubeSkin)
                }
                applyCubeConfig(existing, config)
            }
            
            existing?.setInteractionEnabled(config.isInteractive)
        }
    }

    override fun hidePersistentCubeOverlay() {
        runOnUiThread {
            cubeContainer?.visibility = View.GONE
            val manager = supportFragmentManager
            (manager.findFragmentById(cubeContainerId) as? GdxNavigationFragment)?.let { fragment ->
                if (!manager.isStateSaved && !fragment.isRemoving) {
                    manager.beginTransaction()
                        .remove(fragment)
                        .commitAllowingStateLoss()
                }
            }
        }
    }

    override fun showConfettiOverlay() {
        runOnUiThread {
            val container = confettiContainer ?: return@runOnUiThread
            container.visibility = View.VISIBLE
            val manager = supportFragmentManager
            val existing = manager.findFragmentById(confettiContainerId) as? ConfettiAnimationFragment
            if (existing == null || existing.isRemoving) {
                if (!manager.isStateSaved) {
                    manager.beginTransaction()
                        .replace(confettiContainerId, ConfettiAnimationFragment())
                        .commitAllowingStateLoss()
                }
            } else {
                existing.restartConfetti()
            }
        }
    }

    override fun hideConfettiOverlay() {
        runOnUiThread {
            confettiContainer?.visibility = View.GONE
            val manager = supportFragmentManager
            (manager.findFragmentById(confettiContainerId) as? ConfettiAnimationFragment)?.let { fragment ->
                if (!manager.isStateSaved && !fragment.isRemoving) {
                    manager.beginTransaction()
                        .remove(fragment)
                        .commitAllowingStateLoss()
                }
            }
        }
    }

    override fun setConfettiMuted(muted: Boolean) {
        runOnUiThread {
            val manager = supportFragmentManager
            (manager.findFragmentById(confettiContainerId) as? ConfettiAnimationFragment)?.setMuted(muted)
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
            onTabSelected = if (config.isInteractive) config.onTabSelected else { _: Int -> },
        ).apply {
            setFaceTitles(
                mapTitle = config.mapCubeFaceTitle,
                cameraTitle = config.cameraCubeFaceTitle,
                feedTitle = config.feedCubeFaceTitle,
                profileTitle = config.profileCubeIconTitle,
            )
            setInteractionEnabled(config.isInteractive)
            setSkin(config.cubeSkin)
            setModelAssetPathOverride(config.modelAssetPathOverride)
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
                profileTitle = config.profileCubeIconTitle,
            )
            setInteractionEnabled(config.isInteractive)
            setSkin(config.cubeSkin)
            setModelAssetPathOverride(config.modelAssetPathOverride)
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

private const val CUBE_OVERLAY_SIZE_DP = 200f
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
