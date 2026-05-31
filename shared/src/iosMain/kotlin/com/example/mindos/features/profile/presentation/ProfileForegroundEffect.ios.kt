package com.example.mindos.features.profile.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * iOS best-effort: treat the screen as foreground while composed. A real
 * UIApplication active/resign lifecycle binding lands with the iOS app shell.
 */
@Composable
actual fun ProfileForegroundEffect(onChanged: (isForeground: Boolean) -> Unit) {
    val currentOnChanged by rememberUpdatedState(onChanged)
    DisposableEffect(Unit) {
        currentOnChanged(true)
        onDispose { currentOnChanged(false) }
    }
}
