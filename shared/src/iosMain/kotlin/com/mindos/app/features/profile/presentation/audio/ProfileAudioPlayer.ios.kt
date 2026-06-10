package com.mindos.app.features.profile.presentation.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.mindos.app.features.profile.domain.model.ProfileAudioClip

/** iOS audio playback is deferred (documented stub). */
private class UnsupportedProfileAudioPlayer(
    private val onError: (String) -> Unit,
) : ProfileAudioPlayer {
    override val isSupported: Boolean = false
    override suspend fun play(clip: ProfileAudioClip): Boolean {
        onError("La reproducción de audio aún no está disponible en iOS.")
        return false
    }
    override fun stop() = Unit
}

@Composable
actual fun rememberProfileAudioPlayer(onError: (String) -> Unit): ProfileAudioPlayer {
    val currentOnError by rememberUpdatedState(onError)
    return remember { UnsupportedProfileAudioPlayer { currentOnError(it) } }
}
