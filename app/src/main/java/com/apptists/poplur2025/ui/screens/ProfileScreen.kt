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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

private enum class ProfilePanel {
    Home,
    Notifications,
    Store
}

@Composable
fun ProfileScreen(
    profile: UserProfile = UserProfile(),
    authViewModel: AuthViewModel? = null,
    onOpenSettings: () -> Unit = {},
    onOpenMessages: () -> Unit = {}
) {
    ProfileScreenContent(
        profile = profile,
        onOpenSettings = onOpenSettings,
        onOpenMessages = onOpenMessages
    )
}

@Composable
fun ProfileScreenContent(
    profile: UserProfile = UserProfile(),
    onOpenSettings: () -> Unit = {},
    onOpenMessages: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentProfile by remember { mutableStateOf(profile) }
    var isEditing by remember { mutableStateOf(false) }
    var activePanel by remember { mutableStateOf(ProfilePanel.Home) }
    var hasUncheckedNotifications by remember { mutableStateOf(true) }
    var purchasedRadius by remember { mutableIntStateOf(0) }

    if (isEditing) {
        EditProfileScreen(
            profile = currentProfile,
            onSave = { updated, _ -> 
                currentProfile = updated
                isEditing = false 
            },
            onDismiss = { isEditing = false }
        )
    } else when (activePanel) {
        ProfilePanel.Notifications -> ProfileNotificationsPanel(
            onBack = { activePanel = ProfilePanel.Home },
            onMarkRead = { hasUncheckedNotifications = false }
        )
        ProfilePanel.Store -> ProfileStorePanel(
            purchasedRadius = purchasedRadius,
            onPurchase = { purchasedRadius += it },
            onBack = { activePanel = ProfilePanel.Home }
        )
        ProfilePanel.Home -> ProfileHome(
            profile = currentProfile,
            selectedTab = selectedTab,
            onTab = { selectedTab = it },
            onOpenSettings = onOpenSettings,
            onOpenMessages = onOpenMessages,
            hasUncheckedNotifications = hasUncheckedNotifications,
            onOpenNotifications = {
                hasUncheckedNotifications = false
                activePanel = ProfilePanel.Notifications
            },
            onOpenStore = { activePanel = ProfilePanel.Store },
            onEdit = { isEditing = true }
        )
    }
}

@Composable
private fun ProfileHome(
    profile: UserProfile,
    selectedTab: Int,
    onTab: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMessages: () -> Unit,
    hasUncheckedNotifications: Boolean,
    onOpenNotifications: () -> Unit,
    onOpenStore: () -> Unit,
    onEdit: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 24.dp, end = 24.dp, top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileHeaderActionIcon(Icons.Default.Email, "Messages", onOpenMessages)
                ProfileHeaderActionIcon(Icons.Default.CardGiftcard, "Notifications", onOpenNotifications, hasUncheckedNotifications)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileHeaderActionIcon(Icons.Default.Settings, "Settings", onOpenSettings)
                ProfileHeaderActionIcon(Icons.Default.AttachMoney, "Store", onOpenStore)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    profile.displayName.uppercase(), 
                    fontSize = 30.sp, 
                    fontWeight = FontWeight.Black, 
                    color = Color.Black
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Profile",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp).clickable { onEdit() }
                )
            }
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
        
        RadiusDisplay(
            modifier = Modifier.padding(bottom = 48.dp, end = 20.dp).align(Alignment.BottomEnd),
            radius = 56,
            unit = "mi",
            contentColor = Color.DarkGray
        )
    }
}

@Composable
private fun ProfileHeaderActionIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    showBadge: Boolean = false
) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
        BadgedBox(
            badge = {
                if (showBadge) {
                    Badge(containerColor = Color.Red)
                }
            }
        ) {
            Icon(
                icon,
                contentDescription,
                tint = Color.DarkGray,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun ProfileNotificationsPanel(
    onBack: () -> Unit,
    onMarkRead: () -> Unit
) {
    LaunchedEffect(Unit) {
        onMarkRead()
    }

    val items = listOf(
        Triple("Post liked", "Your latest post picked up new likes.", Icons.Default.Email),
        Triple("Post radius jumped", "One of your posts expanded significantly in the last hour.", Icons.Default.CardGiftcard),
        Triple("Accumulated radius dipped", "Your total radius is down from the previous check.", Icons.Default.CardGiftcard)
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.Black)
            }
            Text("Notifications", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.Black)
        }
        Spacer(Modifier.height(18.dp))
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.05f))
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                BadgedBox(
                    badge = {
                        if (index < 2) Badge(containerColor = Color.Red)
                    }
                ) {
                    Surface(shape = CircleShape, color = Color(0xFFEFF4FF), modifier = Modifier.size(48.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(item.third, null, tint = Color(0xFF3E63DD), modifier = Modifier.size(22.dp))
                        }
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(item.first, fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.Black)
                    Text(item.second, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black.copy(alpha = 0.58f))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ProfileStorePanel(
    purchasedRadius: Int,
    onPurchase: (Int) -> Unit,
    onBack: () -> Unit
) {
    val packages = listOf(
        Triple("Starter Radius", 5_000, "$1.99"),
        Triple("City Boost", 25_000, "$4.99"),
        Triple("World Push", 100_000, "$9.99")
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.Black)
            }
            Text("Store", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.Black)
        }
        Spacer(Modifier.height(8.dp))
        Text("Purchase radius to expand your reach.", color = Color.Black.copy(alpha = 0.58f), fontWeight = FontWeight.SemiBold)
        if (purchasedRadius > 0) {
            Spacer(Modifier.height(10.dp))
            Text("Purchased radius: ${purchasedRadius / 1000} km", color = Color(0xFF178A3D), fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(18.dp))
        packages.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.05f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = CircleShape, color = Color(0xFFE9F8EE), modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AttachMoney, null, tint = Color(0xFF178A3D), modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.first, fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.Black)
                    Text("+${item.second / 1000} km", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black.copy(alpha = 0.58f))
                }
                Button(onClick = { onPurchase(item.second) }) {
                    Text(item.third, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
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

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    MaterialTheme {
        ProfileScreenContent(profile = UserProfile())
    }
}
