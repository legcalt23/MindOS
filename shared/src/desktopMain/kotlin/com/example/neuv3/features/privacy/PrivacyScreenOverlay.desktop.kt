package com.example.neuv3.features.privacy

import androidx.compose.runtime.Composable

@Composable
actual fun PrivacyScreenOverlay(enabled: Boolean, content: @Composable () -> Unit) {
    content() // Stub for non-Android platforms
}
