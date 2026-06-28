package com.apptists.poplur2025.ui.components

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.util.Locale

/**
 * Animated speaking avatar for the Profile screen.
 * Replaces the static profile image with an animated avatar that speaks stats on tap.
 *
 * - 2D mode: circular image + animated ellipse mouth below it.
 * - 3D mode: same image with a floating + subtle rotation animation (Compose 3D-like).
 * Controlled via [is3D]. Both modes tap to speak stats via Android TTS.
 */
@Composable
fun ProfileAvatarSpeaker(
    avatarUrl: String?,
    totalRadiusMeters: Int,
    postCount: Int,
    size: androidx.compose.ui.unit.Dp = 96.dp,
    is3D: Boolean = false,
    accentColor: Color = Color(0xFF9B59F5),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isSpeaking by remember { mutableStateOf(false) }
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }

    // Init TTS
    DisposableEffect(context) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.value?.language = Locale.US
            }
        }
        tts.value = engine
        onDispose { engine.stop(); engine.shutdown() }
    }

    // Listen for utterance finish to reset isSpeaking
    val listener = remember {
        object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { isSpeaking = false }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { isSpeaking = false }
        }
    }
    LaunchedEffect(tts.value) {
        tts.value?.setOnUtteranceProgressListener(listener)
    }

    // 3D floating animation
    val infiniteTransition = rememberInfiniteTransition(label = "avatar3d")
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "floatY"
    )
    val rotY by infiniteTransition.animateFloat(
        initialValue = -4f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "rotY"
    )

    // Mouth animation
    val mouthScale by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(120, easing = LinearEasing), RepeatMode.Reverse),
        label = "mouth"
    )

    // Glow pulse when speaking
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(500, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(onClick = {
                val engine = tts.value ?: return@clickable
                if (isSpeaking) { engine.stop(); isSpeaking = false; return@clickable }
                isSpeaking = true
                val radiusKm = totalRadiusMeters / 1000
                val radiusMi = (totalRadiusMeters * 0.000621371).toInt()
                val radiusText = when {
                    radiusKm >= 1000 -> "${radiusKm / 1000} thousand kilometers"
                    radiusKm >= 1 -> "$radiusKm kilometers"
                    else -> "just getting started"
                }
                val postText = if (postCount == 1) "1 post" else "$postCount posts"
                val text = "You have $radiusText of radius worldwide, from $postText."
                engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "profile_stats")
            })
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Glow ring when speaking
            if (isSpeaking) {
                Box(
                    modifier = Modifier
                        .size(size + 10.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = if (isSpeaking) glowAlpha else 0f))
                )
            }

            // Profile image
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Color.Gray)
                    .then(
                        if (is3D) Modifier.graphicsLayer {
                            translationY = floatY
                            rotationY = rotY
                            cameraDistance = 8f * density
                        } else Modifier
                    )
                    .shadow(if (isSpeaking) 12.dp else 4.dp, CircleShape)
            )
        }

        Spacer(Modifier.height(6.dp))

        // Mouth (shown below image)
        Box(
            modifier = Modifier
                .width(size * 0.55f)
                .height(size * 0.14f)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.75f))
                .graphicsLayer { scaleY = if (isSpeaking) mouthScale else 0.1f }
        )

        // Speaking indicator bars
        if (isSpeaking) {
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                repeat(4) { i ->
                    val barHeight by infiniteTransition.animateFloat(
                        initialValue = 4f, targetValue = 14f,
                        animationSpec = infiniteRepeatable(
                            tween(200, delayMillis = i * 60, easing = EaseInOutSine),
                            RepeatMode.Reverse
                        ),
                        label = "bar$i"
                    )
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(barHeight.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White)
                    )
                }
            }
        } else {
            // Hint icon when idle
            Spacer(Modifier.height(6.dp))
            Text("▶ tap to hear stats", fontSize = 9.sp, color = accentColor.copy(alpha = 0.72f), fontWeight = FontWeight.Medium)
        }
    }
}

private val EaseInOutSine = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)
