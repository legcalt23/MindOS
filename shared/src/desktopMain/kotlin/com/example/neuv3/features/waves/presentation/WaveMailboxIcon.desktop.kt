package com.example.neuv3.features.waves.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.neuv3.features.dm.presentation.DefaultMailboxIcon

@Composable
actual fun WaveMailboxIcon(
    hasPendingRequests: Boolean,
    modifier: Modifier,
) {
    DefaultMailboxIcon(modifier = modifier)
}
