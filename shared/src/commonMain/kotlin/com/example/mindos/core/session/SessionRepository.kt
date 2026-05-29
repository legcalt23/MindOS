package com.example.mindos.core.session

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
}
