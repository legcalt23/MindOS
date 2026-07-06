package com.mindos.app.features.mindboost.emotions.data.local

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.SessionRecord
import com.mindos.app.features.mindboost.core.session.SessionRecordStore
import com.mindos.app.features.mindboost.core.session.SessionStatus
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto

/**
 * MC8g/MC8h durable-terminal recovery for Emociones. Runs on entry (BEFORE the generic orphan scanner) and makes
 * the write-ahead `SessionRecord` and the derived aggregate consistent with the CANONICAL emotion terminal:
 *
 *  1. Retries every durable PENDING terminal (a COMPLETED/SAFETY_STOP/ABANDONED whose summary save failed). If the
 *     summary now persists, the pending is deleted so the rich data is never lost; a still-failing save increments
 *     the pending's `attemptCount` (+ `lastAttemptAtMillis`) so a persistently failing terminal is observable.
 *  2. If any pending was recovered, RECOMPUTES the aggregate from the completed summaries (MC8h P1) — a completion
 *     saved by retry advances calibration without waiting for a future session.
 *  3. Reconciles every Emotions `SessionRecord` against its canonical summary (matched by opaque `sessionId`):
 *     CREATES a missing record when the recorder never wrote one (hung/failed onStart), and REPAIRS an existing
 *     one's status AND its trials/duration so a repaired FINALIZED row is never analytically corrupt. A
 *     completed/safety-stop summary forces FINALIZED (repairing a STARTED left by a failed finalize, or an
 *     ABANDONED the generic scanner wrongly set); an abandoned summary forces ABANDONED.
 *
 * The EMOTION SUMMARY is the authority; the `SessionRecord` and aggregate follow it. This guarantees no durable
 * `summary.completed=true` + `SessionRecord.ABANDONED`/missing for the same session. Pure, idempotent, unit-testable.
 */
class EmotionTerminalReconciler(
    private val repository: EmotionRepository,
    private val recordStore: SessionRecordStore,
    private val clock: EpochClock,
    private val userId: String,
) {
    /** Retries pending terminals, recomputes the aggregate if any recovered, then repairs records. Safe to repeat. */
    suspend fun reconcile() {
        val recovered = retryPendingTerminals()
        if (recovered) recomputeAggregate()
        reconcileRecords()
    }

    /** Returns true iff at least one pending terminal's summary was persisted this pass. */
    private suspend fun retryPendingTerminals(): Boolean {
        val now = clock.nowMillis()
        var recoveredAny = false
        repository.listPendingTerminals(userId).forEach { pending ->
            val sessionId = pending.summary.sessionId ?: return@forEach
            val saved = runCatching { repository.saveSessionSummary(userId, pending.summary) }.isSuccess
            if (saved) {
                runCatching { repository.deletePendingTerminal(userId, sessionId) }
                recoveredAny = true
            } else {
                runCatching {
                    repository.savePendingTerminal(
                        userId,
                        pending.copy(attemptCount = pending.attemptCount + 1, lastAttemptAtMillis = now),
                    )
                }
            }
        }
        return recoveredAny
    }

    /** Derives the aggregate from the completed summaries via the single authority; null → nothing to advance. */
    private suspend fun recomputeAggregate() {
        val summaries = runCatching { repository.recentSummaries(userId) }.getOrDefault(emptyList())
        EmotionAggregateFactory.fromSummaries(summaries, clock.nowMillis())
            ?.let { aggregate -> runCatching { repository.saveAggregate(userId, aggregate) } }
    }

    private suspend fun reconcileRecords() {
        val summaries = runCatching { repository.recentSummaries(userId) }.getOrDefault(emptyList())
            .filter { it.sessionId != null }
        val now = clock.nowMillis()
        val existing = recordStore.all()
            .filter { it.block == ScoreModuleId.EMOTIONS }
            .associateBy { it.sessionId }
        summaries.forEach { summary ->
            val sessionId = summary.sessionId ?: return@forEach
            val target = if (summary.abandoned) SessionStatus.ABANDONED else SessionStatus.FINALIZED
            val record = existing[sessionId]
            if (record == null) {
                runCatching { recordStore.upsert(newRecordFrom(summary, sessionId, target, now)) }
            } else {
                val repaired = repair(record, summary, target, now)
                if (repaired != null) runCatching { recordStore.upsert(repaired) }
            }
        }
    }

    /** A minimal write-ahead row derived entirely from the canonical summary (no raw content). */
    private fun newRecordFrom(
        summary: EmotionSessionSummaryDto,
        sessionId: String,
        target: SessionStatus,
        now: Long,
    ) =
        SessionRecord(
            sessionId = sessionId,
            block = ScoreModuleId.EMOTIONS,
            status = target,
            startedAtMs = summary.startedAtMillis,
            updatedAtMs = now,
            completedTrials = if (target == SessionStatus.FINALIZED) 1 else 0,
            plannedTrials = 1,
            activeDurationMs = summary.durationMs,
        )

    /**
     * The record repaired to match its summary, or null if it is already coherent (idempotent — a no-op pass
     * never re-bumps `updatedAtMs`). Status, trials, and duration are all reconciled: a FINALIZED row gets at
     * least one completed trial, and the active duration never falls below the summary's.
     */
    private fun repair(
        record: SessionRecord,
        summary: EmotionSessionSummaryDto,
        target: SessionStatus,
        now: Long,
    ): SessionRecord? {
        val completedTrials =
            if (target == SessionStatus.FINALIZED) maxOf(record.completedTrials, 1) else record.completedTrials
        val activeDurationMs = maxOf(record.activeDurationMs, summary.durationMs)
        val coherent = record.status == target &&
            record.completedTrials == completedTrials &&
            record.activeDurationMs == activeDurationMs
        return if (coherent) {
            null
        } else {
            record.copy(
                status = target,
                completedTrials = completedTrials,
                activeDurationMs = activeDurationMs,
                updatedAtMs = now,
            )
        }
    }
}
