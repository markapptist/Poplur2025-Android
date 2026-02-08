@file:OptIn(ExperimentalMaterial3Api::class)

package com.poplur.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poplur.app.data.models.DMThread
import com.poplur.app.ui.vm.MessagesViewModel

@Composable
fun MessagesScreen(
    onOpenThread: (String) -> Unit,
    vm: MessagesViewModel = viewModel()
) {
    val threads by vm.threads.collectAsState()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Messages") })
        if (threads.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No threads")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(threads, key = { it.id }) { thread ->
                    ThreadRow(
                        thread = thread,
                        title = vm.displayName(thread.participantIds),
                        hasUnread = vm.hasUnread(thread.id),
                        onClick = {
                            vm.markRead(thread.id)
                            onOpenThread(thread.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThreadRow(thread: DMThread, title: String, hasUnread: Boolean, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (hasUnread) {
                    AssistChip(onClick = {}, label = { Text("New") })
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                thread.lastMessage?.text ?: "",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
        }
    }
}