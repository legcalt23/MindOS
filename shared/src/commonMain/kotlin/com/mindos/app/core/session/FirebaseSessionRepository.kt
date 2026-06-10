package com.mindos.app.core.session

import com.mindos.app.core.network.AuthDefaults
import com.mindos.app.core.network.withAuthTimeout
import com.mindos.app.core.network.withFunctionsWriteTimeout
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.functions.functions
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
            val result = withAuthTimeout { auth.signInWithEmailAndPassword(normalizedEmail, password) }
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

        val result = withAuthTimeout { auth.createUserWithEmailAndPassword(normalizedEmail, password) }
        val user = result.user ?: auth.currentUser
            ?: error("Firebase Auth creo la cuenta pero no devolvio usuario.")
        runCatching {
            withAuthTimeout(AuthDefaults.TokenRefreshTimeout) {
                user.updateProfile(displayName = normalizedDisplayName, photoUrl = user.photoURL)
                user.reload()
            }
        }
        // Fire the verification email automatically on sign-up. Wrapped in
        // runCatching so a transient email-send failure never aborts an otherwise
        // successful account creation — the user can resend later.
        runCatching {
            withAuthTimeout(AuthDefaults.PasswordResetTimeout) {
                (auth.currentUser ?: user).sendEmailVerification()
            }
            logAuth("event=email_verification_sent reason=signup")
        }
        return (auth.currentUser ?: user).toAuthenticatedUser(displayNameFallback = normalizedDisplayName)
    }

    override suspend fun signOut() {
        withAuthTimeout(AuthDefaults.SignOutTimeout) { auth.signOut() }
    }

    override suspend fun deleteAccount() {
        require(auth.currentUser != null) { "No hay un usuario autenticado para eliminar." }
        logAuth("event=account_delete_start")
        try {
            // Server purges PII + anonymizes the ledger + deletes the Auth user.
            withFunctionsWriteTimeout {
                Firebase.functions.httpsCallable("deleteAccount").invoke()
            }
            // The Auth user is now gone server-side; clear the local session so
            // authState emits null and the app routes back to Auth.
            runCatching {
                withAuthTimeout(AuthDefaults.SignOutTimeout) { auth.signOut() }
            }
            logAuth("event=account_delete_success")
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            logAuth("event=account_delete_failure exception=${error::class.simpleName}")
            throw error
        }
    }

    override suspend fun sendEmailVerification() {
        val user = auth.currentUser ?: error("No hay un usuario autenticado para verificar.")
        logAuth("event=email_verification_start")
        try {
            withAuthTimeout(AuthDefaults.PasswordResetTimeout) { user.sendEmailVerification() }
            logAuth("event=email_verification_sent reason=resend")
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            logAuth("event=email_verification_failure exception=${error::class.simpleName}")
            throw error
        }
    }

    override suspend fun reloadCurrentUser(): AuthenticatedUser? {
        val user = auth.currentUser ?: return null
        runCatching {
            withAuthTimeout(AuthDefaults.TokenRefreshTimeout) { user.reload() }
        }
        return (auth.currentUser ?: user).toAuthenticatedUser()
    }

    override suspend fun sendPasswordResetEmail(email: String) {
        val normalizedEmail = email.trim()
        require(normalizedEmail.isNotBlank()) { "El email es obligatorio." }
        logAuth("event=password_reset_start")
        try {
            withAuthTimeout(AuthDefaults.PasswordResetTimeout) {
                auth.sendPasswordResetEmail(normalizedEmail)
            }
            logAuth("event=password_reset_sent")
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            logAuth("event=password_reset_failure exception=${error::class.simpleName}")
            throw error
        }
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
