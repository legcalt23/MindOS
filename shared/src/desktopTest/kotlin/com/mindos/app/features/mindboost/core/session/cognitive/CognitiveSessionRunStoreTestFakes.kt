package com.mindos.app.features.mindboost.core.session.cognitive

import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureReadResult
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred

/** Wraps a real KV store and records every key read — lets a test prove a list only reads the keys it must. */
internal class CountingSecureKeyValueStore(
    private val delegate: SecureKeyValueStore = InMemorySecureKeyValueStore(),
) : SecureKeyValueStore {
    val reads = mutableListOf<String>()

    override suspend fun read(key: String): SecureReadResult {
        reads.add(key)
        return delegate.read(key)
    }

    override suspend fun put(key: String, value: String) = delegate.put(key, value)
    override suspend fun remove(key: String) = delegate.remove(key)
    override suspend fun quarantine(key: String, failure: SecureReadResult.Failure) = delegate.quarantine(key, failure)
    override suspend fun keys(prefix: String): Set<String> = delegate.keys(prefix)
    override suspend fun clearNamespace() = delegate.clearNamespace()
}

/**
 * Wraps a real KV store and fails every `put` whose key starts with [failPrefix] while [active]. Simulates a
 * partial write (e.g. primary succeeds, index put fails) or a backend that cannot write the primary at all.
 */
internal class PutFailingSecureKeyValueStore(
    private val failPrefix: String,
    private val delegate: SecureKeyValueStore = InMemorySecureKeyValueStore(),
) : SecureKeyValueStore {
    var active: Boolean = true

    override suspend fun put(key: String, value: String) {
        check(!(active && key.startsWith(failPrefix))) { "simulated write failure for '$failPrefix'" }
        delegate.put(key, value)
    }

    override suspend fun read(key: String): SecureReadResult = delegate.read(key)
    override suspend fun remove(key: String) = delegate.remove(key)
    override suspend fun quarantine(key: String, failure: SecureReadResult.Failure) = delegate.quarantine(key, failure)
    override suspend fun keys(prefix: String): Set<String> = delegate.keys(prefix)
    override suspend fun clearNamespace() = delegate.clearNamespace()
}

/**
 * Wraps a real KV store and makes `remove` FAIL for keys matching [failOn] while [active]; non-matching removes
 * pass through, so a test can prove `deleteAll` still attempts (and erases) every OTHER key after a mid-loop
 * fault. With [cancel] = true the matching remove throws [CancellationException] (structured cancellation) instead
 * of a backend fault, to prove deleteAll never swallows cancellation into a typed Failed.
 */
internal class RemoveFailingSecureKeyValueStore(
    private val failOn: (String) -> Boolean,
    private val cancel: Boolean = false,
    private val delegate: SecureKeyValueStore = InMemorySecureKeyValueStore(),
) : SecureKeyValueStore {
    override suspend fun remove(key: String) {
        if (failOn(key)) {
            if (cancel) throw CancellationException("simulated cancellation during remove of '$key'")
            error("simulated backend remove fault for '$key'")
        }
        delegate.remove(key)
    }

    override suspend fun read(key: String): SecureReadResult = delegate.read(key)
    override suspend fun put(key: String, value: String) = delegate.put(key, value)
    override suspend fun quarantine(key: String, failure: SecureReadResult.Failure) = delegate.quarantine(key, failure)
    override suspend fun keys(prefix: String): Set<String> = delegate.keys(prefix)
    override suspend fun clearNamespace() = delegate.clearNamespace()
}

/**
 * SYSTEM-1 slice 6-R3 (P2-01 triadic) · a STATE store whose `read` is driven by flags so a test can put the
 * revocation authority into ACTIVE (delegate Missing), REVOKED (a real tombstone in the delegate), or
 * STATUS_UNAVAILABLE ([throwOnRead]) INDEPENDENTLY of the payload store — the separation the triadic contract
 * needs (a fake that fails ALL reads could not tell a status-read fault from a payload-read fault).
 * [cancelOnRead] throws [CancellationException] to prove `statusOf` rethrows cancellation, never a false status.
 */
internal class ControllableReadStore(
    var throwOnRead: Boolean = false,
    var cancelOnRead: Boolean = false,
    var failureOnRead: Boolean = false,
    private val delegate: SecureKeyValueStore = InMemorySecureKeyValueStore(),
) : SecureKeyValueStore {
    override suspend fun read(key: String): SecureReadResult {
        if (cancelOnRead) throw CancellationException("simulated cancellation during revocation status read")
        if (throwOnRead) throw IllegalStateException("simulated revocation status read fault")
        // A NON-throwing SecureReadResult.Failure (corrupt tombstone) — distinct from a throw, exercises statusOf's
        // `is SecureReadResult.Failure -> StatusUnavailable(READ_FAILURE)` branch.
        if (failureOnRead) return SecureReadResult.Corrupt
        return delegate.read(key)
    }

    override suspend fun put(key: String, value: String) = delegate.put(key, value)
    override suspend fun remove(key: String) = delegate.remove(key)
    override suspend fun quarantine(key: String, failure: SecureReadResult.Failure) = delegate.quarantine(key, failure)
    override suspend fun keys(prefix: String): Set<String> = delegate.keys(prefix)
    override suspend fun clearNamespace() = delegate.clearNamespace()
}

/** Every `read` THROWS — simulates a transient backend/storage fault (distinct from a corrupt data row). */
internal class ReadThrowingSecureKeyValueStore(
    private val delegate: SecureKeyValueStore = InMemorySecureKeyValueStore(),
) : SecureKeyValueStore {
    override suspend fun read(key: String): SecureReadResult = throw IllegalStateException("simulated backend read fault")
    override suspend fun put(key: String, value: String) = delegate.put(key, value)
    override suspend fun remove(key: String) = delegate.remove(key)
    override suspend fun quarantine(key: String, failure: SecureReadResult.Failure) = delegate.quarantine(key, failure)
    override suspend fun keys(prefix: String): Set<String> = delegate.keys(prefix)
    override suspend fun clearNamespace() = delegate.clearNamespace()
}

/**
 * SYSTEM-1 slice 6-R3 (P2-01) · a `put()` whose key starts with [gateOnKeyPrefix] SUSPENDS mid-call until the
 * test releases it via [releasePut] — lets a test deterministically pause a write-in-progress (e.g. `save()`'s
 * primary write, still holding its caller's per-subject lock) and then dispatch a SECOND, concurrent operation
 * (e.g. `deleteAll()`) to prove it cannot interleave partway through the first. [awaitPutEntered] suspends until
 * the gated `put` has actually been reached, so the test never races the gate itself.
 */
internal class GatedPutSecureKeyValueStore(
    private val gateOnKeyPrefix: String,
    private val delegate: SecureKeyValueStore = InMemorySecureKeyValueStore(),
) : SecureKeyValueStore {
    private val entered = CompletableDeferred<Unit>()
    private val release = CompletableDeferred<Unit>()

    suspend fun awaitPutEntered() = entered.await()

    fun releasePut() {
        release.complete(Unit)
    }

    override suspend fun put(key: String, value: String) {
        if (key.startsWith(gateOnKeyPrefix)) {
            entered.complete(Unit)
            release.await()
        }
        delegate.put(key, value)
    }

    override suspend fun read(key: String): SecureReadResult = delegate.read(key)
    override suspend fun remove(key: String) = delegate.remove(key)
    override suspend fun quarantine(key: String, failure: SecureReadResult.Failure) = delegate.quarantine(key, failure)
    override suspend fun keys(prefix: String): Set<String> = delegate.keys(prefix)
    override suspend fun clearNamespace() = delegate.clearNamespace()
}

/**
 * SYSTEM-1 slice 6-R3 (R3-AUD-P1-02) · `remove(ghostKey)` returns NORMALLY (no throw) but never actually deletes
 * it from the delegate — simulates a backend whose delete call succeeds without durably persisting (index/backend
 * desync), so a post-delete physical re-scan (not just "no exception was thrown") is required to catch it.
 */
internal class GhostRemoveSecureKeyValueStore(
    private val ghostKey: String,
    private val delegate: SecureKeyValueStore = InMemorySecureKeyValueStore(),
) : SecureKeyValueStore {
    override suspend fun remove(key: String) {
        if (key != ghostKey) delegate.remove(key)
    }

    override suspend fun read(key: String): SecureReadResult = delegate.read(key)
    override suspend fun put(key: String, value: String) = delegate.put(key, value)
    override suspend fun quarantine(key: String, failure: SecureReadResult.Failure) = delegate.quarantine(key, failure)
    override suspend fun keys(prefix: String): Set<String> = delegate.keys(prefix)
    override suspend fun clearNamespace() = delegate.clearNamespace()
}
