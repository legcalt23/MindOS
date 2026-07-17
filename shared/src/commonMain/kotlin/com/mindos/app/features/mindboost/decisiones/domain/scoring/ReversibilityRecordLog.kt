package com.mindos.app.features.mindboost.decisiones.domain.scoring

/**
 * E2 Reversibilidad (Decisiones · P2) · The per-user accumulation of reversibility-triage CARDS across
 * sessions. The durable signal is one growing block over the retained cards (so the ≥20-card min-n is
 * reached over a few sessions, ~6 cards each). Bounded to the most recent [MAX_RETAINED_TRIALS] cards: a
 * recency window, copy-on-write, never an unbounded log of raw behavior.
 */
data class ReversibilityRecordLog(
    val trials: List<ReversibilityTrial> = emptyList(),
    // S3 write-ahead replay dedup: the recently-merged session ids. `appendOnce` skips a session already here so a
    // replayed journal entry (persisted-but-not-confirmed before a process death) does NOT re-append its trials —
    // exactly-once by session id, co-located with `trials` so both are written in ONE atomic `put`. Bounded like the
    // trial window; NOT read by the scorer (it consumes only `trials`).
    val mergedSessionIds: List<String> = emptyList(),
) {

    /** Appends a finished session's [newTrials], capping to the recent window (drops oldest). Preserves dedup ids. */
    fun append(newTrials: List<ReversibilityTrial>): ReversibilityRecordLog =
        copy(trials = (trials + newTrials).takeLast(MAX_RETAINED_TRIALS))

    /**
     * Idempotent by [sessionId]: a no-op (returns `this`) if [sessionId] was already merged; otherwise appends
     * [newTrials] AND records [sessionId] (both capped). This is the write-ahead-safe merge — replaying the same
     * session's journal entry twice can never double-count its trials.
     */
    fun appendOnce(sessionId: String, newTrials: List<ReversibilityTrial>): ReversibilityRecordLog =
        if (sessionId in mergedSessionIds) {
            this
        } else {
            ReversibilityRecordLog(
                trials = (trials + newTrials).takeLast(MAX_RETAINED_TRIALS),
                mergedSessionIds = (mergedSessionIds + sessionId).takeLast(MAX_RETAINED_SESSION_IDS),
            )
        }

    companion object {
        const val MAX_RETAINED_TRIALS = 120
        const val MAX_RETAINED_SESSION_IDS = 32
    }
}
