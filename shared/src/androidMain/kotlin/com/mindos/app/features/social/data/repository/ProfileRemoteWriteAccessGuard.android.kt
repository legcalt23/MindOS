package com.mindos.app.features.social.data.repository

import com.mindos.app.core.network.AuthDefaults
import com.mindos.app.core.network.withAuthTimeout
import com.mindos.app.features.social.domain.error.ProfileSaveAppCheckFailed
import com.mindos.app.features.social.domain.error.ProfileSaveException
import com.mindos.app.features.social.domain.valueobject.UserId
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.tasks.await

actual fun defaultProfileRemoteWriteAccessGuard(): ProfileRemoteWriteAccessGuard =
    FirebaseAppCheckProfileRemoteWriteAccessGuard()

private class FirebaseAppCheckProfileRemoteWriteAccessGuard : ProfileRemoteWriteAccessGuard {
    private val validatedAttemptIds = mutableSetOf<String>()

    override suspend fun assertCanWrite(
        attemptId: String,
        phase: String,
        operation: String,
        userId: UserId,
        path: String?,
    ) {
        if (attemptId.isNotBlank()) {
            synchronized(validatedAttemptIds) {
                if (attemptId in validatedAttemptIds) return
            }
        }

        val tokenResult = runCatching {
            withAuthTimeout(AuthDefaults.TokenRefreshTimeout) {
                FirebaseAppCheck.getInstance().getAppCheckToken(false).await()
            }
        }.getOrElse { error ->
            throw ProfileSaveException(
                ProfileSaveAppCheckFailed(
                    attemptId = attemptId,
                    phase = phase,
                    operation = operation,
                    userId = userId,
                    path = path,
                    causeMessage = "App Check no pudo emitir un token antes de $operation. ${error.message.orEmpty()}",
                ),
                error,
            )
        }

        if (tokenResult.token.trim().isBlank()) {
            throw ProfileSaveException(
                ProfileSaveAppCheckFailed(
                    attemptId = attemptId,
                    phase = phase,
                    operation = operation,
                    userId = userId,
                    path = path,
                    causeMessage = "App Check devolvio un token vacio antes de $operation.",
                ),
            )
        }

        if (attemptId.isNotBlank()) {
            synchronized(validatedAttemptIds) {
                validatedAttemptIds += attemptId
            }
        }
    }
}
