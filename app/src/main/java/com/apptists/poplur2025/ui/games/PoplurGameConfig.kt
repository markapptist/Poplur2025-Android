package com.apptists.poplur2025.ui.games

enum class PoplurGameType(val key: String, val displayName: String) {
    SideScroller("pop_runner", "Side Scroller"),
    ArcadeShooter("stella_blast", "Arcade Shooter"),
    ActionAdventure("zyla_quest", "Action / Adventure"),
    Sports("rex_hoops", "Sports")
}

data class PoplurGameConfig(
    val game: PoplurGameType,
    val character: String,  // character id
    val gameName: String,
    val theme: String,
    val skyColor: String,   // hex
    val groundColor: String,
    val accentColor: String,
    val difficulty: String,
    val speed: Float,
    val powerUps: List<String>,
    val enemyType: String,
    val specialMechanic: String
) {
    companion object {
        val Fallback = PoplurGameConfig(
            game = PoplurGameType.ArcadeShooter,
            character = "stella",
            gameName = "Stella Starfall",
            theme = "Space",
            skyColor = "#050713",
            groundColor = "#111B3D",
            accentColor = "#00F5FF",
            difficulty = "medium",
            speed = 280f,
            powerUps = listOf("triple shot", "shield"),
            enemyType = "neon drones",
            specialMechanic = "drag-to-fly auto fire"
        )
    }
}
