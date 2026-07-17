package com.mindos.app.design

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private const val ANIMATOR_SCALE_DEFAULT: Float = 1f
private const val ANIMATOR_SCALE_DISABLED: Float = 0f

/**
 * Android reduce-motion: true when the system "Remove animations" accessibility
 * setting is enabled, which forces `ANIMATOR_DURATION_SCALE` to 0. Read once per
 * composition (keyed on the context); a mid-session toggle is not observed.
 */
@Composable
actual fun rememberSystemReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            ANIMATOR_SCALE_DEFAULT,
        ) == ANIMATOR_SCALE_DISABLED
    }
}
