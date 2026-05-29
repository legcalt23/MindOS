package com.example.mindos.core.logging

expect fun isPlatformDebugLoggingEnabled(): Boolean

expect fun platformDebugLog(tag: String, message: String)

fun safeDebugLog(tag: String, message: String) {
    if (isPlatformDebugLoggingEnabled()) {
        platformDebugLog(tag, message)
    }
}

expect fun platformErrorLog(tag: String, message: String, error: Throwable? = null)

/**
 * Callback for forwarding non-fatal errors to a crash reporter (e.g. Crashlytics).
 * Set by the platform application layer at startup; the shared module never
 * depends on a crash-reporting SDK directly.
 */
object NonFatalReporter {
    var handler: ((Throwable) -> Unit)? = null
}

fun safeErrorLog(tag: String, message: String, error: Throwable? = null) {
    platformErrorLog(tag, message, error)
    error?.let {
        // Backwards-compatible handler — still invoked.
        NonFatalReporter.handler?.invoke(it)
        // Structured sink — replaces the handler in the long run.
        ObservabilityHub.recordNonFatal(it)
    }
}
