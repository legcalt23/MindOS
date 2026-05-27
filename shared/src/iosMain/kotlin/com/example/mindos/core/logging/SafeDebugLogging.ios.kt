package com.example.mindos.core.logging

actual fun isPlatformDebugLoggingEnabled(): Boolean = false

actual fun platformDebugLog(tag: String, message: String) {
}

actual fun platformErrorLog(tag: String, message: String, error: Throwable?) {
    // In iOS, we can just print for now or use NSLog if needed.
    println("ERROR: [$tag] $message")
    error?.let { println(it.message) }
}
