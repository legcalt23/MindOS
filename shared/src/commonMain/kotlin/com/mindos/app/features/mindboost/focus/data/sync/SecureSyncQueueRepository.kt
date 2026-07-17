package com.mindos.app.features.mindboost.focus.data.sync

import com.mindos.app.features.mindboost.core.sync.SyncEntityType
import com.mindos.app.features.mindboost.core.sync.SyncQueueEntry
import com.mindos.app.features.mindboost.core.sync.SyncQueueRepository
import com.mindos.app.features.mindboost.core.sync.SyncQueueStatus
import com.mindos.app.features.mindboost.focus.data.local.FocusJson
import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.readVersioned
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Storage constants for the persisted sync queue. */
private object SyncQueueSchema {
    const val CURRENT = 1
    const val KEY_PREFIX = "entry:"
}

/**
 * Versioned persistence DTO for one queue row. Enums are stored by `.name` (stable JSON, mirrors the focus
 * DTOs). Contains only queue metadata + the fingerprint hash — never raw trials.
 */
@Serializable
private data class SyncQueueEntryDto(
    val entryId: String,
    val userId: String,
    val sessionId: String,
    val entityType: String,
    val status: String,
    val attemptCount: Int,
    val lastAttemptAtMs: Long,
    val lastErrorCode: String?,
    val fingerprint: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val schemaVersion: Int = SyncQueueSchema.CURRENT,
)

private fun SyncQueueEntry.toDto(): SyncQueueEntryDto = SyncQueueEntryDto(
    entryId = entryId,
    userId = userId,
    sessionId = sessionId,
    entityType = entityType.name,
    status = status.name,
    attemptCount = attemptCount,
    lastAttemptAtMs = lastAttemptAtMs,
    lastErrorCode = lastErrorCode,
    fingerprint = fingerprint,
    createdAtMs = createdAtMs,
    updatedAtMs = updatedAtMs,
)

/** Maps to domain, tolerating an unknown/future enum name (corrupt or newer schema) → null; never throws. */
private fun SyncQueueEntryDto.toDomainOrNull(): SyncQueueEntry? {
    val type = enumOrNull<SyncEntityType>(entityType)
    val stat = enumOrNull<SyncQueueStatus>(status)
    return if (type != null && stat != null) {
        SyncQueueEntry(
            entryId = entryId,
            userId = userId,
            sessionId = sessionId,
            entityType = type,
            status = stat,
            attemptCount = attemptCount,
            lastAttemptAtMs = lastAttemptAtMs,
            lastErrorCode = lastErrorCode,
            fingerprint = fingerprint,
            createdAtMs = createdAtMs,
            updatedAtMs = updatedAtMs,
        )
    } else {
        null
    }
}

private inline fun <reified E : Enum<E>> enumOrNull(name: String): E? =
    enumValues<E>().firstOrNull { it.name == name }

/**
 * [SyncQueueRepository] backed by [SecureKeyValueStore]. Durability follows the platform store:
 *  - **Android:** durable AND encrypted at rest (AES-256-GCM + Keystore).
 *  - **iOS:** durable AND encrypted at rest via the Keychain (IndexedSecureKeyValueStore + IosKeychainBackend,
 *    Phase 1.2); the native binding is pending iOS compile/simulator verification.
 *  - **desktop:** in-memory only (NOT durable) — a dev/test target.
 *
 * One JSON row per entry keyed by `entry:{entryId}`, so recording the same entryId upserts (idempotent by
 * sessionId — no duplicates). **Total AND observable:** no operation throws on storage/serialization/enum errors
 * — only `CancellationException` propagates — but [record] REPORTS whether the row was persisted (`true`) or not
 * (`false`) so callers keep attempt traceability. A corrupt / unknown-enum / future-version row is skipped (never
 * crashes the whole queue), `limit <= 0` returns an empty list, and a store failure degrades to a safe default
 * (record → `false`; find → null; pending → empty; deleteByUser → 0). A `false` write only delays the cloud
 * mirror; local session data is the durable source of truth. Per-user erasable ([deleteByUser]); the whole
 * namespace is also cleared on account deletion via the AppGraph eraser.
 *
 * Corrupt-row deletion policy: a row that cannot be decoded cannot be attributed to a user, so [deleteByUser]
 * leaves it in place (never cross-removes another user's data); it is reclaimed by the full-namespace eraser on
 * account deletion.
 */
class SecureSyncQueueRepository(
    private val store: SecureKeyValueStore,
    private val json: Json = FocusJson.instance,
) : SyncQueueRepository {

    override suspend fun record(entry: SyncQueueEntry): Boolean = queueSafe(false) {
        store.put(SyncQueueSchema.KEY_PREFIX + entry.entryId, json.encodeToString(entry.toDto()))
        true
    }

    override suspend fun find(entryId: String): SyncQueueEntry? = decode(SyncQueueSchema.KEY_PREFIX + entryId)

    override suspend fun pending(userId: String, limit: Int): List<SyncQueueEntry> {
        if (limit <= 0) return emptyList()
        return allEntries()
            .filter { it.userId == userId && it.status == SyncQueueStatus.PENDING }
            .sortedByDescending { it.updatedAtMs }
            .take(limit)
    }

    override suspend fun deleteByUser(userId: String): Int {
        var removed = 0
        for (key in keysSafe()) {
            val entry = decode(key)
            if (entry != null && entry.userId == userId) {
                if (removeSafe(key)) removed++
            }
        }
        return removed
    }

    private suspend fun allEntries(): List<SyncQueueEntry> = keysSafe().mapNotNull { decode(it) }

    private suspend fun decode(key: String): SyncQueueEntry? = queueSafe<SyncQueueEntry?>(null) {
        store.readVersioned(
            key = key,
            currentVersion = SyncQueueSchema.CURRENT,
            parser = json::decodeFromString,
            versionOf = SyncQueueEntryDto::schemaVersion,
        )?.toDomainOrNull()
    }

    private suspend fun keysSafe(): Set<String> = queueSafe(emptySet()) { store.keys(SyncQueueSchema.KEY_PREFIX) }

    private suspend fun removeSafe(key: String): Boolean = queueSafe(false) {
        store.remove(key)
        true
    }

    /** Runs [block], returning [default] on any non-cancellation failure. Never swallows CancellationException. */
    private suspend fun <T> queueSafe(default: T, block: suspend () -> T): T =
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") error: Throwable) {
            default
        }
}
