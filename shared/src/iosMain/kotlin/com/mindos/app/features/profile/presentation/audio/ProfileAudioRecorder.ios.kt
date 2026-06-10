package com.mindos.app.features.profile.presentation.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * iOS profile audio capture is deferred (verification requires Xcode/macOS).
 * Documented stub: reports unsupported and surfaces a clear message. A real
 * AVAudioRecorder-based implementation can replace this without touching common
 * code.
 */
private class UnsupportedProfileAudioRecorder(
    private val onError: (String) -> Unit,
) : ProfileAudioRecorder {
    override val isSupported: Boolean = false
    override suspend fun start() {
        onError("La grabación de audio aún no está disponible en iOS.")
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
