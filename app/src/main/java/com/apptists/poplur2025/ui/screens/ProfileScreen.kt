package com.apptists.poplur2025.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apptists.poplur2025.data.UserProfile

@Composable fun ProfileScreen(profile: UserProfile = UserProfile()) { Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { Text(profile.displayName, style = MaterialTheme.typography.displaySmall); Text("@${profile.username}"); Text(profile.bio);     Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ElevatedCard { Text("${profile.posts}\nPosts", Modifier.padding(16.dp)) }
        ElevatedCard { Text("${profile.followers}\nFollowers", Modifier.padding(16.dp)) }
        ElevatedCard { Text("${profile.radius}\nRadius", Modifier.padding(16.dp)) }
    }
} }
