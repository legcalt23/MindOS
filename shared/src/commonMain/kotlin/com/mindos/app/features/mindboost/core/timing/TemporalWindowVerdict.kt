package com.mindos.app.features.mindboost.core.timing

/**
 * S5 — unified typed exclusion taxonomy. One closed enum for
 * BOTH runtime-origin discards (pause, cross-epoch) and temporal exclusions, counted PER CONDITION
 * so selective loss is visible (the per-session-only counter was the cherry-picking blind spot).
 *
 * The taxonomy decision originates in the R2 and is ratified by the LIVE contract in
 * ADR-FOCUS-TEMPORAL-QUALITY R3-15 ("CONFORME (Paso 5)"); the plainly numbered R2 sections of that
 * ADR are history and are explicitly NOT authoritative, so do not cite them here.
 *
 * EVERY entry here has a live producer, and that is a rule, not an observation (Paso 5, AC#4) — and
 * it is ENFORCED, not promised:
 * `TemporalClassPolicyTest.everyExclusionReasonIsProducedSomewhereInProductionCode` scans commonMain
 * and fails on an entry nothing constructs. The
 * R2 shipped `ONSET_INTEGRITY` and `PRE_ONSET_RESPONSE` with no producer anywhere in the tree: a
 * taxonomy that enumerates exclusions nothing can emit reads, to any later auditor, as coverage that
 * does not exist. They were deleted rather than left "for when someone implements it" — when a real
 * onset anchor exists, the entry comes back WITH its producer and its test.
 */
enum class WindowExclusionReason {
    /** Open trial discarded by pause/interruption (previously only `contaminatedCount`). */
    PAUSE_DISCARD,

    /** Cross-epoch clock guard (`atElapsedNanos < presentedAt`). */
    CLOCK_EPOCH,

    /** Degradation could have truncated the effective window (drops above the NO-GO floor, or a stall). */
    TRUNCATION_RISK,
}

/**
 * S5 — per-window verdict. Only [Excluded] withdraws accuracy (the
 * truncation carve-out); [DegradedUsable] and [NoObservation] both keep behaviour attributable
 * (conduct is conduct).
 *
 * Below that they are NOT interchangeable, and this doc used to say they were — one clause claiming
 * both "only constrain shadow RT / coverage", which was true of one and false of the other. Each
 * now states the consequence it actually has, because a vocabulary that promises an effect the code
 * does not implement reads to a later auditor as protection that is not there.
 */
sealed interface TemporalWindowVerdict {
    data object Clean : TemporalWindowVerdict

    /**
     * Anomalies present but below the exclusion floor (`windowDropCleanMax < drops <=
     * windowDropExcludeMin`) — a real, reachable band, not a placeholder.
     *
     * **Constrains nothing today, deliberately.** At session level it is indistinguishable from
     * [Clean]: it counts as classified — so it enters the DENOMINATOR of the excluded ratio and adds
     * nothing to its numerator — and leaves the shadow verdict Eligible. That eligibility is a
     * SHADOW-lane fact, not a measurement one: the live contract (R3-11, `D-R3-02` CLOSED) keeps
     * `excludedFraction` and the simulations in shadow and states that NO temporal diagnostic
     * produces measurement evidence in S5. So there is no measurement claim for a degraded window to
     * soften — and the softening tier the superseded R2 sketched does not exist in the R3 contract
     * at all. How much a degraded window should weigh is arithmetic S6 owns; inventing it here would
     * be freezing it. The equivalence with [Clean] is pinned by test, so
     * the day someone gives this bucket a consequence the pin fails and this doc must be rewritten
     * with it.
     */
    data object DegradedUsable : TemporalWindowVerdict

    /**
     * The probe observed nothing about this window (report loss, or `sampledFrames <
     * MIN_FRAMES_WINDOW`).
     *
     * **Does constrain, and differently from [DegradedUsable]:** it is not a classification at all,
     * so it enters neither side of the excluded ratio and leaves session coverage incomplete, which
     * withdraws the shadow RT lane. Behaviour stays attributable on the accuracy lane.
     */
    data object NoObservation : TemporalWindowVerdict

    data class Excluded(val reason: WindowExclusionReason) : TemporalWindowVerdict
}

/** Persistable bucket key of a verdict (wire form of the stratified tallies; fail-closed lookup). */
enum class TemporalWindowBucket {
    CLEAN,
    DEGRADED_USABLE,
    NO_OBSERVATION,
    EXCLUDED,
    ;

    companion object {
        fun fromWire(raw: String?): TemporalWindowBucket? = entries.firstOrNull { it.name == raw }
    }
}

val TemporalWindowVerdict.bucket: TemporalWindowBucket
    get() = when (this) {
        TemporalWindowVerdict.Clean -> TemporalWindowBucket.CLEAN
        TemporalWindowVerdict.DegradedUsable -> TemporalWindowBucket.DEGRADED_USABLE
        TemporalWindowVerdict.NoObservation -> TemporalWindowBucket.NO_OBSERVATION
        is TemporalWindowVerdict.Excluded -> TemporalWindowBucket.EXCLUDED
    }
