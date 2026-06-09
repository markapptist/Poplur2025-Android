package com.apptists.poplur2025.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage

import com.apptists.poplur2025.auth.AuthViewModel
import com.apptists.poplur2025.data.DemoPostStore
import com.apptists.poplur2025.data.UserProfile
import com.apptists.poplur2025.ui.components.RadiusDisplay
import com.apptists.poplur2025.ui.components.TrendingDisplay

@Composable
fun ProfileScreen(profile: UserProfile = UserProfile(), authViewModel: AuthViewModel? = null) {
    ProfileScreenContent(profile = profile) { authViewModel?.signOut() }
}

@Composable
fun ProfileScreenContent(
    profile: UserProfile = UserProfile(),
    onSignOut: (() -> Unit)? = null,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentProfile by remember { mutableStateOf(profile) }
    var sheet by remember { mutableStateOf<ProfileSheet?>(null) }

    when (sheet) {
        ProfileSheet.Edit -> EditProfileScreen(currentProfile, onSave = { updated, _ -> currentProfile = updated; sheet = null }, onDismiss = { sheet = null })
        ProfileSheet.Messages -> MessagesScreen(onBack = { sheet = null })
        ProfileSheet.Settings -> SettingsScreen(onBack = { sheet = null }, onSignOut = onSignOut)
        null -> ProfileHome(currentProfile, selectedTab, { selectedTab = it }, { sheet = it }, onSignOut)
    }
}

@Composable
private fun ProfileHome(
    profile: UserProfile,
    selectedTab: Int,
    onTab: (Int) -> Unit,
    onSheet: (ProfileSheet) -> Unit,
    onSignOut: (() -> Unit)? = null
) {
    Box(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 24.dp, end = 24.dp, top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Email, "Messages", tint = Color.DarkGray, modifier = Modifier.size(32.dp).clickable { onSheet(ProfileSheet.Messages) })
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Icon(Icons.Default.Settings, "Settings", tint = Color.DarkGray, modifier = Modifier.size(32.dp).clickable { onSheet(ProfileSheet.Settings) })
                if (onSignOut != null) Icon(Icons.AutoMirrored.Filled.Logout, "Log out", tint = Color.DarkGray, modifier = Modifier.size(30.dp).clickable { onSignOut() })
            }
        }

        Column(Modifier.fillMaxSize().padding(top = 112.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(
                model = profile.avatarUrl,
                contentDescription = "Avatar",
                modifier = Modifier.size(96.dp).clip(CircleShape).background(Color.Gray),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(18.dp))
            Text(profile.displayName.uppercase(), fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color.Black)
            Row(Modifier.fillMaxWidth().padding(top = 28.dp), horizontalArrangement = Arrangement.Center) {
                ProfileTabItem("Media", selectedTab == 0) { onTab(0) }
                Spacer(Modifier.width(56.dp))
                ProfileTabItem("Messages", selectedTab == 1) { onTab(1) }
            }
            Spacer(Modifier.height(24.dp))
            if (selectedTab == 0) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 300.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(DemoPostStore.posts) { post -> MediaItem(post.mediaUrl ?: post.videoUrl, post.radius) }
                }
            } else {
                MessagesScreen()
            }
        }

        TrendingDisplay(
            postsCount = 0,
            radiusChange = 0,
            contentColor = Color.DarkGray,
            modifier = Modifier
                .padding(bottom = 48.dp, start = 20.dp)
                .align(Alignment.BottomStart)
        )
        // Removed navigation cube placeholder
        RadiusDisplay(
            modifier = Modifier.padding(bottom = 48.dp, end = 20.dp).align(Alignment.BottomEnd),
            radius = 56,
            unit = "mi",
            contentColor = Color.DarkGray
        )
    }
}

@Composable private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Black, color = Color.Black)
        Text(label, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun ProfileTabItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable { onClick() }, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Black, color = if (isSelected) Color.Black else Color(0xFF222222))
        if (isSelected) Spacer(Modifier.height(4.dp))
        if (isSelected) Box(Modifier.width(48.dp).height(3.dp).background(Color(0xFF7B61FF), RoundedCornerShape(2.dp)))
    }
}

@Composable
fun MediaItem(url: String? = null, radius: Int = 5000) {
    Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(18.dp)).background(Color.DarkGray)) {
        AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Surface(modifier = Modifier.padding(8.dp).align(Alignment.BottomStart), color = Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp)) {
            Text("${radius / 1000} KM", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}

enum class ProfileSheet { Edit, Messages, Settings }

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    MaterialTheme {
        ProfileScreenContent(profile = UserProfile())
    }
}

