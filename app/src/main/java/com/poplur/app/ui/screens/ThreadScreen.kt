package com.poplur.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    if (thread == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("Thread not found", color = Color.White, fontWeight = FontWeight.Bold)
        }
        return
    }

    val title = vm.displayName(thread.participantIds).ifBlank { "Messages" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIconButton(onClick = onBack, modifier = Modifier.size(52.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            InitialsAvatar(name = title, isOnline = true, modifier = Modifier.size(48.dp))

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Active now",
                    color = Color(0xFF8F8F96),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp
                )
            }

            RoundIconButton(onClick = {}, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Default.Call, contentDescription = "Call $title", tint = Color.White)
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(Modifier.height(12.dp)) }
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Today",
                        color = Color(0xFF9F9FA8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF141417))
                            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
            items(thread.messages, key = { it.id }) { msg ->
                MessageBubble(msg = msg, isMe = msg.senderId == vm.currentUser.id)
            }
            item { Spacer(Modifier.height(12.dp)) }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIconButton(onClick = {}, modifier = Modifier.size(46.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Add attachment", tint = Color(0xFFB8B8C4))
            }

            Spacer(Modifier.width(10.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Message $title", color = Color.White.copy(alpha = 0.45f), fontWeight = FontWeight.Bold)
                },
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                ),
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White.copy(alpha = 0.2f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedContainerColor = Color(0xFF171719),
                    unfocusedContainerColor = Color(0xFF171719),
                    cursorColor = Color.White
                )
            )

            Spacer(Modifier.width(10.dp))

            IconButton(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (text.isBlank()) Color(0xFF2C2C31) else Color(0xFF3B97FF)),
                enabled = text.isNotBlank(),
                onClick = {
                    val trimmed = text.trim()
                    if (trimmed.isNotEmpty()) {
                        vm.send(threadId, trimmed)
                        text = ""
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: DMMessage, isMe: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .clip(
                    RoundedCornerShape(
                        topStart = 22.dp,
                        topEnd = 22.dp,
                        bottomStart = if (isMe) 22.dp else 6.dp,
                        bottomEnd = if (isMe) 6.dp else 22.dp
                    )
                )
                .background(if (isMe) Color(0xFF3B97FF) else Color(0xFF1D1D20))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                Text(
                    text = msg.text,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 22.sp,
                    letterSpacing = 0.sp
                )
                Spacer(Modifier.height(7.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = shortTime(msg.timestampMs),
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.sp
                    )
                    if (isMe) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "✓",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    IconButton(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFF171719))
            .border(1.dp, Color.White.copy(alpha = 0.09f), CircleShape),
        onClick = onClick
    ) {
        content()
    }
}
