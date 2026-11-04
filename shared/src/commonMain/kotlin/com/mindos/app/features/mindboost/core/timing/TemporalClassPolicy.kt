package com.mindos.app.features.mindboost.core.timing

import kotlin.math.abs

/**
 * S5 · ADR-FOCUS-TEMPORAL-QUALITY §4 [R2-HIST] — pure classification. Two guards are load-bearing:
 *
 * 1. Sample gate FIRST (psicómetra P0-3): an absent/empty observation must never read as CLEAN —
 *    `FrameStats.UNAVAILABLE` carries `frameDropFraction = 0.0`, so any threshold check that runs
 *    before the sample gate fabricates perfection from absence.
 * 2. [TemporalClass.T2_LATENCY_BOUNDED]/[TemporalClass.T3_EXTERNALLY_CALIBRATED] are unreachable
 *    from this policy (no bench exists); a test pins the codomain to {T0, T1}.
 */
object TemporalClassPolicy {

    /** Session-level class from sealed capability + observed session sample. Fail-closed to T0. */
    fun sessionClass(
        capability: ProbeCapability,
        clockDomain: ClockDomain,
        sampledFramesSession: Int,
        thresholds: TemporalQualityThresholds = TemporalQualityThresholds.DEFAULT,
    ): TemporalClass {
        val expected = capability.expectedDomain
        val authoritative = expected != null && expected == clockDomain
        return if (authoritative && sampledFramesSession >= thresholds.minFramesSession) {
            TemporalClass.T1_FRAME_OBSERVED
        } else {
            TemporalClass.T0_NON_AUTHORITATIVE
        }
    }

    /**
     * Per-window integrity verdict from the drained observation of THAT window.
     *
     * **The render marker does NOT classify (Paso 5, B6/L1, D-R3-03).** The R2 had a second branch:
     * when a marker and a per-dossier slack were both present, a marker delay past the slack excluded
     * the trial outright. That made a PROXY act as a gate. What the platform reports is the first
     * frame ISSUED at or after the state-set — not the photons the user saw, and on iOS not even the
     * frame this app composited. Deriving "the response window was truncated" from it asserts an
     * end-to-end error bound nobody has measured, and it did so in the one direction that costs the
     * user their accuracy. Until a VALIDATED bound exists (Paso 6, physical), the only honest
     * position is the conservative one below, applied uniformly: a critical stall or drops above the
     * NO-GO floor are truncation risk (eficacia P0-A). The marker stays on the observation as a
     * descriptive shadow quantity — recorded, never load-bearing.
     */
    fun windowIntegrity(
        observation: TemporalWindowObservation?,
        thresholds: TemporalQualityThresholds = TemporalQualityThresholds.DEFAULT,
    ): TemporalWindowVerdict = when {
        observation == null ||
            observation.bufferOverflow ||
            observation.sampledFrames < thresholds.minFramesWindow ||
            !cadenceResolved(observation, thresholds) ->
            TemporalWindowVerdict.NoObservation

        else -> conservativeVerdict(observation, thresholds)
    }

    /**
     * S5 · Paso 5 (B4/F-R3-E) — a window is classifiable only when we know BOTH what cadence the
     * platform said to expect and what cadence actually arrived, AND they agree.
     *
     * A "dropped frame" is defined relative to an expected period, so with no stated expectation
     * there is nothing to drop from (iOS in S5). And when the two disagree — 60 fps on a 120 Hz
     * panel, or any half-rate pacing — the drop fraction stops describing jank and starts describing
     * the pacing MODE: classify against the panel and every frame is a drop (an honest session
     * excluded); classify against the stream itself and a genuinely degraded session reads flawless
     * (the worse error, absence fabricating perfection). Neither is a measurement, so S5 declines to
     * classify at all rather than pick the convenient one.
     */
    private fun cadenceResolved(
        observation: TemporalWindowObservation,
        thresholds: TemporalQualityThresholds,
    ): Boolean {
        val expected = observation.expectedCadenceHz.asCadence()
        val detected = observation.detectedCadenceHz.asCadence()
        return expected != null &&
            detected != null &&
            abs(detected - expected) <= expected * thresholds.cadenceMatchTolerance
    }

    /** A rate is a statement about frames only if it is present, finite and positive. */
    private fun Double?.asCadence(): Double? = this?.takeIf { it.isFinite() && it > 0.0 }

    private fun conservativeVerdict(
        observation: TemporalWindowObservation,
        thresholds: TemporalQualityThresholds,
    ): TemporalWindowVerdict = when {
        observation.criticalFrameJitter || dropFractionOf(observation) > thresholds.windowDropExcludeMin ->
            TemporalWindowVerdict.Excluded(WindowExclusionReason.TRUNCATION_RISK)

        dropFractionOf(observation) > thresholds.windowDropCleanMax ->
            TemporalWindowVerdict.DegradedUsable

        else -> TemporalWindowVerdict.Clean
    }

    /** Callers above have already passed the sample gate, so the fraction is derivable. */
    private fun dropFractionOf(observation: TemporalWindowObservation): Double =
        checkNotNull(observation.frameDropFraction()) { "sample gate must run before thresholds" }
}
