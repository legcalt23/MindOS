package com.mindos.app.features.mindboost.core.scoring

/**
 * INFRA-01 · The 8 honesty states a scored session can resolve to. `VALID` is the ONLY state that may
 * update the cognitive profile; every other state is "playable but does not count" and serializes/
 * renders as `—`/"calibrating", NEVER as a number (invariant `null != 0`).
 *
 * Order encodes the cascade priority used by [ScoreStateMachine]: safety verdicts (abandonment,
 * timebase, jank, bank, gaming, invalidation) win over evidence verdicts (calibrating, insufficient-n)
 * which win over `VALID`.
 */
enum class ScoreState {
    /** Valid + sufficient signal — the ONLY state that updates the profile. */
    VALID,

    /** Playable, accumulating evidence; primary signal not yet computable. value == null. */
    CALIBRATING,

    /** n < nMin for the metric cell. value == null. */
    INSUFFICIENT_N,

    /** Contaminated/anomalous (e.g. AGRS high, quality LOW): the session is discarded. */
    INVALIDATED,

    /** Timebase not authoritative for an RT-critical metric, or measured jank over threshold. */
    DEVICE_UNRELIABLE,

    /** Item bank memorized / unvalidated / holdout exhausted (invariant: small bank blocks profile). */
    BANK_CONTAMINATED,

    /** A real, tested anti-gaming detector fired (invariant: anti-gaming counts only if tested). */
    SUSPECTED_GAMING,

    /** Abandonment in an emotional/frustration exercise — a safety signal, NEVER blamed. */
    ABANDONED_SAFE,
}
