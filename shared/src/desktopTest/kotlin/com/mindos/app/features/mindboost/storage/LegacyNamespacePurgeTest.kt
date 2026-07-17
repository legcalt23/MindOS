package com.mindos.app.features.mindboost.storage

import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureReadResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest

/**
 * SYSTEM-1 slice 6-R3 (CEO closure contract, §18 C1) · [LegacyNamespacePurge] in isolation, against a bare
 * [SecureKeyValueStore] and a synthetic single-pattern [LegacyPurgeSpec]. The store-level integration (real
 * legacy shapes seeded before construction, the marker surviving a subject's own `deleteAll`) lives alongside
 * each store's own tests: `SecureSessionRecordStoreLegacyPurgeTest` and
 * `SecureCognitiveSessionRunStoreLegacyPurgeTest`.
 */
class LegacyNamespacePurgeTest {

    private val testSpec = LegacyPurgeSpec(
        markerKey = "test_marker:v1",
        markerVersion = 1,
        patterns = listOf(
            LegacyKeyPattern(scanPrefix = "thing:", regex = Regex("^thing:[^:]+$")),
        ),
    )

    @Test
    fun purgesOnlyKeysMatchingTheLegacyShape() = runTest {
        val store = InMemorySecureKeyValueStore()
        store.put("thing:legacy1", "a")
        store.put("thing:scoped:legacy2", "b") // two colons -> NOT legacy under this spec's anchored regex
        store.put("unrelated:key", "c")

        LegacyNamespacePurge(testSpec).ensurePurged(store)

        assertEquals(setOf("thing:scoped:legacy2", "unrelated:key", "test_marker:v1"), store.keys(""))
    }

    @Test
    fun writesTheMarkerOnlyAfterPurging() = runTest {
        val store = InMemorySecureKeyValueStore()
        store.put("thing:legacy1", "a")

        LegacyNamespacePurge(testSpec).ensurePurged(store)

        assertEquals(SecureReadResult.Value("1"), store.read("test_marker:v1"))
    }

    @Test
    fun aNamespaceWithNothingLegacy_stillGetsTheMarker_asAnIdempotentNoOp() = runTest {
        val store = InMemorySecureKeyValueStore()

        LegacyNamespacePurge(testSpec).ensurePurged(store)

        assertEquals(SecureReadResult.Value("1"), store.read("test_marker:v1"))
        assertEquals(setOf("test_marker:v1"), store.keys(""))
    }

    @Test
    fun aMidPurgeFailure_leavesTheMarkerAbsent_andARetryFinishesTheJob() = runTest {
        val failing = FailingRemoveOnceStore(InMemorySecureKeyValueStore())
        failing.put("thing:legacy1", "a")
        failing.put("thing:legacy2", "b")

        LegacyNamespacePurge(testSpec).ensurePurged(failing) // the first remove() throws mid-purge
        assertEquals(
            SecureReadResult.Missing,
            failing.read("test_marker:v1"),
            "a purge that failed partway through must never write the marker",
        )
        assertTrue(failing.keys("thing:").isNotEmpty(), "the un-removed legacy keys must still be there to retry")

        // A fresh instance simulates the next process/app-launch retrying after the crash.
        LegacyNamespacePurge(testSpec).ensurePurged(failing)

        assertEquals(SecureReadResult.Value("1"), failing.read("test_marker:v1"))
        assertTrue(failing.keys("thing:").isEmpty(), "the retry must finish removing every remaining legacy key")
    }

    @Test
    fun reRunOverAnAlreadyPurgedNamespace_neverReScans() = runTest {
        val store = CountingKeysSecureKeyValueStore(InMemorySecureKeyValueStore())
        store.put("thing:legacy1", "a")
        LegacyNamespacePurge(testSpec).ensurePurged(store) // first run: scans, purges, writes the marker
        store.keysCallCount = 0

        // A FRESH instance (e.g. a new sign-in constructing a new store wrapper) over the SAME backing store.
        LegacyNamespacePurge(testSpec).ensurePurged(store)

        assertEquals(0, store.keysCallCount, "a namespace already carrying the marker must never be re-scanned")
    }

    @Test
    fun sameInstance_secondCall_touchesTheBackingStoreNotAtAll() = runTest {
        val store = CountingKeysSecureKeyValueStore(InMemorySecureKeyValueStore())
        val purge = LegacyNamespacePurge(testSpec)
        purge.ensurePurged(store)
        store.keysCallCount = 0
        store.reads.clear()

        purge.ensurePurged(store) // SAME instance — the in-memory flag alone must short-circuit, no I/O at all

        assertEquals(0, store.keysCallCount)
        assertTrue(store.reads.isEmpty(), "the second call on the same instance must not even re-read the marker")
    }

    @Test
    fun concurrentCallers_onTheSameInstance_purgeExactlyOnce() = runTest {
        val store = CountingKeysSecureKeyValueStore(InMemorySecureKeyValueStore())
        store.put("thing:legacy1", "a")
        val purge = LegacyNamespacePurge(testSpec)

        val jobs = (1..20).map { async { purge.ensurePurged(store) } }
        jobs.awaitAll()

        assertEquals(1, store.keysCallCount, "20 concurrent callers must trigger exactly one legacy scan")
        assertEquals(SecureReadResult.Value("1"), store.read("test_marker:v1"))
        assertTrue(store.keys("thing:").isEmpty())
    }

    @Test
    fun aCorruptMarkerValue_isTreatedAsNotYetPurged_andSafelyReSweeps() = runTest {
        val store = InMemorySecureKeyValueStore()
        store.put("test_marker:v1", "not-a-number")
        store.put("thing:legacy1", "a")

        LegacyNamespacePurge(testSpec).ensurePurged(store)

        assertEquals(SecureReadResult.Value("1"), store.read("test_marker:v1"), "a garbled marker is overwritten")
        assertTrue(store.keys("thing:").isEmpty())
    }
}

/** Every FIRST `remove()` call throws once (simulating a crash mid-purge); every call after that succeeds. */
private class FailingRemoveOnceStore(
    private val delegate: SecureKeyValueStore,
) : SecureKeyValueStore {
    private var hasFailedOnce = false

    override suspend fun read(key: String): SecureReadResult = delegate.read(key)
    override suspend fun put(key: String, value: String) = delegate.put(key, value)

    override suspend fun remove(key: String) {
        if (!hasFailedOnce) {
            hasFailedOnce = true
            throw IllegalStateException("simulated crash mid-purge")
        }
        delegate.remove(key)
    }

    override suspend fun quarantine(key: String, failure: SecureReadResult.Failure) =
        delegate.quarantine(key, failure)

    override suspend fun keys(prefix: String): Set<String> = delegate.keys(prefix)
    override suspend fun clearNamespace() = delegate.clearNamespace()
}

/** Wraps a real KV store and counts `keys(prefix)` calls — proves a re-run does (or doesn't) re-scan. */
private class CountingKeysSecureKeyValueStore(
    private val delegate: SecureKeyValueStore,
) : SecureKeyValueStore {
    var keysCallCount = 0
    val reads = mutableListOf<String>()

    override suspend fun read(key: String): SecureReadResult {
        reads.add(key)
        return delegate.read(key)
    }

    override suspend fun put(key: String, value: String) = delegate.put(key, value)
    override suspend fun remove(key: String) = delegate.remove(key)

    override suspend fun quarantine(key: String, failure: SecureReadResult.Failure) =
        delegate.quarantine(key, failure)

    override suspend fun keys(prefix: String): Set<String> {
        keysCallCount++
        return delegate.keys(prefix)
    }

    override suspend fun clearNamespace() = delegate.clearNamespace()
}
