package com.apptists.poplur2025.ui.previews

import android.opengl.GLSurfaceView
import android.graphics.PixelFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import com.apptists.poplur2025.ui.CombinedGdxAdapter
import com.apptists.poplur2025.ui.GdxNavigationFragment
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import java.util.Locale

class CubeDebugFragment : GdxNavigationFragment() {
    var scale: Float = 0.65f
    var cornerRadius: Float = 92f
    var rotation: Float = -90f
    var opacity: Float = 1.0f
    var selectedTexture: String = "Record"
    var onAdapterReady: (CombinedGdxAdapter) -> Unit = {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useImmersiveMode = false
            useGL30 = true
            r = 8; g = 8; b = 8; a = 8
        }
        val adapter = CombinedGdxAdapter(
            initialTabIndex = 1,
            onVerticalSwipe = {},
            onLongPress = {},
            onLongPressEnd = {},
            onTap = {},
            onTabSelected = {}
        )
        adapter.cubeScale = scale
        adapter.cornerRadius = cornerRadius
        adapter.yawDegrees = rotation
        adapter.opacity = opacity
        adapter.currentIconTitle = selectedTexture
        
        onAdapterReady(adapter)
        
        val gdxView = initializeForView(adapter, config)
        gdxView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        if (gdxView is GLSurfaceView) {
            gdxView.setZOrderOnTop(true)
            gdxView.holder.setFormat(PixelFormat.TRANSLUCENT)
        }
        return gdxView
    }
}

@Composable
fun CubeDevelopmentScreen(onBack: () -> Unit = {}) {
    var scale by remember { mutableStateOf(0.65f) }
    var cornerRadius by remember { mutableStateOf(92f) }
    var rotation by remember { mutableStateOf(-90f) }
    var opacity by remember { mutableStateOf(1.0f) }
    var selectedTexture by remember { mutableStateOf("Record") }
    val textures = listOf("Record", "Feed", "Profile", "Map")
    
    val containerId = remember { android.view.View.generateViewId() }
    val adapterRef = remember { mutableStateOf<CombinedGdxAdapter?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Cube Debug", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        // Controls
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Cube Scale: ${String.format(Locale.US, "%.2f", scale)}", color = Color.White)
                Slider(
                    value = scale,
                    onValueChange = { 
                        scale = it
                        adapterRef.value?.cubeScale = it
                    },
                    valueRange = 0.1f..2.0f
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Corner Radius: ${cornerRadius.toInt()}", color = Color.White)
                Slider(
                    value = cornerRadius,
                    onValueChange = { 
                        cornerRadius = it
                        adapterRef.value?.cornerRadius = it
                    },
                    valueRange = 0f..150f
                )

                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Rotation: ${rotation.toInt()}°", color = Color.White)
                Slider(
                    value = rotation,
                    onValueChange = { 
                        rotation = it
                        adapterRef.value?.yawDegrees = it
                    },
                    valueRange = -360f..360f
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Opacity: ${String.format(Locale.US, "%.2f", opacity)}", color = Color.White)
                Slider(
                    value = opacity,
                    onValueChange = { 
                        opacity = it
                        adapterRef.value?.opacity = it
                    },
                    valueRange = 0.0f..1.0f
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Texture Focus:", color = Color.White)
                ScrollableTabRow(
                    selectedTabIndex = textures.indexOf(selectedTexture),
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    edgePadding = 0.dp,
                    divider = {}
                ) {
                    textures.forEach { title ->
                        Tab(
                            selected = selectedTexture == title,
                            onClick = { 
                                selectedTexture = title
                                adapterRef.value?.currentIconTitle = title
                            },
                            text = { Text(title, fontSize = 12.sp) }
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(32.dp)
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (androidx.compose.ui.platform.LocalInspectionMode.current) {
                // Placeholder for IDE Preview
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size((300 * scale).dp)
                            .graphicsLayer { 
                                alpha = opacity
                                rotationY = rotation + 90f // Visual hint of rotation in preview
                            }
                            .background(Color(0xFF1A1C2E), RoundedCornerShape((cornerRadius / 4).dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Mock icon for preview
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = when(selectedTexture) {
                                    "Record" -> Icons.Default.AddCircle
                                    "Feed" -> Icons.Default.List
                                    "Profile" -> Icons.Default.AccountCircle
                                    else -> Icons.Default.Place
                                },
                                contentDescription = null,
                                tint = Color(0xFF8E8EFA),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                selectedTexture,
                                color = Color(0xFF8E8EFA),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("GL Cube (Visible on Device)", color = Color.White.copy(alpha = 0.5f))
                }
            } else {
                AndroidView(
                    factory = { ctx ->
                        FragmentContainerView(ctx).apply {
                            id = containerId
                            setBackgroundColor(0)
                            val activity = ctx as? FragmentActivity
                            val frag = CubeDebugFragment().apply {
                                this.scale = scale
                                this.cornerRadius = cornerRadius
                                this.rotation = rotation
                                this.opacity = opacity
                                this.selectedTexture = selectedTexture
                                this.onAdapterReady = { adapterRef.value = it }
                            }
                            activity?.supportFragmentManager?.beginTransaction()
                                ?.replace(id, frag)
                                ?.commit()
                        }
                    },
                    modifier = Modifier.size(300.dp)
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun CubeDevelopmentPreview() {
    MaterialTheme {
        CubeDevelopmentScreen()
    }
}
