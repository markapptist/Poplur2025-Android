package com.apptists.poplur2025.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CreateEntryScreen(
    onOriginalContent: () -> Unit,
    onPoplurAI: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp)
                .navigationBarsPadding()
                .statusBarsPadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    "Create",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            PipelineCard(
                emoji = "📹",
                title = "My Content",
                subtitle = "Photo · Video · Live",
                gradient = Brush.linearGradient(
                    colors = listOf(Color(0xFF0D0D1A), Color(0xFF12002C), Color(0xFF060D1A))
                ),
                borderColor = Color(0xFF4DFF58).copy(alpha = 0.55f),
                accentColor = Color(0xFF4DFF58),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 14.dp),
                onClick = onOriginalContent
            )

            PipelineCard(
                emoji = "✨",
                title = "Poplur AI",
                subtitle = "Prompt → 3 AI clips → Post",
                gradient = Brush.linearGradient(
                    colors = listOf(Color(0xFF1A0A2E), Color(0xFF0A0E2E), Color(0xFF200A20))
                ),
                borderColor = Color(0xFFB47DFF).copy(alpha = 0.65f),
                accentColor = Color(0xFFB47DFF),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 14.dp),
                onClick = onPoplurAI
            )
        }
    }
}

@Composable
private fun PipelineCard(
    emoji: String,
    title: String,
    subtitle: String,
    gradient: Brush,
    borderColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(90),
        label = "card-scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(28.dp))
            .background(gradient)
            .border(1.dp, borderColor, RoundedCornerShape(28.dp))
            .pointerInput(onClick) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = emoji,
                fontSize = 64.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                text = title,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                color = accentColor.copy(alpha = 0.88f),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}
