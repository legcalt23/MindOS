package com.mindos.app.core.navigation

import androidx.compose.runtime.Composable

/**
 * Hooks the platform's native back gesture / back button to [onBack].
 *
 * - On Android: wraps `androidx.activity.compose.BackHandler` so the hardware/gesture
 *   back invokes [onBack] instead of finishing the activity.
 * - On desktop / iOS: no-op. Those platforms drive navigation through UI affordances
 *   (close buttons, swipe-back gestures, menus) rather than a global back button.
 *
 * When [enabled] is false the handler is detached and the system performs the default action.
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
