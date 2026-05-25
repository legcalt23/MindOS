package com.example.neuv3.features.waves.presentation

import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.neuv3.features.dm.presentation.rememberRawImageBitmap

@Composable
actual fun WaveMailboxIcon(
    hasPendingRequests: Boolean,
    modifier: Modifier,
) {
    val imageBitmap = rememberRawImageBitmap(
        resourceName = if (hasPendingRequests) "buzon_lleno_icon" else "buzon_icon"
    )

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = "Abrir buzón de WAVES",
            modifier = modifier,
        )
    } else {
        Icon(
            imageVector = Icons.Default.MailOutline,
            contentDescription = "Abrir buzón de WAVES",
            modifier = modifier,
        )
    }
}
