package com.apptists.poplur2025.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptists.poplur2025.data.ConversationThread
import com.apptists.poplur2025.data.FakeChatStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DMScreen(
    onBack: () -> Unit = {},
    onPhoneCallVisibilityChanged: (Boolean) -> Unit = {}
) {
    var callingUserName by remember { mutableStateOf<String?>(null) }
    var selectedThread by remember { mutableStateOf<ConversationThread?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val threads = remember { FakeChatStore.threads }
    val filteredThreads = remember(searchQuery, threads) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            threads
        } else {
            threads.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.lastMessage.contains(query, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(callingUserName) {
        onPhoneCallVisibilityChanged(callingUserName != null)
    }

    DisposableEffect(Unit) {
        onDispose { onPhoneCallVisibilityChanged(false) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        if (selectedThread == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                DmTopBar()

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = "Recent",
                    color = Color(0xFF9A9AA0),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.sp,
                    modifier = Modifier.padding(start = 20.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                DmSearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    filteredThreads.forEach { thread ->
                        DmThreadRow(
                            threadId = thread.id,
                            name = thread.title,
                            preview = thread.lastMessage,
                            onOpen = { selectedThread = thread },
                            onCall = { callingUserName = thread.title }
                        )
                    }
                }
            }
        } else {
            DMChatDetailScreen(
                thread = selectedThread!!,
                onBack = { selectedThread = null },
                onCall = { callingUserName = selectedThread!!.title }
            )
        }

        callingUserName?.let { userName ->
            PopularPhoneCallScreen(
                userName = userName,
                onHangUp = { callingUserName = null }
            )
        }
    }
}

@Composable
private fun DmTopBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
    ) {
        Text(
            text = "Messages",
            color = Color.White,
            fontSize = 33.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.sp,
            modifier = Modifier.align(Alignment.Center)
        )

        DmCircleIconButton(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(58.dp),
            onClick = {}
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "New message",
                tint = Color.White,
                modifier = Modifier.size(35.dp)
            )
        }
    }
}

@Composable
private fun DmCircleIconButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFF171719))
            .border(1.dp, Color.White.copy(alpha = 0.09f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun DmSearchField(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color(0xFF94949C),
                modifier = Modifier.size(20.dp)
            )
        },
        placeholder = {
            Text(
                text = "Search",
                color = Color(0xFF8F8F96),
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp
            )
        },
        textStyle = androidx.compose.ui.text.TextStyle(
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        ),
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.White.copy(alpha = 0.12f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
            focusedContainerColor = Color(0xFF171719),
            unfocusedContainerColor = Color(0xFF171719),
            cursorColor = Color.White
        )
    )
}

@Composable
private fun DmThreadRow(
    threadId: String,
    name: String,
    preview: String,
    onOpen: () -> Unit,
    onCall: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .clip(RoundedCornerShape(42.dp))
            .background(Color(0xFF1D1D20))
            .clickable(onClick = onOpen)
            .padding(start = 28.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DmAvatar(name = name, isOnline = true, modifier = Modifier.size(52.dp))

        Spacer(modifier = Modifier.width(18.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = threadTimestamp(threadId),
                    color = if (threadUnreadCount(threadId) > 0) Color(0xFF61FF78) else Color(0xFF8F8F96),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = preview,
                    color = Color(0xFFA8A8B0),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                val unreadCount = threadUnreadCount(threadId)
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(21.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF61FF78)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = unreadCount.toString(),
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B97FF))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        DmCircleIconButton(
            modifier = Modifier.size(44.dp),
            onClick = onCall
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "Call $name",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF6D6D72),
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun DmAvatar(
    name: String,
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color(0xFF24242A))
                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initialsFor(name),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp
            )
        }
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color(0xFF61FF78))
                    .border(2.dp, Color.Black, CircleShape)
            )
        }
    }
}

private fun initialsFor(name: String): String {
    val initials = name
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
        .joinToString("")
    return initials.ifBlank { "P" }
}

private fun threadTimestamp(threadId: String): String = when (threadId) {
    "t1" -> "9:42 AM"
    "t2" -> "Yesterday"
    else -> "Mon"
}

private fun threadUnreadCount(threadId: String): Int = when (threadId) {
    "t1" -> 2
    "t2" -> 1
    else -> 1
}

@Composable
private fun DMChatDetailScreen(
    thread: ConversationThread,
    onBack: () -> Unit,
    onCall: () -> Unit
) {
    val messages = remember(thread.id) {
        mutableStateListOf(
            ChatBubbleState(
                id = "${thread.id}-incoming-0",
                text = thread.lastMessage,
                isMine = false,
                time = "9:42 AM"
            ),
            ChatBubbleState(
                id = "${thread.id}-outgoing-0",
                text = "I saw it. I can respond here now.",
                isMine = true,
                time = "9:43 AM"
            )
        )
    }
    var draft by remember(thread.id) { mutableStateOf("") }

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
            DmAvatar(name = thread.title, isOnline = true, modifier = Modifier.size(48.dp))

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = thread.title,
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

            DmCircleIconButton(
                modifier = Modifier.size(52.dp),
                onClick = onCall
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call ${thread.title}",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
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
            items(messages, key = { it.id }) { message ->
                DMMessageBubble(message)
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
            .padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF171719))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                onClick = {}
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add attachment",
                    tint = Color(0xFFB8B8C4),
                    modifier = Modifier.size(25.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Message ${thread.title}",
                        color = Color.White.copy(alpha = 0.45f),
                        fontWeight = FontWeight.Bold
                    )
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

            Spacer(modifier = Modifier.width(10.dp))

            IconButton(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (draft.isBlank()) Color(0xFF2C2C31) else Color(0xFF3B97FF)),
                onClick = {
                    val text = draft.trim()
                    if (text.isNotEmpty()) {
                        messages.add(
                            ChatBubbleState(
                                id = "${thread.id}-${System.currentTimeMillis()}",
                                text = text,
                                isMine = true,
                                time = currentBubbleTime()
                            )
                        )
                        draft = ""
                    }
                },
                enabled = draft.isNotBlank()
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
private fun DMMessageBubble(message: ChatBubbleState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .clip(
                    RoundedCornerShape(
                        topStart = 22.dp,
                        topEnd = 22.dp,
                        bottomStart = if (message.isMine) 22.dp else 6.dp,
                        bottomEnd = if (message.isMine) 6.dp else 22.dp
                    )
                )
                .background(if (message.isMine) Color(0xFF3B97FF) else Color(0xFF1D1D20))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 22.sp,
                    letterSpacing = 0.sp
                )
                Spacer(modifier = Modifier.height(7.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.time,
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.sp
                    )
                    if (message.isMine) {
                        Spacer(modifier = Modifier.width(4.dp))
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

private data class ChatBubbleState(
    val id: String,
    val text: String,
    val isMine: Boolean,
    val time: String
)

private fun currentBubbleTime(): String =
    SimpleDateFormat("h:mm a", Locale.US).format(Date())

@Preview
@Composable
fun DMScreenPreview() {
    MaterialTheme {
        DMScreen()
    }
}
