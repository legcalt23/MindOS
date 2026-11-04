package com.mindos.app.features.mindboost.core.timing

/**
 * S5 · ADR-FOCUS-TEMPORAL-QUALITY §11 [R2-HIST] — versioned SHADOW config. Values are DESIGNED_NOT_RUN
 * (initial derivations from the orden-2 protocol §C.6/§C.7); they freeze before any confirmatory
 * evidence, and changing any value requires a new [version] — retroactive re-classification is
 * prohibited (every classified result carries the version that classified it).
 *
 * Role separation (never reuse a constant across roles): UX frame budget ≠ these integrity
 * thresholds ≠ `MinSampleGate` sufficiency floors ≠ observation→class mapping floors.
 *
 * DECLARED NON-VERSIONED EXCEPTION (S5 · Paso 5, ADR §R3-18 «resolver-o-quitar-consumidor»):
 * `criticalJitterFactor` is deliberately NOT a field here. The critical-jitter flag is derived in
 * [FrameStatsCalculator] as a MULTIPLE OF THE PLATFORM'S NOMINAL FRAME PERIOD (a cadence-relative
 * physical fact, not a product threshold) and that calculator is SHARED with the Speed lane; pulling
 * it into this focus2-scoped config would couple an unrelated lane to `tqt-1` versioning. It stays
 * where it is derived, is pinned BEHAVIOURALLY by a golden test (2.4× nominal ⇒ not critical, 2.6×
 * ⇒ critical) so it cannot drift silently, and any change to it invalidates comparability in BOTH
 * lanes — which is exactly why it is recorded here rather than left unstated.
 */
data class TemporalQualityThresholds(
    val version: String = VERSION_1,
    /** Below this, a window has no classifiable observation ⇒ NO_OBSERVATION. */
    val minFramesWindow: Int = MIN_FRAMES_WINDOW,
    /** Below this, the session cannot claim an authoritative cadence ⇒ T0. */
    val minFramesSession: Int = MIN_FRAMES_SESSION,
    /** GO ceiling (orden-2 §C.6): drop fraction at or under this is CLEAN. */
    val windowDropCleanMax: Double = WINDOW_DROP_CLEAN_MAX,
    /** NO-GO floor (orden-2 §C.6): above this without a render marker ⇒ full-trial exclusion. */
    val windowDropExcludeMin: Double = WINDOW_DROP_EXCLUDE_MIN,
    /** Session backstop (orden-2 §C.7): global excluded fraction above this ⇒ TRAINING_ONLY. */
    val sessionExcludedMax: Double = SESSION_EXCLUDED_MAX,
    /**
     * How far the DETECTED cadence may sit from the EXPECTED one before the window is treated as
     * unresolved (S5 · Paso 5, B4). Beyond it, the drop fraction is measuring the pacing MODE rather
     * than jank, so no verdict is derivable.
     */
    val cadenceMatchTolerance: Double = CADENCE_MATCH_TOLERANCE,
) {
    /**
     * S5 · Paso 5 (F-R3-L) — fail-closed config validator. Only the invariants that compare
     * quantities of the SAME population are asserted as orderings:
     *  - `minFramesWindow < minFramesSession` (both are frame counts of the same stream);
     *  - `windowDropCleanMax < windowDropExcludeMin` (both are per-window drop fractions).
     * `sessionExcludedMax` is a fraction of EXCLUDED WINDOWS, a different population from the
     * per-window drop fractions, so `windowDropExcludeMin < sessionExcludedMax` is deliberately NOT
     * asserted (ADR §R3-18: it is not a dimensional invariant, and asserting it would forbid valid
     * configurations). An unclassifiable config must fail LOUDLY at construction rather than
     * silently classify with nonsense bounds.
     */
    init {
        require(version.isNotBlank()) { "thresholds version must identify the config that classified a result" }
        require(minFramesWindow >= MIN_FRAMES_FLOOR) {
            "minFramesWindow must be >= $MIN_FRAMES_FLOOR (fewer frames yield no derivable cadence): $minFramesWindow"
        }
        require(minFramesWindow < minFramesSession) {
            "same population: a session floor must exceed a window floor ($minFramesWindow, $minFramesSession)"
        }
        requireFraction(windowDropCleanMax, "windowDropCleanMax")
        requireFraction(windowDropExcludeMin, "windowDropExcludeMin")
        requireFraction(sessionExcludedMax, "sessionExcludedMax")
        requireFraction(cadenceMatchTolerance, "cadenceMatchTolerance")
        require(windowDropCleanMax < windowDropExcludeMin) {
            "same population: the CLEAN ceiling must sit below the EXCLUDE floor, or a window would be " +
                "both clean and excluded ($windowDropCleanMax, $windowDropExcludeMin)"
        }
    }

    private fun requireFraction(value: Double, name: String) {
        require(value.isFinite() && value >= FRACTION_MIN && value <= FRACTION_MAX) {
            "$name must be a finite fraction in [$FRACTION_MIN, $FRACTION_MAX]: $value"
        }
    }

    companion object {
        const val VERSION_1: String = "tqt-1"
        private const val FRACTION_MIN: Double = 0.0
        private const val FRACTION_MAX: Double = 1.0

        /** `FrameStatsCalculator` needs two frame times to derive one delta; below that nothing is classifiable. */
        private const val MIN_FRAMES_FLOOR: Int = 2
        private const val MIN_FRAMES_WINDOW: Int = 3
        private const val MIN_FRAMES_SESSION: Int = 120
        private const val WINDOW_DROP_CLEAN_MAX: Double = 0.05
        private const val WINDOW_DROP_EXCLUDE_MIN: Double = 0.15
        private const val SESSION_EXCLUDED_MAX: Double = 0.50
        // REMOVED in Paso 7 (`MB-FOCUS-0054`): `differentialExclusionDelta`. The R2's within-session
        // differential-exclusion (MNAR) detector was dropped in R3 and never rebuilt, so the constant
        // sat here for three phases with ZERO consumers while reading — to any auditor, and to the
        // Paso 6 protocol that pinned it — as a live, versioned threshold implying that differential
        // exclusion is detected. It is not. The missingness policy it belonged to is `MB-FOCUS-0053`,
        // still open; when that lands, the threshold returns WITH its detector and its test.

        /** ±5%, the same width [HzBand] uses to call two rates the same panel cadence. */
        private const val CADENCE_MATCH_TOLERANCE: Double = 0.05
        // REMOVED in Paso 5 (B6/L1): `rtPlausibleCeilingNanos` + `truncationSlackNanosFor` existed only
        // to feed the marker gate this phase deleted. Config whose last consumer is gone is left in
        // place at everyone's peril — it reads as a live, versioned threshold and re-grows a consumer.

        val DEFAULT = TemporalQualityThresholds()
    }
}
