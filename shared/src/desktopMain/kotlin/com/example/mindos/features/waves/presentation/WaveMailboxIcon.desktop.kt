package com.example.mindos.features.waves.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.mindos.features.dm.presentation.DefaultMailboxIcon

@Composable
actual fun WaveMailboxIcon(
    hasPendingRequests: Boolean,
    modifier: Modifier,
) {
    DefaultMailboxIcon(modifier = modifier)
}
