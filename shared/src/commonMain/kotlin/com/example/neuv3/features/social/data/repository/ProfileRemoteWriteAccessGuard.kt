package com.example.neuv3.features.social.data.repository

import com.example.neuv3.features.social.domain.valueobject.UserId

interface ProfileRemoteWriteAccessGuard {
    suspend fun assertCanWrite(
        attemptId: String,
        phase: String,
        operation: String,
        userId: UserId,
        path: String?,
    )
}

internal object NoOpProfileRemoteWriteAccessGuard : ProfileRemoteWriteAccessGuard {
    override suspend fun assertCanWrite(
        attemptId: String,
        phase: String,
        operation: String,
        userId: UserId,
        path: String?,
    ) = Unit
}

expect fun defaultProfileRemoteWriteAccessGuard(): ProfileRemoteWriteAccessGuard
