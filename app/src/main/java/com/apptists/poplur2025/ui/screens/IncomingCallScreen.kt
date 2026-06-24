package com.apptists.poplur2025.ui.screens

import android.media.RingtoneManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import com.apptists.poplur2025.R
import com.apptists.poplur2025.call.CallSessionManager

private const val PHONE_IMG_ASPECT = 1024f / 1536f
// LCD screen bounds as fraction of phone image size (matches PopularPhoneCallScreen constants)
private const val LCD_LEFT = 0.240f
private const val LCD_TOP  = 0.183f
private const val LCD_W    = 0.520f
private const val LCD_H    = 0.245f

@Composable
fun IncomingCallScreen(
    callerName: String,
    channelId: String,
    documentId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val ringtone = remember(context) {
        runCatching {
            RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
        }.getOrNull()
    }

    LaunchedEffect(Unit) { ringtone?.play() }
    DisposableEffect(ringtone) { onDispose { ringtone?.stop() } }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Phone image fills screen (silver colorway)
        Image(
            painter = painterResource(R.drawable.poplur_bling_phone_01_silver_diamond),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // LCD text overlay positioned proportionally on the phone screen
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val phoneAspect = PHONE_IMG_ASPECT
            val w = maxWidth
            val h = maxHeight
            val phoneW = if (w / h < phoneAspect) w else h * phoneAspect
            val phoneH = phoneW / phoneAspect
            val phoneOffX = (w - phoneW) / 2
            val phoneOffY = (h - phoneH) / 2

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(
                        x = phoneOffX + phoneW * LCD_LEFT,
                        y = phoneOffY + phoneH * LCD_TOP
                    )
                    .width(phoneW * LCD_W)
                    .height(phoneH * LCD_H),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Text(
                    text = "INCOMING",
                    fontSize = (phoneW.value * LCD_W * 0.09f).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF11FF44),
                    textAlign = TextAlign.Center,
                    lineHeight = (phoneW.value * LCD_W * 0.11f).sp
                )
                Text(
                    text = callerName.uppercase(),
                    fontSize = (phoneW.value * LCD_W * 0.10f).sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF11FF44),
                    textAlign = TextAlign.Center,
                    lineHeight = (phoneW.value * LCD_W * 0.12f).sp
                )
            }

            // Accept / Decline buttons near bottom of screen
            Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(64.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = (h * 0.08f))
            ) {
                IncomingCallButton(
                    icon = Icons.Default.CallEnd,
                    label = "Decline",
                    tint = Color(0xFFE53935)
                ) {
                    CallSessionManager.declineCall(documentId)
                }

                IncomingCallButton(
                    icon = Icons.Default.Call,
                    label = "Accept",
                    tint = Color(0xFF00C853)
                ) {
                    CallSessionManager.acceptCall(
                        channelId = channelId,
                        documentId = documentId,
                        callerName = callerName
                    )
                }
            }
        }
    }
}

@Composable
private fun IncomingCallButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .background(tint, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onClick, modifier = Modifier.size(70.dp)) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 13.sp
        )
    }
}
