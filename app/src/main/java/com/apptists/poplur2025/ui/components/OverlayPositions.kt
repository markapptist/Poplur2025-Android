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

object PoplurCounterLayout {
    val counterHeight = 136.dp
    val labelHeight = 16.dp
    val labelToNumeratorSpacing = 4.dp
    val numeratorHeight = 42.dp
    val numeratorToSeparatorSpacing = 2.dp
    val separatorWidth = 70.dp
    val separatorHeight = 2.dp
    val separatorToDenominatorSpacing = 4.dp
    val denominatorHeight = 40.dp
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
