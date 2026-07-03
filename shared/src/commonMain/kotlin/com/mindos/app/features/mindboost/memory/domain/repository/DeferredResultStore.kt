package com.mindos.app.features.mindboost.memory.domain.repository

import com.mindos.app.features.mindboost.memory.domain.scoring.DeferredRecallOutcome
import com.mindos.app.features.mindboost.memory.domain.scoring.DeferredRecordLog

/**
 * EX3 deferred recall (P3) · Durable, per-user accumulation of completed 24h delayed-recall outcomes. The
 * accumulated [DeferredRecordLog] makes EX3's episodic (DELAYED_RECALL_ACCURACY) signal durable across
 * sessions: one outcome per completed delayed test, and the ≥20-valid-outcome gate is only reached over
 * many days.
 *
 * Production persistence is local-first + encrypted and lives in the `mindboost_memory` namespace, so
 * account deletion (which clears the whole namespace) already erases it — no separate erasure wiring. This
 * is DISTINCT from the deferred-recall ITEM registry (`DeferredRecallRepository`, "deferred:" keys): that
 * holds pending material to test; this holds the SCORED results of completed tests ("delayed:" keys).
 */
interface DeferredResultStore {
    /** The accumulated log for [userId], or an empty log if nothing recorded / it was corrupt. */
    suspend fun load(userId: String): DeferredRecordLog

    /** Appends one completed delayed test's [outcome], caps, persists, and returns the new log. */
    suspend fun record(userId: String, outcome: DeferredRecallOutcome): DeferredRecordLog
}
