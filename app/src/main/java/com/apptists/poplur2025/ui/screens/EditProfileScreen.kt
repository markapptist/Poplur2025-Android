package com.apptists.poplur2025.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.apptists.poplur2025.data.UserProfile

@Composable
fun EditProfileScreen(
    profile: UserProfile,
    onSave: (UserProfile, Uri?) -> Unit,
    onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf(profile.username) }
    var displayName by remember { mutableStateOf(profile.displayName) }
    var bio by remember { mutableStateOf(profile.bio) }
    var pickedImage by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { pickedImage = it }

    Column(
        Modifier.fillMaxSize().background(Color.White).statusBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Edit Profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Box(contentAlignment = Alignment.BottomEnd) {
            AsyncImage(
                model = pickedImage ?: profile.avatarUrl,
                contentDescription = "Profile picture",
                modifier = Modifier.size(142.dp).clip(CircleShape).background(Color.LightGray)
            )
            Surface(onClick = { picker.launch("image/*") }, shape = CircleShape, color = Color.Black) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.padding(10.dp).size(22.dp))
            }
        }
        OutlinedTextField(username, { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(displayName, { displayName = it }, label = { Text("Display name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(bio, { bio = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth().height(120.dp))
        Spacer(Modifier.weight(1f))
        Button(
            onClick = { onSave(profile.copy(username = username, displayName = displayName, bio = bio), pickedImage) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(26.dp)
        ) { Text("Save Changes", fontWeight = FontWeight.Black) }
        TextButton(onClick = onDismiss) { Text("Cancel") }
    }
}
