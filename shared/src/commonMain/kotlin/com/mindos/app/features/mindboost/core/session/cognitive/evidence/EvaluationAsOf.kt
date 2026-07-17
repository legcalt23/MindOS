package com.mindos.app.features.mindboost.core.session.cognitive.evidence

/**
 * SYSTEM-1 slice 6-R2 · Where a clock reading came from — surfaced so a consumer can judge how much to trust the
 * freshness / future-exclusion derived from it. It NEVER changes the exclusion RULE (future exclusion is always
 * applied); it only annotates provenance.
 */
enum class ClockProvenance {
    /** A real, trusted, injected clock reading (production default). */
    AUTHORITATIVE_INJECTED,

    /** The caller lacked a trustworthy clock and supplied its best monotonic reading (still applied — see below). */
    BEST_EFFORT_MONOTONIC,
}

/**
 * SYSTEM-1 slice 6-R2 · A REQUIRED, typed "as of" clock reading that replaces a bare `nowMs: Long` (P1-09). It
 * carries [provenance] so a downstream reader of freshness can distinguish an authoritative "now" from a
 * best-effort one — WITHOUT weakening the rule: future-timestamp exclusion is ALWAYS applied regardless of
 * provenance (a clock-skew safety valve, not a quality gate). There is deliberately no "disable future exclusion"
 * mode. Injecting the clock (never reading a global one) keeps the extractor pure and deterministic.
 */
data class EvaluationAsOf(
    val epochMs: Long,
    val provenance: ClockProvenance,
) {
    init {
        require(epochMs > 0L) { "EvaluationAsOf.epochMs must be a real epoch (> 0), was $epochMs." }
    }

    companion object {
        /** A trusted, injected clock reading (production). */
        fun authoritative(epochMs: Long): EvaluationAsOf =
            EvaluationAsOf(epochMs, ClockProvenance.AUTHORITATIVE_INJECTED)

        /** A best-effort monotonic reading when no trusted wall clock is available; future exclusion still applies. */
        fun bestEffort(epochMs: Long): EvaluationAsOf =
            EvaluationAsOf(epochMs, ClockProvenance.BEST_EFFORT_MONOTONIC)
    }
}
