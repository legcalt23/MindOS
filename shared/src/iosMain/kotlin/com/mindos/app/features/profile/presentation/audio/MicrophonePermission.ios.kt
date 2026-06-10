package com.mindos.app.features.profile.presentation.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS mic permission gating is deferred. The recorder stub is unsupported, so
 * this reports granted to avoid a misleading "blocked" UI; a real AVAudioSession
 * permission flow lands with the iOS recorder.
 */
@Composable
actual fun rememberMicrophonePermission(): MicrophonePermissionHandle =
    remember {
        MicrophonePermissionHandle(
            state = MicrophonePermissionState(isGranted = true),
            onRequest = { onResult -> onResult(true) },
        )
    }
