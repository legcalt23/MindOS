package com.example.neuv3.features.social.presentation.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.neuv3.features.social.domain.feed.GlobalFeedAttachment
import com.example.neuv3.features.social.domain.feed.GlobalFeedPost
import com.example.neuv3.features.social.domain.feed.GlobalFeedPostType
import com.example.neuv3.features.social.presentation.profile.ProfileMediaRenderSource
import com.example.neuv3.features.social.presentation.profile.ProfilePlatformMediaPreview

private class DesktopFeedAudioPlayer : FeedAudioPlayer {
    private val _state = MutableStateFlow(FeedAudioPlayerState())
    override val state: StateFlow<FeedAudioPlayerState> = _state

    override fun playOrPause(source: FeedAudioSource) {
        _state.value = FeedAudioPlayerState(activeAudioId = source.audioId, isPlaying = false)
    }

    override fun stop() {
        _state.value = FeedAudioPlayerState()
    }
}

@Composable
actual fun rememberFeedAudioPlayer(): FeedAudioPlayer = remember { DesktopFeedAudioPlayer() }

private object DesktopFeedAttachmentDownloader : FeedAttachmentDownloader {
    override fun download(attachment: GlobalFeedAttachment, onProgress: (Float) -> Unit, onStateChanged: (Boolean) -> Unit) {
        onStateChanged(false)
        onProgress(0f)
    }

    override fun cancel() = Unit
}

@Composable
actual fun rememberFeedAttachmentDownloader(): FeedAttachmentDownloader = remember { DesktopFeedAttachmentDownloader }

@Composable
actual fun FeedPostMediaSurface(
    post: GlobalFeedPost,
    attachment: GlobalFeedAttachment?,
    isActive: Boolean,
    isPlaying: Boolean,
    modifier: Modifier,
) {
    when (post.postType) {
        GlobalFeedPostType.PHOTO,
        GlobalFeedPostType.VIDEO -> {
            val source = attachment?.let {
                ProfileMediaRenderSource(
                    storagePath = it.storagePath,
                    thumbnailStoragePath = it.thumbnailUrl,
                    pendingPreviewBytes = null,
                    isVideo = post.postType == GlobalFeedPostType.VIDEO,
                    contentDescription = it.fileName,
                )
            }
            if (source != null) {
                ProfilePlatformMediaPreview(source = source, modifier = modifier, contentScale = ContentScale.Crop) {
                    FeedDesktopFallback(modifier)
                }
            } else {
                FeedDesktopFallback(modifier)
            }
        }
        GlobalFeedPostType.TEXT,
        GlobalFeedPostType.AUDIO -> FeedDesktopFallback(modifier)
    }
}

@Composable
private fun FeedDesktopFallback(modifier: Modifier) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(Color(0xFF091615), Color(0xFF020304), Color(0xFF12201D))),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Text("MEDIA", color = Color.White.copy(alpha = 0.72f))
    }
}
