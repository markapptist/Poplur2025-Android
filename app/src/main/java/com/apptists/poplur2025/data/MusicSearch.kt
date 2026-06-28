package com.apptists.poplur2025.data

enum class MusicProvider(val displayName: String) {
    AppleMusic("Apple Music"),
    Spotify("Spotify"),
    Tidal("TIDAL"),
    SoundCloud("SoundCloud"),
    YouTubeMusic("YouTube Music"),
    Deezer("Deezer"),
    AmazonMusic("Amazon Music"),
    Bandcamp("Bandcamp"),
    Audiomack("Audiomack")
}

data class MusicSearchTrack(
    val id: String,
    val provider: MusicProvider,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String? = null,
    val previewUrl: String? = null,
    val externalUrl: String? = null,
    val durationText: String
)

object MusicSearchCatalog {
    fun search(query: String, provider: MusicProvider?): List<MusicSearchTrack> {
        val filtered = seedTracks.filter { provider == null || it.provider == provider }
        val needle = query.trim().lowercase()
        if (needle.isBlank()) return filtered.take(18)
        if (needle.contains("poplur")) return filtered.take(18)

        return filtered.filter { track ->
            track.title.lowercase().contains(needle) ||
                track.artist.lowercase().contains(needle) ||
                track.album.lowercase().contains(needle) ||
                track.provider.displayName.lowercase().contains(needle)
        }
    }

    private val seedTracks = listOf(
        track(MusicProvider.AppleMusic, "City Signal", "Poplur Radio", "Radius Nights", "3:04", "https://music.apple.com/search?term=city%20signal"),
        track(MusicProvider.Spotify, "Post It Where It Matters", "Poplur Creative", "Local Discovery", "2:38", "https://open.spotify.com/search/post%20it%20where%20it%20matters"),
        track(MusicProvider.Tidal, "Live Radius", "The Signal Team", "Neighborhood Energy", "2:51", "https://listen.tidal.com/search?q=live%20radius"),
        track(MusicProvider.SoundCloud, "Poplur Street Loop", "Radius FM", "Street Clips", "1:47", "https://soundcloud.com/search?q=poplur%20street%20loop"),
        track(MusicProvider.YouTubeMusic, "Channel 101 Intro", "Poplur TV", "Broadcast Pack", "0:45", "https://music.youtube.com/search?q=channel%20101%20intro"),
        track(MusicProvider.Deezer, "Mall Sale Sweep", "Local Signal", "Shopping Breaks", "2:13", "https://www.deezer.com/search/mall%20sale%20sweep"),
        track(MusicProvider.AmazonMusic, "Game Day Cut", "Stadium Pulse", "Sports Radius", "2:25", "https://music.amazon.com/search/game%20day%20cut"),
        track(MusicProvider.Bandcamp, "Late Feed Glow", "Independent Poplur", "Creator Bumps", "3:19", "https://bandcamp.com/search?q=late%20feed%20glow"),
        track(MusicProvider.Audiomack, "City Camera Roll", "Poplur Uploads", "Creator Cuts", "2:07", "https://audiomack.com/search?q=city%20camera%20roll"),
        track(MusicProvider.AppleMusic, "Food Channel Bounce", "CH 101", "Live Channels", "1:58", "https://music.apple.com/search?term=food%20channel%20bounce"),
        track(MusicProvider.Spotify, "Beauty Channel Flash", "CH 103", "Live Channels", "2:09", "https://open.spotify.com/search/beauty%20channel%20flash"),
        track(MusicProvider.SoundCloud, "Tech Late Feed", "CH 108", "Live Channels", "2:42", "https://soundcloud.com/search?q=tech%20late%20feed")
    )

    private fun track(
        provider: MusicProvider,
        title: String,
        artist: String,
        album: String,
        duration: String,
        externalUrl: String
    ): MusicSearchTrack {
        val id = "${provider.name}-$title-$artist".lowercase().replace(" ", "-")
        return MusicSearchTrack(
            id = id,
            provider = provider,
            title = title,
            artist = artist,
            album = album,
            externalUrl = externalUrl,
            durationText = duration
        )
    }
}
