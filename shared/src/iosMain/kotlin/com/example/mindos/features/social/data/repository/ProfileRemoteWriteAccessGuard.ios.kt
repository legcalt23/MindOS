package com.example.mindos.features.social.data.repository

import com.example.mindos.features.social.domain.error.ProfileSaveAppCheckFailed
import com.example.mindos.features.social.domain.error.ProfileSaveException
import com.example.mindos.features.social.domain.valueobject.UserId

actual fun defaultProfileRemoteWriteAccessGuard(): ProfileRemoteWriteAccessGuard =
    IosProfileRemoteWriteBlockedGuard

private object IosProfileRemoteWriteBlockedGuard : ProfileRemoteWriteAccessGuard {
    override suspend fun assertCanWrite(
        attemptId: String,
        phase: String,
        operation: String,
        userId: UserId,
        path: String?,
    ) {
        throw ProfileSaveException(
            ProfileSaveAppCheckFailed(
                attemptId = attemptId,
                phase = phase,
                operation = operation,
                userId = userId,
                path = path,
                causeMessage = "iOS profile writes are disabled until Firebase App Check is wired for iOS.",
            ),
        )
    }
}
