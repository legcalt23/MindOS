package com.mindos.app.features.privacy.application

import com.mindos.app.core.session.AuthenticatedUser
import com.mindos.app.core.session.PreviewSessionRepository
import com.mindos.app.core.session.SessionRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccountDeletionCoordinatorTest {

    @Test
    fun deleteCurrentAccount_erasesLocalDataAndClearsJournal() = runTest {
        val session = PreviewSessionRepository(isAuthenticated = true)
        val eraser = RecordingEraser()
        val journal = InMemoryPendingAccountDeletionStore()
        val coordinator = AccountDeletionCoordinator(session, eraser, journal)

        coordinator.deleteCurrentAccount()

        assertEquals(listOf("preview_uid"), eraser.erasedUserIds)
        assertNull(session.currentUser())
        assertNull(journal.pendingUserId())
    }

    @Test
    fun failedRemoteDeletion_keepsJournalAndDoesNotEraseLocalData() = runTest {
        val session = FailingDeletionSessionRepository(authenticatedUser("owner"))
        val eraser = RecordingEraser()
        val journal = InMemoryPendingAccountDeletionStore()
        val coordinator = AccountDeletionCoordinator(session, eraser, journal)

        runCatching { coordinator.deleteCurrentAccount() }

        assertEquals("owner", journal.pendingUserId())
        assertEquals(emptyList(), eraser.erasedUserIds)
    }

    @Test
    fun recoveryWithoutSession_erasesLocalDataAndWaitsForAuthentication() = runTest {
        val journal = InMemoryPendingAccountDeletionStore().also { it.markPending("owner") }
        val eraser = RecordingEraser()
        val coordinator = AccountDeletionCoordinator(
            PreviewSessionRepository(isAuthenticated = false),
            eraser,
            journal,
        )

        val recovery = coordinator.recoverPendingDeletion()

        assertEquals(AccountDeletionRecovery.AwaitingAuthentication, recovery)
        assertEquals(listOf("owner"), eraser.erasedUserIds)
        assertEquals("owner", journal.pendingUserId())
    }

    @Test
    fun recoveryWithMatchingSession_completesRemoteDeletionAndClearsJournal() = runTest {
        val user = authenticatedUser("owner")
        val session = PreviewSessionRepository(initialUser = user)
        val journal = InMemoryPendingAccountDeletionStore().also { it.markPending(user.uid) }
        val eraser = RecordingEraser()
        val coordinator = AccountDeletionCoordinator(session, eraser, journal)

        val recovery = coordinator.recoverPendingDeletion()

        assertEquals(AccountDeletionRecovery.Completed, recovery)
        assertEquals(listOf("owner"), eraser.erasedUserIds)
        assertNull(session.currentUser())
        assertNull(journal.pendingUserId())
    }

    @Test
    fun recoveryWithDifferentSession_doesNotTouchEitherAccount() = runTest {
        val session = PreviewSessionRepository(initialUser = authenticatedUser("other"))
        val journal = InMemoryPendingAccountDeletionStore().also { it.markPending("owner") }
        val eraser = RecordingEraser()
        val coordinator = AccountDeletionCoordinator(session, eraser, journal)

        val recovery = coordinator.recoverPendingDeletion()

        assertEquals(AccountDeletionRecovery.DeferredForDifferentUser, recovery)
        assertEquals(emptyList(), eraser.erasedUserIds)
        assertEquals("owner", journal.pendingUserId())
        assertEquals("other", session.currentUser()?.uid)
    }

    @Test
    fun recoveryKeepsJournalWhenRemoteRetryFails() = runTest {
        val user = authenticatedUser("owner")
        val session = FailingDeletionSessionRepository(user)
        val journal = InMemoryPendingAccountDeletionStore().also { it.markPending(user.uid) }
        val eraser = RecordingEraser()
        val coordinator = AccountDeletionCoordinator(session, eraser, journal)

        val recovery = coordinator.recoverPendingDeletion()

        assertEquals(AccountDeletionRecovery.RetryPending, recovery)
        assertEquals(listOf("owner"), eraser.erasedUserIds)
        assertEquals("owner", journal.pendingUserId())
    }

    private class RecordingEraser : UserDataEraser {
        val erasedUserIds = mutableListOf<String>()

        override suspend fun eraseUserData(userId: String) {
            erasedUserIds += userId
        }
    }

    private class FailingDeletionSessionRepository(
        initialUser: AuthenticatedUser,
    ) : SessionRepository by PreviewSessionRepository(initialUser) {
        override suspend fun deleteAccount() {
            error("remote deletion unavailable")
        }
    }

    private fun authenticatedUser(uid: String) = AuthenticatedUser(
        uid = uid,
        email = null,
        displayName = null,
        isEmailVerified = true,
    )
}
