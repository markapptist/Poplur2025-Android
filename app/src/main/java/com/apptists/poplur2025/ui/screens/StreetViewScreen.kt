package com.apptists.poplur2025.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import java.net.URLEncoder

/**
 * Full-screen Mapillary street-level viewer, loaded from a local HTML asset via WebView.
 * Android-only — iOS uses Apple MapKit Look Around instead, never this WebView.
 */
@Composable
fun StreetViewScreen(
    imageId: String,
    accessToken: String,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.let { webView ->
                webView.stopLoading()
                (webView.parent as? ViewGroup)?.removeView(webView)
                webView.destroy()
            }
            webViewRef = null
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .zIndex(2000f)
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.BLACK)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    addJavascriptInterfaceSafely(
                        onLoaded = { isLoading = false },
                        onError = { message ->
                            isLoading = false
                            errorMessage = message
                        }
                    )
                    webViewRef = this
                    val encodedToken = URLEncoder.encode(accessToken, "UTF-8")
                    val encodedImageId = URLEncoder.encode(imageId, "UTF-8")
                    loadUrl("file:///android_asset/mapillary_viewer.html?accessToken=$encodedToken&imageId=$encodedImageId")
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading && errorMessage == null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        }

        errorMessage?.let {
            Surface(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Street View could not be loaded. Try again.",
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp, start = 16.dp)
        ) {
            Surface(color = Color.Black.copy(alpha = 0.55f), shape = CircleShape) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Street View",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp).padding(4.dp)
                )
            }
        }
    }
}

@SuppressLint("JavascriptInterface")
private fun WebView.addJavascriptInterfaceSafely(onLoaded: () -> Unit, onError: (String) -> Unit) {
    addJavascriptInterface(
        object {
            @JavascriptInterface
            fun onLoaded() {
                post { onLoaded() }
            }

            @JavascriptInterface
            fun onError(message: String) {
                post { onError(message) }
            }
        },
        "AndroidStreetView"
    )
}
