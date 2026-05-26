package com.apptists.poplur2025.gdx

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun LibGdxSceneView(scene: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("LibGDX $scene scene")
            Button(onClick = {
                context.startActivity(Intent(context, LibGdxSceneActivity::class.java).putExtra("scene", scene))
            }) { Text("Open animated LibGDX layer") }
        }
    }
}
