package com.mindos.app.core.logging

expect fun isPlatformDebugLoggingEnabled(): Boolean

expect fun platformDebugLog(tag: String, message: String)

fun safeDebugLog(tag: String, message: String) {
    if (isPlatformDebugLoggingEnabled()) {
        platformDebugLog(tag, message)
    }
}

expect fun platformErrorLog(tag: String, message: String, error: Throwable? = null)

/**
 * Legacy callback for runtimes that have not registered an [ObservabilitySink].
 * Prefer [ObservabilityHub] for new platform integrations.
 */
object NonFatalReporter {
    var handler: ((Throwable) -> Unit)? = null
}

fun safeErrorLog(tag: String, message: String, error: Throwable? = null) {
    platformErrorLog(tag, message, error)
    error?.let {
        if (ObservabilityHub.hasConfiguredSink) {
            ObservabilityHub.recordNonFatal(it)
        } else {
            NonFatalReporter.handler?.invoke(it)
        }
    }
}
