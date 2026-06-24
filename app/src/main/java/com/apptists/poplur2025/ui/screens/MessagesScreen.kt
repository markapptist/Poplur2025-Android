package com.apptists.poplur2025.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun MessagesScreen(onBack: () -> Unit = {}) {
    DMScreen(onBack = onBack)
}

@Preview
@Composable
fun MessagesScreenPreview() {
    MaterialTheme {
        MessagesScreen()
    }
}
