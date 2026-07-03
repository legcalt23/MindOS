package com.mindos.app.features.mindboost.memory.domain.scoring

/**
 * P1 EX1 (P4) · The accumulated, bounded complex-span record for one user. Each live session contributes
 * ONE [SpanTrial] — a compact per-session SUMMARY (4 fields, no RTs / no per-keystroke data), so this
 * honours "store summaries, never raw high-volume trials" (project learning 2026-06-05). It also keeps a
 * window of that session's processing RESPONSES (the yes/no judgements as 0/1), used ONLY by the
 * processing anti-gaming detector to spot button-spamming.
 *
 * Both lists are bounded to their most recent entries so the encrypted footprint stays small while still
 * being able to cross the ≥20-valid-trial profile gate (which spans many sessions). [append] is pure
 * copy-on-write.
 */
data class SpanRecordLog(
    val trials: List<SpanTrial> = emptyList(),
    val processingResponses: List<Int> = emptyList(),
) {
    /** Returns a new log with [trial] and [responses] appended and each list capped to its retention. */
    fun append(trial: SpanTrial, responses: List<Int>): SpanRecordLog =
        SpanRecordLog(
            trials = (trials + trial).takeLast(MAX_RETAINED_TRIALS),
            processingResponses = (processingResponses + responses).takeLast(MAX_RETAINED_RESPONSES),
        )

    companion object {
        /** Plenty above the 20-valid-trial gate, while bounding the encrypted footprint. */
        const val MAX_RETAINED_TRIALS = 60

        /** A recent window of yes/no judgements for the response-entropy detector. */
        const val MAX_RETAINED_RESPONSES = 240
    }
}
