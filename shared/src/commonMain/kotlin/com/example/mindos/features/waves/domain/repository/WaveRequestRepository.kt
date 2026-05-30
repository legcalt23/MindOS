package com.example.mindos.features.waves.domain.repository

import com.example.mindos.features.waves.domain.model.WaveRelationshipStatus
import com.example.mindos.features.waves.domain.model.WaveRequest
import kotlinx.coroutines.flow.Flow

interface WaveRequestRepository {
    fun observePendingRequestsReceived(userId: String): Flow<List<WaveRequest>>

    fun observePendingRequestCountReceived(userId: String): Flow<Int>

    suspend fun sendRequest(fromUserId: String, toUserId: String): WaveRequest

    suspend fun acceptRequest(requestId: String, actingUserId: String): WaveRequest

    suspend fun rejectRequest(requestId: String, actingUserId: String): WaveRequest

    suspend fun cancelRequest(requestId: String, actingUserId: String): WaveRequest

    suspend fun canUsersChat(userIdA: String, userIdB: String): Boolean

    suspend fun getRelationshipStatus(userIdA: String, userIdB: String): WaveRelationshipStatus
}

sealed interface WaveRequestRepositoryError {
    data object InvalidUsers : WaveRequestRepositoryError
    data class DuplicatePending(val requestId: String) : WaveRequestRepositoryError
    data class AlreadyAccepted(val requestId: String) : WaveRequestRepositoryError
    data class BlockedRelationship(val requestId: String) : WaveRequestRepositoryError
    data object RequestNotFound : WaveRequestRepositoryError
    data object UnauthorizedAction : WaveRequestRepositoryError
    data object InvalidState : WaveRequestRepositoryError
}

class WaveRequestRepositoryException(
    val error: WaveRequestRepositoryError,
    cause: Throwable? = null,
) : IllegalStateException(error.toString(), cause)
