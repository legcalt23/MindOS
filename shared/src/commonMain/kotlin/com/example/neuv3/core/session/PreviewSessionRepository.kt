package com.example.neuv3.core.session

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
                displayName = "Preview NEU",
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

    private fun unsupportedPreviewAuth(): Nothing =
        error("Firebase Auth real no esta configurado para este entorno de preview.")
}
