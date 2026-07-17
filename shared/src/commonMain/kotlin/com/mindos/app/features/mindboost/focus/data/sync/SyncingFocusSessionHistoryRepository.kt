package com.mindos.app.features.mindboost.focus.data.sync

import com.mindos.app.core.config.FeatureFlagsHub
import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.core.sync.SyncEntityType
import com.mindos.app.features.mindboost.core.sync.SyncQueueEntry
import com.mindos.app.features.mindboost.core.sync.SyncQueueRepository
import com.mindos.app.features.mindboost.core.sync.SyncQueueStatus
import com.mindos.app.features.mindboost.core.sync.SyncWriteResult
import com.mindos.app.features.mindboost.core.sync.toErrorCode
import com.mindos.app.features.mindboost.core.sync.toQueueStatus
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.repository.SessionHistoryRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Local-first decorator around a [SessionHistoryRepository]. Local save is the source of truth; cloud sync is
 * best-effort and NEVER blocks training or masks a local failure.
 *
 * `append` order: (1) save local — if it throws, the failure propagates and no cloud work happens; (2) if the
 * feature flag is off, behave exactly as the plain local repo; (3) if there is no authenticated user, stop
 * (local is already durable); (4) otherwise launch the best-effort remote attempt on [syncScope] and return
 * immediately (never awaiting the network). Reads ([recent]) always come from local — Phase 1 does not read
 * from the cloud.
 *
 * [syncScope] should be a long-lived SupervisorScope (with a reporting exception handler) in production;
 * [syncNow] is defensively total — queue and remote failures map to typed [SyncWriteResult]s and only
 * CancellationException propagates — so the launched job does not throw in normal operation.
 */
@Suppress("LongParameterList") // Local-first sync seam: injectable collaborators + config, all for testability.
class SyncingFocusSessionHistoryRepository(
    private val local: SessionHistoryRepository,
    private val remote: FocusSessionRemoteRepository,
    private val queue: SyncQueueRepository,
    private val syncScope: CoroutineScope,
    private val clock: EpochClock,
    private val currentUserId: () -> String?,
    private val syncEnabled: () -> Boolean = { FeatureFlagsHub.current.mindboostSyncFirestoreEnabled },
    private val scoringVersion: String = FocusSessionCloudSchema.SCORING_VERSION,
    private val sourcePlatform: String? = null,
) : SessionHistoryRepository {

    override suspend fun append(result: SessionResult) {
        local.append(result) // durable source of truth; a local failure propagates and stops sync.
        if (!syncEnabled()) return
        val userId = currentUserId() ?: return
        syncScope.launch { syncNow(result, userId) }
    }

    override suspend fun recent(userId: String, limit: Int): List<SessionResult> = local.recent(userId, limit)

    /**
     * Deterministic, awaited sync of one session. Total: it records the classified outcome in [queue] and
     * returns it, and never treats [SyncWriteResult.PermissionDenied] as success. Exposed for tests and for a
     * future durable retry loop; [append] fires it on [syncScope].
     */
    suspend fun syncNow(result: SessionResult, userId: String): SyncWriteResult {
        val nowMs = clock.nowMillis()
        val dto = FocusSessionCloudMapper.toCloudDtoOrNull(result, userId, nowMs, scoringVersion, sourcePlatform)
        return if (dto == null) {
            recordQuietly { recordValidationFailure(result, userId, nowMs) }
            SyncWriteResult.ValidationFailure
        } else {
            syncDto(dto, nowMs)
        }
    }

    private suspend fun syncDto(dto: FocusSessionCloudDto, at: Long): SyncWriteResult {
        // If the queue can't even register the PENDING attempt, do NOT call remote — we would lose all
        // traceability of the attempt. Fail closed with UnknownFailure (never Synced).
        val attempt = beginAttemptOrNull(dto, at) ?: return SyncWriteResult.UnknownFailure
        val outcome = remote.write(dto, FocusSessionCloudMapper.fingerprintOf(dto))
        // Best-effort final write. If it fails, the row stays PENDING (never a false SYNCED) and we still
        // return the real remote outcome — a documented observability/queue risk, not a correctness one.
        recordQuietly { finishAttempt(attempt, outcome, clock.nowMillis()) }
        return outcome
    }

    /** Records (or upserts) a PENDING row with this attempt's number, or null if the queue op fails. */
    private suspend fun beginAttemptOrNull(dto: FocusSessionCloudDto, at: Long): SyncQueueEntry? {
        val recorded = runCatching {
            val existing = queue.find(dto.sessionId)
            val entry = SyncQueueEntry(
                entryId = dto.sessionId,
                userId = dto.userId,
                sessionId = dto.sessionId,
                entityType = SyncEntityType.FOCUS_SESSION,
                status = SyncQueueStatus.PENDING,
                attemptCount = (existing?.attemptCount ?: 0) + 1,
                lastAttemptAtMs = at,
                lastErrorCode = null,
                fingerprint = dto.fingerprint,
                createdAtMs = existing?.createdAtMs ?: at,
                updatedAtMs = at,
            )
            // record returns false if the PENDING row could not be persisted → null → remote is NOT called
            // (attempt traceability preserved). A throwing (contract-violating) queue is also caught → null.
            if (queue.record(entry)) entry else null
        }
        return recorded.getOrElse { throwable -> throwable.rethrowIfCancellation(); null }
    }

    /**
     * Updates the same attempt's row with the classified outcome (attempt count unchanged). Returns whether the
     * final write persisted: if `false` the row stays at its prior state (PENDING) — the queue NEVER shows a
     * fabricated SYNCED it could not write. The caller still returns the real remote outcome (a documented
     * observability/queue risk, not a correctness one).
     */
    private suspend fun finishAttempt(attempt: SyncQueueEntry, outcome: SyncWriteResult, at: Long): Boolean =
        queue.record(
            attempt.copy(
                status = outcome.toQueueStatus(),
                lastErrorCode = outcome.toErrorCode(),
                updatedAtMs = at,
            ),
        )

    private suspend fun recordValidationFailure(result: SessionResult, userId: String, at: Long) {
        queue.record(
            SyncQueueEntry(
                entryId = result.sessionId.value,
                userId = userId,
                sessionId = result.sessionId.value,
                entityType = SyncEntityType.FOCUS_SESSION,
                status = SyncQueueStatus.FAILED,
                attemptCount = 1,
                lastAttemptAtMs = at,
                lastErrorCode = SyncWriteResult.ValidationFailure.toErrorCode(),
                fingerprint = "",
                createdAtMs = at,
                updatedAtMs = at,
            ),
        )
    }

    /**
     * Best-effort queue write: swallows a queue failure (local is already durable and the outcome is decided),
     * but ALWAYS rethrows CancellationException. A swallowed failure leaves the row at its previous state — it
     * never fabricates a SYNCED — and only delays queue observability (documented Phase 1.1 risk).
     */
    private suspend fun recordQuietly(block: suspend () -> Unit) {
        runCatching { block() }.onFailure { it.rethrowIfCancellation() }
    }
}

private fun Throwable.rethrowIfCancellation() {
    if (this is CancellationException) throw this
}
