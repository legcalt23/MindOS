package com.mindos.app.features.mindboost.focus.data.local

import kotlinx.coroutines.test.runTest
import com.mindos.app.core.logging.NonFatalTelemetryEvent
import com.mindos.app.core.logging.ObservabilityHub
import com.mindos.app.core.logging.ObservabilitySink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SecureKeyValueStoreTest {

    @Test
    fun put_get_roundTrips() = runTest {
        val store = InMemorySecureKeyValueStore()
        store.put("profile:user", "payload")
        assertEquals(SecureReadResult.Value("payload"), store.read("profile:user"))
    }

    @Test
    fun get_returnsNull_forMissingKey() = runTest {
        val store = InMemorySecureKeyValueStore()
        assertEquals(SecureReadResult.Missing, store.read("missing"))
    }

    @Test
    fun remove_deletesEntry() = runTest {
        val store = InMemorySecureKeyValueStore()
        store.put("k", "v")
        store.remove("k")
        assertEquals(SecureReadResult.Missing, store.read("k"))
    }

    @Test
    fun keys_filtersByPrefix() = runTest {
        val store = InMemorySecureKeyValueStore()
        store.put("session:user:1", "a")
        store.put("session:user:2", "b")
        store.put("baseline:user", "c")
        assertEquals(setOf("session:user:1", "session:user:2"), store.keys("session:user:"))
    }

    @Test
    fun clearNamespace_removesEverything() = runTest {
        val store = InMemorySecureKeyValueStore()
        store.put("a", "1")
        store.put("b", "2")
        store.clearNamespace()
        assertTrue(store.keys("").isEmpty())
    }

    @Test
    fun corruptValue_isQuarantinedWithoutOverwrite() = runTest {
        val store = InMemorySecureKeyValueStore()
        store.put("profile:secret-user", "not-json")

        val decoded = store.readParsed("profile:secret-user") { error("invalid payload") }

        assertNull(decoded)
        assertEquals(SecureReadResult.Missing, store.read("profile:secret-user"))
        assertEquals("not-json", store.quarantinedValue("profile:secret-user"))
        assertEquals(SecureReadResult.Corrupt, store.quarantineFailure("profile:secret-user"))
    }

    @Test
    fun keyInvalidation_isTypedAndTelemetryDoesNotExposeLogicalKey() = runTest {
        val store = InMemorySecureKeyValueStore()
        val logicalKey = "profile:secret-user"
        store.put(logicalKey, "ciphertext")
        store.simulateReadFailure(logicalKey, SecureReadResult.KeyInvalidated)
        val sink = RecordingSink()
        val previousSink = ObservabilityHub.sink
        ObservabilityHub.sink = sink
        try {
            assertNull(store.readValueOrNull(logicalKey))
        } finally {
            ObservabilityHub.sink = previousSink
        }

        assertIs<SecureReadResult.KeyInvalidated>(store.quarantineFailure(logicalKey))
        assertEquals(listOf("secure_storage_quarantined:key_invalidated"), sink.breadcrumbs)
        assertTrue(sink.breadcrumbs.none { it.contains(logicalKey) })
    }

    private class RecordingSink : ObservabilitySink {
        val breadcrumbs = mutableListOf<String>()
        override fun breadcrumb(message: String) {
            breadcrumbs += message
        }
        override fun customKey(key: String, value: String) = Unit
        override fun customKey(key: String, value: Boolean) = Unit
        override fun customKey(key: String, value: Int) = Unit
        override fun recordNonFatal(event: NonFatalTelemetryEvent) = Unit
    }
}
