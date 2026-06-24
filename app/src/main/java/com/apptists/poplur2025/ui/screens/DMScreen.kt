package com.apptists.poplur2025.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
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
    var selectedThread by remember { mutableStateOf<ConversationThread?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var callTarget by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // name, isVideo
    val threads = remember { FakeChatStore.threads }
    val filteredThreads = remember(searchQuery, threads) {
        val query = searchQuery.trim()
        if (query.isEmpty()) threads
        else threads.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.lastMessage.contains(query, ignoreCase = true)
        }
    }

    LaunchedEffect(Unit) { onPhoneCallVisibilityChanged(false) }
    DisposableEffect(Unit) { onDispose { onPhoneCallVisibilityChanged(false) } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        if (selectedThread == null) {
            Column(modifier = Modifier.fillMaxSize()) {
                DmTopBar()

                DmSearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 18.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 10.dp)
                ) {
                    itemsIndexed(filteredThreads, key = { _, thread -> thread.id }) { index, thread ->
                        DmThreadRow(
                            threadId = thread.id,
                            name = thread.title,
                            preview = thread.lastMessage,
                            onOpen = { selectedThread = thread },
                            onVideoCall = { callTarget = thread.title to true }
                        )
                        if (index < filteredThreads.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 88.dp, end = 20.dp),
                                thickness = 1.dp,
                                color = Color.White.copy(alpha = 0.08f)
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(220.dp)) }
                }
            }
        } else {
            DMChatDetailScreen(
                thread = selectedThread!!,
                onBack = { selectedThread = null },
                onVideoCall = { callTarget = selectedThread!!.title to true },
                onAudioCall = { callTarget = selectedThread!!.title to false }
            )
        }

        // Full-screen call overlay — routes through Poplur phone UI
        callTarget?.let { (name, isVideo) ->
            DMCallCoordinatorScreen(
                calleeName = name,
                isVideo = isVideo,
                onDismiss = { callTarget = null }
            )
        }
    }
}

@Composable
private fun DmTopBar() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DmLogo(modifier = Modifier.size(84.dp))

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Chats",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp
                )
                Text(
                    text = "Recent messages",
                    color = Color(0xFF94949C),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF141416))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .clickable(onClick = {}),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "New message",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun DmLogo(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val logoBitmap = remember {
        runCatching {
            context.assets.open("logo.png").use { BitmapFactory.decodeStream(it).asImageBitmap() }
        }.getOrNull()
    }

    if (logoBitmap != null) {
        Image(
            bitmap = logoBitmap,
            contentDescription = "Poplur",
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    } else {
        Text(
            text = "POPLUR",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
            modifier = modifier
        )
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
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF131316))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = Color(0xFF94949C),
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Search",
                    color = Color(0xFF8F8F96),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp
                ),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DmThreadRow(
    threadId: String,
    name: String,
    preview: String,
    onOpen: () -> Unit,
    onVideoCall: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clickable(onClick = onOpen)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DmAvatar(name = name, isOnline = true, modifier = Modifier.size(52.dp))

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = threadTimestamp(threadId),
                    color = if (threadUnreadCount(threadId) > 0) Color(0xFF61FF78) else Color(0xFF8F8F96),
                    fontSize = 13.sp,
                    fontWeight = if (threadUnreadCount(threadId) > 0) FontWeight.SemiBold else FontWeight.Normal,
                    letterSpacing = 0.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = preview,
                    color = Color(0xFFA8A8B0),
                    fontSize = 15.sp,
                    fontWeight = if (threadUnreadCount(threadId) > 0) FontWeight.SemiBold else FontWeight.Normal,
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

        Box(
            modifier = Modifier
                .size(36.dp)
                .clickable(onClick = onVideoCall),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "Video call $name",
                tint = Color(0xFFD0D0D8),
                modifier = Modifier.size(18.dp)
            )
        }
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
                .background(Color(0xFF1A1A1F))
                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initialsFor(name),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
        }
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(11.dp)
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
    onVideoCall: () -> Unit,
    onAudioCall: () -> Unit = {}
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

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DmCircleIconButton(
                    modifier = Modifier.size(44.dp),
                    onClick = onAudioCall
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call ${thread.title}",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DmCircleIconButton(
                    modifier = Modifier.size(44.dp),
                    onClick = onVideoCall
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Video call ${thread.title}",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
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
