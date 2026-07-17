package com.mindos.app.features.mindboost.emotions.data.local

import com.mindos.app.core.logging.ObservabilityHub
import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.SessionRecord
import com.mindos.app.features.mindboost.core.session.SessionRecordStore
import com.mindos.app.features.mindboost.core.session.SessionRecordUpsertOutcome
import com.mindos.app.features.mindboost.core.session.SessionStatus
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto
import kotlin.coroutines.cancellation.CancellationException

/**
 * MC8g/MC8h/MC8i durable-terminal recovery for Emociones. Runs on entry (BEFORE the generic orphan scanner) and
 * makes the write-ahead `SessionRecord` and the derived aggregate consistent with the CANONICAL emotion terminal:
 *
 *  1. Retries every durable PENDING terminal (a COMPLETED/SAFETY_STOP/ABANDONED whose summary save failed). If the
 *     summary now persists, the pending is deleted so the rich data is never lost; a still-failing save increments
 *     the pending's `attemptCount` (+ `lastAttemptAtMillis`) so a persistently failing terminal is observable.
 *  2. ALWAYS recomputes the aggregate from the DEDUPED summaries (MC8i): the aggregate is a derived, self-healing
 *     view. If completed sessions exist it is rewritten; if none exist any stale aggregate is DELETED (no phantom
 *     profile). `endReason` governs eligibility — SAFETY_STOP/ABANDONED never count.
 *  3. Reconciles every Emotions `SessionRecord` against its canonical summary (matched by opaque `sessionId`):
 *     CREATES a missing record when the recorder never wrote one (hung/failed onStart), and REPAIRS an existing
 *     one's status AND its trials/duration so neither a FINALIZED nor an ABANDONED row is analytically corrupt.
 *     `endReason` is the primary target authority; the booleans are legacy fallback. ABANDONED forces
 *     `completedTrials = 0`.
 *
 * The EMOTION SUMMARY is the authority; the `SessionRecord` and aggregate follow it. Each of the three sections is
 * independently fault-isolated (MC8i): a recoverable storage failure in one never prevents the others, so entering
 * Emociones never crashes. Pure, idempotent, unit-testable.
 */
class EmotionTerminalReconciler(
    private val repository: EmotionRepository,
    private val recordStore: SessionRecordStore,
    private val clock: EpochClock,
    private val userId: String,
) {
    /**
     * Retries pending terminals, self-heals the aggregate, then repairs records — each section fault-isolated so a
     * partial storage failure can never propagate out of a MindBoost entry. Cooperative cancellation IS respected
     * ([recoverable] rethrows [CancellationException]); only recoverable storage failures are swallowed. Idempotent.
     *
     * TODO(MC-follow-up): emit a local analytics/log event for each isolated recovery failure — a swallowed storage
     * error here is currently invisible in production (no analytics infra wired for emotions recovery yet).
     */
    suspend fun reconcile() {
        recoverable { retryPendingTerminals() }
        recoverable { recomputeAggregate() }
        recoverable { reconcileRecords() }
    }

    /**
     * Runs [block], returning its Result. A recoverable storage failure is captured (swallowed by the caller);
     * a [CancellationException] is RETHROWN so cooperative cancellation is never mistaken for a recoverable fault.
     */
    private inline fun <T> recoverable(block: () -> T): Result<T> {
        val result = runCatching { block() }
        // Fatal defects (Error: OOM/StackOverflow/AssertionError) and cooperative cancellation are NEVER a
        // recoverable storage failure — rethrow them (MC8l); isolate only ordinary non-fatal exceptions.
        result.exceptionOrNull()?.let { if (it is CancellationException || it is Error) throw it }
        return result
    }

    /** Retries each pending terminal's summary save; deletes it on success, else bumps attemptCount/lastAttemptAt. */
    private suspend fun retryPendingTerminals() {
        val now = clock.nowMillis()
        repository.listPendingTerminals(userId).forEach { pending ->
            val sessionId = pending.summary.sessionId
            // MC8l: a corrupt pending (invalid/blank sessionId) can't be retried (saveSessionSummary would reject it)
            // nor key-deleted safely — leave it UNTOUCHED: no crash, no invalid write, no attemptCount thrash.
            // MC8p: likewise skip a pending whose terminal semantics are INCOHERENT (unknown/contradictory endReason)
            // — retrying would always fail the write boundary and re-saving would thrash attemptCount forever.
            // TODO(MC-follow-up): quarantine/cleanup (key-level delete + local analytics) for corrupt pendings.
            if (sessionId == null || !EmotionTerminalSemantics.isValidSessionId(sessionId)) return@forEach
            if (!EmotionTerminalSemantics.hasValidTerminalSemantics(pending.summary)) return@forEach
            val saved = recoverable { repository.saveSessionSummary(userId, pending.summary) }.isSuccess
            if (saved) {
                recoverable { repository.deletePendingTerminal(userId, sessionId) }
            } else {
                recoverable {
                    repository.savePendingTerminal(
                        userId,
                        pending.copy(attemptCount = pending.attemptCount + 1, lastAttemptAtMillis = now),
                    )
                }
            }
        }
    }

    /**
     * Self-heals the aggregate from the DEDUPED summaries (MC8i): rewrites it when a completed session exists,
     * DELETES a stale one when none remains. `recentSummaries()` is called WITHOUT a fallback so a transient read
     * failure propagates (and the caller's fault-isolation leaves the aggregate untouched) — a read failure must
     * NEVER be misread as "no completed sessions" and wipe a valid aggregate.
     */
    private suspend fun recomputeAggregate() {
        val summaries = repository.recentSummaries(userId)
        val aggregate = EmotionAggregateFactory.fromSummaries(summaries, clock.nowMillis())
        if (aggregate != null) repository.saveAggregate(userId, aggregate) else repository.deleteAggregate(userId)
    }

    private suspend fun reconcileRecords() {
        // MC8l write barrier: only a VALID sessionId may be reconciled into the (global) SessionRecordStore. A
        // legacy/corrupt summary with an invalid/blank sessionId is tolerated on READ but NEVER written to a
        // write-ahead/global store, and never passed to a key builder. Read-tolerant, write-safe.
        val summaries = repository.recentSummaries(userId)
            .filter { EmotionTerminalSemantics.hasReliableSessionIdentity(it) }
        val now = clock.nowMillis()
        val existing = recordStore.all()
            .filter { it.block == ScoreModuleId.EMOTIONS }
            .associateBy { it.sessionId }
        summaries.forEach { summary ->
            val sessionId = summary.sessionId ?: return@forEach
            // MC8n: skip ambiguous/corrupt terminals (unknown endReason, or null with both/neither boolean) — a
            // row that isn't an unambiguous terminal is NEVER finalized into the write-ahead store.
            val target = EmotionTerminalSemantics.targetStatusOrNull(summary) ?: return@forEach
            val record = existing[sessionId]
            // MC8k: use `recoverable` (never a bare `runCatching`) so a storage failure is isolated but a
            // CancellationException/Error from upsert during create/repair is rethrown, not swallowed.
            if (record == null) {
                observeUpsert(recoverable { recordStore.upsert(newRecordFrom(summary, sessionId, target, now)) })
            } else {
                val repaired = repair(record, summary, target, now)
                if (repaired != null) observeUpsert(recoverable { recordStore.upsert(repaired) })
            }
        }
    }

    // P2-01: this reconcile is best-effort BY CONTRACT (idempotent, re-run on every entry — a dropped write
    // self-heals next run), but "best-effort" is not "silent": a non-Stored upsert outcome (a revoked subject, or a
    // retryable revocation/backend fault) is surfaced as a PII-free breadcrumb so it is observable, not swallowed.
    private fun observeUpsert(result: Result<SessionRecordUpsertOutcome>) {
        val outcome = result.getOrNull() ?: return // a thrown storage fault was already isolated by `recoverable`
        if (outcome !is SessionRecordUpsertOutcome.Stored) {
            ObservabilityHub.breadcrumb("emotion_terminal_reconcile_upsert_not_stored")
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
            // MC8n/MC8o: completed TRIALS follow the terminal REASON via the single completedTrialsFor authority,
            // not the raw status — a SAFETY_STOP is FINALIZED (lifecycle) but did NO completed work, so it must
            // not look like a completed session to future analytics.
            completedTrials = EmotionTerminalSemantics.completedTrialsFor(summary),
            plannedTrials = 1,
            activeDurationMs = summary.durationMs,
        )

    /**
     * The record repaired to match its summary, or null if it is already coherent (idempotent — a no-op pass
     * never re-bumps `updatedAtMs`). Status, trials, and duration are all reconciled: a FINALIZED row gets at
     * least one completed trial, an ABANDONED row is forced to ZERO completed trials (never an abandoned session
     * that claims completed work), and the active duration never falls below the summary's.
     */
    private fun repair(
        record: SessionRecord,
        summary: EmotionSessionSummaryDto,
        target: SessionStatus,
        now: Long,
    ): SessionRecord? {
        // MC8n/MC8o: trials follow the terminal REASON via the single completedTrialsFor authority — a repaired
        // SAFETY_STOP (FINALIZED) is reset to ZERO completed trials so it can never look like completed work, even
        // if a prior write left completedTrials > 0; a completion keeps at least one (never lowering a prior count).
        val isCompletion = EmotionTerminalSemantics.completedTrialsFor(summary) > 0
        val completedTrials = if (isCompletion) maxOf(record.completedTrials, 1) else 0
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
