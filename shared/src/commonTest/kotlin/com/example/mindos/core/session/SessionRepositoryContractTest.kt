package com.example.mindos.core.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionRepositoryContractTest {

    @Test
    fun previewRepository_initialState_isNull() {
        val repo = PreviewSessionRepository(isAuthenticated = false)
        assertNull(repo.currentUser())
    }

    @Test
    fun previewRepository_authenticatedState_hasUser() {
        val repo = PreviewSessionRepository(isAuthenticated = true)
        val user = repo.currentUser()
        assertEquals("preview_uid", user?.uid)
    }

    @Test
    fun previewRepository_sendPasswordResetEmail_doesNotThrow() {
        val repo = PreviewSessionRepository(isAuthenticated = false)
        // Should not throw — preview implementation is no-op.
        kotlinx.coroutines.test.runTest {
            repo.sendPasswordResetEmail("test@example.com")
        }
    }

    @Test
    fun previewRepository_signOut_clearsUser() {
        val repo = PreviewSessionRepository(isAuthenticated = true)
        assertEquals("preview_uid", repo.currentUser()?.uid)
        kotlinx.coroutines.test.runTest {
            repo.signOut()
        }
        assertNull(repo.currentUser())
    }

    @Test
    fun previewRepository_sendEmailVerification_doesNotThrow() {
        val repo = PreviewSessionRepository(isAuthenticated = true)
        kotlinx.coroutines.test.runTest {
            // Preview implementation is a no-op; it must satisfy the contract
            // without throwing so callers can wire a "resend" affordance safely.
            repo.sendEmailVerification()
        }
    }

    @Test
    fun previewRepository_reloadCurrentUser_returnsCurrentSnapshot() {
        val repo = PreviewSessionRepository(isAuthenticated = true)
        kotlinx.coroutines.test.runTest {
            val reloaded = repo.reloadCurrentUser()
            assertEquals("preview_uid", reloaded?.uid)
        }
    }

    @Test
    fun previewRepository_reloadCurrentUser_isNullWhenSignedOut() {
        val repo = PreviewSessionRepository(isAuthenticated = false)
        kotlinx.coroutines.test.runTest {
            assertNull(repo.reloadCurrentUser())
        }
    }
}
