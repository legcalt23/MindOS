package com.example.mindos.features.waves.data

import com.example.mindos.core.time.SystemEpochClock
import com.example.mindos.features.waves.domain.model.WaveRelationshipStatus
import com.example.mindos.features.waves.domain.model.WaveRequest
import com.example.mindos.features.waves.domain.model.WaveRequestStatus
import com.example.mindos.features.waves.domain.repository.WaveRequestRepository
import com.example.mindos.features.waves.domain.repository.WaveRequestRepositoryError
import com.example.mindos.features.waves.domain.repository.WaveRequestRepositoryException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryWaveRequestRepository(
    initialRequests: List<WaveRequest> = emptyList(),
    private val idGenerator: WaveRequestIdGenerator = DefaultWaveRequestIdGenerator(),
    private val timeProvider: WaveTimeProvider = SystemWaveTimeProvider,
) : WaveRequestRepository {
    private val mutex = Mutex()
    private val requests = MutableStateFlow(initialRequests.sortedByDescending { it.updatedAt })

    override fun observePendingRequestsReceived(userId: String): Flow<List<WaveRequest>> =
        requests.map { current ->
            current
                .filter { it.toUserId == userId && it.status == WaveRequestStatus.Pending }
                .sortedByDescending { it.createdAt }
        }.distinctUntilChanged()

    override fun observePendingRequestCountReceived(userId: String): Flow<Int> =
        observePendingRequestsReceived(userId)
            .map { it.size }
            .distinctUntilChanged()

    override suspend fun sendRequest(fromUserId: String, toUserId: String): WaveRequest =
        mutex.withLock {
            validateUsers(fromUserId, toUserId)
            when (val relationship = resolveRelationshipStatus(fromUserId, toUserId, requests.value)) {
                is WaveRelationshipStatus.Accepted ->
                    throw WaveRequestRepositoryException(
                        WaveRequestRepositoryError.AlreadyAccepted(relationship.request.id)
                    )
                is WaveRelationshipStatus.PendingIncoming ->
                    throw WaveRequestRepositoryException(
                        WaveRequestRepositoryError.DuplicatePending(relationship.request.id)
                    )
                is WaveRelationshipStatus.PendingOutgoing ->
                    throw WaveRequestRepositoryException(
                        WaveRequestRepositoryError.DuplicatePending(relationship.request.id)
                    )
                is WaveRelationshipStatus.Blocked ->
                    throw WaveRequestRepositoryException(
                        WaveRequestRepositoryError.BlockedRelationship(relationship.request.id)
                    )
                is WaveRelationshipStatus.Cancelled,
                is WaveRelationshipStatus.Rejected,
                WaveRelationshipStatus.None -> {
                    val now = timeProvider.nowMillis()
                    val request = WaveRequest(
                        id = idGenerator.nextId(),
                        fromUserId = fromUserId,
                        toUserId = toUserId,
                        status = WaveRequestStatus.Pending,
                        createdAt = now,
                        updatedAt = now,
                    )
                    requests.value = (requests.value + request).sortedByDescending { it.updatedAt }
                    request
                }
            }
        }

    override suspend fun acceptRequest(requestId: String, actingUserId: String): WaveRequest =
        updatePendingRequest(requestId, actingUserId) { request ->
            if (request.toUserId != actingUserId) {
                throw WaveRequestRepositoryException(WaveRequestRepositoryError.UnauthorizedAction)
            }
            request.copy(status = WaveRequestStatus.Accepted, updatedAt = timeProvider.nowMillis())
        }

    override suspend fun rejectRequest(requestId: String, actingUserId: String): WaveRequest =
        updatePendingRequest(requestId, actingUserId) { request ->
            if (request.toUserId != actingUserId) {
                throw WaveRequestRepositoryException(WaveRequestRepositoryError.UnauthorizedAction)
            }
            request.copy(status = WaveRequestStatus.Rejected, updatedAt = timeProvider.nowMillis())
        }

    override suspend fun cancelRequest(requestId: String, actingUserId: String): WaveRequest =
        updatePendingRequest(requestId, actingUserId) { request ->
            if (request.fromUserId != actingUserId) {
                throw WaveRequestRepositoryException(WaveRequestRepositoryError.UnauthorizedAction)
            }
            request.copy(status = WaveRequestStatus.Cancelled, updatedAt = timeProvider.nowMillis())
        }

    override suspend fun canUsersChat(userIdA: String, userIdB: String): Boolean =
        when (getRelationshipStatus(userIdA, userIdB)) {
            is WaveRelationshipStatus.Accepted -> true
            else -> false
        }

    override suspend fun getRelationshipStatus(
        userIdA: String,
        userIdB: String,
    ): WaveRelationshipStatus = mutex.withLock {
        if (!isValidUserPair(userIdA, userIdB)) {
            WaveRelationshipStatus.None
        } else {
            resolveRelationshipStatus(userIdA, userIdB, requests.value)
        }
    }

    private suspend fun updatePendingRequest(
        requestId: String,
        actingUserId: String,
        transform: (WaveRequest) -> WaveRequest,
    ): WaveRequest = mutex.withLock {
        if (actingUserId.isBlank()) {
            throw WaveRequestRepositoryException(WaveRequestRepositoryError.InvalidUsers)
        }

        val current = requests.value
        val index = current.indexOfFirst { it.id == requestId }
        if (index < 0) {
            throw WaveRequestRepositoryException(WaveRequestRepositoryError.RequestNotFound)
        }

        val existing = current[index]
        if (existing.status != WaveRequestStatus.Pending) {
            throw WaveRequestRepositoryException(WaveRequestRepositoryError.InvalidState)
        }

        val updated = transform(existing)
        requests.value = current.toMutableList().apply { set(index, updated) }.sortedByDescending { it.updatedAt }
        updated
    }

    private fun resolveRelationshipStatus(
        userIdA: String,
        userIdB: String,
        allRequests: List<WaveRequest>,
    ): WaveRelationshipStatus {
        val pairRequests = allRequests.filter { it.matchesPair(userIdA, userIdB) }
        if (pairRequests.isEmpty()) {
            return WaveRelationshipStatus.None
        }

        pairRequests
            .filter { it.status == WaveRequestStatus.Blocked }
            .maxByOrNull { it.updatedAt }
            ?.let { return WaveRelationshipStatus.Blocked(it) }

        pairRequests
            .filter { it.status == WaveRequestStatus.Accepted }
            .maxByOrNull { it.updatedAt }
            ?.let { return WaveRelationshipStatus.Accepted(it) }

        pairRequests
            .filter { it.status == WaveRequestStatus.Pending }
            .maxByOrNull { it.updatedAt }
            ?.let { pending ->
                return if (pending.toUserId == userIdA) {
                    WaveRelationshipStatus.PendingIncoming(pending)
                } else {
                    WaveRelationshipStatus.PendingOutgoing(pending)
                }
            }

        pairRequests
            .filter { it.status == WaveRequestStatus.Rejected }
            .maxByOrNull { it.updatedAt }
            ?.let { return WaveRelationshipStatus.Rejected(it) }

        pairRequests
            .filter { it.status == WaveRequestStatus.Cancelled }
            .maxByOrNull { it.updatedAt }
            ?.let { return WaveRelationshipStatus.Cancelled(it) }

        return WaveRelationshipStatus.None
    }

    private fun validateUsers(fromUserId: String, toUserId: String) {
        if (!isValidUserPair(fromUserId, toUserId)) {
            throw WaveRequestRepositoryException(WaveRequestRepositoryError.InvalidUsers)
        }
    }

    private fun isValidUserPair(fromUserId: String, toUserId: String): Boolean =
        fromUserId.isNotBlank() &&
            toUserId.isNotBlank() &&
            fromUserId != toUserId
}

private fun WaveRequest.matchesPair(userIdA: String, userIdB: String): Boolean =
    (fromUserId == userIdA && toUserId == userIdB) ||
        (fromUserId == userIdB && toUserId == userIdA)

fun interface WaveRequestIdGenerator {
    fun nextId(): String
}

fun interface WaveTimeProvider {
    fun nowMillis(): Long
}

class DefaultWaveRequestIdGenerator : WaveRequestIdGenerator {
    private var next = 1L

    override fun nextId(): String = "wave_request_${next++}"
}

object SystemWaveTimeProvider : WaveTimeProvider {
    override fun nowMillis(): Long = SystemEpochClock.nowMillis()
}
