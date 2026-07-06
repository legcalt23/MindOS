package com.mindos.app.features.mindboost.emotions.data.local

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.SessionRecordStore
import com.mindos.app.features.mindboost.core.session.SessionStatus

/**
 * MC8g durable-terminal recovery for Emociones. Runs on entry (BEFORE the generic orphan scanner) and makes the
 * write-ahead `SessionRecord` consistent with the CANONICAL emotion terminal:
 *
 *  1. Retries every durable PENDING terminal (a COMPLETED/SAFETY_STOP/ABANDONED whose summary save failed): if the
 *     summary now persists, the pending is deleted so the rich data is never lost. A still-failing save keeps the
 *     pending for a future attempt.
 *  2. Reconciles each Emotions `SessionRecord` against its saved summary (matched by opaque `sessionId`): a
 *     completed/safety-stop summary forces the record to FINALIZED (repairing a STARTED left by a failed finalize,
 *     OR an ABANDONED the generic scanner wrongly set); an abandoned summary forces ABANDONED. Idempotent.
 *
 * The EMOTION SUMMARY is the authority; the `SessionRecord` follows it. This guarantees no durable
 * `summary.completed=true` + `SessionRecord.ABANDONED` for the same session. Pure and unit-testable.
 */
class EmotionTerminalReconciler(
    private val repository: EmotionRepository,
    private val recordStore: SessionRecordStore,
    private val clock: EpochClock,
    private val userId: String,
) {
    /** Retries pending terminals then repairs Emotions records to match their canonical summary. Safe to repeat. */
    suspend fun reconcile() {
        retryPendingTerminals()
        reconcileRecords()
    }

    private suspend fun retryPendingTerminals() {
        repository.listPendingTerminals(userId).forEach { pending ->
            val sessionId = pending.summary.sessionId ?: return@forEach
            val saved = runCatching { repository.saveSessionSummary(userId, pending.summary) }.isSuccess
            if (saved) runCatching { repository.deletePendingTerminal(userId, sessionId) }
        }
    }

    private suspend fun reconcileRecords() {
        val summaries = runCatching { repository.recentSummaries(userId) }.getOrDefault(emptyList())
        val summaryById = summaries.filter { it.sessionId != null }.associateBy { it.sessionId }
        val now = clock.nowMillis()
        recordStore.all()
            .filter { it.block == ScoreModuleId.EMOTIONS }
            .forEach { record ->
                val summary = summaryById[record.sessionId] ?: return@forEach
                val target = if (summary.abandoned) SessionStatus.ABANDONED else SessionStatus.FINALIZED
                if (record.status != target) {
                    runCatching { recordStore.upsert(record.copy(status = target, updatedAtMs = now)) }
                }
            }
    }
}
