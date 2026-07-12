package com.apptists.poplur2025.ui.games.screens

import android.content.pm.ActivityInfo
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import com.apptists.poplur2025.gdx.PoplurFpsFragment

/**
 * Native libGDX PoplurFPS game (replaces the old WKWebView build). Hosts a
 * PoplurFpsFragment (AndroidFragmentApplication) inside a FragmentContainerView,
 * mirroring the app's existing Compose→libGDX fragment hosting (see CubePreview).
 */
@Composable
fun GameLaunchScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val containerId = remember { View.generateViewId() }
    val activity = context as? FragmentActivity

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                FragmentContainerView(ctx).apply {
                    id = containerId
                    setBackgroundColor(0xFF000000.toInt())
                    (ctx as? FragmentActivity)?.supportFragmentManager
                        ?.beginTransaction()
                        ?.replace(id, PoplurFpsFragment())
                        ?.commitAllowingStateLoss()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Force landscape while the FPS is on screen (it's a 16:9 game — portrait
        // letterboxes it tiny); restore portrait on exit. Also remove the game
        // fragment so the libGDX GL surface is torn down and the cube can resume.
        DisposableEffect(Unit) {
            // Allow both orientations: portrait = arcade cabinet, landscape = immersive.
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            onDispose {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                val fm = activity?.supportFragmentManager ?: return@onDispose
                fm.findFragmentById(containerId)?.let { frag ->
                    fm.beginTransaction().remove(frag).commitAllowingStateLoss()
                }
            }
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 14.dp, top = 46.dp)
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.42f), CircleShape)
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
        }
    }
}
