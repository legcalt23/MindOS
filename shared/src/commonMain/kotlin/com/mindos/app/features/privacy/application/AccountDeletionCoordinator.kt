package com.mindos.app.features.privacy.application

import com.mindos.app.core.session.SessionRepository
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
 * INVARIANT — single user per device: this app authenticates exactly one account at a time
 * (Firebase Auth, no multi-account UI), so every account-bound namespace holds the data of ONLY
 * the currently-installed user. Under that invariant, [SecureKeyValueStore.clearNamespace] (a full
 * namespace wipe) is the correct AND complete erasure of the deleting user's data — there is no other
 * user's data in those namespaces to preserve. We therefore intentionally do NOT filter by [userId]:
 * the per-namespace key conventions differ (`span:<uid>`, `mbprofile:<uid>`, `run:<uid>::<sid>`, …),
 * and a single-user device has nothing to scope to.
 *
 * OUT OF SCOPE (documented decision, 2026-06-29): a device shared by two accounts that BOTH accumulated
 * local cognitive data — there this wipe would erase both. Supporting that requires per-user,
 * per-namespace deletion (each repo's own `deleteUserData(userId)`) and is deferred until the product
 * supports multiple local accounts. The [userId] is still validated (and the journal/recovery flow uses
 * it to match the pending deletion) so the erasure request is well-formed.
 */
class SecureNamespaceUserDataEraser(
    private val accountBoundStores: List<SecureKeyValueStore>,
) : UserDataEraser {
    override suspend fun eraseUserData(userId: String) {
        require(userId.isNotBlank()) { "User id cannot be blank." }
        // Single-user-per-device invariant (see class KDoc): the full namespace IS this user's data.
        accountBoundStores.forEach { store -> store.clearNamespace() }
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
