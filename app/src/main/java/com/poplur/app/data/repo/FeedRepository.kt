package com.poplur.app.data.repo

import com.poplur.app.data.models.FeedPost
import com.poplur.app.data.youtube.YouTubeService
import com.poplur.app.data.youtube.YTPlaylistItemsResponse
import kotlin.random.Random

class FeedRepository(
    private val youtube: YouTubeService? = null
) {

    suspend fun loadDemoPage(start: Int, count: Int): List<FeedPost> {
        val totalCap = 30
        val end = (start + count).coerceAtMost(totalCap)
        if (start >= end) return emptyList()

        return (start until end).map { i ->
            FeedPost(
                id = "demo_$i",
                latitude = 37.3349 + (i % 5) * 0.01,
                longitude = -122.0090 - (i % 7) * 0.01,
                mediaUrl = "https://picsum.photos/800/1400?random=$i",
                radius = listOf(50, 100, 250, 500, 1000, 5000).random().toDouble(),
                viewCount = Random.nextInt(0, 10_000),
                userId = if (i % 3 == 0) "fake_user_$i" else "user_${"%06d".format(i)}"
            )
        }
    }

    suspend fun loadFromYouTube(playlistId: String, maxResults: Int): List<FeedPost> {
        val svc = youtube ?: throw IllegalStateException("YouTubeService not configured")
        val items = svc.fetchTopFromPlaylist(playlistId = playlistId, maxResults = maxResults)
        return items.mapToFeedPosts()
    }
}

private fun List<YTPlaylistItemsResponse.Item>.mapToFeedPosts(): List<FeedPost> {
    return map { item ->
        val vid = item.snippet.resourceId.videoId
        FeedPost(
            id = vid,
            latitude = 0.0,
            longitude = 0.0,
            mediaUrl = "https://www.youtube.com/watch?v=$vid",
            radius = 0.0,
            viewCount = 0,
            userId = "youtube"
        )
    }
}
