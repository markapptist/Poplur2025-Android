package com.apptists.poplur2025.ui

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.apptists.poplur2025.R

/**
 * Loads a drawable resource as an [ImageBitmap], rasterizing it via the platform. Unlike Compose's
 * `painterResource`, this works for adaptive launcher icons (mipmap `<adaptive-icon>` XML), which
 * `painterResource` rejects with "Only VectorDrawables and rasterized asset types are supported".
 */
@Composable
fun rememberDrawableBitmap(@DrawableRes resId: Int): ImageBitmap? {
    val context = LocalContext.current
    return remember(resId) {
        ContextCompat.getDrawable(context, resId)
            ?.toBitmap(width = 192, height = 192)
            ?.asImageBitmap()
    }
}

@get:DrawableRes
val CubeSkin.appIconResId: Int
    get() = when (this) {
        CubeSkin.Default -> R.mipmap.ic_launcher
        CubeSkin.Gold -> R.mipmap.ic_launcher_gold
        CubeSkin.Toy -> R.mipmap.ic_launcher_toy
        CubeSkin.Rainbow -> R.mipmap.ic_launcher_rainbow
        // Clay is the house skin — its icon is the clay-P (ic_launcher), not the
        // old four-petal sparkle logo. Carbon still uses the sparkle art for now.
        CubeSkin.Clay -> R.mipmap.ic_launcher
        CubeSkin.Carbon -> R.mipmap.ic_launcher_sparkle_pink
        CubeSkin.Ice -> R.mipmap.ic_launcher_ice
        CubeSkin.Chocolate -> R.mipmap.ic_launcher_chocolate
        CubeSkin.Space -> R.mipmap.ic_launcher_space
        CubeSkin.Camo -> R.mipmap.ic_launcher_camo
}
