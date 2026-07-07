package com.apptists.poplur2025.ui.games.screens

import android.webkit.WebChromeClient
import android.webkit.WebView
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun GameLaunchScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val webView = remember(context) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            @Suppress("SetJavaScriptEnabled")
            settings.allowFileAccessFromFileURLs = true
            @Suppress("SetJavaScriptEnabled")
            settings.allowUniversalAccessFromFileURLs = true
            settings.mediaPlaybackRequiresUserGesture = false
            webChromeClient = WebChromeClient()
            setBackgroundColor(android.graphics.Color.BLACK)
            loadUrl("file:///android_asset/poplur_fps/index.html")
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )

        IconButton(
            onClick = {
                webView.destroy()
                onDismiss()
            },
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
