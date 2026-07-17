package com.mindos.app.features.mindboost.core.session

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import kotlin.coroutines.cancellation.CancellationException

/**
 * Observes non-fatal recorder errors WITHOUT leaking sensitive data: only a neutral operation label crosses
 * this boundary — never the exception's message or cause (telemetry-minimization rule, 2026-06-20). Fakeable
 * in tests so a swallowed storage error is still assertable.
 */
interface SessionRecorderDiagnostics {
    fun onRecorderError(operation: String)
}

/** Diagnostics sink that ignores errors — the production default when no telemetry is wired. */
object NoOpSessionRecorderDiagnostics : SessionRecorderDiagnostics {
    override fun onRecorderError(operation: String) = Unit
}

/**
 * Write-ahead session lifecycle hooks. TOTAL by contract: a storage/serialization failure NEVER breaks
 * training (each op is best-effort and, on failure, only signals [SessionRecorderDiagnostics]); the ONLY
 * throwable that propagates is [CancellationException] (structured concurrency is preserved). Records
 * lifecycle only — never raw trials, free text, or raw responses (it writes the minimized [SessionRecord]).
 */
interface SessionRecorder {
    /**
     * Opens the session's row. [identity], when present, is written IN THE SAME upsert as STARTED — one row, one
     * write — so a session can never exist with a lifecycle but no reproducible identity, nor the reverse. That
     * atomicity is why the identity is a parameter here rather than a second write by the caller: two writes
     * would race, and the loser would silently blank the winner's field.
     *
     * Returns the TYPED outcome so a caller that must not present a trial without a durable identity can fail
     * closed. Best-effort callers may ignore it (they keep the pre-existing behaviour), which is why the return
     * type is additive rather than a breaking change.
     */
    suspend fun onStart(
        sessionId: String,
        block: ScoreModuleId,
        startedAtMs: Long,
        plannedTrials: Int,
        identity: ReproducibleSessionIdentity? = null,
    ): SessionRecordUpsertOutcome

    suspend fun onCheckpoint(sessionId: String, completedTrials: Int, activeDurationMs: Long)
    suspend fun onFinalize(sessionId: String, completedTrials: Int, activeDurationMs: Long)
    suspend fun onAbandon(sessionId: String)
}

/**
 * No-op recorder: the default collaborator, so a block that has not opted in records nothing.
 *
 * [onStart] reports [SessionRecordUpsertOutcome.Stored] because there is nothing to store and nothing failed —
 * a block that opted out of recording is not a block whose write failed. A caller that REQUIRES a durable
 * identity must therefore inject a real recorder; that is enforced at the wiring, not by this default.
 */
object NoOpSessionRecorder : SessionRecorder {
    override suspend fun onStart(
        sessionId: String,
        block: ScoreModuleId,
        startedAtMs: Long,
        plannedTrials: Int,
        identity: ReproducibleSessionIdentity?,
    ): SessionRecordUpsertOutcome = SessionRecordUpsertOutcome.Stored

    override suspend fun onCheckpoint(sessionId: String, completedTrials: Int, activeDurationMs: Long) = Unit
    override suspend fun onFinalize(sessionId: String, completedTrials: Int, activeDurationMs: Long) = Unit
    override suspend fun onAbandon(sessionId: String) = Unit
}

/**
 * Durable recorder over [SessionRecordStore]. Upserts by sessionId (write-ahead): STARTED creates the row;
 * CHECKPOINT/FINALIZED/ABANDONED read the current row and update status + counters in place, preserving
 * block/startedAt/plannedTrials. A transition on a MISSING row (no prior STARTED persisted) never creates a
 * session; it emits a neutral `<op>.missingRecord` diagnostic and returns. Every store call is wrapped so a
 * failure is diagnosed, not thrown. Uses the injected [clock] for updatedAt and never touches scoring/profile.
 */
class SecureSessionRecorder(
    private val store: SessionRecordStore,
    private val clock: EpochClock,
    private val diagnostics: SessionRecorderDiagnostics = NoOpSessionRecorderDiagnostics,
) : SessionRecorder {

    /**
     * ONE write carrying both lifecycle and [identity]. A storage fault becomes a
     * [SessionRecordUpsertOutcome.RetryableFailure] rather than a throw (the total-by-contract rule holds), but
     * it is RETURNED as well as diagnosed, so a caller that must not proceed without a durable identity can see
     * it. Retrying is idempotent: the upsert is keyed by [sessionId], so a repeat re-writes the same row.
     */
    override suspend fun onStart(
        sessionId: String,
        block: ScoreModuleId,
        startedAtMs: Long,
        plannedTrials: Int,
        identity: ReproducibleSessionIdentity?,
    ): SessionRecordUpsertOutcome = safelyUpsert("onStart") {
        store.upsert(
            SessionRecord(
                sessionId = sessionId,
                block = block,
                status = SessionStatus.STARTED,
                startedAtMs = startedAtMs,
                updatedAtMs = startedAtMs,
                completedTrials = 0,
                plannedTrials = plannedTrials,
                activeDurationMs = 0,
                reproducibleIdentity = identity,
            ),
        )
    }

    override suspend fun onCheckpoint(sessionId: String, completedTrials: Int, activeDurationMs: Long) =
        transition(sessionId, SessionStatus.CHECKPOINT, completedTrials, activeDurationMs, "onCheckpoint")

    override suspend fun onFinalize(sessionId: String, completedTrials: Int, activeDurationMs: Long) =
        transition(sessionId, SessionStatus.FINALIZED, completedTrials, activeDurationMs, "onFinalize")

    override suspend fun onAbandon(sessionId: String) = safely("onAbandon") {
        val current = store.find(sessionId)
        if (current == null) {
            diagnostics.onRecorderError("onAbandon.missingRecord")
            return@safely
        }
        reportIfNotStored(
            store.upsert(current.copy(status = SessionStatus.ABANDONED, updatedAtMs = clock.nowMillis())),
            "onAbandon",
        )
    }

    private suspend fun transition(
        sessionId: String,
        status: SessionStatus,
        completedTrials: Int,
        activeDurationMs: Long,
        operation: String,
    ) = safely(operation) {
        val current = store.find(sessionId)
        if (current == null) {
            diagnostics.onRecorderError("$operation.missingRecord")
            return@safely
        }
        reportIfNotStored(
            store.upsert(
                current.copy(
                    status = status,
                    completedTrials = completedTrials,
                    activeDurationMs = activeDurationMs,
                    updatedAtMs = clock.nowMillis(),
                ),
            ),
            operation,
        )
    }

    // P2-01: the write-ahead recorder is best-effort BY CONTRACT, but "best-effort" is not "silent" — a
    // non-Stored upsert outcome is routed to diagnostics (a neutral op label only, never the payload), so a
    // dropped write is observable, not swallowed.
    private fun reportIfNotStored(outcome: SessionRecordUpsertOutcome, operation: String) {
        if (outcome !is SessionRecordUpsertOutcome.Stored) diagnostics.onRecorderError("$operation.notStored")
    }

    /**
     * As [safely], but RETURNS the outcome instead of only diagnosing it: a fault becomes a typed
     * [SessionRecordUpsertOutcome.RetryableFailure] the caller can act on, never a silent success.
     * [CancellationException] always propagates.
     */
    @Suppress("SwallowedException")
    private suspend fun safelyUpsert(
        operation: String,
        action: suspend () -> SessionRecordUpsertOutcome,
    ): SessionRecordUpsertOutcome = try {
        action().also { reportIfNotStored(it, operation) }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
        diagnostics.onRecorderError(operation)
        SessionRecordUpsertOutcome.RetryableFailure("${operation}_threw")
    }

    // The Throwable is deliberately NOT propagated or logged (privacy: no message/cause to telemetry); only a
    // neutral op label signals it. CancellationException is re-thrown so structured concurrency is preserved.
    @Suppress("SwallowedException")
    private suspend fun safely(operation: String, action: suspend () -> Unit) {
        try {
            action()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            diagnostics.onRecorderError(operation)
        }
    }
}
