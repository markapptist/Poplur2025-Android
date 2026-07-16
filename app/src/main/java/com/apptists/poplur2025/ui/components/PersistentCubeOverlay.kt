package com.apptists.poplur2025.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.apptists.poplur2025.ui.CubeSkin

data class PersistentCubeOverlayConfig(
    val selectedTabIndex: Int,
    val mapCubeFaceTitle: String,
    val cameraCubeFaceTitle: String,
    val feedCubeFaceTitle: String,
    val profileCubeIconTitle: String,
    val cubeSkin: CubeSkin,
    val isInteractive: Boolean,
    val onVerticalSwipe: (Boolean) -> Unit,
    val onLongPress: () -> Unit,
    val onLongPressMove: (Float, Float) -> Unit,
    val onLongPressEnd: () -> Unit,
    val onTap: () -> Unit,
    val onTabSelected: (Int) -> Unit,
    val onFirstFrameRendered: () -> Unit,
    val channelObjectId: String,
    val channelObjectPopToken: Int,
    val modelAssetPathOverride: String? = null,
    val overlaySizeDp: Float = 200f,
    val centerVertically: Boolean = false,
    val verticalLiftDp: Float = 0f
)

interface PersistentCubeOverlayHost {
    fun showPersistentCubeOverlay(config: PersistentCubeOverlayConfig)
    fun hidePersistentCubeOverlay()
    fun showConfettiOverlay()
    fun hideConfettiOverlay()
    fun setConfettiMuted(muted: Boolean)
}

@Composable
fun PersistentCubeOverlay(
    modifier: Modifier = Modifier,
    selectedTabIndex: Int = 2,
    mapCubeFaceTitle: String = "Map",
    cameraCubeFaceTitle: String = "Camera",
    feedCubeFaceTitle: String = "Feed",
    profileCubeIconTitle: String = "Profile",
    cubeSkin: CubeSkin = CubeSkin.Default,
    isInteractive: Boolean = true,
    onVerticalSwipe: (Boolean) -> Unit = {},
    onLongPress: () -> Unit = {},
    onLongPressMove: (Float, Float) -> Unit = { _, _ -> },
    onLongPressEnd: () -> Unit = {},
    onTap: () -> Unit = {},
    onTabSelected: (Int) -> Unit = {},
    onFirstFrameRendered: () -> Unit = {},
    channelObjectId: String = "star",
    channelObjectPopToken: Int = 0,
    modelAssetPathOverride: String? = null,
    overlaySizeDp: Float = 200f,
    centerVertically: Boolean = false,
    verticalLiftDp: Float = 0f
) {
    @Suppress("UNUSED_EXPRESSION")
    modifier

    val host = LocalContext.current as? PersistentCubeOverlayHost
    val config = PersistentCubeOverlayConfig(
        selectedTabIndex = selectedTabIndex,
        mapCubeFaceTitle = mapCubeFaceTitle,
        cameraCubeFaceTitle = cameraCubeFaceTitle,
        feedCubeFaceTitle = feedCubeFaceTitle,
        profileCubeIconTitle = profileCubeIconTitle,
        cubeSkin = cubeSkin,
        isInteractive = isInteractive,
        onVerticalSwipe = onVerticalSwipe,
        onLongPress = onLongPress,
        onLongPressMove = onLongPressMove,
        onLongPressEnd = onLongPressEnd,
        onTap = onTap,
        onTabSelected = onTabSelected,
        onFirstFrameRendered = onFirstFrameRendered,
        channelObjectId = channelObjectId,
        channelObjectPopToken = channelObjectPopToken,
        modelAssetPathOverride = modelAssetPathOverride,
        overlaySizeDp = overlaySizeDp,
        centerVertically = centerVertically,
        verticalLiftDp = verticalLiftDp
    )

    LaunchedEffect(
        selectedTabIndex, mapCubeFaceTitle, cameraCubeFaceTitle, 
        feedCubeFaceTitle, profileCubeIconTitle, cubeSkin, isInteractive,
        channelObjectId, channelObjectPopToken, modelAssetPathOverride,
        overlaySizeDp, centerVertically, verticalLiftDp
    ) {
        host?.showPersistentCubeOverlay(config)
    }

    DisposableEffect(host) {
        onDispose {
            host?.hidePersistentCubeOverlay()
        }
    }
}
