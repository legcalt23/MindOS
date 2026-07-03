package com.mindos.app.features.privacy.application

import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Pins the SINGLE-USER-PER-DEVICE erasure contract of [SecureNamespaceUserDataEraser] (decision A,
 * 2026-06-29): account deletion wipes each account-bound namespace in full, which is correct because —
 * by the app's one-account-per-install invariant — every account-bound namespace holds exactly the
 * deleting user's data. A device shared by two data-bearing accounts is explicitly out of scope (see
 * the eraser KDoc). A blank user id is rejected and leaves storage untouched.
 */
class SecureNamespaceUserDataEraserTest {

    @Test
    fun eraseUserData_wipesEveryAccountBoundNamespace_underSingleUserInvariant() = runTest {
        val memoryStore = InMemorySecureKeyValueStore()
        val decisionsStore = InMemorySecureKeyValueStore()
        memoryStore.put("span:u1", "span-data")
        memoryStore.put("mbprofile:u1", "profile-data")
        decisionsStore.put("reversibility:u1", "cards-data")
        val eraser = SecureNamespaceUserDataEraser(listOf(memoryStore, decisionsStore))

        eraser.eraseUserData("u1")

        assertTrue(memoryStore.keys("").isEmpty(), "memory namespace must be fully erased")
        assertTrue(decisionsStore.keys("").isEmpty(), "decisions namespace must be fully erased")
    }

    @Test
    fun eraseUserData_rejectsBlankUserId_andLeavesStorageUntouched() = runTest {
        val store = InMemorySecureKeyValueStore()
        store.put("span:u1", "span-data")
        val eraser = SecureNamespaceUserDataEraser(listOf(store))

        assertFailsWith<IllegalArgumentException> { eraser.eraseUserData("   ") }

        assertEquals(setOf("span:u1"), store.keys(""), "a rejected request must not touch storage")
    }
}
