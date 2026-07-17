package com.mindos.app.features.privacy.application

import com.mindos.app.core.session.SessionRepository
import com.mindos.app.features.mindboost.core.session.SecureSessionRecordStore
import com.mindos.app.features.mindboost.core.session.SessionRecordDeleteResult
import com.mindos.app.features.mindboost.core.session.cognitive.DeleteResult
import com.mindos.app.features.mindboost.core.session.cognitive.SecureCognitiveSessionRunStore
import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureReadResult
import com.mindos.app.features.mindboost.focus.data.local.SecureStorageReadException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface UserDataEraser {
    suspend fun eraseUserData(userId: String)
}

object NoOpUserDataEraser : UserDataEraser {
    override suspend fun eraseUserData(userId: String) = Unit
}

/**
 * Erases all account-bound local cognitive data on account deletion.
 *
 * INVARIANT — single user per device (LEGACY for every namespace EXCEPT [subjectScopedRunsStore], see below):
 * this app was assumed to authenticate exactly one account at a time (Firebase Auth, no multi-account UI), so
 * every OTHER account-bound namespace was assumed to hold the data of ONLY the currently-installed user. Under
 * that invariant, [SecureKeyValueStore.clearNamespace] (a full namespace wipe) is the correct AND complete
 * erasure of the deleting user's data for those namespaces. In reality the invariant does not hold: sign-out
 * never purges local storage (`FirebaseSessionRepository.signOut()` only calls `auth.signOut()`), so a second
 * account signing in on the SAME device reuses the same physical namespaces, and this blanket
 * `clearNamespace()` would collaterally wipe THEIR data too on THIS account's deletion. We therefore intentionally
 * do NOT filter [accountBoundStores] by [userId]: the per-namespace key conventions differ (`span:<uid>`,
 * `mbprofile:<uid>`, `run:<uid>::<sid>`, …), and fully fixing every namespace requires each repo's own
 * `deleteUserData(userId)` (OUT OF SCOPE, documented decision 2026-06-29, deferred until the product supports
 * multiple local accounts explicitly).
 *
 * [subjectScopedRunsStore] and [subjectScopedSessionsStore] ARE THE EXCEPTIONS (SYSTEM-1 slice 6-R3: P0 fix
 * 6R3-P0C-01 for the runs store; the sibling P1 fold-in, §14, for the sessions store): unlike every other
 * namespace above, both `CognitiveSessionRunStore`'s and `SessionRecordStore`'s physical keys are subject-scoped
 * by a MANDATORY, first-class constructor parameter (see [SecureCognitiveSessionRunStore] and
 * [SecureSessionRecordStore]), not a per-repo convention. A blanket `clearNamespace()` on either would still
 * destroy every OTHER locally-authenticated account's rows sharing that one physical namespace file — so when
 * supplied, each is EXCLUDED from the blanket clear below (matched by reference against [accountBoundStores])
 * and instead erased via its own `deleteAll()`, which touches ONLY [userId]'s own subject-prefixed rows. The
 * deleting user's OWN rows are still fully erased — only OTHER accounts' rows are now preserved instead of
 * collaterally wiped. A `null` default preserves the exact legacy blanket-clear-everything behavior for any
 * caller (e.g. existing tests) that does not pass it.
 */
class SecureNamespaceUserDataEraser(
    private val accountBoundStores: List<SecureKeyValueStore>,
    private val subjectScopedRunsStore: SecureKeyValueStore? = null,
    private val subjectScopedSessionsStore: SecureKeyValueStore? = null,
    // SYSTEM-1 slice 6-R3 (P2-01): the dedicated `mindboost_revocation` backing store the two subject-scoped
    // stores share for their revocation tombstone. Required whenever a subject-scoped store is supplied; NEVER
    // itself blanket-cleared (it is NOT an accountBoundStores entry — the tombstone must survive deletion).
    private val revocationStore: SecureKeyValueStore? = null,
) : UserDataEraser {
    override suspend fun eraseUserData(userId: String) {
        require(userId.isNotBlank()) { "User id cannot be blank." }
        // Legacy single-user-per-device blanket clear for every namespace EXCEPT the subject-scoped ones (see
        // class KDoc). Referential exclusion: `subjectScopedRunsStore`/`subjectScopedSessionsStore`, when
        // supplied, ARE entries in `accountBoundStores` (both wired to the SAME store instances in AppGraph) —
        // they must be skipped here so they are erased ONLY via their subject-scoped path below, never
        // blanket-cleared.
        accountBoundStores
            .filterNot { store -> store === subjectScopedRunsStore || store === subjectScopedSessionsStore }
            .forEach { store -> store.clearNamespace() }
        subjectScopedRunsStore?.let { runsStore ->
            val revStore = requireNotNull(revocationStore) { "revocationStore is required for subject-scoped erasure." }
            when (val result = SecureCognitiveSessionRunStore(runsStore, revStore, subject = userId).deleteAll()) {
                is DeleteResult.Cleared -> Unit
                is DeleteResult.Failed ->
                    error("SessionRuns subject-scoped erasure failed: ${result.reason}")
            }
        }
        subjectScopedSessionsStore?.let { sessionsStore ->
            val revStore = requireNotNull(revocationStore) { "revocationStore is required for subject-scoped erasure." }
            when (val result = SecureSessionRecordStore(sessionsStore, revStore, subject = userId).deleteAll()) {
                is SessionRecordDeleteResult.Cleared -> Unit
                SessionRecordDeleteResult.Failed ->
                    error("Sessions subject-scoped erasure failed.")
            }
        }
    }
}

interface PendingAccountDeletionStore {
    suspend fun pendingUserId(): String?
    suspend fun markPending(userId: String)
    suspend fun clear(userId: String)
}

object NoOpPendingAccountDeletionStore : PendingAccountDeletionStore {
    override suspend fun pendingUserId(): String? = null
    override suspend fun markPending(userId: String) = Unit
    override suspend fun clear(userId: String) = Unit
}

class SecurePendingAccountDeletionStore(
    private val store: SecureKeyValueStore,
) : PendingAccountDeletionStore {
    override suspend fun pendingUserId(): String? = when (val result = store.read(PENDING_USER_KEY)) {
        is SecureReadResult.Value -> result.value
        SecureReadResult.Missing -> null
        is SecureReadResult.Failure -> {
            store.quarantine(PENDING_USER_KEY, result)
            throw SecureStorageReadException(result)
        }
    }

    override suspend fun markPending(userId: String) {
        require(userId.isNotBlank()) { "Pending deletion uid cannot be blank." }
        store.put(PENDING_USER_KEY, userId)
    }

    override suspend fun clear(userId: String) {
        if (pendingUserId() == userId) store.remove(PENDING_USER_KEY)
    }

    private companion object {
        const val PENDING_USER_KEY = "pending_account_deletion_uid"
    }
}

class InMemoryPendingAccountDeletionStore : PendingAccountDeletionStore {
    private val mutex = Mutex()
    private var pending: String? = null

    override suspend fun pendingUserId(): String? = mutex.withLock { pending }

    override suspend fun markPending(userId: String) {
        require(userId.isNotBlank()) { "Pending deletion uid cannot be blank." }
        mutex.withLock { pending = userId }
    }

    override suspend fun clear(userId: String) {
        mutex.withLock { if (pending == userId) pending = null }
    }
}

enum class AccountDeletionRecovery {
    NothingPending,
    Completed,
    AwaitingAuthentication,
    DeferredForDifferentUser,
    RetryPending,
    LocalEraseBlocked,
}

class AccountDeletionCoordinator(
    private val sessionRepository: SessionRepository,
    private val userDataEraser: UserDataEraser,
    private val pendingStore: PendingAccountDeletionStore,
) {
    private val mutex = Mutex()

    suspend fun deleteCurrentAccount() = mutex.withLock {
        val user = requireNotNull(sessionRepository.currentUser()) {
            "No authenticated account is available for deletion."
        }
        pendingStore.markPending(user.uid)
        sessionRepository.deleteAccount()
        userDataEraser.eraseUserData(user.uid)
        pendingStore.clear(user.uid)
    }

    @Suppress("SwallowedException")
    suspend fun recoverPendingDeletion(): AccountDeletionRecovery = mutex.withLock {
        val pendingUserId = pendingStore.pendingUserId() ?: return@withLock AccountDeletionRecovery.NothingPending
        val currentUser = sessionRepository.currentUser()
        if (currentUser != null && currentUser.uid != pendingUserId) {
            return@withLock AccountDeletionRecovery.DeferredForDifferentUser
        }

        try {
            userDataEraser.eraseUserData(pendingUserId)
        } catch (error: CancellationException) {
            throw error
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            return@withLock AccountDeletionRecovery.LocalEraseBlocked
        }

        if (currentUser == null) return@withLock AccountDeletionRecovery.AwaitingAuthentication

        try {
            sessionRepository.deleteAccount()
            pendingStore.clear(pendingUserId)
            AccountDeletionRecovery.Completed
        } catch (error: CancellationException) {
            throw error
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            AccountDeletionRecovery.RetryPending
        }
    }
}
