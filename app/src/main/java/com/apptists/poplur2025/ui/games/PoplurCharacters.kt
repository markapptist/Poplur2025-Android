package com.apptists.poplur2025.ui.games

import androidx.compose.ui.graphics.Color

sealed class PoplurCharacter(
    val id: String,
    val displayName: String,
    val role: String,
    val primaryColor: Color,
    val accentColor: Color,
    val specialAbility: String
) {
    data object Pop : PoplurCharacter(
        id = "pop",
        displayName = "Pop",
        role = "Hero",
        primaryColor = Color(0xFFE94560),
        accentColor = Color(0xFF3A86FF),
        specialAbility = "Double Jump"
    )

    data object Zyla : PoplurCharacter(
        id = "zyla",
        displayName = "Zyla",
        role = "Adventurer",
        primaryColor = Color(0xFF2ECC71),
        accentColor = Color(0xFF1ABC9C),
        specialAbility = "Energy Blade"
    )

    data object Blip : PoplurCharacter(
        id = "blip",
        displayName = "Blip",
        role = "Speedster",
        primaryColor = Color(0xFF3A86FF),
        accentColor = Color(0xFFFFD60A),
        specialAbility = "Sonic Dash"
    )

    data object Rex : PoplurCharacter(
        id = "rex",
        displayName = "Rex",
        role = "Powerhouse",
        primaryColor = Color(0xFFFF6B35),
        accentColor = Color(0xFFFFB347),
        specialAbility = "Ground Smash"
    )

    data object Stella : PoplurCharacter(
        id = "stella",
        displayName = "Stella",
        role = "Tech Hero",
        primaryColor = Color(0xFF00F5FF),
        accentColor = Color(0xFF7B2FBE),
        specialAbility = "Laser Beam"
    )

    data object Glitch : PoplurCharacter(
        id = "glitch",
        displayName = "Glitch",
        role = "Wildcard",
        primaryColor = Color(0xFFFF6EFF),
        accentColor = Color(0xFFB026FF),
        specialAbility = "Phase Shift"
    )

    companion object {
        val all: List<PoplurCharacter> = listOf(Pop, Zyla, Blip, Rex, Stella, Glitch)

        fun fromId(id: String): PoplurCharacter =
            all.firstOrNull { it.id == id } ?: Pop
    }
}
