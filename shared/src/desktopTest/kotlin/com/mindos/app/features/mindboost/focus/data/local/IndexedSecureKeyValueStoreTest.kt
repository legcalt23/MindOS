package com.mindos.app.features.mindboost.focus.data.local

import com.mindos.app.features.mindboost.core.sync.SyncEntityType
import com.mindos.app.features.mindboost.core.sync.SyncQueueEntry
import com.mindos.app.features.mindboost.core.sync.SyncQueueStatus
import com.mindos.app.features.mindboost.focus.data.sync.SecureSyncQueueRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Contract suite for the pure durable-store core [IndexedSecureKeyValueStore]. Runs on the JVM with a fake
 * backend; the logic is platform-agnostic, so this validates exactly what the iOS Keychain store will use
 * (only [IosKeychainBackend] differs, verified via `:shared:compileKotlinIosSimulatorArm64` + simulator tests).
 */
class IndexedSecureKeyValueStoreTest {

    private fun store(backend: FakeKeyValueBackend = FakeKeyValueBackend()) = IndexedSecureKeyValueStore(backend)

    @Test
    fun put_thenRead_returnsValue() = runTest {
        val s = store()
        s.put("k", "v")
        assertEquals(SecureReadResult.Value("v"), s.read("k"))
    }

    @Test
    fun readMissing_returnsMissing() = runTest {
        assertEquals(SecureReadResult.Missing, store().read("nope"))
    }

    @Test
    fun put_overwritesSameKey() = runTest {
        val s = store()
        s.put("k", "v1")
        s.put("k", "v2")
        assertEquals(SecureReadResult.Value("v2"), s.read("k"))
        assertEquals(setOf("k"), s.keys(""))
    }

    @Test
    fun remove_deletesKey_andUpdatesIndex() = runTest {
        val s = store()
        s.put("k", "v")
        s.remove("k")
        assertEquals(SecureReadResult.Missing, s.read("k"))
        assertTrue(s.keys("").isEmpty(), "index no longer lists a removed key")
    }

    @Test
    fun keys_filtersByPrefix() = runTest {
        val s = store()
        s.put("entry:a", "1")
        s.put("entry:b", "2")
        s.put("profile:x", "3")
        assertEquals(setOf("entry:a", "entry:b"), s.keys("entry:"))
    }

    @Test
    fun clearNamespace_removesAllKeysAndIndex() = runTest {
        val backend = FakeKeyValueBackend()
        val s = store(backend)
        s.put("a", "1")
        s.put("b", "2")
        s.clearNamespace()
        assertTrue(s.keys("").isEmpty())
        assertEquals(SecureReadResult.Missing, s.read("a"))
        assertTrue(backend.entries.isEmpty(), "index item is also removed")
    }

    @Test
    fun clearNamespace_doesNotAffectOtherNamespace() = runTest {
        val one = store()
        val two = store()
        one.put("k", "1")
        two.put("k", "2")
        one.clearNamespace()
        assertEquals(SecureReadResult.Missing, one.read("k"))
        assertEquals(SecureReadResult.Value("2"), two.read("k"), "separate namespaces are isolated")
    }

    @Test
    fun quarantine_removesBadValue() = runTest {
        val s = store()
        s.put("k", "bad")
        s.quarantine("k", SecureReadResult.Corrupt)
        assertEquals(SecureReadResult.Missing, s.read("k"))
        assertFalse("k" in s.keys(""))
    }

    @Test
    fun valuesWithSpecialChars_roundTrip() = runTest {
        val s = store()
        val value = "a\"b\n\t{json:1}❤c"
        s.put("k", value)
        assertEquals(SecureReadResult.Value(value), s.read("k"))
    }

    @Test
    fun largeValue_roundTrip() = runTest {
        val s = store()
        val value = "x".repeat(50_000)
        s.put("k", value)
        assertEquals(SecureReadResult.Value(value), s.read("k"))
    }

    @Test
    fun staleIndexEntry_readsAsMissing_withoutCrashing() = runTest {
        val backend = FakeKeyValueBackend()
        val s = store(backend)
        s.put("real", "v")
        backend.entries["__index__"] = "real\nghost" // a key in the index with no backing item
        assertTrue("ghost" in s.keys(""))
        assertEquals(SecureReadResult.Missing, s.read("ghost"), "a stale index entry reads as Missing, never crashes")
    }

    @Test
    fun secureSyncQueueRepository_worksOverIndexedStore() = runTest {
        val queue = SecureSyncQueueRepository(store())
        assertTrue(queue.record(entry("user-1:1", userId = "user-1")))
        assertTrue(queue.record(entry("user-1:2", userId = "user-1")))
        assertEquals(2, queue.pending("user-1", limit = 10).size)
        assertEquals(SyncQueueStatus.PENDING, queue.find("user-1:1")?.status)
        assertEquals(2, queue.deleteByUser("user-1"), "erasure works over the durable-store design")
        assertTrue(queue.pending("user-1", limit = 10).isEmpty())
    }

    // --- Hardening: atomicity, rollback, best-effort erasure, and total/observable queue over a throwing backend.

    @Test
    fun put_whenBackendWriteFails_doesNotIndexKey_andThrows() = runTest {
        val backend = FakeKeyValueBackend(failWrite = { it == "k" })
        val s = IndexedSecureKeyValueStore(backend)
        assertFailsWith<KeyValueBackendException> { s.put("k", "v") }
        assertEquals(SecureReadResult.Missing, s.read("k"))
        assertTrue(s.keys("").isEmpty(), "a key whose value never persisted is never indexed")
        assertFalse("__index__" in backend.entries, "the index is not created for a failed write")
    }

    @Test
    fun put_whenIndexWriteFails_rollsBackValue_andThrows() = runTest {
        val backend = FakeKeyValueBackend(failWrite = { it == "__index__" })
        val s = IndexedSecureKeyValueStore(backend)
        assertFailsWith<KeyValueBackendException> { s.put("k", "v") }
        assertFalse("k" in backend.entries, "the value is rolled back when the index write fails (atomic put)")
    }

    @Test
    fun put_rejectsReservedIndexKey() = runTest {
        assertFailsWith<IllegalArgumentException> { store().put("__index__", "v") }
    }

    @Test
    fun remove_whenBackendDeleteFails_surfacesTheFailure() = runTest {
        val backend = FakeKeyValueBackend(failDelete = { it == "k" })
        backend.entries["k"] = "v"
        backend.entries["__index__"] = "k"
        val s = IndexedSecureKeyValueStore(backend)
        assertFailsWith<KeyValueBackendException> { s.remove("k") }
        assertTrue("k" in backend.entries, "a genuine delete failure is surfaced, not silently ignored")
    }

    @Test
    fun clearNamespace_deletesEveryKey_evenWhenOneFails_thenThrows() = runTest {
        val backend = FakeKeyValueBackend(failDelete = { it == "b" })
        backend.entries["a"] = "1"
        backend.entries["b"] = "2"
        backend.entries["c"] = "3"
        backend.entries["__index__"] = "a\nb\nc"
        val s = IndexedSecureKeyValueStore(backend)
        assertFailsWith<KeyValueBackendException> { s.clearNamespace() }
        assertFalse("a" in backend.entries, "best-effort erasure deletes a even though b fails")
        assertFalse("c" in backend.entries, "best-effort erasure deletes c even though b fails")
        assertTrue("b" in backend.entries, "b genuinely failed to delete and is surfaced via the throw")
        assertTrue("__index__" in backend.entries, "the index is kept so a retry can still find the survivor")
    }

    @Test
    fun clearNamespace_whenIndexReadFails_throws_notSilentSuccess() = runTest {
        // Regression (review P1): a GENUINE index read failure must fail-closed (throw), never silently skip the
        // data and report success — which would orphan user data in the Keychain while erasure "succeeds".
        val backend = FakeKeyValueBackend(failRead = { it == "__index__" })
        backend.entries["entry:a"] = "1"
        backend.entries["__index__"] = "entry:a"
        val s = IndexedSecureKeyValueStore(backend)
        assertFailsWith<KeyValueBackendException> { s.clearNamespace() }
        assertTrue("entry:a" in backend.entries, "user data is NOT silently orphaned on a failed index read")
    }

    @Test
    fun read_whenBackendReadGenuinelyFails_propagates() = runTest {
        // A real read failure must NOT masquerade as Missing (which could trigger a quarantine-delete of good data).
        val backend = FakeKeyValueBackend(failRead = { it == "k" })
        backend.entries["k"] = "v"
        assertFailsWith<KeyValueBackendException> { IndexedSecureKeyValueStore(backend).read("k") }
    }

    @Test
    fun secureSyncQueueRecord_returnsFalse_whenBackendWriteFails_keepingTheQueueTotal() = runTest {
        // The queue's total/observable contract must survive a hardened backend that THROWS on write:
        // record must report false (not crash, not fabricate a persisted row).
        val backend = FakeKeyValueBackend(failWrite = { it.startsWith("entry:") })
        val queue = SecureSyncQueueRepository(IndexedSecureKeyValueStore(backend))
        assertFalse(
            queue.record(entry("user-1:1", userId = "user-1")),
            "a failed durable write is reported as false, never a fabricated SYNCED row",
        )
        assertTrue(queue.pending("user-1", limit = 10).isEmpty(), "nothing was persisted")
    }

    private fun entry(sessionId: String, userId: String) = SyncQueueEntry(
        entryId = sessionId,
        userId = userId,
        sessionId = sessionId,
        entityType = SyncEntityType.FOCUS_SESSION,
        status = SyncQueueStatus.PENDING,
        attemptCount = 1,
        lastAttemptAtMs = 10L,
        lastErrorCode = null,
        fingerprint = "fp-$sessionId",
        createdAtMs = 10L,
        updatedAtMs = 10L,
    )
}

/**
 * In-memory [KeyValueBackend] mirroring the iOS Keychain seam (one instance = one namespace/service). The
 * optional [failWrite] / [failDelete] predicates inject a durable-op failure for specific keys — throwing
 * exactly like a hardened backend does — so the store's atomicity / rollback / best-effort-erasure paths are
 * exercised without a device.
 */
private class FakeKeyValueBackend(
    private val failWrite: (String) -> Boolean = { false },
    private val failDelete: (String) -> Boolean = { false },
    private val failRead: (String) -> Boolean = { false },
) : KeyValueBackend {
    val entries = linkedMapOf<String, String>()
    override suspend fun read(key: String): String? {
        if (failRead(key)) throw KeyValueBackendException(-3)
        return entries[key]
    }
    override suspend fun write(key: String, value: String) {
        if (failWrite(key)) throw KeyValueBackendException(-1)
        entries[key] = value
    }
    override suspend fun delete(key: String) {
        if (failDelete(key)) throw KeyValueBackendException(-2)
        entries.remove(key)
    }
}
