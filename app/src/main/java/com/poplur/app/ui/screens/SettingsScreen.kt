package com.poplur.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poplur.app.data.RadioPlaybackMode
import com.poplur.app.data.RadioSettings
import com.poplur.app.radio.RadioPlaybackService

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf(RadioSettings.getMode(context)) }
    var isPlaying by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFEEF7)),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 260.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Settings", fontSize = 42.sp, fontWeight = FontWeight.Black, color = Color(0xFF26131F))
            Text("Radio playback", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7A6872))
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RadioPlaybackMode.entries.forEach { mode ->
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedMode = mode
                                RadioSettings.setMode(context, mode)
                                if (mode == RadioPlaybackMode.SCREEN_ONLY && isPlaying) {
                                    RadioPlaybackService.stop(context)
                                    isPlaying = false
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (selectedMode == mode) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = Color(0xFFE91E7A)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(mode.title, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                Text(mode.subtitle, fontWeight = FontWeight.Bold, color = Color(0xFF7A6872))
                            }
                        }
                    }
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(26.dp), color = Color.Black.copy(alpha = 0.08f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(if (isPlaying) "Radio is playing" else "Radio is stopped", fontWeight = FontWeight.Black)
                        Text("CBC Radio One - Toronto", style = MaterialTheme.typography.labelMedium, color = Color(0xFF7A6872))
                    }
                    if (isPlaying) {
                        OutlinedButton(onClick = {
                            RadioPlaybackService.stop(context)
                            isPlaying = false
                        }) { Text("Stop") }
                    } else {
                        Button(onClick = {
                            RadioPlaybackService.play(context)
                            isPlaying = true
                        }) { Text("Play") }
                    }
                }
            }
        }
    }
}
