package com.mindos.app.core.logging

/**
 * Sink-based observability layer for the shared module.
 *
 * The shared module never depends on Crashlytics / Sentry / Datadog directly
 * (those SDKs are platform-specific and live in `:androidApp` / `:iosApp`).
 * Instead we expose a tiny abstraction here: every observability call routes
 * through [ObservabilityHub] which forwards to a registered [ObservabilitySink].
 *
 * Wiring:
 *  - `MindOSApplication.onCreate()` on Android registers a `CrashlyticsSink`.
 *  - In tests and previews the default no-op sink is used.
 *
 * Three primitives are supported:
 *  - [breadcrumb]: a transient string entry attached to the next crash report.
 *    Use for navigation events, lifecycle moments, network state changes.
 *  - [customKey]: a key/value annotation that persists across breadcrumbs.
 *    Use for stable context: current_route, user_uid (hashed), feature_flag.
 *  - [recordNonFatal]: report a caught exception as a minimized event. The
 *    original throwable, message and cause never cross the telemetry boundary.
 */
interface ObservabilitySink {
    fun breadcrumb(message: String)
    fun customKey(key: String, value: String)
    fun customKey(key: String, value: Boolean)
    fun customKey(key: String, value: Int)
    fun recordNonFatal(event: NonFatalTelemetryEvent)
}

/** Allowlisted diagnostic fields that are safe to forward to a remote reporter. */
data class NonFatalTelemetryEvent(
    val operation: String,
    val errorType: String,
)

/** Local exception created only from minimized fields; it never retains the source throwable. */
class SanitizedNonFatalException(event: NonFatalTelemetryEvent) : RuntimeException(
    "operation=${event.operation};error_type=${event.errorType}",
)

/** No-op sink used by default in tests, desktop, iOS, and any unconfigured runtime. */
private object NoopObservabilitySink : ObservabilitySink {
    override fun breadcrumb(message: String) = Unit
    override fun customKey(key: String, value: String) = Unit
    override fun customKey(key: String, value: Boolean) = Unit
    override fun customKey(key: String, value: Int) = Unit
    override fun recordNonFatal(event: NonFatalTelemetryEvent) = Unit
}

/**
 * Global registry. Platform layers swap [sink] at app startup. Reads from
 * shared code are lock-free; writes happen exactly once during init.
 */
object ObservabilityHub {
    @Volatile
    var sink: ObservabilitySink = NoopObservabilitySink

    internal val hasConfiguredSink: Boolean
        get() = sink !== NoopObservabilitySink

    fun breadcrumb(message: String) {
        sink.breadcrumb(message)
    }

    fun customKey(key: String, value: String) {
        sink.customKey(key, value)
    }

    fun customKey(key: String, value: Boolean) {
        sink.customKey(key, value)
    }

    fun customKey(key: String, value: Int) {
        sink.customKey(key, value)
    }

    fun recordNonFatal(operation: String, error: Throwable) {
        sink.recordNonFatal(sanitizedNonFatalEvent(operation, error))
    }
}

internal fun sanitizedNonFatalEvent(operation: String, error: Throwable): NonFatalTelemetryEvent =
    NonFatalTelemetryEvent(
        operation = operation.toTelemetryToken(fallback = "unknown_operation"),
        errorType = error::class.simpleName.orEmpty().toTelemetryToken(fallback = "unknown_error"),
    )

private fun String.toTelemetryToken(fallback: String): String {
    val token = lowercase()
        .map { char ->
            when (char) {
                in 'a'..'z', in '0'..'9', '.', '_', '-' -> char
                else -> '_'
            }
        }
        .joinToString("")
        .trim('_')
        .take(MAX_TELEMETRY_TOKEN_LENGTH)
    return token.ifBlank { fallback }
}

private const val MAX_TELEMETRY_TOKEN_LENGTH = 64
