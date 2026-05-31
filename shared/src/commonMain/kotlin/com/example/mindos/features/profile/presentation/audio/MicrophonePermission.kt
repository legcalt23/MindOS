package com.example.mindos.features.profile.presentation.audio

import androidx.compose.runtime.Composable

/**
 * Snapshot of the microphone permission for the current platform.
 *
 * Mirrors the project's existing notification-permission model so gating logic
 * stays pure and testable.
 */
data class MicrophonePermissionState(
    val isGranted: Boolean,
    val isPermanentlyDenied: Boolean = false,
    val dismissedInThisSession: Boolean = false,
) {
    /** Whether tapping the mic should trigger the OS prompt. */
    val shouldShowPrompt: Boolean get() = !isGranted && !isPermanentlyDenied

    /** Whether the user must be sent to system settings to re-enable it. */
    val shouldDirectToSettings: Boolean get() = !isGranted && isPermanentlyDenied
}

/**
 * Handle exposing the live [state] and a [request] entry point that triggers the
 * OS prompt (or resolves immediately on platforms without runtime permissions),
 * reporting the outcome to [request]'s callback.
 */
class MicrophonePermissionHandle internal constructor(
    val state: MicrophonePermissionState,
    private val onRequest: (onResult: (Boolean) -> Unit) -> Unit,
) {
    fun request(onResult: (Boolean) -> Unit) = onRequest(onResult)
}

/** Provides a composition-scoped [MicrophonePermissionHandle]. */
@Composable
expect fun rememberMicrophonePermission(): MicrophonePermissionHandle
