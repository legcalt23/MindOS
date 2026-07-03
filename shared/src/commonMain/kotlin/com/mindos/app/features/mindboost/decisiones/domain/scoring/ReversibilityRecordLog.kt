package com.mindos.app.features.mindboost.decisiones.domain.scoring

/**
 * E2 Reversibilidad (Decisiones · P2) · The per-user accumulation of reversibility-triage CARDS across
 * sessions. The durable signal is one growing block over the retained cards (so the ≥20-card min-n is
 * reached over a few sessions, ~6 cards each). Bounded to the most recent [MAX_RETAINED_TRIALS] cards: a
 * recency window, copy-on-write, never an unbounded log of raw behavior.
 */
data class ReversibilityRecordLog(val trials: List<ReversibilityTrial> = emptyList()) {

    /** Appends a finished session's [newTrials], capping to the recent window (drops oldest). */
    fun append(newTrials: List<ReversibilityTrial>): ReversibilityRecordLog =
        ReversibilityRecordLog((trials + newTrials).takeLast(MAX_RETAINED_TRIALS))

    companion object {
        const val MAX_RETAINED_TRIALS = 120
    }
}
