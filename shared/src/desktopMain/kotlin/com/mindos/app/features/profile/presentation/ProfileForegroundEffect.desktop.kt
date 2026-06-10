package com.mindos.app.features.profile.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * Desktop best-effort: treat the window as foreground while the screen is
 * composed (no per-window focus lifecycle wired yet — documented).
 */
@Composable
actual fun ProfileForegroundEffect(onChanged: (isForeground: Boolean) -> Unit) {
    val currentOnChanged by rememberUpdatedState(onChanged)
    DisposableEffect(Unit) {
        currentOnChanged(true)
        onDispose { currentOnChanged(false) }
    }
}
