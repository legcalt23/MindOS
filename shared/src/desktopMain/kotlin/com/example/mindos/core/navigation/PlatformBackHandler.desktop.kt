package com.example.mindos.core.navigation

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Desktop has no global back affordance; UI components own their own navigation.
}
