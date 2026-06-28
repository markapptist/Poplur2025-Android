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
            game = PoplurGameType.SideScroller,
            character = "pop",
            gameName = "Pop's City Run",
            theme = "Neon City",
            skyColor = "#1A1A2E",
            groundColor = "#16213E",
            accentColor = "#E94560",
            difficulty = "medium",
            speed = 280f,
            powerUps = listOf("star", "shield"),
            enemyType = "robo_blob",
            specialMechanic = "double_jump"
        )
    }
}
