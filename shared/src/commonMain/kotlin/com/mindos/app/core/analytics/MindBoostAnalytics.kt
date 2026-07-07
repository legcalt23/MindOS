@file:Suppress("MaxLineLength") // Contract KDoc reads best whole.

package com.mindos.app.core.analytics

/**
 * MC8u-E §2 — the app-wide PRODUCT analytics surface for MindBoost. Mirrors [com.mindos.app.core.logging.ObservabilityHub]:
 * the shared module NEVER depends on Firebase Analytics / GA directly (those are Android-only SDKs and would invert the
 * KMP dependency, see the 2026-05-28 Crashlytics learning). Instead a tiny commonMain abstraction is exposed here, and
 * the platform layer registers a concrete sink at app startup.
 *
 * Wiring (deferred to the app layer — see the MC8u-E app-wiring contract):
 *  - `MindOSApplication.onCreate()` on Android registers a Firebase-backed [MindBoostAnalyticsSink] via
 *    [MindBoostAnalyticsHub.sink] and then calls [MindBoostAnalyticsPolicy.requireRealSinkInRelease].
 *  - Tests / desktop / iOS / any unconfigured runtime keep the no-op.
 *
 * PRIVACY: events are PII-free by construction — a stable snake_case [MindBoostAnalyticsEvent.name] plus COARSE,
 * non-identifying string params (ids, counts, coarse flags). Producers (e.g. the emotions bridge) never put content,
 * self-report values, a user id, or free text into a param.
 */
data class MindBoostAnalyticsEvent(val name: String, val params: Map<String, String> = emptyMap())

/** App-wide product-analytics sink. A concrete implementation is registered at app startup. */
interface MindBoostAnalyticsSink {
    fun log(event: MindBoostAnalyticsEvent)
}

/** No-op sink: the default in tests/desktop/iOS and in a debug build until a real sink is registered. */
object NoOpMindBoostAnalyticsSink : MindBoostAnalyticsSink {
    override fun log(event: MindBoostAnalyticsEvent) = Unit
}

/**
 * Global registry. The platform layer swaps [sink] once at startup; shared code logs through the hub. Logging is
 * FAIL-SAFE — a throwing sink can never break a caller (a fatal [Error] still propagates). Reads are lock-free.
 */
object MindBoostAnalyticsHub : MindBoostAnalyticsSink {
    @Volatile
    var sink: MindBoostAnalyticsSink = NoOpMindBoostAnalyticsSink

    val hasConfiguredSink: Boolean get() = sink !== NoOpMindBoostAnalyticsSink

    override fun log(event: MindBoostAnalyticsEvent) {
        runCatching { sink.log(event) }.exceptionOrNull()?.let { if (it is Error) throw it }
    }
}

/**
 * MC8u-E §2 release-blocking guard: a RELEASE build must register a real product-analytics sink (not the no-op).
 * `MindOSApplication.onCreate()` calls this with the registered [MindBoostAnalyticsHub.sink] and the real build flag;
 * it fails fast if a release build is still on [NoOpMindBoostAnalyticsSink]. A debug build may keep the no-op.
 */
object MindBoostAnalyticsPolicy {
    fun requireRealSinkInRelease(sink: MindBoostAnalyticsSink, isReleaseBuild: Boolean) {
        require(!(isReleaseBuild && sink === NoOpMindBoostAnalyticsSink)) {
            "MindBoost analytics: a release build must register a real MindBoostAnalyticsSink at startup, not the no-op."
        }
    }
}
