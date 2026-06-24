package com.apptists.poplur2025.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object PoplurOverlayPositions {
    val horizontalPadding = 20.dp
    val bottomPadding = 48.dp
}

fun BoxScope.poplurRadiusDisplayModifier(
    modifier: Modifier = Modifier
): Modifier = modifier
    .align(Alignment.BottomEnd)
    .navigationBarsPadding()
    .padding(
        end = PoplurOverlayPositions.horizontalPadding,
        bottom = PoplurOverlayPositions.bottomPadding
    )

fun BoxScope.poplurCountdownDisplayModifier(
    modifier: Modifier = Modifier
): Modifier = modifier
    .align(Alignment.BottomStart)
    .navigationBarsPadding()
    .padding(
        start = PoplurOverlayPositions.horizontalPadding,
        bottom = PoplurOverlayPositions.bottomPadding
    )
