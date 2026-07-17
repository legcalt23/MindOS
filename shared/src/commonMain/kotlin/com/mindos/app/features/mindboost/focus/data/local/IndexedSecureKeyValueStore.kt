package com.mindos.app.features.mindboost.focus.data.local

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A minimal, namespace-scoped durable byte-store seam behind [IndexedSecureKeyValueStore]. ONE backend
 * instance per namespace (the namespace scopes the underlying store — e.g. the iOS Keychain `service`), so keys
 * are stored verbatim. Implementations do only the raw, platform-specific persistence of a `String` value; all
 * index / prefix / namespacing logic lives in [IndexedSecureKeyValueStore]. `CancellationException` may
 * propagate; other failures follow the per-method contract below.
 */
interface KeyValueBackend {
    /**
     * The stored value for [key], or null ONLY for a truly absent / non-decodable item. A GENUINE backend read
     * failure MUST throw (not return null), so a failed read never masquerades as "absent" — erasure and
     * consumers then fail-closed instead of silently skipping data. `CancellationException` may propagate.
     */
    suspend fun read(key: String): String?

    /**
     * Durably upserts [value] for [key]. MUST throw (e.g. [KeyValueBackendException]) — never return normally —
     * when the durable write did NOT happen, so the store above never indexes a key that was not persisted.
     */
    suspend fun write(key: String, value: String)

    /** Deletes [key] (an absent key is a no-op, not a failure). Throws only on a genuine backend failure. */
    suspend fun delete(key: String)
}

/**
 * Raised by a [KeyValueBackend] when a genuine durable write/delete fails, so [IndexedSecureKeyValueStore] never
 * indexes a key that was not persisted (and erasure surfaces a delete that did not happen). [code] is a platform
 * status code (e.g. an iOS `OSStatus`) for diagnostics only — never a key, value, or user identifier.
 */
class KeyValueBackendException(val code: Int) : IllegalStateException("keyvalue backend failure: $code")

/**
 * A [SecureKeyValueStore] that adds a per-namespace KEY INDEX on top of a flat [KeyValueBackend], so prefix
 * listing ([keys]) and [clearNamespace] work even when the backend has no native enumeration (e.g. iOS
 * Keychain). The backend is already namespace-scoped, so this store never mixes namespaces.
 *
 * The index (the set of live keys) is persisted under the reserved [INDEX_KEY]. Consistency guarantees:
 *  - **[put] is atomic**: it writes the value first; only if that persists does it index the key. If indexing
 *    then fails, the just-written value is rolled back (best-effort) and the failure is surfaced — a key is
 *    never left half-written, and a non-persisted key is never indexed.
 *  - **[remove]** deletes the value first (surfacing a genuine delete failure); a failed index cleanup only
 *    leaves a stale entry, which reads as [SecureReadResult.Missing].
 *  - **[clearNamespace]** is best-effort TOTAL for GDPR erasure: it attempts to delete every indexed item even
 *    if some fail, keeps the index when a data delete failed (so a retry can still find the survivors), and
 *    surfaces the first failure so the account-deletion coordinator retries (fail-closed).
 *  - **[keys]/[read]** tolerate stale/degenerate index CONTENT without crashing (a degenerate index → empty),
 *    but a GENUINE backend read failure PROPAGATES (fail-closed) rather than masquerading as empty/absent — so
 *    erasure blocks-and-retries instead of silently skipping data.
 *
 * Pure and fully unit-testable with a fake backend; the durable behaviour is the injected [backend]. NOTE:
 * [INDEX_KEY] is reserved — [put] rejects it, and all repo keys are prefix+id (e.g. `entry:…`, `profile:…`),
 * none equal to `__index__`.
 */
@Suppress("TooManyFunctions")
class IndexedSecureKeyValueStore(
    private val backend: KeyValueBackend,
) : SecureKeyValueStore {
    private val mutex = Mutex()

    override suspend fun read(key: String): SecureReadResult = mutex.withLock {
        backend.read(key)?.let(SecureReadResult::Value) ?: SecureReadResult.Missing
    }

    override suspend fun put(key: String, value: String) = mutex.withLock {
        require(key != INDEX_KEY) { "The reserved index key cannot be used to store data." }
        backend.write(key, value)
        try {
            addToIndex(key)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (@Suppress("TooGenericExceptionCaught") indexError: Throwable) {
            // The value persisted but indexing failed: roll back the orphan value (best-effort) so `put` is
            // atomic — either (value + index) or (neither) — then surface the failure (never a silent success).
            deleteCatching(key)
            throw indexError
        }
    }

    override suspend fun remove(key: String) = mutex.withLock {
        backend.delete(key)
        removeFromIndex(key)
    }

    override suspend fun quarantine(key: String, failure: SecureReadResult.Failure) {
        mutex.withLock {
            backend.delete(key)
            removeFromIndex(key)
        }
        reportSecureStorageQuarantine(failure)
    }

    override suspend fun keys(prefix: String): Set<String> = mutex.withLock {
        readIndex().filter { it.startsWith(prefix) }.toSet()
    }

    override suspend fun clearNamespace() = mutex.withLock {
        var firstFailure: Throwable? = null
        for (key in readIndex()) {
            val failure = deleteCatching(key)
            if (firstFailure == null) firstFailure = failure
        }
        // Drop the index ONLY if every data key was deleted; otherwise keep it so a retry finds the survivors.
        if (firstFailure == null) firstFailure = deleteCatching(INDEX_KEY)
        if (firstFailure != null) throw firstFailure
    }

    /** Deletes [key], returning the failure (if any) instead of throwing. `CancellationException` propagates. */
    private suspend fun deleteCatching(key: String): Throwable? =
        try {
            backend.delete(key)
            null
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (@Suppress("TooGenericExceptionCaught") error: Throwable) {
            error
        }

    private suspend fun readIndex(): Set<String> =
        backend.read(INDEX_KEY)?.split(INDEX_SEPARATOR)?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()

    private suspend fun writeIndex(keys: Set<String>) {
        if (keys.isEmpty()) backend.delete(INDEX_KEY) else backend.write(INDEX_KEY, keys.joinToString(INDEX_SEPARATOR))
    }

    private suspend fun addToIndex(key: String) {
        val current = readIndex()
        if (key !in current) writeIndex(current + key)
    }

    private suspend fun removeFromIndex(key: String) {
        val current = readIndex()
        if (key in current) writeIndex(current - key)
    }

    private companion object {
        /** Reserved key holding the newline-separated set of live keys. Callers must not use it as a data key. */
        const val INDEX_KEY = "__index__"

        /** Keys are prefix+id (no newlines), so a newline is a safe, collision-free index separator. */
        const val INDEX_SEPARATOR = "\n"
    }
}
