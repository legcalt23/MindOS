package com.mindos.app.core.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Android-side handle that exposes the live [NotificationPermissionState] and
 * a [requestIfNeeded] function for features to trigger the OS prompt.
 *
 * Use:
 * ```
 * val notifPerm = rememberNotificationPermissionHandle()
 * Button(onClick = { notifPerm.requestIfNeeded() }) { Text("Enable alerts") }
 * if (notifPerm.state.shouldDirectToSettings) { /* show "Open settings" UI */ }
 * ```
 *
 * The handle is scoped to its composition. Dismissals do NOT persist across
 * process death — features that need persistence must layer their own
 * SharedPreferences on top.
 */
class NotificationPermissionHandle internal constructor(
    initialState: NotificationPermissionState,
    private val onRequest: () -> Unit,
) {
    var state: NotificationPermissionState = initialState
        internal set

    fun requestIfNeeded() {
        if (state.shouldShowPrompt) onRequest()
    }
}

@Composable
fun rememberNotificationPermissionHandle(): NotificationPermissionHandle {
    val context = LocalContext.current
    var state by remember { mutableStateOf(currentState(context, dismissed = false)) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        // When the user denies, Android does NOT distinguish "deny" from "don't ask again"
        // via this contract. Re-checking shouldShowRequestPermissionRationale() requires
        // the Activity. For now we treat any denial as a session-level dismissal and rely
        // on the system to suppress further prompts if user picks "don't ask again".
        state = currentState(context, dismissed = !granted)
    }

    val handle = remember {
        NotificationPermissionHandle(
            initialState = state,
            onRequest = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
        )
    }
    handle.state = state
    return handle
}

private fun currentState(context: Context, dismissed: Boolean): NotificationPermissionState {
    val apiLevel = Build.VERSION.SDK_INT
    val granted = if (apiLevel >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    return NotificationPermissionState(
        platformApiLevel = apiLevel,
        isGranted = granted,
        // Permanent denial detection requires Activity.shouldShowRequestPermissionRationale,
        // which is not available from a Context here. We surface false; features that need
        // the distinction can implement it from within their Activity.
        isPermanentlyDenied = false,
        dismissedInThisSession = dismissed,
    )
}
