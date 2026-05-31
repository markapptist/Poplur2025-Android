package com.apptists.poplur2025.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptists.poplur2025.data.UserProfile

@Composable fun ProfileScreen(profile: UserProfile = UserProfile()) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Box(Modifier.fillMaxSize().background(Color.White)) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = "Messages",
                tint = Color.DarkGray,
                modifier = Modifier.size(32.dp)
            )
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.DarkGray,
                modifier = Modifier.size(32.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Surface(
                modifier = Modifier.size(140.dp),
                shape = CircleShape,
                color = Color.LightGray
            ) {
                // Placeholder for real image
                Box(Modifier.fillMaxSize().background(Color.Gray))
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Name
            Text(
                text = profile.displayName.uppercase(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black
            )
            
            Spacer(Modifier.height(24.dp))
            
            // Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                ProfileTabItem("Media", selectedTab == 0) { selectedTab = 0 }
                Spacer(Modifier.width(40.dp))
                ProfileTabItem("Messages", selectedTab == 1) { selectedTab = 1 }
            }
            
            Spacer(Modifier.height(24.dp))
            
            // Media Grid (Horizontal row as per screenshot)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(List(3) { it }) { _ ->
                    MediaItem()
                }
            }
        }

        // Bottom Overlays (Manual implementations to match colors on white background)
        
        // 1. Trending (Bottom Left)
        Column(
            modifier = Modifier
                .padding(bottom = 80.dp, start = 20.dp)
                .align(Alignment.BottomStart)
        ) {
            Text(
                text = "TRENDING",
                color = Color.LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider(
                color = Color.LightGray.copy(alpha = 0.5f), 
                thickness = 2.dp, 
                modifier = Modifier.width(80.dp).padding(vertical = 4.dp)
            )
            Text(
                text = "0",
                color = Color.LightGray,
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "posts",
                color = Color.LightGray,
                fontSize = 18.sp
            )
        }

        // 2. Cube Spacer
        Spacer(
            modifier = Modifier
                .padding(bottom = 60.dp)
                .size(80.dp)
                .align(Alignment.BottomCenter)
        )

        // 3. Radius (Bottom Right)
        Column(
            modifier = Modifier
                .padding(bottom = 60.dp, end = 20.dp)
                .align(Alignment.BottomEnd),
            horizontalAlignment = Alignment.End
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "RADIUS",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(6.dp))
                Text("🛡️", fontSize = 14.sp)
            }
            Text(
                text = "56",
                color = Color(0xFFCD7F32), // Bronze/Orange color
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 72.sp
            )
            HorizontalDivider(
                color = Color(0xFFCD7F32), 
                thickness = 2.dp, 
                modifier = Modifier.width(100.dp).padding(vertical = 4.dp)
            )
            Text(
                text = "3959",
                color = Color.LightGray,
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 56.sp
            )
            Text(
                text = "mi",
                color = Color.LightGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ProfileTabItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.Black else Color.Gray
        )
        if (isSelected) {
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .width(40.dp)
                    .height(3.dp)
                    .background(Color(0xFF7B61FF), RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun MediaItem() {
    Box(
        modifier = Modifier
            .size(150.dp, 160.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.DarkGray)
    ) {
        // Item content badge
        Surface(
            modifier = Modifier
                .padding(8.dp)
                .align(Alignment.BottomStart),
            color = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "5 KM",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
