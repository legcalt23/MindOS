package com.example.mindos.features.social.shared.util

import com.example.mindos.core.logging.safeDebugLog

fun logProfileSave(message: String) {
    safeDebugLog("ProfileSave", message)
}

fun Throwable.profileSaveExceptionLog(): String =
    this::class.simpleName ?: "Throwable"
