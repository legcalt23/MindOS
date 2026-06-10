package com.mindos.app.features.profile.presentation.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Desktop has no runtime mic permission model; treat as granted. */
@Composable
actual fun rememberMicrophonePermission(): MicrophonePermissionHandle =
    remember {
        MicrophonePermissionHandle(
            state = MicrophonePermissionState(isGranted = true),
            onRequest = { onResult -> onResult(true) },
        )
    }
