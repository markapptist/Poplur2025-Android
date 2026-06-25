package com.apptists.poplur2025.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.badlogic.gdx.graphics.Color

enum class CubeSkin(val id: String, val label: String, val assetPath: String?, val gdxColor: Color?) {
    Default    ("default",   "Classic",     null,                    null),
    Gold       ("gold",      "24K Gold",    "cube/skin_gold.png",    Color(0.88f, 0.68f, 0.10f, 1f)),
    Toy        ("toy",       "Toy Box",     "cube/skin_toy.png",     Color(0.95f, 0.20f, 0.20f, 1f)),
    Rainbow    ("rainbow",   "Neon Rainbow","cube/skin_rainbow.png",  Color(0.55f, 0.10f, 0.95f, 1f)),
    Carbon     ("carbon",    "Carbon Pink", "cube/skin_carbon.png",  Color(1.00f, 0.40f, 0.70f, 1f)),
    Ice        ("ice",       "Arctic Ice",  "cube/skin_ice.png",     Color(0.75f, 0.92f, 1.00f, 1f)),
    Chocolate  ("chocolate", "Rich Cocoa",  "cube/skin_chocolate.png",Color(0.45f, 0.25f, 0.08f, 1f)),
    Space      ("space",     "Deep Space",  "cube/skin_space.png",   Color(0.08f, 0.05f, 0.20f, 1f)),
    Camo       ("camo",      "Stealth Camo","cube/skin_camo.png",    Color(0.30f, 0.48f, 0.18f, 1f))
}

object GdxState {
    var isSceneActive by mutableStateOf(false)
    var currentScene by mutableStateOf("radius")
    
    var selectedCubeSkin by mutableStateOf(CubeSkin.Default)

    var sceneBoundsX by mutableStateOf(0f)
    var sceneBoundsY by mutableStateOf(0f)
    var sceneBoundsWidth by mutableStateOf(0f)
    var sceneBoundsHeight by mutableStateOf(0f)
}
