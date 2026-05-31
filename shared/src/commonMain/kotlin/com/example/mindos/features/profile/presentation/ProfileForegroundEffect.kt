package com.example.mindos.features.profile.presentation

import androidx.compose.runtime.Composable

/**
 * Observes the host's foreground/background lifecycle and invokes [onChanged]
 * with `true` when the app becomes active (foreground) and `false` when it is
 * backgrounded. This is the single source of truth for the local user's
 * presence (online = actively in the app foreground).
 *
 * Android wires this to the activity lifecycle. Desktop/iOS treat the window as
 * foreground while composed (documented best-effort until those platforms ship
 * real lifecycle hooks).
 */
@Composable
expect fun ProfileForegroundEffect(onChanged: (isForeground: Boolean) -> Unit)
