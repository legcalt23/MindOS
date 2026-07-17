package com.mindos.app.features.mindboost.decisiones.domain.repository

import com.mindos.app.features.mindboost.decisiones.domain.scoring.ReversibilityRecordLog
import com.mindos.app.features.mindboost.decisiones.domain.scoring.ReversibilityTrial

/**
 * E2 Reversibilidad (Decisiones · P2) · Durable, per-user accumulation of completed reversibility-triage
 * cards. The accumulated [ReversibilityRecordLog] makes the decision-reversibility discrimination signal
 * durable across sessions; the ≥20-card gate is reached over a few sessions.
 *
 * The production implementation will be local-first + encrypted via `SecureKeyValueStore`. Its namespace
 * MUST be registered in `MindBoostStorageNamespaces.AccountBound` and the AppGraph account-deletion eraser
 * BEFORE it is wired in production (GDPR Art. 17) — that wiring is the next sub-phase; this interface and
 * its implementations persist nothing until a caller constructs them with a namespaced store.
 */
interface ReversibilityResultStore {
    /** The accumulated log for [userId], or an empty log if nothing recorded / it was corrupt. */
    suspend fun load(userId: String): ReversibilityRecordLog

    /** Appends one finished session's [trials], caps, persists, and returns the new log. */
    suspend fun record(userId: String, trials: List<ReversibilityTrial>): ReversibilityRecordLog

    /**
     * S3 write-ahead replay: like [record] but IDEMPOTENT by [sessionId] — merging the same session's [trials]
     * twice (e.g. a journal entry re-applied after a process death that persisted the merge but not the journal
     * confirm) counts them once. Durable implementations MUST dedup atomically (co-locate the merged-id set with
     * the log in one write). The default is the non-idempotent [record] fallback: acceptable ONLY for non-durable
     * stores whose data does not survive a process death anyway (so replay never re-runs against them).
     */
    suspend fun recordOnce(
        userId: String,
        sessionId: String,
        trials: List<ReversibilityTrial>,
    ): ReversibilityRecordLog = record(userId, trials)
}
