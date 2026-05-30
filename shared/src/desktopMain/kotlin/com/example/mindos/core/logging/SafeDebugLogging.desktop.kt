package com.example.mindos.core.logging

actual fun isPlatformDebugLoggingEnabled(): Boolean = false

actual fun platformDebugLog(tag: String, message: String) {
}

actual fun platformErrorLog(tag: String, message: String, error: Throwable?) {
    System.err.println("[$tag] $message")
    error?.printStackTrace()
}
