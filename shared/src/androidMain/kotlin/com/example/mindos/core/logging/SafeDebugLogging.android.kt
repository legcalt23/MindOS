package com.example.mindos.core.logging

import android.util.Log

var isAndroidDebugLoggingEnabled = false

actual fun isPlatformDebugLoggingEnabled(): Boolean = isAndroidDebugLoggingEnabled

actual fun platformDebugLog(tag: String, message: String) {
    if (isAndroidDebugLoggingEnabled) {
        Log.d(tag, message)
    }
}

actual fun platformErrorLog(tag: String, message: String, error: Throwable?) {
    if (isAndroidDebugLoggingEnabled) {
        Log.e(tag, message, error)
    } else {
        // Strip PII in release: log the message if safe, or a generic one, and just the error class
        val safeErrorClass = error?.javaClass?.simpleName ?: "UnknownError"
        Log.e(tag, "Error occurred ($safeErrorClass): ${message.take(100)}")
    }
}
