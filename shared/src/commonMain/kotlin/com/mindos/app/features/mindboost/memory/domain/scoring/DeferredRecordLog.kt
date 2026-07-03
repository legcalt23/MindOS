package com.mindos.app.features.mindboost.memory.domain.scoring

/**
 * EX3 deferred recall (P3) · The accumulated, bounded delayed-recall record for one user. Each completed
 * 24h delayed-recall test contributes ONE [DeferredRecallOutcome] — a compact summary (4 ints, no RTs /
 * no per-keystroke data), honouring "store summaries, never raw high-volume trials" (project learning
 * 2026-06-05). Bounded to the most recent [MAX_RETAINED_OUTCOMES] so the encrypted footprint stays small
 * while still crossing the ≥20-valid-outcome profile gate (which spans many days). [append] is pure
 * copy-on-write.
 */
data class DeferredRecordLog(
    val outcomes: List<DeferredRecallOutcome> = emptyList(),
) {
    fun append(outcome: DeferredRecallOutcome): DeferredRecordLog =
        DeferredRecordLog(outcomes = (outcomes + outcome).takeLast(MAX_RETAINED_OUTCOMES))

    companion object {
        /** Plenty above the 20-valid-outcome gate, while bounding the encrypted footprint. */
        const val MAX_RETAINED_OUTCOMES = 60
    }
}
