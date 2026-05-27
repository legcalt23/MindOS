package com.example.mindos.core.logging

expect fun isPlatformDebugLoggingEnabled(): Boolean

expect fun platformDebugLog(tag: String, message: String)

fun safeDebugLog(tag: String, message: String) {
    if (isPlatformDebugLoggingEnabled()) {
        platformDebugLog(tag, message)
    }
}

expect fun platformErrorLog(tag: String, message: String, error: Throwable? = null)

fun safeErrorLog(tag: String, message: String, error: Throwable? = null) {
    platformErrorLog(tag, message, error)
}
