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
}
