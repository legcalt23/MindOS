package com.mindos.app.core.session

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreviewSessionRepository(
    initialUser: AuthenticatedUser? = null,
) : SessionRepository {
    constructor(isAuthenticated: Boolean) : this(
        initialUser = if (isAuthenticated) {
            AuthenticatedUser(
                uid = "preview_uid",
                email = null,
                displayName = "Preview MindOS",
                isEmailVerified = false,
            )
        } else {
            null
        },
    )

    private val state = MutableStateFlow(initialUser)

    override val authState: Flow<AuthenticatedUser?> = state.asStateFlow()

    override fun currentUser(): AuthenticatedUser? =
        state.value

    override suspend fun signInWithEmailAndPassword(email: String, password: String): AuthenticatedUser =
        unsupportedPreviewAuth()

    override suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        displayName: String,
    ): AuthenticatedUser =
        unsupportedPreviewAuth()

    override suspend fun signOut() {
        state.value = null
    }

    override suspend fun sendPasswordResetEmail(email: String) {
        // No-op in preview.
    }

    override suspend fun sendEmailVerification() {
        // No-op in preview — there is no real Firebase user to email.
    }

    override suspend fun reloadCurrentUser(): AuthenticatedUser? = state.value

    override suspend fun deleteAccount() {
        // No real backend in preview — just clear the local session.
        state.value = null
    }

    private fun unsupportedPreviewAuth(): Nothing =
        error("Firebase Auth real no esta configurado para este entorno de preview.")
}
