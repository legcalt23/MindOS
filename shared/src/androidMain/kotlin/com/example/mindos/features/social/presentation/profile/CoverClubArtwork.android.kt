package com.example.mindos.features.social.presentation.profile

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight

private val ClubFallbackSurface = Color(0xFF18181B)
private val ClubFallbackText = Color(0xFFFAFAFA)

@Composable
internal actual fun CoverClubArtwork(
    index: Int,
    fallbackLabel: String,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val resourceName = when (index) {
        0 -> "psico_club"
        1 -> "actor_club"
        2 -> "fisica_club"
        else -> null
    }
    val artwork = remember(context, resourceName) {
        resourceName?.let { name ->
            val resourceId = context.resources.getIdentifier(name, "raw", context.packageName)
            if (resourceId == 0) {
                null
            } else {
                context.resources.openRawResource(resourceId).use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }
        }
    }

    if (artwork != null) {
        Image(
            bitmap = artwork,
            contentDescription = fallbackLabel,
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize(),
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(ClubFallbackSurface),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = fallbackLabel,
                color = ClubFallbackText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}
