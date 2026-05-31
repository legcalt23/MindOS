package com.example.mindos.features.profile.presentation.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.example.mindos.features.profile.domain.model.ProfileAudioClip

/** Desktop audio playback is not wired yet (documented stub). */
private class UnsupportedProfileAudioPlayer(
    private val onError: (String) -> Unit,
) : ProfileAudioPlayer {
    override val isSupported: Boolean = false
    override suspend fun play(clip: ProfileAudioClip): Boolean {
        onError("La reproducción de audio está disponible en Android.")
        return false
    }
    override fun stop() = Unit
}

@Composable
actual fun rememberProfileAudioPlayer(onError: (String) -> Unit): ProfileAudioPlayer {
    val currentOnError by rememberUpdatedState(onError)
    return remember { UnsupportedProfileAudioPlayer { currentOnError(it) } }
}
