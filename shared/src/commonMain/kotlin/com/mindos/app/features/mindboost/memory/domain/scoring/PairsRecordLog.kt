package com.mindos.app.features.mindboost.memory.domain.scoring

/**
 * P1 EX4 (P2b) · The accumulated, bounded paired-associates record for one user. Each live session
 * contributes ONE [PairsTrial] — a compact per-session SUMMARY (4 ints: targets/hits/distractors/
 * false-alarms, no RTs / no per-keystroke data), honouring "store summaries, never raw high-volume
 * trials" (project learning 2026-06-05). The discrimination signal is self-contained in the trial, so —
 * unlike the span log — there is no separate response window. Bounded to the most recent
 * [MAX_RETAINED_TRIALS] so the encrypted footprint stays small while still crossing the ≥20-valid-trial
 * profile gate. [append] is pure copy-on-write.
 */
data class PairsRecordLog(
    val trials: List<PairsTrial> = emptyList(),
) {
    fun append(trial: PairsTrial): PairsRecordLog =
        PairsRecordLog(trials = (trials + trial).takeLast(MAX_RETAINED_TRIALS))

    companion object {
        /** Plenty above the 20-valid-trial gate, while bounding the encrypted footprint. */
        const val MAX_RETAINED_TRIALS = 60
    }
}
