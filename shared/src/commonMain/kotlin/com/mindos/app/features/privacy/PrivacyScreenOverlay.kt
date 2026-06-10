package com.mindos.app.features.privacy

import androidx.compose.runtime.Composable

@Composable
expect fun PrivacyScreenOverlay(enabled: Boolean, content: @Composable () -> Unit)
