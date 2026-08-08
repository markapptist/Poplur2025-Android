package com.poplur.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.poplur.app.data.models.FeedPost
import com.poplur.app.ui.vm.FeedViewModel

@Composable
fun FeedScreen(vm: FeedViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.bootstrap() }

    val cfg = LocalConfiguration.current
    val pageHeight = cfg.screenHeightDp.dp

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (state.posts.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text("No posts available")
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { vm.reload() }) { Text("Reload") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(0.dp)
            ) {
                item {
                    FeedTopBar(
                        useYouTube = state.useYouTube,
                        onToggle = { vm.setUseYouTube(it) },
                        onReload = { vm.reload() }
                    )
                }
                itemsIndexed(state.posts, key = { _, p -> p.id }) { idx, post ->
                    FeedPage(
                        post = post,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(pageHeight)
                    )
                    LaunchedEffect(idx) { vm.loadMoreIfNeeded(idx) }
                }
                if (state.isLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedTopBar(useYouTube: Boolean, onToggle: (Boolean) -> Unit, onReload: () -> Unit) {
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Feed", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            Text("YouTube")
            Switch(checked = useYouTube, onCheckedChange = onToggle)
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onReload) { Text("Reload") }
        }
    }
}

@Composable
private fun FeedPage(post: FeedPost, modifier: Modifier = Modifier) {
    var radiusDelta by remember(post.id) { mutableStateOf(0.0) }
    var feedback by remember(post.id) { mutableStateOf<Int?>(null) }
    val cfg = LocalConfiguration.current
    val isExpandedFeed = cfg.screenWidthDp >= 700

    LaunchedEffect(post.id) {
        radiusDelta += 1.0
        if (post.viewCount <= 0) {
            kotlinx.coroutines.delay(60 * 60 * 1000L)
            radiusDelta -= 1.0
        }
    }

    LaunchedEffect(feedback) {
        if (feedback != null) {
            kotlinx.coroutines.delay(750)
            feedback = null
        }
    }

    Box(modifier = modifier) {
        AsyncImage(
            model = post.mediaUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        PoplurInlineAdSlot(
            modifier = if (isExpandedFeed) {
                Modifier
                    .align(Alignment.BottomEnd)
                    .width((cfg.screenWidthDp * 0.32f).dp.coerceAtMost(360.dp))
                    .padding(end = 28.dp, bottom = 228.dp)
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 18.dp, vertical = 148.dp)
            }
        )

        Row(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(post.id) {
                        detectTapGestures(
                            onDoubleTap = {
                                radiusDelta += 1.0
                                feedback = 1
                            }
                        )
                    }
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(post.id) {
                        detectTapGestures(
                            onDoubleTap = {
                                radiusDelta -= 1.0
                                feedback = -1
                            }
                        )
                    }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = post.userId,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "%.4f, %.4f".format(post.latitude, post.longitude),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "%.0f km radius".format((post.radius + radiusDelta).coerceAtLeast(0.0)),
                style = MaterialTheme.typography.labelSmall
            )
        }

        feedback?.let { value ->
            Icon(
                imageVector = if (value > 0) Icons.Default.ThumbUp else Icons.Default.ThumbDown,
                contentDescription = if (value > 0) "Liked" else "Down voted",
                tint = Color.White,
                modifier = Modifier
                    .align(if (value > 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 42.dp)
                    .size(84.dp)
            )
        }
    }
}

@Composable
private fun PoplurInlineAdSlot(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.32f),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("POPLUR AD", color = Color.White, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(10.dp))
            Text(
                "Google feed ads render here when production AdMob IDs are configured.",
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
