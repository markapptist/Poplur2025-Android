package com.apptists.poplur2025.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object DemoPostStore {
    // Videos with people speaking — required for closed-caption testing
    private val videoUrls = listOf(
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFuns.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4"
    )

    private val imageUrls = listOf(
        "file:///android_asset/feed_media/feed_10.jpg",
        "file:///android_asset/feed_media/feed_28.jpg",
        "file:///android_asset/feed_media/feed_37.jpg",
        "file:///android_asset/feed_media/feed_58.jpg",
        "file:///android_asset/feed_media/feed_82.jpg",
        "file:///android_asset/feed_media/feed_96.jpg",
        "file:///android_asset/feed_media/feed_102.jpg",
        "file:///android_asset/feed_media/feed_119.jpg",
        "file:///android_asset/feed_media/feed_128.jpg",
        "file:///android_asset/feed_media/feed_164.jpg",
        "file:///android_asset/feed_media/feed_175.jpg",
        "file:///android_asset/feed_media/feed_184.jpg"
    )

    private val adImageUrls = listOf(
        "file:///android_asset/feed_ads/poplur_ad_news_first.jpg",
        "file:///android_asset/feed_ads/poplur_ad_3d_group.jpg",
        "file:///android_asset/feed_ads/poplur_ad_3d_creator.jpg"
    )

    private val userNames = listOf("mark", "mia", "jay", "food_u", "sky_high", "pixel_art", "traveler", "coder", "gamer", "chef", "vibe_check", "tech_wiz", "nature_lover", "urban_explorer", "fitness_junkie")
    private val displayNames = listOf("Mark", "Mia", "Jay", "Foodie", "Sky High", "Pixel Artist", "World Traveler", "Code Master", "Gamer Pro", "Chef Extra", "Vibe Queen", "Tech Wiz", "Forest Dweller", "City Slicker", "Fitness Pro")
    private val cities = listOf("Toronto, ON", "Las Vegas, NV", "San Francisco, CA", "New York, NY", "London, UK", "Tokyo, JP", "Paris, FR", "Berlin, DE", "Sydney, AU", "Dubai, UAE", "Seoul, KR", "Mumbai, IN", "Rio, BR", "Cairo, EG", "Rome, IT")

    private val userCreatedPosts = mutableStateListOf<Post>()
    val liveBroadcastRevision = mutableIntStateOf(0)

    val liveLocationChoices = listOf(
        LiveBroadcastLocation("austin", "Austin", "Downtown live radius", 30.2672, -97.7431, 350_000),
        LiveBroadcastLocation("toronto", "Toronto", "City center broadcast", 43.6532, -79.3832, 700_000),
        LiveBroadcastLocation("new-york", "New York", "Metro live channel", 40.7128, -74.0060, 650_000),
        LiveBroadcastLocation("los-angeles", "Los Angeles", "West coast live channel", 34.0522, -118.2437, 800_000),
        LiveBroadcastLocation("london", "London", "Central city broadcast", 51.5074, -0.1278, 650_000)
    )

    private val liveBroadcasts = mutableStateListOf<Post>().apply {
        addAll(
            listOf(
                livePost("seed_live_austin", "Austin Live", liveLocationChoices[0], 214),
                livePost("seed_live_toronto", "Toronto Live", liveLocationChoices[1], 188),
                livePost("seed_live_new_york", "New York Live", liveLocationChoices[2], 241),
                livePost("seed_live_los_angeles", "Los Angeles Live", liveLocationChoices[3], 176)
            )
        )
    }

    private val staticPosts: List<Post> = (0 until 200).map { i ->
        val type = i % 10
        val isVideo = type < 6 // 60% videos
        val isAd = false // Poplur-owned ad slots are inserted by FeedScreen.
        
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
        val allPosts = posts
        val positiveIndex = if (index < 0) {
            (allPosts.size + (index % allPosts.size)) % allPosts.size
        } else {
            index % allPosts.size
        }
        return allPosts[positiveIndex]
    }

    fun addUserPost(
        uri: String,
        isVideo: Boolean,
        latitude: Double,
        longitude: Double,
        channelId: String,
        channelTitle: String,
        filterId: String,
        trimSeconds: Float?,
        tvMetadata: TVStyleMetadata?
    ): Post {
        val caption = tvMetadata?.caption?.takeIf { it.isNotBlank() }
            ?: "POPLUR ${channelTitle.uppercase()} BROADCAST"
        val post = Post(
            id = "user_post_${System.currentTimeMillis()}",
            userId = "demo-user",
            username = "mark",
            displayName = "Mark",
            userPostsCount = userCreatedPosts.size + 13,
            caption = caption,
            mediaUrl = if (isVideo) null else uri,
            videoUrl = if (isVideo) uri else null,
            latitude = latitude,
            longitude = longitude,
            locationName = "Selected Location",
            likes = 0,
            comments = 0,
            detectedObjects = listOfNotNull(filterId, channelId, tvMetadata?.tvFormat),
            radius = 5_000,
            totalRadius = 5_000 + ((latitude + longitude).roundToInt().mod(600)),
            radiusChange = 120,
            channelId = channelId,
            filterId = filterId,
            tvBroadcastName = tvMetadata?.broadcastName ?: "POPLUR ${channelTitle.uppercase()}",
            videoTrimSeconds = trimSeconds
        )
        userCreatedPosts.add(0, post)
        return post
    }

    val posts: List<Post> get() = userCreatedPosts + staticPosts
    val livePosts: List<Post> get() = liveBroadcasts

    fun startLiveBroadcast(location: LiveBroadcastLocation, viewerCount: Int = 128): Post {
        liveBroadcasts.removeAll { it.isLiveStream && it.userId == "demo-user" }
        val now = System.currentTimeMillis()
        val post = Post(
            id = "local_live_$now",
            userId = "demo-user",
            username = "mark",
            displayName = "Mark Live",
            userPostsCount = userCreatedPosts.size + 13,
            caption = "LIVE from ${location.title}",
            mediaUrl = null,
            videoUrl = null,
            latitude = location.latitude,
            longitude = location.longitude,
            locationName = location.title,
            likes = viewerCount.coerceAtLeast(1),
            comments = 0,
            detectedObjects = listOf("live", "radius"),
            radius = location.radiusMeters,
            totalRadius = location.radiusMeters,
            radiusChange = 120,
            channelId = "live",
            tvBroadcastName = "POPLUR LIVE",
            isLiveStream = true,
            liveStartedAtMillis = now,
            liveRadiusMeters = location.radiusMeters
        )
        liveBroadcasts.add(0, post)
        liveBroadcastRevision.intValue += 1
        return post
    }

    fun stopLocalLiveBroadcast() {
        val removed = liveBroadcasts.removeAll { it.isLiveStream && it.userId == "demo-user" }
        if (removed) {
            liveBroadcastRevision.intValue += 1
        }
    }

    fun visibleLivePostsForViewer(anchorPost: Post?, limit: Int = 4): List<Post> {
        val anchorLat = anchorPost?.latitude
        val anchorLon = anchorPost?.longitude
        val viewerRadius = (anchorPost?.radius ?: 80_000).coerceAtLeast(5_000)
        val visible = liveBroadcasts.filter { post ->
            if (post.userId == "demo-user") return@filter true
            val liveLat = post.latitude
            val liveLon = post.longitude
            if (anchorLat == null || anchorLon == null || liveLat == null || liveLon == null) {
                return@filter true
            }
            radiiIntersect(
                anchorLat,
                anchorLon,
                viewerRadius,
                liveLat,
                liveLon,
                post.liveRadiusMeters ?: post.radius
            )
        }
        return visible.take(limit)
    }

    val dominance = listOf(
        CityDominance("Toronto", "mia", 88, 43.6532, -79.3832),
        CityDominance("Las Vegas", "mark", 97, 36.1699, -115.1398),
        CityDominance("San Francisco", "jay", 79, 37.7749, -122.4194)
    )

    private fun livePost(id: String, title: String, location: LiveBroadcastLocation, viewers: Int): Post =
        Post(
            id = id,
            userId = id.removePrefix("seed_"),
            username = title.lowercase().replace(" ", "_"),
            displayName = title,
            userPostsCount = viewers,
            caption = "LIVE from ${location.title}",
            mediaUrl = null,
            videoUrl = null,
            latitude = location.latitude,
            longitude = location.longitude,
            locationName = location.title,
            likes = viewers,
            comments = 0,
            detectedObjects = listOf("live", "radius"),
            radius = location.radiusMeters,
            totalRadius = location.radiusMeters,
            radiusChange = 90,
            channelId = "live",
            tvBroadcastName = "POPLUR LIVE",
            isLiveStream = true,
            liveStartedAtMillis = System.currentTimeMillis() - viewers * 5_000L,
            liveRadiusMeters = location.radiusMeters
        )

    private fun radiiIntersect(
        firstLatitude: Double,
        firstLongitude: Double,
        firstRadius: Int,
        secondLatitude: Double,
        secondLongitude: Double,
        secondRadius: Int
    ): Boolean {
        return distanceMeters(firstLatitude, firstLongitude, secondLatitude, secondLongitude) <= firstRadius + secondRadius
    }

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6_371_000.0
        val dLat = (lat2 - lat1) * PI / 180.0
        val dLon = (lon2 - lon1) * PI / 180.0
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) *
            sin(dLon / 2) * sin(dLon / 2)
        return earthRadius * 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
    }
}

object FakeChatStore {
    val threads = listOf(
        ConversationThread("t1", "Mia", "Let’s post at the food market", listOf(ChatUser("u2", "Mia"))),
        ConversationThread("t2", "Jay", "The map animation is ready", listOf(ChatUser("u3", "Jay"))),
        ConversationThread("t3", "Alex Johnson", "Reply to keep the conversation going", listOf(ChatUser("u4", "Alex Johnson")))
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
