package com.mindos.app.features.waves.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mindos.app.features.dm.presentation.DefaultMailboxIcon

@Composable
actual fun WaveMailboxIcon(
    hasPendingRequests: Boolean,
    modifier: Modifier,
) {
    DefaultMailboxIcon(modifier = modifier)
}
