package com.mindos.app.features.social.presentation.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.mindos.app.features.social.domain.model.AppMediaContent
import com.mindos.app.features.social.presentation.profile.ProfileMediaRenderSource
import com.mindos.app.features.social.presentation.profile.ProfilePlatformMediaPreview
import com.mindos.app.features.social.shared.util.MediaContentResolver
import com.mindos.app.features.social.shared.util.MediaContentType

@Composable
fun AppMediaThumbnail(
    content: AppMediaContent,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable () -> Unit,
) {
    val contentType = remember(content.mimeType, content.url, content.storagePath) {
        MediaContentResolver.resolve(
            mimeType = content.mimeType,
            url = content.url.ifBlank { content.storagePath },
        )
    }

    ProfilePlatformMediaPreview(
        source = ProfileMediaRenderSource(
            storagePath = content.storagePath.takeIf { it.isNotBlank() } ?: content.url.takeIf { it.isNotBlank() },
            thumbnailStoragePath = content.thumbnailUrl?.takeIf { it.isNotBlank() },
            pendingPreviewBytes = null,
            isVideo = contentType == MediaContentType.Video,
            contentDescription = content.fileName,
        ),
        modifier = modifier,
        contentScale = contentScale,
        fallback = fallback,
    )
}
