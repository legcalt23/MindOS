package com.mindos.app.core.session

import kotlinx.coroutines.flow.Flow

data class AuthenticatedUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val isEmailVerified: Boolean,
) {
    init {
        require(uid.isNotBlank()) { "Authenticated uid cannot be blank." }
    }
}

interface SessionRepository {
    val authState: Flow<AuthenticatedUser?>

    fun currentUser(): AuthenticatedUser?
    suspend fun signInWithEmailAndPassword(email: String, password: String): AuthenticatedUser
    suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        displayName: String,
    ): AuthenticatedUser

    suspend fun signOut()
    suspend fun sendPasswordResetEmail(email: String)

    /**
     * Sends (or re-sends) an email-verification message to the currently
     * authenticated user. Used both automatically right after sign-up and
     * manually from a "resend" affordance. Throws if no user is signed in.
     */
    suspend fun sendEmailVerification()

    /**
     * Reloads the current user from the backend and returns the refreshed
     * snapshot (e.g. to pick up `isEmailVerified` flipping to true after the
     * user clicks the link). Returns null if no user is signed in.
     */
    suspend fun reloadCurrentUser(): AuthenticatedUser?

    /**
     * Permanently deletes the current user's account (store-required, GDPR Art. 17).
     *
     * Server-authoritative: invokes the `deleteAccount` Cloud Function, which purges
     * the user's PII across Firestore/Storage and anonymizes the financial ledger
     * (retained for legal obligation — see docs/security/ACCOUNT_DELETION_DECISION.md),
     * then deletes the Firebase Auth user. After it returns, the local session is
     * cleared so `authState` emits null and the app routes back to Auth.
     *
     * The caller (UI) MUST gate this behind explicit double-confirmation and a recent
     * re-authentication. Irreversible.
     */
    suspend fun deleteAccount()
}
