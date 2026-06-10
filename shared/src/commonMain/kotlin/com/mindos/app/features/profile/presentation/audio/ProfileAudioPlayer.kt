package com.mindos.app.features.profile.presentation.audio

import androidx.compose.runtime.Composable
import com.mindos.app.features.profile.domain.model.ProfileAudioClip

/**
 * Plays a saved (or just-recorded) profile audio clip. [play] suspends for the
 * duration of playback and completes when the clip ends or [stop] is called.
 * Decoding/playback runs off the main thread.
 */
interface ProfileAudioPlayer {
    /** True on platforms with a real implementation (Android today). */
    val isSupported: Boolean

    /** Play [clip]; suspends until it finishes (returns true) or is stopped (false). */
    suspend fun play(clip: ProfileAudioClip): Boolean

    /** Stop any in-progress playback. */
    fun stop()
}

/** Provides a composition-scoped [ProfileAudioPlayer]; [onError] surfaces playback failures. */
@Composable
expect fun rememberProfileAudioPlayer(onError: (String) -> Unit): ProfileAudioPlayer
