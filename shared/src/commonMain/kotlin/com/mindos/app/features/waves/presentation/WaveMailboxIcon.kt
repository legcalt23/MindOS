package com.mindos.app.features.waves.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun WaveMailboxIcon(
    hasPendingRequests: Boolean,
    modifier: Modifier = Modifier,
)
