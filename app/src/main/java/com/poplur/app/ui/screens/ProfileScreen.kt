package com.poplur.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onOpenSettings: () -> Unit = {}) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Profile") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFFFEEF7)),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 220.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Stats", fontSize = 44.sp, fontWeight = FontWeight.Black, color = Color(0xFF271722))
                Text("Creator performance and radius progress.", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7D6A75))
            }
            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White.copy(alpha = 0.78f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenSettings)
                ) {
                    Row(
                        Modifier.padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Settings", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color(0xFF271722))
                            Text("Radio playback and app controls", fontWeight = FontWeight.Bold, color = Color(0xFF7D6A75))
                        }
                        Text(">", fontWeight = FontWeight.Black, fontSize = 28.sp, color = Color(0xFFE91E7A))
                    }
                }
            }
            item { RadiusRing(progress = 0.74f, radiusText = "41.8 km") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("128", "Total posts", Modifier.weight(1f))
                    StatCard("793.8K", "Users in reach", Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("+312", "Likes gained", Modifier.weight(1f))
                    StatCard("-27", "Down votes", Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("86%", "Profile opens", Modifier.weight(1f))
                    StatCard("12", "Creator streak", Modifier.weight(1f))
                }
            }
            item {
                Surface(shape = RoundedCornerShape(28.dp), color = Color.White.copy(alpha = 0.74f)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Dominating areas", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color(0xFF271722))
                        Spacer(Modifier.height(12.dp))
                        listOf("Downtown" to "43%", "Painswick South" to "28%", "Waterfront" to "19%").forEach { (area, pct) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(area, fontWeight = FontWeight.Bold)
                                Text(pct, fontWeight = FontWeight.Black, color = Color(0xFFE91E7A))
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RadiusRing(progress: Float, radiusText: String) {
    var started by remember { mutableStateOf(false) }
    val animated by animateFloatAsState(
        targetValue = if (started) progress else 0f,
        animationSpec = tween(1100),
        label = "radiusRing"
    )
    LaunchedEffect(Unit) { started = true }

    Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val side = size.minDimension * 0.72f
            val left = (size.width - side) / 2
            val top = (size.height - side) / 2
            drawArc(
                color = Color(0x28E91E7A),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(left, top),
                size = Size(side, side),
                style = Stroke(width = 34f, cap = StrokeCap.Round)
            )
            drawArc(
                color = Color(0xFFE91E7A),
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = Offset(left, top),
                size = Size(side, side),
                style = Stroke(width = 34f, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(animated * 100).toInt()}%", fontSize = 42.sp, fontWeight = FontWeight.Black, color = Color(0xFF111827))
            Text(radiusText, fontWeight = FontWeight.Black, color = Color(0xFFE91E7A))
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.height(128.dp), shape = RoundedCornerShape(28.dp), color = Color.White.copy(alpha = 0.72f)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.Center) {
            Text(value, fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color(0xFF271722))
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF7D6A75))
        }
    }
}
