package com.apptists.poplur2025.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apptists.poplur2025.data.DemoPostStore

@Composable fun FeedScreen() { LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { items(DemoPostStore.posts) { post -> Card { Column(Modifier.padding(16.dp)) { Text("@${post.username}", style = MaterialTheme.typography.titleMedium); Text(post.caption); Text("${post.likes} likes • ${post.comments} comments"); if (post.detectedObjects.isNotEmpty()) Text("ML tags: ${post.detectedObjects.joinToString()}") } } } } }
