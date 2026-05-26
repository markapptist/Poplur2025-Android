package com.apptists.poplur2025.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apptists.poplur2025.data.FakeChatStore

@Composable fun MessagesScreen() { LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { item { Text("Messages", style = MaterialTheme.typography.headlineMedium) }; items(FakeChatStore.threads) { t -> ListItem(headlineContent = { Text(t.title) }, supportingContent = { Text(t.lastMessage) }) } } }
