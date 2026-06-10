package com.mindos.app.features.mindboost.focus.data.local

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SecureKeyValueStoreTest {

    @Test
    fun put_get_roundTrips() = runTest {
        val store = InMemorySecureKeyValueStore()
        store.put("profile:user", "payload")
        assertEquals("payload", store.get("profile:user"))
    }

    @Test
    fun get_returnsNull_forMissingKey() = runTest {
        val store = InMemorySecureKeyValueStore()
        assertNull(store.get("missing"))
    }

    @Test
    fun remove_deletesEntry() = runTest {
        val store = InMemorySecureKeyValueStore()
        store.put("k", "v")
        store.remove("k")
        assertNull(store.get("k"))
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
}
