package com.example.neuv3.core.session

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirebaseSessionRepository(
    private val auth: FirebaseAuth = Firebase.auth,
) : SessionRepository {
    override val authState: Flow<AuthenticatedUser?> =
        auth.authStateChanged.map { user -> user?.toAuthenticatedUser() }

    override fun currentUser(): AuthenticatedUser? =
        auth.currentUser?.toAuthenticatedUser()

    override suspend fun signInWithEmailAndPassword(
        email: String,
        password: String,
    ): AuthenticatedUser {
        val normalizedEmail = email.trim()
        val passwordLog = passwordPresenceForLog(password)
        logAuth(
            "event=login_start method=email_password $passwordLog"
        )

        require(normalizedEmail.isNotBlank()) { "El email es obligatorio." }
        require(password.isNotBlank()) { "La contrasena es obligatoria." }

        try {
            val result = auth.signInWithEmailAndPassword(normalizedEmail, password)
            val user = result.user?.toAuthenticatedUser()
                ?: auth.currentUser?.toAuthenticatedUser()
                ?: error("Firebase Auth no devolvio un usuario autenticado.")
            logAuth(
                "event=login_success method=email_password authenticated=true"
            )
            return user
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            val diagnostic = error.toAuthFailureDiagnostic()
            logAuth(
                "event=login_failure method=email_password $passwordLog " +
                    "exception=${diagnostic.exceptionClass} firebase_code=${diagnostic.code} " +
                    "kind=${diagnostic.kind.logValue}"
            )
            throw error
        }
    }

    override suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        displayName: String,
    ): AuthenticatedUser {
        val normalizedEmail = email.trim()
        val normalizedDisplayName = displayName.trim()
        require(normalizedEmail.isNotBlank()) { "El email es obligatorio." }
        require(password.length >= 8) { "La contrasena debe tener al menos 8 caracteres." }
        require(normalizedDisplayName.isNotBlank()) { "El nombre visible es obligatorio." }

        val result = auth.createUserWithEmailAndPassword(normalizedEmail, password)
        val user = result.user ?: auth.currentUser
            ?: error("Firebase Auth creo la cuenta pero no devolvio usuario.")
        runCatching {
            user.updateProfile(displayName = normalizedDisplayName, photoUrl = user.photoURL)
            user.reload()
        }
        return (auth.currentUser ?: user).toAuthenticatedUser(displayNameFallback = normalizedDisplayName)
    }

    override suspend fun signOut() {
        auth.signOut()
    }
}

private fun FirebaseUser.toAuthenticatedUser(
    displayNameFallback: String? = null,
): AuthenticatedUser =
    AuthenticatedUser(
        uid = uid,
        email = email,
        displayName = displayName?.takeIf { it.isNotBlank() } ?: displayNameFallback?.takeIf { it.isNotBlank() },
        isEmailVerified = isEmailVerified,
    )
