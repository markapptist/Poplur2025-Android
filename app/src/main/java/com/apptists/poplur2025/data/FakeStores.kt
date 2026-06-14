package com.apptists.poplur2025.data

object DemoPostStore {
    private val videoUrls = listOf(
        "https://media.w3.org/2010/05/sintel/trailer.mp4",
        "https://media.w3.org/2010/05/bunny/trailer.mp4",
        "https://media.w3.org/2010/05/video/movie_300.mp4",
        "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
        "https://download.samplelib.com/mp4/sample-10s.mp4",
        "https://download.samplelib.com/mp4/sample-15s.mp4"
    )

    private val imageUrls = listOf(
        "https://picsum.photos/id/10/1080/1920",
        "https://picsum.photos/id/20/1080/1920",
        "https://picsum.photos/id/28/1080/1920",
        "https://picsum.photos/id/37/1080/1920",
        "https://picsum.photos/id/42/1080/1920",
        "https://picsum.photos/id/58/1080/1920",
        "https://picsum.photos/id/64/1080/1920",
        "https://picsum.photos/id/82/1080/1920",
        "https://picsum.photos/id/96/1080/1920",
        "https://picsum.photos/id/102/1080/1920",
        "https://picsum.photos/id/103/1080/1920",
        "https://picsum.photos/id/106/1080/1920",
        "https://picsum.photos/id/119/1080/1920",
        "https://picsum.photos/id/128/1080/1920",
        "https://picsum.photos/id/133/1080/1920",
        "https://picsum.photos/id/164/1080/1920",
        "https://picsum.photos/id/175/1080/1920",
        "https://picsum.photos/id/184/1080/1920",
        "https://picsum.photos/id/190/1080/1920",
        "https://picsum.photos/id/1025/1080/1920"
    )

    private val adImageUrls = listOf(
        "https://picsum.photos/id/21/1080/1920",
        "https://picsum.photos/id/26/1080/1920",
        "https://picsum.photos/id/48/1080/1920",
        "https://picsum.photos/id/1060/1080/1920"
    )

    private val userNames = listOf("mark", "mia", "jay", "food_u", "sky_high", "pixel_art", "traveler", "coder", "gamer", "chef", "vibe_check", "tech_wiz", "nature_lover", "urban_explorer", "fitness_junkie")
    private val displayNames = listOf("Mark", "Mia", "Jay", "Foodie", "Sky High", "Pixel Artist", "World Traveler", "Code Master", "Gamer Pro", "Chef Extra", "Vibe Queen", "Tech Wiz", "Forest Dweller", "City Slicker", "Fitness Pro")
    private val cities = listOf("Toronto, ON", "Las Vegas, NV", "San Francisco, CA", "New York, NY", "London, UK", "Tokyo, JP", "Paris, FR", "Berlin, DE", "Sydney, AU", "Dubai, UAE", "Seoul, KR", "Mumbai, IN", "Rio, BR", "Cairo, EG", "Rome, IT")

    private val staticPosts: List<Post> = (0 until 200).map { i ->
        val type = i % 10
        val isVideo = type < 6 // 60% videos
        val isAd = type == 9 // 10% ads (simulated in post store for more variety)
        
        Post(
            id = "post_$i",
            userId = if (isAd) "ad_partner" else "u${i % 15}",
            username = if (isAd) "sponsored" else userNames[i % 15],
            displayName = if (isAd) "Featured Brand" else displayNames[i % 15],
            userPostsCount = 10 + (i * 2),
            caption = when {
                isAd -> "Special offer for you! #ad"
                isVideo -> "Epic shot #$i"
                else -> "Captured moment #$i"
            },
            videoUrl = if (isVideo) videoUrls[i % videoUrls.size] else null,
            mediaUrl = if (isAd) {
                adImageUrls[i % adImageUrls.size]
            } else {
                imageUrls[(i * 7 + i / imageUrls.size) % imageUrls.size]
            },
            latitude = 30.0 + (i * 0.05),
            longitude = -100.0 + (i * 0.05),
            locationName = if (isAd) "Nearby" else cities[i % cities.size],
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
