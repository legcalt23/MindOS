package com.example.mindos.features.social.presentation.media

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindos.features.social.shared.util.MediaContentType

@Composable
actual fun NativeMediaRenderer(
    url: String?,
    storagePath: String,
    thumbnailUrl: String?,
    type: MediaContentType,
    isPlaying: Boolean,
    onDismiss: () -> Unit,
    onError: (MediaError) -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ViewerDesign.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "VISTA PREVIA",
                color = ViewerDesign.Green,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "La vista previa nativa no está disponible en Escritorio todavía.",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
    }
}
