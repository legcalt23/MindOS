package com.mindos.app.features.mindboost.core.sync

/**
 * The kind of entity a queue entry tracks. Phase 1 only syncs focus sessions; the enum leaves room for the
 * other motors without pre-building their pipelines.
 */
enum class SyncEntityType {
    FOCUS_SESSION,
}

/**
 * Lifecycle of a single queued sync.
 *
 * - [PENDING]   — not yet mirrored to the cloud (or a retryable transport failure): still to sync.
 * - [SYNCED]    — the remote document matches local (a fresh write OR a verified duplicate).
 * - [FAILED]    — a non-retryable failure (permission denied, validation, unknown). NEVER means "synced".
 * - [CONFLICT]  — the remote document diverges from local and needs resolution, not a silent overwrite.
 */
enum class SyncQueueStatus {
    PENDING,
    SYNCED,
    FAILED,
    CONFLICT,
}

/**
 * A minimal, motor-agnostic record of one session's sync state. Contains ONLY the queue metadata and the
 * privacy-preserving [fingerprint] hash — never raw trials or per-item content.
 *
 * [entryId] is stable and idempotent (for a focus session it equals the [sessionId]); re-recording the same
 * [entryId] upserts, so a retry updates the row in place instead of duplicating it.
 */
data class SyncQueueEntry(
    val entryId: String,
    val userId: String,
    val sessionId: String,
    val entityType: SyncEntityType,
    val status: SyncQueueStatus,
    val attemptCount: Int,
    val lastAttemptAtMs: Long,
    val lastErrorCode: String?,
    val fingerprint: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)

/**
 * Local record of what still needs to reach the cloud. Implementations MUST be total AND observable — no
 * operation throws on normal storage/serialization errors (only CancellationException propagates), so a queue
 * failure never breaks local training (local save is the source of truth); but [record] REPORTS whether the row
 * was actually persisted so callers can preserve attempt traceability (e.g. skip a remote write when the PENDING
 * row could not be recorded). A non-positive [pending] limit returns an empty list rather than throwing.
 */
interface SyncQueueRepository {
    /**
     * Upsert by [SyncQueueEntry.entryId] (idempotent). Returns **true only when the row was actually
     * persisted/recorded**; **false** when it could not be written but no exception was raised. False is never
     * success — it must not be interpreted as a durable attempt.
     */
    suspend fun record(entry: SyncQueueEntry): Boolean

    /** The current row for [entryId], or null if none was recorded. */
    suspend fun find(entryId: String): SyncQueueEntry?

    /** Rows still awaiting sync for a user, newest first. A non-positive [limit] returns an empty list. */
    suspend fun pending(userId: String, limit: Int): List<SyncQueueEntry>

    /** Removes every entry owned by [userId] (per-user erasure / account deletion). Returns the count removed. */
    suspend fun deleteByUser(userId: String): Int
}

/**
 * In-memory queue. Phase 1 default: durable-queue persistence (encrypted + wired into the account-deletion
 * eraser) is deferred to Phase 1.1 — losing an in-memory PENDING row never loses PROGRESS, because the local
 * session summary is already persisted; only the cloud mirror is delayed until the next attempt.
 */
class InMemorySyncQueueRepository : SyncQueueRepository {
    private val rows = mutableMapOf<String, SyncQueueEntry>()

    override suspend fun record(entry: SyncQueueEntry): Boolean {
        rows[entry.entryId] = entry
        return true
    }

    override suspend fun find(entryId: String): SyncQueueEntry? = rows[entryId]

    override suspend fun pending(userId: String, limit: Int): List<SyncQueueEntry> {
        if (limit <= 0) return emptyList()
        return rows.values
            .filter { it.userId == userId && it.status == SyncQueueStatus.PENDING }
            .sortedByDescending { it.updatedAtMs }
            .take(limit)
    }

    override suspend fun deleteByUser(userId: String): Int {
        val toRemove = rows.values.filter { it.userId == userId }.map { it.entryId }
        toRemove.forEach { rows.remove(it) }
        return toRemove.size
    }
}

/**
 * Maps a classified [SyncWriteResult] to the queue [SyncQueueStatus]. A retryable [SyncWriteResult.NetworkFailure]
 * stays [SyncQueueStatus.PENDING]; a verified duplicate is a logical success ([SyncQueueStatus.SYNCED]); a
 * [SyncWriteResult.PermissionDenied] is ALWAYS [SyncQueueStatus.FAILED] — never SYNCED.
 */
fun SyncWriteResult.toQueueStatus(): SyncQueueStatus = when (this) {
    SyncWriteResult.Synced, SyncWriteResult.AlreadySyncedDuplicate -> SyncQueueStatus.SYNCED
    is SyncWriteResult.Conflict -> SyncQueueStatus.CONFLICT
    SyncWriteResult.NetworkFailure -> SyncQueueStatus.PENDING
    SyncWriteResult.PermissionDenied,
    SyncWriteResult.ValidationFailure,
    SyncWriteResult.UnknownFailure,
    -> SyncQueueStatus.FAILED
}

/** A short, stable diagnostic code for the queue row (null on success). Never contains PII. */
fun SyncWriteResult.toErrorCode(): String? = when (this) {
    SyncWriteResult.Synced, SyncWriteResult.AlreadySyncedDuplicate -> null
    is SyncWriteResult.Conflict -> reason.name
    SyncWriteResult.PermissionDenied -> "PERMISSION_DENIED"
    SyncWriteResult.NetworkFailure -> "NETWORK"
    SyncWriteResult.ValidationFailure -> "VALIDATION"
    SyncWriteResult.UnknownFailure -> "UNKNOWN"
}
