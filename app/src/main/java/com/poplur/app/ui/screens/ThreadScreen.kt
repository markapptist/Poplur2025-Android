package com.poplur.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poplur.app.data.models.DMMessage
import com.poplur.app.ui.vm.MessagesViewModel

@Composable
fun ThreadScreen(
    threadId: String,
    onBack: () -> Unit,
    vm: MessagesViewModel = viewModel()
) {
    val threads by vm.threads.collectAsState()
    val thread = threads.firstOrNull { it.id == threadId }

    var text by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(thread?.let { vm.displayName(it.participantIds) } ?: "Thread") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            }
        )

        if (thread == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Thread not found")
            }
            return
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(thread.messages, key = { it.id }) { msg ->
                MessageBubble(
                    msg = msg,
                    isMe = (msg.senderId == vm.currentUser.id),
                    senderName = vm.user(msg.senderId)?.displayName ?: "User"
                )
            }
        }

        Divider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message") }
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    val trimmed = text.trim()
                    if (trimmed.isNotEmpty()) {
                        vm.send(threadId, trimmed)
                        text = ""
                    }
                }
            ) { Text("Send") }
        }
    }
}

@Composable
private fun MessageBubble(msg: DMMessage, isMe: Boolean, senderName: String) {
    val align = if (isMe) Alignment.End else Alignment.Start
    Column(Modifier.fillMaxWidth(), horizontalAlignment = align) {
        Text(
            text = if (isMe) "You" else senderName,
            style = MaterialTheme.typography.labelSmall
        )
        Surface(
            tonalElevation = 2.dp,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                msg.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}
