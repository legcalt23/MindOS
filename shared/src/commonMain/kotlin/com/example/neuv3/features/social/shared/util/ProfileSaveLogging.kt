package com.example.neuv3.features.social.shared.util

import com.example.neuv3.core.logging.safeDebugLog

fun logProfileSave(message: String) {
    safeDebugLog("ProfileSave", message)
}

fun Throwable.profileSaveExceptionLog(): String =
    this::class.simpleName ?: "Throwable"
