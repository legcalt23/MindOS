package com.mindos.app.features.mindboost.memory.domain.repository

import com.mindos.app.features.mindboost.memory.domain.scoring.SpanRecordLog
import com.mindos.app.features.mindboost.memory.domain.scoring.SpanTrial

/**
 * P1 EX1 (P4) · Durable, per-user accumulation of complex-span records. The accumulated [SpanRecordLog]
 * is what makes the EX1 span score durable across sessions: a live session contributes one [SpanTrial],
 * and the ≥20-valid-trial profile gate is only reached over many sessions.
 *
 * Production persistence is local-first + encrypted and lives in the `mindboost_memory` namespace, so
 * account deletion (which clears the whole namespace) already erases it — no separate erasure wiring.
 */
interface SpanResultStore {
    /** The accumulated log for [userId], or an empty log if nothing has been recorded / it was corrupt. */
    suspend fun load(userId: String): SpanRecordLog

    /** Appends one session's [trial] + [processingResponses], caps, persists, and returns the new log. */
    suspend fun record(userId: String, trial: SpanTrial, processingResponses: List<Int>): SpanRecordLog
}
