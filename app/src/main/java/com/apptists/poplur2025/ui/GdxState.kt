package com.apptists.poplur2025.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object GdxState {
    var isSceneActive by mutableStateOf(false)
    var currentScene by mutableStateOf("radius")

    var sceneBoundsX by mutableStateOf(0f)
    var sceneBoundsY by mutableStateOf(0f)
    var sceneBoundsWidth by mutableStateOf(0f)
    var sceneBoundsHeight by mutableStateOf(0f)
}
