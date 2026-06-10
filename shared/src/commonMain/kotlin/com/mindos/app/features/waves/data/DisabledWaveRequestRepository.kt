package com.mindos.app.features.waves.data

import com.mindos.app.features.waves.domain.model.WaveRelationshipStatus
import com.mindos.app.features.waves.domain.model.WaveRequest
import com.mindos.app.features.waves.domain.repository.WaveRequestRepository
import com.mindos.app.features.waves.domain.repository.WaveRequestRepositoryError
import com.mindos.app.features.waves.domain.repository.WaveRequestRepositoryException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Explicit production-safe placeholder until Waves has a backend-authoritative
 * repository. It never grants new chat authorization from local process state.
 */
class DisabledWaveRequestRepository : WaveRequestRepository {
    override fun observePendingRequestsReceived(userId: String): Flow<List<WaveRequest>> =
        flowOf(emptyList())

    override fun observePendingRequestCountReceived(userId: String): Flow<Int> =
        flowOf(0)

    override suspend fun sendRequest(fromUserId: String, toUserId: String): WaveRequest =
        throw WaveRequestRepositoryException(WaveRequestRepositoryError.InvalidState)

    override suspend fun acceptRequest(requestId: String, actingUserId: String): WaveRequest =
        throw WaveRequestRepositoryException(WaveRequestRepositoryError.InvalidState)

    override suspend fun rejectRequest(requestId: String, actingUserId: String): WaveRequest =
        throw WaveRequestRepositoryException(WaveRequestRepositoryError.InvalidState)

    override suspend fun cancelRequest(requestId: String, actingUserId: String): WaveRequest =
        throw WaveRequestRepositoryException(WaveRequestRepositoryError.InvalidState)

    override suspend fun canUsersChat(userIdA: String, userIdB: String): Boolean =
        false

    override suspend fun getRelationshipStatus(userIdA: String, userIdB: String): WaveRelationshipStatus =
        WaveRelationshipStatus.None
}
