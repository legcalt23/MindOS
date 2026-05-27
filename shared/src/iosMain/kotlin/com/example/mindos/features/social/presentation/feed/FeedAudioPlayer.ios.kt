package com.example.mindos.features.social.presentation.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.mindos.features.social.domain.feed.GlobalFeedAttachment
import com.example.mindos.features.social.domain.feed.GlobalFeedPost
import com.example.mindos.features.social.domain.feed.GlobalFeedPostType
import com.example.mindos.features.social.presentation.profile.ProfileMediaRenderSource
import com.example.mindos.features.social.presentation.profile.ProfilePlatformMediaPreview
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.play
import platform.AVFoundation.pause
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVAudioSession
import platform.AVFoundation.AVAudioSessionCategoryPlayback
import platform.AVFoundation.setActive
import platform.Foundation.NSURL
import platform.Foundation.NSNotificationCenter
import platform.darwin.NSObject
import platform.CoreMedia.CMTimeMake

private class IosFeedAudioPlayer : FeedAudioPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(FeedAudioPlayerState())
    override val state: StateFlow<FeedAudioPlayerState> = _state

    private var player: AVPlayer? = null
    private var observer: NSObject? = null

    init {
        val session = AVAudioSession.sharedInstance()
        try {
            session.setCategory(AVAudioSessionCategoryPlayback, null)
            session.setActive(true, null)
        } catch (_: Exception) {}
        scope.launch {
            while (true) {
                val activePlayer = player
                if (activePlayer != null && _state.value.activeAudioId != null) {
                    val currentTime = activePlayer.currentTime()
                    val positionMs = ((currentTime.value.toDouble() / currentTime.timescale.toDouble()) * 1000.0).toLong().coerceAtLeast(0L)
                    val durationMs = _state.value.durationMs.takeIf { it > 0L } ?: positionMs
                    _state.value = _state.value.copy(
                        positionMs = positionMs,
                        durationMs = durationMs,
                        progress = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f),
                    )
                }
                delay(42L)
            }
        }
    }

    override fun playOrPause(source: FeedAudioSource) {
        if (_state.value.activeAudioId == source.audioId) {
            val nextPlaying = !_state.value.isPlaying
            if (nextPlaying) {
                val activePlayer = player
                if (activePlayer != null) {
                    val currentTime = activePlayer.currentTime()
                    val duration = activePlayer.currentItem?.duration
                    if (duration != null && currentTime.value >= duration.value) {
                        activePlayer.seekToTime(CMTimeMake(0, 1))
                    }
                    activePlayer.play()
                }
            } else {
                player?.pause()
            }
            _state.value = _state.value.copy(isPlaying = nextPlaying)
            return
        }
        val urlString = source.localPath?.takeIf { it.isNotBlank() }?.let { "file://$it" }
            ?: source.remoteUrl?.takeIf { it.isNotBlank() }
            ?: return
        val url = NSURL.URLWithString(urlString) ?: return
        val item = AVPlayerItem(uRL = url)
        val newPlayer = AVPlayer(playerItem = item)
        newPlayer.volume = 1.0f
        
        observer?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = item,
            queue = null
        ) { _ ->
            _state.value = _state.value.copy(isPlaying = false, progress = 1f)
        }

        player?.pause()
        player = newPlayer
        newPlayer.play()
        _state.value = FeedAudioPlayerState(
            activeAudioId = source.audioId,
            isPlaying = true,
            progress = 0f,
            positionMs = 0L,
            durationMs = source.durationMs ?: 0L,
        )
    }

    override fun stop() {
        player?.pause()
        player = null
        _state.value = FeedAudioPlayerState()
    }

    fun dispose() {
        observer?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        stop()
        scope.cancel()
    }
}

@Composable
actual fun rememberFeedAudioPlayer(): FeedAudioPlayer {
    val player = remember { IosFeedAudioPlayer() }
    DisposableEffect(player) {
        onDispose { player.dispose() }
    }
    return player
}

private object IosFeedAttachmentDownloader : FeedAttachmentDownloader {
    override fun download(attachment: GlobalFeedAttachment, onProgress: (Float) -> Unit, onStateChanged: (Boolean) -> Unit) {
        onStateChanged(false)
        onProgress(0f)
    }

    override fun cancel() = Unit
}

@Composable
actual fun rememberFeedAttachmentDownloader(): FeedAttachmentDownloader = remember { IosFeedAttachmentDownloader }

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
                    FeedIosFallback(modifier)
                }
            } else {
                FeedIosFallback(modifier)
            }
        }
        GlobalFeedPostType.TEXT,
        GlobalFeedPostType.AUDIO -> FeedIosFallback(modifier)
    }
}

@Composable
private fun FeedIosFallback(modifier: Modifier) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(Color(0xFF091615), Color(0xFF020304), Color(0xFF12201D))),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Text("MEDIA", color = Color.White.copy(alpha = 0.72f))
    }
}
