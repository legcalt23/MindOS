package com.mindos.app.features.mindboost.focus.data.local

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * S5 — caché de valor compartida a nivel de proceso sobre [SecureKeyValueStore]. El spy [CountingStore]
 * cuenta las lecturas del delegado (cada una = un descifrado en el store real), así una 2ª lectura sin
 * incremento demuestra que no se re-descifra. Cada test usa un namespace único para aislar el registro
 * global.
 */
class CachingSecureKeyValueStoreTest {

    private class CountingStore(
        private val backing: MutableMap<String, String> = mutableMapOf(),
        private val forcedFailure: SecureReadResult.Failure? = null,
    ) : SecureKeyValueStore {
        var reads = 0
            private set

        override suspend fun read(key: String): SecureReadResult {
            reads++
            forcedFailure?.let { return it }
            return backing[key]?.let(SecureReadResult::Value) ?: SecureReadResult.Missing
        }

        override suspend fun put(key: String, value: String) {
            backing[key] = value
        }

        override suspend fun remove(key: String) {
            backing.remove(key)
        }

        override suspend fun quarantine(key: String, failure: SecureReadResult.Failure) {
            backing.remove(key)
        }

        override suspend fun keys(prefix: String): Set<String> =
            backing.keys.filter { it.startsWith(prefix) }.toSet()

        override suspend fun clearNamespace() {
            backing.clear()
        }
    }

    @Test
    fun secondRead_doesNotHitDelegate() = runTest {
        val spy = CountingStore(mutableMapOf("k" to "v"))
        val store = CachingSecureKeyValueStore("test_second_read", spy)

        assertEquals(SecureReadResult.Value("v"), store.read("k"))
        assertEquals(SecureReadResult.Value("v"), store.read("k"))
        assertEquals(1, spy.reads)
    }

    @Test
    fun cacheHit_isSharedAcrossInstancesOfSameNamespace() = runTest {
        val spy = CountingStore(mutableMapOf("k" to "v"))
        val ns = "test_shared_hit"
        val a = CachingSecureKeyValueStore(ns, spy)
        val b = CachingSecureKeyValueStore(ns, spy)

        assertEquals(SecureReadResult.Value("v"), a.read("k"))
        assertEquals(SecureReadResult.Value("v"), b.read("k"))
        assertEquals(1, spy.reads) // b reuses a's cached plaintext
    }

    @Test
    fun put_invalidatesSharedCacheAcrossInstances() = runTest {
        val spy = CountingStore(mutableMapOf("k" to "v"))
        val ns = "test_put_invalidates"
        val a = CachingSecureKeyValueStore(ns, spy)
        val b = CachingSecureKeyValueStore(ns, spy)

        a.read("k")
        b.read("k")
        assertEquals(1, spy.reads)

        a.put("k", "v2")

        assertEquals(SecureReadResult.Value("v2"), b.read("k"))
        assertEquals(2, spy.reads) // b re-reads after shared invalidation
    }

    @Test
    fun remove_invalidatesSharedCacheAcrossInstances() = runTest {
        val spy = CountingStore(mutableMapOf("k" to "v"))
        val ns = "test_remove_invalidates"
        val a = CachingSecureKeyValueStore(ns, spy)
        val b = CachingSecureKeyValueStore(ns, spy)

        a.read("k")
        b.read("k")
        a.remove("k")

        assertEquals(SecureReadResult.Missing, b.read("k"))
        assertEquals(2, spy.reads)
    }

    @Test
    fun quarantine_invalidatesSharedCacheAcrossInstances() = runTest {
        val spy = CountingStore(mutableMapOf("k" to "v"))
        val ns = "test_quarantine_invalidates"
        val a = CachingSecureKeyValueStore(ns, spy)
        val b = CachingSecureKeyValueStore(ns, spy)

        a.read("k")
        b.read("k")
        a.quarantine("k", SecureReadResult.Corrupt)

        assertEquals(SecureReadResult.Missing, b.read("k"))
        assertEquals(2, spy.reads)
    }

    @Test
    fun clearNamespace_invalidatesSharedCacheAcrossInstances() = runTest {
        val spy = CountingStore(mutableMapOf("k1" to "a", "k2" to "b"))
        val ns = "test_clear_invalidates"
        val a = CachingSecureKeyValueStore(ns, spy)
        val b = CachingSecureKeyValueStore(ns, spy)

        a.read("k1")
        a.read("k2")
        b.read("k1")
        assertEquals(2, spy.reads)

        a.clearNamespace()

        assertEquals(SecureReadResult.Missing, b.read("k1"))
        assertEquals(3, spy.reads)
    }

    @Test
    fun failureResult_isNeverCached() = runTest {
        // A blob the underlying store reports as KeyInvalidated must be re-evaluated on every read (never
        // served from cache), so the caller's quarantine/gates always run — honesty: null, never a value.
        val spy = CountingStore(forcedFailure = SecureReadResult.KeyInvalidated)
        val store = CachingSecureKeyValueStore("test_failure_uncached", spy)

        assertEquals(SecureReadResult.KeyInvalidated, store.read("k"))
        assertEquals(SecureReadResult.KeyInvalidated, store.read("k"))
        assertEquals(2, spy.reads)
    }

    @Test
    fun differentNamespaces_doNotShareCache() = runTest {
        val spy = CountingStore(mutableMapOf("k" to "v"))
        val a = CachingSecureKeyValueStore("test_ns_a", spy)
        val b = CachingSecureKeyValueStore("test_ns_b", spy)

        a.read("k")
        b.read("k") // different namespace -> cache miss -> delegate hit

        assertEquals(2, spy.reads)
    }

    @Test
    fun evictAll_clearsCacheOnSignOut() = runTest {
        val spy = CountingStore(mutableMapOf("k" to "v"))
        val store = CachingSecureKeyValueStore("test_sign_out", spy)

        store.read("k")
        assertEquals(1, spy.reads)

        SecureStoreValueCache.evictAll()

        assertEquals(SecureReadResult.Value("v"), store.read("k"))
        assertEquals(2, spy.reads) // cache dropped -> re-read
    }

    @Test
    fun writeVersion_incrementsOnEveryMutationButNotOnRead() = runTest {
        val spy = CountingStore(mutableMapOf("k" to "v"))
        val ns = "test_write_version"
        val store = CachingSecureKeyValueStore(ns, spy)

        val v0 = SecureStoreValueCache.writeVersion(ns)
        store.read("k") // successful read must NOT advance the write-version
        assertEquals(v0, SecureStoreValueCache.writeVersion(ns))

        store.put("k", "v2")
        val vPut = SecureStoreValueCache.writeVersion(ns)
        assertTrue(vPut > v0, "put should advance write-version")

        store.remove("k")
        val vRemove = SecureStoreValueCache.writeVersion(ns)
        assertTrue(vRemove > vPut, "remove should advance write-version")

        store.quarantine("k", SecureReadResult.Corrupt)
        val vQuarantine = SecureStoreValueCache.writeVersion(ns)
        assertTrue(vQuarantine > vRemove, "quarantine should advance write-version")

        store.clearNamespace()
        assertTrue(
            SecureStoreValueCache.writeVersion(ns) > vQuarantine,
            "clearNamespace should advance write-version",
        )
    }

    @Test
    fun storeIfUnchanged_skipsWhenNamespaceVersionAdvanced() = runTest {
        // Guards the read-through race: a concurrent write (version bump) between capturing the version and
        // caching the plaintext must discard the now-superseded value.
        val ns = "test_race_guard"
        val v0 = SecureStoreValueCache.writeVersion(ns)
        SecureStoreValueCache.invalidate(ns, "k") // simulates the concurrent write
        SecureStoreValueCache.storeIfUnchanged(ns, "k", "stale", v0)
        assertNull(SecureStoreValueCache.cached(ns, "k"))

        val v1 = SecureStoreValueCache.writeVersion(ns)
        SecureStoreValueCache.storeIfUnchanged(ns, "k", "fresh", v1)
        assertEquals("fresh", SecureStoreValueCache.cached(ns, "k"))
    }
}
