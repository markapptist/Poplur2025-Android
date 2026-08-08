@file:OptIn(ExperimentalMaterial3Api::class)

package com.poplur.app.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poplur.app.data.models.DMThread
import com.poplur.app.ui.vm.MessagesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessagesScreen(
    onOpenThread: (String) -> Unit,
    vm: MessagesViewModel = viewModel()
) {
    val context = LocalContext.current
    val threads by vm.threads.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val filteredThreads = remember(searchQuery, threads) {
        val query = searchQuery.trim()
        if (query.isBlank()) {
            threads
        } else {
            threads.filter { thread ->
                vm.displayName(thread.participantIds).contains(query, ignoreCase = true) ||
                    (thread.lastMessage?.text?.contains(query, ignoreCase = true) == true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 20.dp)
    ) {
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
            CircleIconButton(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(58.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "New message",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Spacer(Modifier.height(48.dp))

        Text(
            text = "Recent",
            color = Color(0xFF9A9AA0),
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.sp,
            modifier = Modifier.padding(start = 20.dp)
        )

        Spacer(Modifier.height(14.dp))

        SearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it }
        )

        Spacer(Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredThreads, key = { it.id }) { thread ->
                ThreadRow(
                    thread = thread,
                    title = vm.displayName(thread.participantIds),
                    hasUnread = vm.hasUnread(thread.id),
                    onCall = {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:")))
                    },
                    onVideo = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://meet.google.com/new")))
                    },
                    onClick = {
                        vm.markRead(thread.id)
                        onOpenThread(thread.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun ThreadRow(
    thread: DMThread,
    title: String,
    hasUnread: Boolean,
    onCall: () -> Unit,
    onVideo: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(RoundedCornerShape(44.dp))
            .background(Color(0xFF1D1D20))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(44.dp))
            .clickable(onClick = onClick)
            .padding(start = 22.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InitialsAvatar(name = title, isOnline = true, modifier = Modifier.size(52.dp))

        Spacer(Modifier.width(18.dp))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title.ifBlank { "Messages" },
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = shortTime(thread.lastMessage?.timestampMs),
                    color = if (hasUnread) Color(0xFF61FF78) else Color(0xFF8F8F96),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = thread.lastMessage?.text ?: "Start the conversation",
                    color = Color(0xFFA8A8B0),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(if (hasUnread) 21.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (hasUnread) Color(0xFF61FF78) else Color(0xFF3B97FF)),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasUnread) {
                        Text("1", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        Spacer(Modifier.width(14.dp))

        CircleIconButton(
            modifier = Modifier
                .size(44.dp)
                .clickable(onClick = onCall)
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "Call $title",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        CircleIconButton(
            modifier = Modifier
                .size(44.dp)
                .clickable(onClick = onVideo)
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "Video call $title",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF6D6D72),
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94949C))
        },
        placeholder = {
            Text("Search", color = Color(0xFF8F8F96), fontWeight = FontWeight.Bold)
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
internal fun InitialsAvatar(name: String, isOnline: Boolean, modifier: Modifier = Modifier) {
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

@Composable
private fun CircleIconButton(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFF171719))
            .border(1.dp, Color.White.copy(alpha = 0.09f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

internal fun initialsFor(name: String): String {
    val initials = name
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
        .joinToString("")
    return initials.ifBlank { "P" }
}

internal fun shortTime(timestampMs: Long?): String {
    timestampMs ?: return "Now"
    return SimpleDateFormat("h:mm a", Locale.US).format(Date(timestampMs))
}
