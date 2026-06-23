package com.apptists.poplur2025.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.apptists.poplur2025.ui.CubeSkin

data class PersistentCubeOverlayConfig(
    val selectedTabIndex: Int,
    val profileCubeIconTitle: String,
    val cubeSkin: CubeSkin,
    val isInteractive: Boolean,
    val onVerticalSwipe: (Boolean) -> Unit,
    val onLongPress: () -> Unit,
    val onLongPressEnd: () -> Unit,
    val onTap: () -> Unit,
    val onTabSelected: (Int) -> Unit,
    val channelObjectId: String,
    val channelObjectPopToken: Int
)

interface PersistentCubeOverlayHost {
    fun showPersistentCubeOverlay(config: PersistentCubeOverlayConfig)
    fun hidePersistentCubeOverlay()
}

@Composable
fun PersistentCubeOverlay(
    modifier: Modifier = Modifier,
    selectedTabIndex: Int = 2,
    profileCubeIconTitle: String = "Profile",
    cubeSkin: CubeSkin = CubeSkin.Default,
    isInteractive: Boolean = true,
    onVerticalSwipe: (Boolean) -> Unit = {},
    onLongPress: () -> Unit = {},
    onLongPressEnd: () -> Unit = {},
    onTap: () -> Unit = {},
    onTabSelected: (Int) -> Unit = {},
    channelObjectId: String = "star",
    channelObjectPopToken: Int = 0
) {
    @Suppress("UNUSED_EXPRESSION")
    modifier

    val host = LocalContext.current as? PersistentCubeOverlayHost
    val config = PersistentCubeOverlayConfig(
        selectedTabIndex = selectedTabIndex,
        profileCubeIconTitle = profileCubeIconTitle,
        cubeSkin = cubeSkin,
        isInteractive = isInteractive,
        onVerticalSwipe = onVerticalSwipe,
        onLongPress = onLongPress,
        onLongPressEnd = onLongPressEnd,
        onTap = onTap,
        onTabSelected = onTabSelected,
        channelObjectId = channelObjectId,
        channelObjectPopToken = channelObjectPopToken
    )

    SideEffect {
        host?.showPersistentCubeOverlay(config)
    }

    DisposableEffect(host) {
        onDispose {
            host?.hidePersistentCubeOverlay()
        }
    }
}
