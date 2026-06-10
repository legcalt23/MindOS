package com.mindos.app.features.social.shared.util

import com.mindos.app.core.logging.safeDebugLog

fun logProfileSave(message: String) {
    safeDebugLog("ProfileSave", message)
}

fun Throwable.profileSaveExceptionLog(): String =
    this::class.simpleName ?: "Throwable"
