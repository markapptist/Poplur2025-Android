package com.poplur.app.data.youtube

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

data class YTPlaylistItemsResponse(
    val items: List<Item> = emptyList()
) {
    data class Item(
        val id: String,
        val snippet: Snippet
    ) {
        data class Snippet(
            val title: String,
            val description: String,
            val thumbnails: Thumbnails,
            @SerializedName("resourceId") val resourceId: ResourceId
        ) {
            data class ResourceId(
                @SerializedName("videoId") val videoId: String
            )
            data class Thumbnails(
                val medium: Thumbnail
            ) {
                data class Thumbnail(
                    val url: String
                )
            }
        }
    }
}

class YouTubeService(
    private val apiKey: String,
    private val client: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson()
) {

    suspend fun fetchTopFromPlaylist(
        playlistId: String,
        maxResults: Int = 20
    ): List<YTPlaylistItemsResponse.Item> {
        val base = "https://www.googleapis.com/youtube/v3/playlistItems".toHttpUrl()
        val url = base.newBuilder()
            .addQueryParameter("part", "snippet")
            .addQueryParameter("playlistId", playlistId)
            .addQueryParameter("maxResults", maxResults.toString())
            .addQueryParameter("key", apiKey)
            .build()

        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("YouTube API error: HTTP ${'$'}{resp.code}")
            }
            val body = resp.body?.string().orEmpty()
            val decoded = gson.fromJson(body, YTPlaylistItemsResponse::class.java)
            return decoded.items
        }
    }
}
