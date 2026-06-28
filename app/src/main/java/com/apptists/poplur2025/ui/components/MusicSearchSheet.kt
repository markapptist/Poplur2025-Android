package com.apptists.poplur2025.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.apptists.poplur2025.data.MusicProvider
import com.apptists.poplur2025.data.MusicSearchCatalog
import com.apptists.poplur2025.data.MusicSearchTrack

@Composable
fun MusicSearchSheet(
    selectedTrack: MusicSearchTrack?,
    onDismiss: () -> Unit,
    onSelect: (MusicSearchTrack) -> Unit,
    onUseLocalFile: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedProvider by remember { mutableStateOf<MusicProvider?>(null) }
    val results = remember(query, selectedProvider) {
        MusicSearchCatalog.search(query, selectedProvider)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.Black,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Add music",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.weight(1f))
                    Surface(
                        onClick = onDismiss,
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.12f),
                        contentColor = Color.White
                    ) {
                        Text("×", modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp), fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Search songs, artists, sounds", color = Color.White.copy(alpha = 0.45f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedBorderColor = Color.White.copy(alpha = 0.42f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.16f),
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.08f)
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProviderChip("All", selectedProvider == null) { selectedProvider = null }
                    MusicProvider.entries.forEach { provider ->
                        ProviderChip(provider.displayName, selectedProvider == provider) {
                            selectedProvider = provider
                        }
                    }
                }

                Surface(
                    onClick = onUseLocalFile,
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    contentColor = Color.White
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("♫", fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Column(Modifier.weight(1f)) {
                            Text("Use local audio/video file", fontSize = 13.sp, fontWeight = FontWeight.Black)
                            Text(
                                "This is the current path for burning music into uploads.",
                                color = Color.White.copy(alpha = 0.58f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text("›", color = Color.White.copy(alpha = 0.5f), fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(results, key = { it.id }) { track ->
                        MusicTrackRow(
                            track = track,
                            isSelected = selectedTrack?.id == track.id,
                            onSelect = { onSelect(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderChip(title: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Color.White else Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            title,
            color = if (selected) Color.Black else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun MusicTrackRow(track: MusicSearchTrack, isSelected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = if (isSelected) 0.16f else 0.075f))
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFF4DFF58) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onSelect)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        listOf(providerColor(track.provider), Color.White.copy(alpha = 0.18f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("♪", color = Color.Black.copy(alpha = 0.82f), fontSize = 24.sp, fontWeight = FontWeight.Black)
        }

        Column(Modifier.weight(1f)) {
            Text(track.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${track.artist} • ${track.album}",
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(track.provider.displayName, color = providerColor(track.provider), fontSize = 10.sp, fontWeight = FontWeight.Black)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(track.durationText, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(if (isSelected) "✓" else "+", color = if (isSelected) Color(0xFF4DFF58) else Color.White.copy(alpha = 0.48f), fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}

private fun providerColor(provider: MusicProvider): Color = when (provider) {
    MusicProvider.AppleMusic -> Color(0xFFFF2D55)
    MusicProvider.Spotify -> Color(0xFF1DB954)
    MusicProvider.Tidal -> Color.White
    MusicProvider.SoundCloud -> Color(0xFFFF6D00)
    MusicProvider.YouTubeMusic -> Color(0xFFFF0000)
    MusicProvider.Deezer -> Color(0xFF8D5CFF)
    MusicProvider.AmazonMusic -> Color(0xFF00C7FF)
    MusicProvider.Bandcamp -> Color(0xFF61C2D1)
    MusicProvider.Audiomack -> Color(0xFFFFCF1C)
}
