package com.mindos.app.features.mindboost.focus.data.local

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Decorator that caches decrypted values in front of any [SecureKeyValueStore] (S5 · D1/D3).
 *
 * The cache is PROCESS-WIDE and shared by [SecureStoreValueCache], keyed by [namespace]: the same
 * physical namespace is opened at many composition call-sites (the Sessions namespace alone at 10+),
 * each constructing its own platform store over the SAME storage. A per-instance cache would never hit,
 * so a write through ANY instance must evict the plaintext seen by ALL instances of that namespace.
 *
 * Honesty (charter): the cache holds the SAME plaintext the underlying store yields; callers still run
 * their `readVersioned`/parse gates on top, so a quarantined or unsupported-version blob still fails
 * closed (null, never a fabricated value). Only successful [SecureReadResult.Value] reads are cached —
 * Missing/Failure results are never cached, so every gate re-runs.
 */
internal class CachingSecureKeyValueStore(
    private val namespace: String,
    private val delegate: SecureKeyValueStore,
) : SecureKeyValueStore {

    override suspend fun read(key: String): SecureReadResult {
        SecureStoreValueCache.cached(namespace, key)?.let { return SecureReadResult.Value(it) }
        // Capture the write-version BEFORE the underlying read; only cache the plaintext if no write
        // (put/remove/quarantine/clearNamespace) landed in between, so a superseded value is never cached.
        val version = SecureStoreValueCache.writeVersion(namespace)
        val result = delegate.read(key)
        if (result is SecureReadResult.Value) {
            SecureStoreValueCache.storeIfUnchanged(namespace, key, result.value, version)
        }
        return result
    }

    override suspend fun put(key: String, value: String) {
        delegate.put(key, value)
        SecureStoreValueCache.invalidate(namespace, key)
    }

    override suspend fun remove(key: String) {
        delegate.remove(key)
        SecureStoreValueCache.invalidate(namespace, key)
    }

    override suspend fun quarantine(key: String, failure: SecureReadResult.Failure) {
        delegate.quarantine(key, failure)
        SecureStoreValueCache.invalidate(namespace, key)
    }

    override suspend fun keys(prefix: String): Set<String> = delegate.keys(prefix)

    override suspend fun clearNamespace() {
        delegate.clearNamespace()
        SecureStoreValueCache.invalidateAll(namespace)
    }
}

/**
 * Process-wide registry of per-namespace decrypted-value caches (see [CachingSecureKeyValueStore]).
 *
 * Also tracks a monotonic write-version per namespace, advanced on every put/remove/quarantine/
 * clearNamespace — exposed via [writeVersion] for the brain-profile dirty-check (S15.D2): a consumer can
 * skip a re-read/recompute unless the version advanced.
 */
internal object SecureStoreValueCache {
    private val registryLock = Mutex()
    private val caches = mutableMapOf<String, NamespaceCache>()

    private suspend fun cacheFor(namespace: String): NamespaceCache =
        registryLock.withLock { caches.getOrPut(namespace) { NamespaceCache() } }

    suspend fun cached(namespace: String, key: String): String? = cacheFor(namespace).get(key)

    suspend fun storeIfUnchanged(namespace: String, key: String, value: String, sinceVersion: Long) {
        cacheFor(namespace).storeIfUnchanged(key, value, sinceVersion)
    }

    suspend fun invalidate(namespace: String, key: String) {
        cacheFor(namespace).invalidate(key)
    }

    suspend fun invalidateAll(namespace: String) {
        cacheFor(namespace).invalidateAll()
    }

    /** Monotonic write-version for [namespace], advanced on every mutating op. For S15.D2 dirty-check. */
    suspend fun writeVersion(namespace: String): Long = cacheFor(namespace).version()

    /** Sign-out: drop every cached plaintext so nothing survives a user switch. Does not bump write-versions. */
    suspend fun evictAll() {
        val snapshot = registryLock.withLock { caches.values.toList() }
        snapshot.forEach { it.clear() }
    }

    private class NamespaceCache {
        private val lock = Mutex()
        private val values = mutableMapOf<String, String>()
        private var version = 0L

        suspend fun get(key: String): String? = lock.withLock { values[key] }

        suspend fun version(): Long = lock.withLock { version }

        suspend fun storeIfUnchanged(key: String, value: String, sinceVersion: Long) {
            lock.withLock { if (version == sinceVersion) values[key] = value }
        }

        suspend fun invalidate(key: String) {
            lock.withLock {
                values.remove(key)
                version++
            }
        }

        suspend fun invalidateAll() {
            lock.withLock {
                values.clear()
                version++
            }
        }

        suspend fun clear() {
            lock.withLock { values.clear() }
        }
    }
}
