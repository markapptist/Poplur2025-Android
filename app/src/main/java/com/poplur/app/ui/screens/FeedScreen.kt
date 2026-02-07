package com.poplur.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    Box(modifier = modifier) {
        // For real video playback, swap AsyncImage for ExoPlayer / Media3 video player.
        AsyncImage(
            model = post.mediaUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
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
        }
    }
}
