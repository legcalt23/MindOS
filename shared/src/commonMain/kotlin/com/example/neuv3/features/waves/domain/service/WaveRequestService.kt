package com.example.neuv3.features.waves.domain.service

import com.example.neuv3.features.waves.domain.model.WaveRelationshipStatus
import com.example.neuv3.features.waves.domain.model.WaveRequest
import com.example.neuv3.features.waves.domain.repository.WaveRequestRepository
import kotlinx.coroutines.flow.Flow

class WaveRequestService(
    private val repository: WaveRequestRepository,
) {
    fun observePendingRequestsReceived(userId: String): Flow<List<WaveRequest>> =
        repository.observePendingRequestsReceived(userId)

    fun observePendingRequestCountReceived(userId: String): Flow<Int> =
        repository.observePendingRequestCountReceived(userId)

    suspend fun sendRequest(fromUserId: String, toUserId: String): WaveSendRequestResult {
        if (!isValidPair(fromUserId, toUserId)) {
            return WaveSendRequestResult.InvalidUsers
        }

        return when (val status = repository.getRelationshipStatus(fromUserId, toUserId)) {
            is WaveRelationshipStatus.Accepted ->
                WaveSendRequestResult.AlreadyAccepted(status.request)
            is WaveRelationshipStatus.PendingIncoming ->
                WaveSendRequestResult.DuplicatePending(status.request)
            is WaveRelationshipStatus.PendingOutgoing ->
                WaveSendRequestResult.DuplicatePending(status.request)
            is WaveRelationshipStatus.Blocked ->
                WaveSendRequestResult.Blocked(status.request)
            is WaveRelationshipStatus.Cancelled,
            is WaveRelationshipStatus.Rejected,
            WaveRelationshipStatus.None ->
                WaveSendRequestResult.Created(repository.sendRequest(fromUserId, toUserId))
        }
    }

    suspend fun acceptRequest(requestId: String, actingUserId: String): WaveRequest =
        repository.acceptRequest(requestId, actingUserId)

    suspend fun rejectRequest(requestId: String, actingUserId: String): WaveRequest =
        repository.rejectRequest(requestId, actingUserId)

    suspend fun cancelRequest(requestId: String, actingUserId: String): WaveRequest =
        repository.cancelRequest(requestId, actingUserId)

    suspend fun canStartWaveChat(currentUserId: String, targetUserId: String): WaveChatAccessResult {
        if (!isValidPair(currentUserId, targetUserId)) {
            return WaveChatAccessResult.Denied(WaveRelationshipStatus.None)
        }

        val relationshipStatus = repository.getRelationshipStatus(currentUserId, targetUserId)
        return if (repository.canUsersChat(currentUserId, targetUserId)) {
            WaveChatAccessResult.Allowed
        } else {
            WaveChatAccessResult.Denied(relationshipStatus)
        }
    }

    suspend fun getRelationshipStatus(currentUserId: String, targetUserId: String): WaveRelationshipStatus =
        if (isValidPair(currentUserId, targetUserId)) {
            repository.getRelationshipStatus(currentUserId, targetUserId)
        } else {
            WaveRelationshipStatus.None
        }

    private fun isValidPair(currentUserId: String, targetUserId: String): Boolean =
        currentUserId.isNotBlank() &&
            targetUserId.isNotBlank() &&
            currentUserId != targetUserId
}

sealed interface WaveSendRequestResult {
    data class Created(val request: WaveRequest) : WaveSendRequestResult
    data class DuplicatePending(val request: WaveRequest) : WaveSendRequestResult
    data class AlreadyAccepted(val request: WaveRequest) : WaveSendRequestResult
    data class Blocked(val request: WaveRequest) : WaveSendRequestResult
    data object InvalidUsers : WaveSendRequestResult
}

sealed interface WaveChatAccessResult {
    data object Allowed : WaveChatAccessResult
    data class Denied(val relationshipStatus: WaveRelationshipStatus) : WaveChatAccessResult
}
