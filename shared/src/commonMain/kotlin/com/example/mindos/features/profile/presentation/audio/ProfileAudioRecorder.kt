package com.example.mindos.features.profile.presentation.audio

import androidx.compose.runtime.Composable

/**
 * Records a short profile audio clip and exposes the live input amplitude so the
 * UI can render a *real* waveform (never random).
 *
 * Threading: [start]/[stop] are suspending and must do their I/O off the main
 * thread. [currentAmplitude] is a cheap, non-blocking read polled by the state
 * holder (~16–60 ms) while recording.
 *
 * Permission is the caller's responsibility — invoke only after microphone
 * permission is granted.
 */
interface ProfileAudioRecorder {
    /** True on platforms with a real implementation (Android today). */
    val isSupported: Boolean

    /** Begin capturing to a temporary buffer. Throws if the device can't record. */
    suspend fun start()

    /** Latest normalized input amplitude in `0f..1f` (0 when idle/unsupported). */
    fun currentAmplitude(): Float

    /** Stop capturing and return the encoded clip, or null if nothing was captured. */
    suspend fun stop(): ProfileAudioCapture?

    /** Abort capture and discard any buffered audio. */
    fun cancel()
}

/** Provides a composition-scoped [ProfileAudioRecorder]; [onError] surfaces capture failures. */
@Composable
expect fun rememberProfileAudioRecorder(onError: (String) -> Unit): ProfileAudioRecorder
