package com.mindos.app.features.mindboost.memory.domain.repository

import com.mindos.app.features.mindboost.memory.domain.scoring.PairsRecordLog
import com.mindos.app.features.mindboost.memory.domain.scoring.PairsTrial

/**
 * P1 EX4 (P2b) · Durable, per-user accumulation of paired-associates records. The accumulated
 * [PairsRecordLog] makes the EX4 associative score durable across sessions: a live session contributes
 * one [PairsTrial], and the ≥20-valid-trial profile gate is only reached over many sessions.
 *
 * Production persistence is local-first + encrypted and lives in the `mindboost_memory` namespace, so
 * account deletion (which clears the whole namespace) already erases it — no separate erasure wiring.
 */
interface PairsResultStore {
    /** The accumulated log for [userId], or an empty log if nothing has been recorded / it was corrupt. */
    suspend fun load(userId: String): PairsRecordLog

    /** Appends one session's [trial], caps, persists, and returns the new log. */
    suspend fun record(userId: String, trial: PairsTrial): PairsRecordLog
}
