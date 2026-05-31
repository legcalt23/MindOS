package com.example.mindos.features.profile.presentation.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * Desktop has no profile audio capture yet (documented stub). It reports
 * unsupported and surfaces a clear message instead of silently doing nothing.
 */
private class UnsupportedProfileAudioRecorder(
    private val onError: (String) -> Unit,
) : ProfileAudioRecorder {
    override val isSupported: Boolean = false
    override suspend fun start() {
        onError("La grabación de audio está disponible en Android.")
    }
    override fun currentAmplitude(): Float = 0f
    override suspend fun stop(): ProfileAudioCapture? = null
    override fun cancel() = Unit
}

@Composable
actual fun rememberProfileAudioRecorder(onError: (String) -> Unit): ProfileAudioRecorder {
    val currentOnError by rememberUpdatedState(onError)
    return remember { UnsupportedProfileAudioRecorder { currentOnError(it) } }
}
