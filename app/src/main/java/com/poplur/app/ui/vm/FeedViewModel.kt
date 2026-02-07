package com.poplur.app.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poplur.app.data.models.FeedPost
import com.poplur.app.data.repo.FeedRepository
import com.poplur.app.data.youtube.YouTubeService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class FeedUiState(
    val isLoading: Boolean = false,
    val posts: List<FeedPost> = emptyList(),
    val reachedEnd: Boolean = false,
    val useYouTube: Boolean = true,
    val error: String? = null
)

class FeedViewModel : ViewModel() {

    // TODO: move API keys to local.properties / remote config
    private val youTubeApiKey: String = "REPLACE_WITH_YOUTUBE_API_KEY"
    private val youTubePlaylistId: String = "REPLACE_WITH_PLAYLIST_ID"

    private val repo = FeedRepository(
        youtube = YouTubeService(apiKey = youTubeApiKey)
    )

    private val _state = MutableStateFlow(FeedUiState())
    val state: StateFlow<FeedUiState> = _state

    private var pageStart = 0
    private val pageSize = 5

    fun bootstrap() {
        if (_state.value.posts.isNotEmpty() || _state.value.isLoading) return
        reload()
    }

    fun setUseYouTube(value: Boolean) {
        _state.value = _state.value.copy(useYouTube = value)
        reload()
    }

    fun reload() {
        pageStart = 0
        _state.value = _state.value.copy(isLoading = true, posts = emptyList(), reachedEnd = false, error = null)
        viewModelScope.launch {
            runCatching {
                if (_state.value.useYouTube) {
                    repo.loadFromYouTube(playlistId = youTubePlaylistId, maxResults = pageSize)
                } else {
                    repo.loadDemoPage(start = 0, count = pageSize)
                }
            }.onSuccess { posts ->
                _state.value = _state.value.copy(isLoading = false, posts = posts, reachedEnd = posts.isEmpty())
                pageStart = posts.size
            }.onFailure { e ->
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Unknown error")
            }
        }
    }

    fun loadMoreIfNeeded(appearingIndex: Int) {
        val s = _state.value
        if (s.isLoading || s.reachedEnd) return
        if (appearingIndex < s.posts.size - 3) return

        _state.value = s.copy(isLoading = true)
        viewModelScope.launch {
            runCatching {
                if (s.useYouTube) {
                    // YouTube playlistItems is offset-based with pageToken; simplified: we don't paginate YouTube in this demo.
                    emptyList()
                } else {
                    repo.loadDemoPage(start = pageStart, count = pageSize)
                }
            }.onSuccess { newItems ->
                val combined = s.posts + newItems
                _state.value = s.copy(isLoading = false, posts = combined, reachedEnd = newItems.isEmpty())
                pageStart = combined.size
            }.onFailure { e ->
                _state.value = s.copy(isLoading = false, error = e.message)
            }
        }
    }
}
