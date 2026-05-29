package com.example.mindos.features.waves.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun WaveMailboxIcon(
    hasPendingRequests: Boolean,
    modifier: Modifier,
) {
    Icon(
        imageVector = Icons.Default.MailOutline,
        contentDescription = "Abrir buzón de WAVES",
        modifier = modifier,
    )
}
