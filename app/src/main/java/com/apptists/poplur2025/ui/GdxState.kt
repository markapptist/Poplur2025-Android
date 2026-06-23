package com.apptists.poplur2025.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class CubeSkin(val id: String, val label: String, val assetPath: String?) {
    Default("default", "Classic", null),
    Gold("gold", "24K Gold", "cube/skin_gold.png"),
    Toy("toy", "Toy Box", "cube/skin_toy.png"),
    Rainbow("rainbow", "Neon Rainbow", "cube/skin_rainbow.png"),
    Carbon("carbon", "Carbon Pink", "cube/skin_carbon.png"),
    Ice("ice", "Arctic Ice", "cube/skin_ice.png"),
    Chocolate("chocolate", "Rich Cocoa", "cube/skin_chocolate.png"),
    Space("space", "Deep Space", "cube/skin_space.png"),
    Camo("camo", "Stealth Camo", "cube/skin_camo.png")
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
