package com.mindos.app.features.dm.presentation

import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun DMCajoneraEntryIcon(
    modifier: Modifier,
) {
    val imageBitmap = rememberRawImageBitmap("cajonera_icon")

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = "Abrir cajonera",
            modifier = modifier,
        )
    } else {
        Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = "Abrir cajonera",
            modifier = modifier,
        )
    }
}
