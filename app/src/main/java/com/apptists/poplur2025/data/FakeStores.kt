package com.apptists.poplur2025.data

object DemoPostStore {
    val posts = listOf(
        Post("1", "u1", "mark", "Casino energy in Las Vegas", latitude = 36.1699, longitude = -115.1398, likes = 991, comments = 44, detectedObjects = listOf("building", "nightlife")),
        Post("2", "u2", "mia", "Best food spot downtown", latitude = 43.6532, longitude = -79.3832, likes = 611, comments = 31, detectedObjects = listOf("food", "restaurant")),
        Post("3", "u3", "jay", "Tech lab preview", latitude = 37.7749, longitude = -122.4194, likes = 820, comments = 19, detectedObjects = listOf("computer", "person"))
    )
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
