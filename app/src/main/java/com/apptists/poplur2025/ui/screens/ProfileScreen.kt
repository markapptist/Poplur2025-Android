package com.apptists.poplur2025.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Logout
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
import coil.compose.AsyncImage
import com.apptists.poplur2025.auth.AuthViewModel
import com.apptists.poplur2025.data.DemoPostStore
import com.apptists.poplur2025.data.UserProfile

@Composable
fun ProfileScreen(profile: UserProfile = UserProfile(), authViewModel: AuthViewModel? = null) {
    var selectedTab by remember { mutableStateOf(0) }
    var currentProfile by remember { mutableStateOf(profile) }
    var sheet by remember { mutableStateOf<ProfileSheet?>(null) }

    when (sheet) {
        ProfileSheet.Edit -> EditProfileScreen(currentProfile, onSave = { updated, _ -> currentProfile = updated; sheet = null }, onDismiss = { sheet = null })
        ProfileSheet.Messages -> MessagesScreen()
        ProfileSheet.Settings -> SettingsScreen(onSignOut = { authViewModel?.signOut() })
        null -> ProfileHome(currentProfile, selectedTab, { selectedTab = it }, { sheet = it }, authViewModel)
    }
}

@Composable
private fun ProfileHome(
    profile: UserProfile,
    selectedTab: Int,
    onTab: (Int) -> Unit,
    onSheet: (ProfileSheet) -> Unit,
    authViewModel: AuthViewModel?
) {
    Box(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Email, "Messages", tint = Color.DarkGray, modifier = Modifier.size(32.dp).clickable { onSheet(ProfileSheet.Messages) })
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Icon(Icons.Default.Edit, "Edit profile", tint = Color.DarkGray, modifier = Modifier.size(30.dp).clickable { onSheet(ProfileSheet.Edit) })
                Icon(Icons.Default.Settings, "Settings", tint = Color.DarkGray, modifier = Modifier.size(32.dp).clickable { onSheet(ProfileSheet.Settings) })
                if (authViewModel != null) Icon(Icons.Default.Logout, "Log out", tint = Color.DarkGray, modifier = Modifier.size(30.dp).clickable { authViewModel.signOut() })
            }
        }

        Column(Modifier.fillMaxSize().padding(top = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(
                model = profile.avatarUrl,
                contentDescription = "Avatar",
                modifier = Modifier.size(140.dp).clip(CircleShape).background(Color.Gray),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(16.dp))
            Text(profile.displayName.uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.Black)
            Text("@${profile.username}", color = Color.Gray, fontWeight = FontWeight.SemiBold)
            Text(profile.bio, color = Color.DarkGray, modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                Stat("Posts", profile.posts.toString())
                Stat("Followers", profile.followers.toString())
                Stat("Radius", "${profile.radius}")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                ProfileTabItem("Media", selectedTab == 0) { onTab(0) }
                Spacer(Modifier.width(40.dp))
                ProfileTabItem("Messages", selectedTab == 1) { onTab(1) }
            }
            Spacer(Modifier.height(24.dp))
            if (selectedTab == 0) {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(DemoPostStore.posts) { post -> MediaItem(post.mediaUrl ?: post.videoUrl, post.radius) }
                }
            } else {
                MessagesScreen()
            }
        }

        Column(Modifier.padding(bottom = 80.dp, start = 20.dp).align(Alignment.BottomStart)) {
            Text("TRENDING", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 2.dp, modifier = Modifier.width(80.dp).padding(vertical = 4.dp))
            Text("0", color = Color.LightGray, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
            Text("posts", color = Color.LightGray, fontSize = 18.sp)
        }
        Spacer(Modifier.padding(bottom = 60.dp).size(80.dp).align(Alignment.BottomCenter))
        Column(Modifier.padding(bottom = 60.dp, end = 20.dp).align(Alignment.BottomEnd), horizontalAlignment = Alignment.End) {
            Text("RADIUS 🛡️", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("56", color = Color(0xFFCD7F32), fontSize = 72.sp, fontWeight = FontWeight.Black, lineHeight = 72.sp)
            HorizontalDivider(color = Color(0xFFCD7F32), thickness = 2.dp, modifier = Modifier.width(100.dp).padding(vertical = 4.dp))
            Text("3959", color = Color.LightGray, fontSize = 56.sp, fontWeight = FontWeight.Black, lineHeight = 56.sp)
            Text("mi", color = Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
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
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.Black else Color.Gray)
        if (isSelected) Spacer(Modifier.height(4.dp))
        if (isSelected) Box(Modifier.width(40.dp).height(3.dp).background(Color(0xFF7B61FF), RoundedCornerShape(2.dp)))
    }
}

@Composable
fun MediaItem(url: String? = null, radius: Int = 5000) {
    Box(Modifier.size(150.dp, 160.dp).clip(RoundedCornerShape(20.dp)).background(Color.DarkGray)) {
        AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Surface(modifier = Modifier.padding(8.dp).align(Alignment.BottomStart), color = Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp)) {
            Text("${radius / 1000} KM", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}

enum class ProfileSheet { Edit, Messages, Settings }
