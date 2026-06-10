package com.mindos.app.features.profile.presentation.audio

import android.Manifest
import android.content.pm.PackageManager
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
 * Android microphone permission handle backed by the runtime RECORD_AUDIO
 * permission (declared in the manifest). The request is contextual — only fired
 * when the user taps the mic, never at startup.
 */
@Composable
actual fun rememberMicrophonePermission(): MicrophonePermissionHandle {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var dismissed by remember { mutableStateOf(false) }
    var pendingResult by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        granted = isGranted
        dismissed = !isGranted
        val callback = pendingResult
        pendingResult = null
        callback?.invoke(isGranted)
    }

    val state = MicrophonePermissionState(
        isGranted = granted,
        // Distinguishing permanent denial requires Activity.shouldShowRequestPermissionRationale;
        // not available from a Context here, so we surface false and treat denials as session-level.
        isPermanentlyDenied = false,
        dismissedInThisSession = dismissed,
    )

    return remember(state, launcher) {
        MicrophonePermissionHandle(state) { onResult ->
            if (granted) {
                onResult(true)
            } else {
                pendingResult = onResult
                launcher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
}
