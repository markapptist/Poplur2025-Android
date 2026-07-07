package com.apptists.poplur2025.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.apptists.poplur2025.data.AIVideoStyle
import com.apptists.poplur2025.ui.components.PoplurText
import com.google.android.gms.location.LocationServices

@Composable
fun AIVideoStudioScreen(
    initialPrompt: String,
    initialStyle: AIVideoStyle,
    initialLocationName: String = "",
    onBack: () -> Unit,
    onGenerate: (prompt: String, style: AIVideoStyle, locationName: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf(initialPrompt) }
    var selectedStyle by remember { mutableStateOf(initialStyle) }
    var locationName by remember { mutableStateOf(initialLocationName) }
    var locationResolved by remember { mutableStateOf(false) }
    var isLocationLoading by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) resolveLocation(context, scope) { name ->
            locationName = name
            locationResolved = true
            isLocationLoading = false
        } else {
            isLocationLoading = false
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission && !locationResolved) {
            isLocationLoading = true
            resolveLocation(context, scope) { name ->
                locationName = name
                locationResolved = true
                isLocationLoading = false
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                PoplurText(
                    "Poplur AI",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Prompt input
            PoplurText(
                "What should happen?",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
            )
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it.take(280) },
                placeholder = {
                    PoplurText(
                        "e.g. A news anchor reporting live from the streets of Miami at night",
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 15.sp
                    )
                },
                minLines = 3,
                maxLines = 5,
                textStyle = LocalTextStyle.current.copy(
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFB47DFF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            )
            PoplurText(
                "${prompt.length}/280",
                color = Color.White.copy(alpha = 0.32f),
                fontSize = 12.sp,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )

            // Style picker
            PoplurText(
                "Style",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 24.dp, bottom = 10.dp)
            )
            StyleGrid(
                selected = selectedStyle,
                onSelect = { selectedStyle = it }
            )

            // Location
            PoplurText(
                "Location",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            OutlinedTextField(
                value = locationName,
                onValueChange = { locationName = it },
                placeholder = {
                    PoplurText(
                        if (isLocationLoading) "Detecting…" else "City, neighborhood, landmark",
                        color = Color.White.copy(alpha = 0.35f)
                    )
                },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 15.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4DFF58).copy(alpha = 0.8f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
                ),
                shape = RoundedCornerShape(14.dp),
                trailingIcon = {
                    TextButton(onClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            isLocationLoading = true
                            resolveLocation(context, scope) { name ->
                                locationName = name
                                locationResolved = true
                                isLocationLoading = false
                            }
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }) {
                        PoplurText("📍", fontSize = 20.sp)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(28.dp))

            // Generate button
            val canGenerate = prompt.isNotBlank()
            Button(
                onClick = {
                    if (canGenerate) onGenerate(prompt.trim(), selectedStyle, locationName)
                },
                enabled = canGenerate,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB47DFF),
                    disabledContainerColor = Color.White.copy(alpha = 0.12f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                PoplurText(
                    "✨ Generate 3 Clips",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StyleGrid(
    selected: AIVideoStyle,
    onSelect: (AIVideoStyle) -> Unit
) {
    val styles = AIVideoStyle.entries
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = false,
        modifier = Modifier.heightIn(max = 600.dp)
    ) {
        items(styles) { style ->
            val isSelected = style == selected
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected)
                            Brush.radialGradient(
                                listOf(Color(0xFF3A1C6E), Color(0xFF0D0D1A))
                            )
                        else
                            Brush.linearGradient(
                                listOf(Color(0xFF0D0D1A), Color(0xFF0D0D1A))
                            )
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color(0xFFB47DFF) else Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onSelect(style) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PoplurText(style.emoji, fontSize = 28.sp)
                    Spacer(Modifier.height(4.dp))
                    PoplurText(
                        style.label,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.72f),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

private fun resolveLocation(
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onResult: (String) -> Unit
) {
    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    try {
        fusedClient.lastLocation.addOnSuccessListener { location ->
            if (location == null) { onResult(""); return@addOnSuccessListener }
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context, java.util.Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    val addr = addresses?.firstOrNull()
                    val name = listOfNotNull(
                        addr?.subLocality ?: addr?.locality,
                        addr?.adminArea
                    ).joinToString(", ").ifBlank { "" }
                    onResult(name)
                } catch (e: Exception) {
                    onResult("")
                }
            }
        }.addOnFailureListener {
            onResult("")
        }
    } catch (e: SecurityException) {
        onResult("")
    }
}
