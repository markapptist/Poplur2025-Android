package com.apptists.poplur2025.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DMScreen(onBack: () -> Unit = {}) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                "Messages",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            IconButton(onClick = { /* New message */ }) {
                Icon(
                    Icons.Default.EditNote,
                    contentDescription = "New Message",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(
                    "Recent",
                    color = Color.Gray,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            // Example Thread from screenshot
            item {
                DMThreadItem(
                    name = "Alex Johnson",
                    lastMessage = "Reply to keep the conversatio...",
                    hasUnread = true
                )
            }
        }
    }
}

@Composable
fun DMThreadItem(name: String, lastMessage: String, hasUnread: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = Color(0xFF1A1A1A), // Dark gray container
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AccountCircle, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
            }
            
            Column(
                Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                Text(name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(lastMessage, color = Color.Gray, fontSize = 14.sp, maxLines = 1)
            }

            if (hasUnread) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3897F0)) // Blue unread dot
                )
            }
            
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                null,
                tint = Color.DarkGray,
                modifier = Modifier.padding(start = 8.dp).size(16.dp)
            )
        }
    }
}

@Preview
@Composable
fun DMScreenPreview() {
    MaterialTheme {
        DMScreen()
    }
}
