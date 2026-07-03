package com.mindos.app.features.arena.observability

import com.mindos.app.features.arena.sanitization.SafeArenaException

/**
 * Phase 07.5 / Task 9: Offline-safe observability boundary.
 *
 * Implementations only need to provide [record], which accepts exclusively [NonFatalTelemetryEvent]
 * (safe by construction). [recordNonFatal] is final-by-default: it converts any throwable into a
 * [SafeArenaException], discarding its message and cause, and records only the resulting safe
 * category. Raw throwables therefore can never be forwarded to a sink.
 *
 * This is NOT a production Crashlytics/Sentry integration — it is the abstraction that prevents one
 * from being wired up unsafely later.
 */
interface ObservabilitySink {

    fun record(event: NonFatalTelemetryEvent)

    fun recordNonFatal(throwable: Throwable) {
        record(NonFatalTelemetryEvent.SafeError(SafeArenaException.from(throwable).category))
    }
}
