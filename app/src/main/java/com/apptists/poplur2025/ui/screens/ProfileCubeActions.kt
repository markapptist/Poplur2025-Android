package com.apptists.poplur2025.ui.screens

enum class ProfileCubeMenuAction {
    Phone,
    Notifications,
    Store,
    Stats,
    Creator,
    GoHome
}

data class ProfileCubeMenuCommand(
    val id: Int,
    val action: ProfileCubeMenuAction
)
