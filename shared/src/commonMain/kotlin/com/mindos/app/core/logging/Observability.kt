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
 *  - [recordNonFatal]: explicitly forward a caught exception to the reporter
 *    without crashing the app.
 */
interface ObservabilitySink {
    fun breadcrumb(message: String)
    fun customKey(key: String, value: String)
    fun customKey(key: String, value: Boolean)
    fun customKey(key: String, value: Int)
    fun recordNonFatal(error: Throwable)
}

/** No-op sink used by default in tests, desktop, iOS, and any unconfigured runtime. */
private object NoopObservabilitySink : ObservabilitySink {
    override fun breadcrumb(message: String) = Unit
    override fun customKey(key: String, value: String) = Unit
    override fun customKey(key: String, value: Boolean) = Unit
    override fun customKey(key: String, value: Int) = Unit
    override fun recordNonFatal(error: Throwable) = Unit
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

    fun recordNonFatal(error: Throwable) {
        sink.recordNonFatal(error)
    }
}
