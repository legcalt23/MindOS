package com.example.mindos.features.social.presentation.media

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.mindos.features.social.domain.model.AppMediaContent
import com.example.mindos.features.social.shared.util.MediaContentResolver
import com.example.mindos.features.social.shared.util.MediaContentType
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.storage.storage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.border
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

@Composable
fun AppMediaViewer(
    content: AppMediaContent,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentKey = "${content.storagePath}|${content.url}|${content.thumbnailUrl.orEmpty()}"
    var downloadUrl by remember(contentKey) { mutableStateOf<String?>(null) }
    var thumbnailDownloadUrl by remember(contentKey) { mutableStateOf<String?>(null) }
    var mediaError by remember(contentKey) { mutableStateOf<MediaError?>(null) }
    var isVisible by remember { mutableStateOf(false) }
    var isPlaying by remember(contentKey) { mutableStateOf(true) }
    var showControls by remember(contentKey) { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    LaunchedEffect(contentKey) {
        downloadUrl = null
        thumbnailDownloadUrl = null
        mediaError = null
        try {
            thumbnailDownloadUrl = resolveOptionalUrl(content.thumbnailUrl)
            downloadUrl = resolveRequiredUrl(content.url, content.storagePath)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            mediaError = mapToMediaError(e)
        }
    }

    val type = remember(content.mimeType, downloadUrl, thumbnailDownloadUrl, content.url) {
        MediaContentResolver.resolve(content.mimeType, downloadUrl ?: thumbnailDownloadUrl ?: content.url)
    }
    val isPlayable = type == MediaContentType.Video || type == MediaContentType.Audio

    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(2500)
            showControls = false
        }
    }

    ViewerShell(
        isVisible = isVisible,
        onDismiss = onDismiss,
        fileName = content.fileName,
        info = if (downloadUrl != null) {
            "${type.name} \u2022 ${content.sizeBytes.formatSize()}"
        } else null,
        modifier = modifier
    ) {
        when {
            mediaError != null -> ViewerErrorView(error = requireNotNull(mediaError), onDismiss = onDismiss)
            else -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    NativeMediaRenderer(
                        url = downloadUrl,
                        storagePath = content.storagePath,
                        thumbnailUrl = thumbnailDownloadUrl,
                        type = type,
                        isPlaying = isPlaying,
                        onDismiss = onDismiss,
                        onError = { mediaError = it },
                        modifier = Modifier.fillMaxSize().clickable { 
                            if (isPlayable) showControls = !showControls
                        }
                    )

                    if (downloadUrl == null) {
                        ViewerLoading()
                    }

                    if (isPlayable && downloadUrl != null) {
                        VideoControlsOverlay(
                            isPlaying = isPlaying,
                            showControls = showControls,
                            onTogglePlay = { isPlaying = !isPlaying },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoControlsOverlay(
    isPlaying: Boolean,
    showControls: Boolean,
    onTogglePlay: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.animation.AnimatedVisibility(
            visible = showControls,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.8f),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(targetScale = 0.8f)
        ) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(CircleShape)
                    .background(ViewerDesign.Black.copy(alpha = 0.4f))
                    .border(1.dp, ViewerDesign.White.copy(alpha = 0.1f), CircleShape)
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = if (isPlaying) androidx.compose.material.icons.Icons.Default.Pause else androidx.compose.material.icons.Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = ViewerDesign.White,
                    modifier = Modifier.size(42.dp)
                )
            }
        }
    }
}

@Composable
expect fun NativeMediaRenderer(
    url: String?,
    storagePath: String,
    thumbnailUrl: String?,
    type: MediaContentType,
    isPlaying: Boolean,
    onDismiss: () -> Unit,
    onError: (MediaError) -> Unit,
    modifier: Modifier = Modifier,
)


private suspend fun resolveRequiredUrl(url: String, storagePath: String): String {
    directUrl(url)?.let { return it }
    if (url.startsWith("local-feed://") || storagePath.startsWith("local-feed/")) {
        throw IllegalStateException("local media is not available")
    }
    val path = storagePath.takeIf { it.isNotBlank() } ?: url
    if (path.isBlank()) throw IllegalArgumentException("blank media url")
    return Firebase.storage.reference.child(path).getDownloadUrl()
}

private suspend fun resolveOptionalUrl(urlOrPath: String?): String? {
    val value = urlOrPath?.takeIf { it.isNotBlank() } ?: return null
    directUrl(value)?.let { return it }
    return try {
        Firebase.storage.reference.child(value).getDownloadUrl()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
}

private fun directUrl(value: String): String? =
    value.takeIf {
        it.startsWith("http://", ignoreCase = true) ||
            it.startsWith("https://", ignoreCase = true) ||
            it.startsWith("file://", ignoreCase = true) ||
            it.startsWith("content://", ignoreCase = true)
    }

private fun mapToMediaError(e: Exception): MediaError = when {
    e.message?.contains("permission", ignoreCase = true) == true -> MediaError.PermissionDenied
    e.message?.contains("timeout", ignoreCase = true) == true -> MediaError.NetworkTimeout
    e.message?.contains("local media", ignoreCase = true) == true -> MediaError.NotFound
    e.message?.contains("blank", ignoreCase = true) == true -> MediaError.InvalidUrl
    else -> MediaError.ConnectionError
}

private fun Long.formatSize(): String = when {
    this >= 1024 * 1024 -> "${this / (1024 * 1024)} MB"
    this >= 1024 -> "${this / 1024} KB"
    else -> "$this B"
}
