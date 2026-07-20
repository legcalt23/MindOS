package com.mindos.app.features.mindboost.focus.presentation

import com.mindos.app.core.logging.ObservabilityHub

/**
 * S0 · emits ONE neutral breadcrumb per app session recording whether the v1 Focus score surfaces are
 * contained, so a crash report can be read against the right UI shape ("no score card" is expected
 * behaviour, not a rendering bug).
 *
 * Deliberately minimal and PII-free: a fixed string, no user id, no session id, no metric value, no
 * score. It reports the STATE OF A FLAG, never anything about the person or their performance — the
 * ledger and telemetry rules forbid cognitive payloads in observability (`recordNonFatal` allowlist
 * precedent, learning 2026-06-20).
 *
 * Once per app session, not once per load: the home screen is re-entered constantly, and a breadcrumb
 * ring buffer flooded with the same line would push out the events that actually explain a crash.
 */
internal object FocusScoreContainmentBreadcrumb {

    private var reported = false

    /** Emits the breadcrumb the first time it is called in this process; subsequent calls are no-ops. */
    fun reportOnce(surfacesVisible: Boolean) {
        if (reported) return
        reported = true
        ObservabilityHub.breadcrumb(
            if (surfacesVisible) "focus_score_surfaces_visible" else "focus_score_surfaces_hidden",
        )
    }
}
