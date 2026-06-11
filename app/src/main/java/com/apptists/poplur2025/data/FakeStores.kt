package com.apptists.poplur2025.data

object DemoPostStore {
    private val videoUrls = listOf(
        "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/gear1/prog_index.m3u8",
        "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/gear1/prog_index.m3u8",
        "https://filesamples.com/samples/video/mp4/sample_640x360.mp4",
        "https://filesamples.com/samples/video/mp4/sample_960x400_ocean_with_audio.mp4",
        "https://media.w3.org/2010/05/sintel/trailer.mp4"
    )

    private val userNames = listOf("mark", "mia", "jay", "food_u", "sky_high", "pixel_art", "traveler", "coder", "gamer", "chef", "vibe_check", "tech_wiz", "nature_lover", "urban_explorer", "fitness_junkie")
    private val displayNames = listOf("Mark", "Mia", "Jay", "Foodie", "Sky High", "Pixel Artist", "World Traveler", "Code Master", "Gamer Pro", "Chef Extra", "Vibe Queen", "Tech Wiz", "Forest Dweller", "City Slicker", "Fitness Pro")
    private val cities = listOf("Toronto, ON", "Las Vegas, NV", "San Francisco, CA", "New York, NY", "London, UK", "Tokyo, JP", "Paris, FR", "Berlin, DE", "Sydney, AU", "Dubai, UAE", "Seoul, KR", "Mumbai, IN", "Rio, BR", "Cairo, EG", "Rome, IT")

    private val staticPosts: List<Post> = (0 until 200).map { i ->
        val isVideo = i % 8 != 0 // Higher percentage of videos
        Post(
            id = "post_$i",
            userId = "u${i % 15}",
            username = userNames[i % 15],
            displayName = displayNames[i % 15],
            userPostsCount = 10 + (i * 2),
            caption = if (isVideo) "Epic shot #$i" else "Captured moment #$i",
            videoUrl = if (isVideo) videoUrls[i % videoUrls.size] else null,
            mediaUrl = "https://picsum.photos/seed/poplur_v2_$i/1080/1920",
            latitude = 30.0 + (i * 0.05),
            longitude = -100.0 + (i * 0.05),
            locationName = cities[i % cities.size],
            likes = 50 + i * 12,
            comments = 5 + i,
            radius = 200 + i * 15,
            totalRadius = 1000 + i * 40,
            radiusChange = if (i % 3 == 0) i * 3 else -i
        )
    }

    /**
     * Infinite virtual store. Returns a post for any index by wrapping around the static pool.
     */
    fun getPostForIndex(index: Int): Post {
        val positiveIndex = if (index < 0) {
            (staticPosts.size + (index % staticPosts.size)) % staticPosts.size
        } else {
            index % staticPosts.size
        }
        return staticPosts[positiveIndex]
    }

    val posts: List<Post> get() = staticPosts

    val dominance = listOf(
        CityDominance("Toronto", "mia", 88, 43.6532, -79.3832),
        CityDominance("Las Vegas", "mark", 97, 36.1699, -115.1398),
        CityDominance("San Francisco", "jay", 79, 37.7749, -122.4194)
    )
}

object FakeChatStore {
    val threads = listOf(
        ConversationThread("t1", "Mia", "Let’s post at the food market", listOf(ChatUser("u2", "Mia"))),
        ConversationThread("t2", "Jay", "The map animation is ready", listOf(ChatUser("u3", "Jay")))
    )
}

object FilterRepository {
    val filters = listOf(
        ARFilter("clean", "Clean", "camera"),
        ARFilter("glow", "Glow", "camera"),
        ARFilter("scan", "Head Scan", "ml"),
        ARFilter("bitmoji", "Bitmoji", "avatar")
    )
}
