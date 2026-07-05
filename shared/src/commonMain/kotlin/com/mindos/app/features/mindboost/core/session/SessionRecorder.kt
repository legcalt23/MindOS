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
    suspend fun onStart(sessionId: String, block: ScoreModuleId, startedAtMs: Long, plannedTrials: Int)
    suspend fun onCheckpoint(sessionId: String, completedTrials: Int, activeDurationMs: Long)
    suspend fun onFinalize(sessionId: String, completedTrials: Int, activeDurationMs: Long)
    suspend fun onAbandon(sessionId: String)
}

/** No-op recorder: the default collaborator, so a block that has not opted in records nothing. */
object NoOpSessionRecorder : SessionRecorder {
    override suspend fun onStart(sessionId: String, block: ScoreModuleId, startedAtMs: Long, plannedTrials: Int) = Unit
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

    override suspend fun onStart(sessionId: String, block: ScoreModuleId, startedAtMs: Long, plannedTrials: Int) =
        safely("onStart") {
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
        store.upsert(current.copy(status = SessionStatus.ABANDONED, updatedAtMs = clock.nowMillis()))
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
        store.upsert(
            current.copy(
                status = status,
                completedTrials = completedTrials,
                activeDurationMs = activeDurationMs,
                updatedAtMs = clock.nowMillis(),
            ),
        )
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
