package com.example.mindos.features.dm.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
actual fun DMCajoneraEntryIcon(
    modifier: Modifier,
) {
    Icon(
        imageVector = Icons.Default.Menu,
        contentDescription = "Abrir cajonera",
        tint = Color.White,
        modifier = modifier,
    )
}
