package com.mindos.app.features.mindboost.core.timing

import androidx.compose.runtime.Composable

/**
 * S5 · ADR-FOCUS-TEMPORAL-QUALITY §5 [R2-HIST] — session-scoped probe drained per response window.
 *
 * Lifecycle (owned by the session holder, same possession discipline as its clock): [start] once per
 * session, [drainWindow] at each boundary (present + resolve: two drains per trial — the ISI drain
 * feeds only session-level trend, never a window's class), [stop] once at terminal. Start/stop out
 * of order are observable no-ops. [markPresent] anchors the render marker (C.3): the platform records
 * the state-set instant and reports the first frame commit at/after it as an intra-domain duration in
 * the NEXT drained observation.
 *
 * [capability]/[clockDomain] are FIRST-CLASS facts to seal into the session descriptor — absence of
 * observation is never inferred from empty stats.
 */
interface WindowedTemporalProbe {
    val capability: ProbeCapability
    val clockDomain: ClockDomain

    fun start()

    /** Anchors the render marker at the stimulus state-set instant. No-op when not running. */
    fun markPresent()

    /**
     * Returns what was observed since the last drain (or [start]), and resets the interval.
     * `null` = this probe measures nothing (no-op platforms / not running) — distinct from an
     * observation with too few frames, which the policy maps to NO_OBSERVATION.
     */
    fun drainWindow(): TemporalWindowObservation?

    fun stop()
}

/** Fallback for platforms without an authoritative frame source: reports its capability, measures nothing. */
object NoOpWindowedTemporalProbe : WindowedTemporalProbe {
    override val capability: ProbeCapability = ProbeCapability.NONE_MONOTONIC_FALLBACK
    override val clockDomain: ClockDomain = ClockDomain.NONE_MONOTONIC_FALLBACK
    override fun start() = Unit
    override fun markPresent() = Unit
    override fun drainWindow(): TemporalWindowObservation? = null
    override fun stop() = Unit
}

/** Platform probe: Android = Choreographer; iOS = NoOp until the CADisplayLink port; desktop = NoOp. */
@Composable
expect fun rememberWindowedTemporalProbe(): WindowedTemporalProbe
