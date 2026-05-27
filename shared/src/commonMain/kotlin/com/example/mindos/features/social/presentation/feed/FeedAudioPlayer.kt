package com.example.mindos.features.social.presentation.feed

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

data class FeedAudioSource(
    val audioId: String,
    val localPath: String? = null,
    val remoteUrl: String? = null,
    val mimeType: String,
    val durationMs: Long? = null,
)

data class FeedAudioPlayerState(
    val activeAudioId: String? = null,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
) {
    fun forAudio(audioId: String): FeedAudioPlaybackUiState =
        if (activeAudioId == audioId) {
            FeedAudioPlaybackUiState(
                isPlaying = isPlaying,
                progress = progress,
                positionMs = positionMs,
                durationMs = durationMs,
            )
        } else {
            FeedAudioPlaybackUiState()
        }
}

interface FeedAudioPlayer {
    val state: StateFlow<FeedAudioPlayerState>

    fun playOrPause(source: FeedAudioSource)
    fun stop()
}

@Composable
expect fun rememberFeedAudioPlayer(): FeedAudioPlayer
