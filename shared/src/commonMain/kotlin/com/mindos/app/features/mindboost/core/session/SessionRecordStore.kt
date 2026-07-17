package com.mindos.app.features.mindboost.core.session

import kotlin.coroutines.cancellation.CancellationException

/**
 * A DIAGNOSTIC read of a [SessionRecord] that distinguishes an absent row from an unreadable one and from a
 * transient backend fault — richer than [SessionRecordStore.find], which collapses missing/corrupt to `null`.
 * Used by the cognitive write-ahead check to attach the right anomaly (missing vs unreadable vs read-failed).
 */
sealed interface SessionRecordRead {
    data class Found(val record: SessionRecord) : SessionRecordRead

    /** No row was recorded for this sessionId. */
    data object Missing : SessionRecordRead

    /** A row EXISTS but could not be parsed/decrypted (corrupt / key-invalidated / unsupported version). */
    data object Unreadable : SessionRecordRead

    /** A transient storage/backend fault: the read THREW. Distinct from a corrupt row. */
    data object ReadFailed : SessionRecordRead
}

/**
 * Outcome of [SessionRecordStore.upsert] (SYSTEM-1 slice 6-R3, P2-01 ENMIENDA 2026-07-14). A write-ahead upsert
 * is NECESSARY for recovery/sealing, so it can NEVER report a silent `Unit` success while the write actually
 * failed or was refused: the caller must be able to distinguish "persisted" from "not persisted" and act on it
 * (retry / honest degrade). [RetryableFailure] carries a PII-free reason only; never the payload or a Throwable.
 */
sealed interface SessionRecordUpsertOutcome {
    /** The row is durably persisted. */
    data object Stored : SessionRecordUpsertOutcome

    /** Refused: the subject was revoked (account-erased) on this backend — terminal, never retried automatically. */
    data object RejectedRevoked : SessionRecordUpsertOutcome

    /** Not persisted due to a transient fault (backend fault, or revocation status unavailable) — retryable. */
    data class RetryableFailure(val reason: String) : SessionRecordUpsertOutcome
}

/**
 * Outcome of [SecureSessionRecordStore.deleteAll] — a subject-scoped bulk erasure used ONLY by account-deletion
 * (SYSTEM-1 slice 6-R3 fold-in fix). Mirrors
 * [com.mindos.app.features.mindboost.core.session.cognitive.DeleteResult]: kept local to this package (no
 * cross-package dependency) since [SessionRecordStore] does not otherwise reference `core.session.cognitive`.
 */
sealed interface SessionRecordDeleteResult {
    /** [recordCount] of the CURRENT subject's own rows were removed. */
    data class Cleared(val recordCount: Int) : SessionRecordDeleteResult

    /** The backend faulted mid-erasure; the caller (the eraser) must fail closed, never report completion. */
    data object Failed : SessionRecordDeleteResult
}

/**
 * The canonical local log of training sessions. Total and suspend-based (matches the encrypted store).
 * Writes are write-ahead upserts keyed by [SessionRecord.sessionId], so STARTED -> CHECKPOINT ->
 * FINALIZED/ABANDONED update the same row in place; a retry or a later transition never duplicates a session.
 */
interface SessionRecordStore {
    /**
     * Insert or replace the row for [record]'s session id, returning a TYPED [SessionRecordUpsertOutcome] — never
     * a silent success (P2-01). A best-effort caller (write-ahead) may ignore the value only if it declares so in
     * its own contract AND routes non-[SessionRecordUpsertOutcome.Stored] to telemetry; a recovery/seal caller
     * must NOT report success unless the outcome is [SessionRecordUpsertOutcome.Stored].
     */
    suspend fun upsert(record: SessionRecord): SessionRecordUpsertOutcome

    /** The record for [sessionId], or null if none was recorded. */
    suspend fun find(sessionId: String): SessionRecord?

    /** Every recorded session, newest first (by startedAtMs, then sessionId). */
    suspend fun all(): List<SessionRecord>

    /** Remove the row for [sessionId] (no-op if absent). */
    suspend fun delete(sessionId: String)

    /**
     * Read-only INTEGRITY / lifecycle diagnostic for the cognitive write-ahead check — NOT a new parallel
     * authority: [find] stays the canonical accessor and the lifecycle `SessionRecord` (written by
     * `SecureSessionRecorder`, reconciled by `SessionRecoveryScanner`) remains the single write-ahead / recovery
     * source of truth. This read exists ONLY to distinguish missing / unreadable / read-failed, which [find]
     * collapses to `null`. The default is find-based (Found/Missing, and [SessionRecordRead.ReadFailed] if a
     * backend read throws); the durable store overrides it to ALSO surface [SessionRecordRead.Unreadable]
     * (a corrupt/undecryptable row). Never throws; never mutates.
     */
    @Suppress("SwallowedException")
    suspend fun findEvidence(sessionId: String): SessionRecordRead = try {
        find(sessionId)?.let { SessionRecordRead.Found(it) } ?: SessionRecordRead.Missing
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
        SessionRecordRead.ReadFailed
    }
}

/** In-memory [SessionRecordStore] for tests and non-persistent contexts. */
class InMemorySessionRecordStore : SessionRecordStore {
    private val rows = mutableMapOf<String, SessionRecord>()

    override suspend fun upsert(record: SessionRecord): SessionRecordUpsertOutcome {
        require(record.sessionId.isNotBlank()) { "sessionId must not be blank." }
        rows[record.sessionId] = record
        return SessionRecordUpsertOutcome.Stored
    }

    override suspend fun find(sessionId: String): SessionRecord? = rows[sessionId]

    override suspend fun all(): List<SessionRecord> =
        rows.values.sortedWith(compareByDescending<SessionRecord> { it.startedAtMs }.thenBy { it.sessionId })

    override suspend fun delete(sessionId: String) {
        rows.remove(sessionId)
    }
}
