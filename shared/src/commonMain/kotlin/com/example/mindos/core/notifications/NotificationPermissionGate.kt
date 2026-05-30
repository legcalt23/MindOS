package com.example.mindos.core.notifications

/**
 * Pure decision model for whether the app should prompt the user for
 * notification permission right now.
 *
 * The actual OS-level permission request is platform-specific (handled by
 * [RememberNotificationPermissionState] on Android). This model decides WHEN
 * to ask, independently of HOW.
 *
 * Rules:
 *  - Pre-Android 13 (apiLevel < 33): permission is granted at install time,
 *    no runtime prompt needed.
 *  - Already granted: no prompt.
 *  - Already denied permanently ("don't ask again"): no prompt — direct the
 *    user to system settings instead.
 *  - User dismissed once in the current session: no prompt — wait for a
 *    different contextual moment.
 *  - Fresh ask: prompt.
 */
data class NotificationPermissionState(
    val platformApiLevel: Int,
    val isGranted: Boolean,
    val isPermanentlyDenied: Boolean,
    val dismissedInThisSession: Boolean,
) {
    val requiresRuntimeRequest: Boolean
        get() = platformApiLevel >= Android13ApiLevel && !isGranted

    val shouldShowPrompt: Boolean
        get() = requiresRuntimeRequest &&
            !isPermanentlyDenied &&
            !dismissedInThisSession

    val shouldDirectToSettings: Boolean
        get() = requiresRuntimeRequest && isPermanentlyDenied

    companion object {
        const val Android13ApiLevel: Int = 33
    }
}
