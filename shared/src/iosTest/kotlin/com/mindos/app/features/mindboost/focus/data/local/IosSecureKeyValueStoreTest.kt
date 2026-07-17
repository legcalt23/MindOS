package com.mindos.app.features.mindboost.focus.data.local

import com.mindos.app.features.mindboost.core.sync.SyncEntityType
import com.mindos.app.features.mindboost.core.sync.SyncQueueEntry
import com.mindos.app.features.mindboost.core.sync.SyncQueueStatus
import com.mindos.app.features.mindboost.focus.data.sync.SecureSyncQueueRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * iOS behavioural suite for the REAL Keychain-backed store: [IndexedSecureKeyValueStore] over
 * [IosKeychainBackend]. Runs only on macOS via `:shared:iosSimulatorArm64Test`. This file lives in `iosTest`,
 * which is INERT on non-Apple hosts — it is not compiled, detekt-scanned, or copy-scanned on Windows/JVM, so it
 * can never break the desktop/Android gates.
 *
 * COMPILE-UNVERIFIED HERE: like [IosKeychainBackend], this file's Kotlin/Native + Security interop is not
 * compiled in the authoring environment; the owner runs the gate on macOS.
 *
 * Keychain caveat: a bare Kotlin/Native simulator test binary may lack a keychain-access-group entitlement and
 * return `errSecMissingEntitlement`. Each test therefore probes availability first ([keychainUsable]) and SKIPS
 * (prints, returns green) if the host cannot use the Keychain — the pure index logic is already fully covered by
 * the JVM `IndexedSecureKeyValueStoreTest`; this suite ADDS real-Keychain coverage wherever the host allows it.
 */
class IosSecureKeyValueStoreTest {

    private fun store(service: String) = IndexedSecureKeyValueStore(IosKeychainBackend(service))

    @Test
    fun put_read_roundTrip() = runTest {
        val s = store("mindboost_test_roundtrip")
        if (!keychainUsable(s)) return@runTest
        try {
            s.put("k", "v")
            assertEquals(SecureReadResult.Value("v"), s.read("k"))
        } finally {
            clearQuietly(s)
        }
    }

    @Test
    fun persistsAcrossInstances_sameService() = runTest {
        val service = "mindboost_test_persist"
        val first = store(service)
        if (!keychainUsable(first)) return@runTest
        try {
            first.put("k", "durable")
            val second = store(service) // a fresh store over the same Keychain service
            assertEquals(SecureReadResult.Value("durable"), second.read("k"))
        } finally {
            clearQuietly(store(service))
        }
    }

    @Test
    fun namespacesAreIsolated() = runTest {
        val a = store("mindboost_test_ns_a")
        val b = store("mindboost_test_ns_b")
        if (!keychainUsable(a)) return@runTest
        clearQuietly(b)
        try {
            a.put("k", "1")
            b.put("k", "2")
            assertEquals(SecureReadResult.Value("1"), a.read("k"))
            assertEquals(SecureReadResult.Value("2"), b.read("k"))
        } finally {
            clearQuietly(a)
            clearQuietly(b)
        }
    }

    @Test
    fun keys_filterByPrefix_andRemoveUpdatesIndex() = runTest {
        val s = store("mindboost_test_keys")
        if (!keychainUsable(s)) return@runTest
        try {
            s.put("entry:a", "1")
            s.put("entry:b", "2")
            s.put("profile:x", "3")
            assertEquals(setOf("entry:a", "entry:b"), s.keys("entry:"))
            s.remove("entry:a")
            assertEquals(setOf("entry:b"), s.keys("entry:"))
            assertEquals(SecureReadResult.Missing, s.read("entry:a"))
        } finally {
            clearQuietly(s)
        }
    }

    @Test
    fun overwrite_doesNotDuplicateIndex() = runTest {
        val s = store("mindboost_test_overwrite")
        if (!keychainUsable(s)) return@runTest
        try {
            s.put("k", "v1")
            s.put("k", "v2")
            assertEquals(SecureReadResult.Value("v2"), s.read("k"))
            assertEquals(setOf("k"), s.keys(""))
        } finally {
            clearQuietly(s)
        }
    }

    @Test
    fun clearNamespace_removesEverything() = runTest {
        val s = store("mindboost_test_clear")
        if (!keychainUsable(s)) return@runTest
        s.put("a", "1")
        s.put("b", "2")
        s.clearNamespace()
        assertTrue(s.keys("").isEmpty())
        assertEquals(SecureReadResult.Missing, s.read("a"))
    }

    @Test
    fun specialCharsAndQueueJson_roundTrip() = runTest {
        val s = store("mindboost_test_chars")
        if (!keychainUsable(s)) return@runTest
        try {
            val value = "a\"b\n\t{\"json\":1}❤c"
            s.put("k", value)
            assertEquals(SecureReadResult.Value(value), s.read("k"))
        } finally {
            clearQuietly(s)
        }
    }

    @Test
    fun syncQueueErasure_overRealKeychain() = runTest {
        val service = "mindboost_test_queue"
        val probe = store(service)
        if (!keychainUsable(probe)) return@runTest
        val queue = SecureSyncQueueRepository(store(service))
        try {
            assertTrue(queue.record(sampleEntry("user-1:1", "user-1")))
            assertTrue(queue.record(sampleEntry("user-1:2", "user-1")))
            assertEquals(2, queue.pending("user-1", limit = 10).size)
            assertEquals(2, queue.deleteByUser("user-1"))
            assertTrue(queue.pending("user-1", limit = 10).isEmpty())
        } finally {
            clearQuietly(store(service))
        }
    }

    /** Probes whether this host can actually use the Keychain; skips the behavioural assertions if not. */
    private suspend fun keychainUsable(store: SecureKeyValueStore): Boolean {
        val usable = try {
            store.put("probe_key", "1")
            store.read("probe_key") == SecureReadResult.Value("1")
        } catch (error: KeyValueBackendException) {
            false
        }
        if (!usable) {
            println("SKIP IosSecureKeyValueStoreTest: Keychain unusable in this test host (see class KDoc).")
        }
        clearQuietly(store)
        return usable
    }

    private suspend fun clearQuietly(store: SecureKeyValueStore) {
        try {
            store.clearNamespace()
        } catch (error: KeyValueBackendException) {
            // best-effort cleanup only
        }
    }

    private fun sampleEntry(sessionId: String, userId: String) = SyncQueueEntry(
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
