package com.example.neuv3.features.social.data.repository

import com.example.neuv3.features.social.data.local.InMemorySocialGraphDatabase
import com.example.neuv3.features.social.domain.model.AuditEvent
import com.example.neuv3.features.social.domain.model.BlockRelation
import com.example.neuv3.features.social.domain.model.FollowRelation
import com.example.neuv3.features.social.domain.model.FollowRequest
import com.example.neuv3.features.social.domain.model.FollowRequestStatus
import com.example.neuv3.features.social.domain.model.PrivacySettings
import com.example.neuv3.features.social.domain.model.ProfileCounters
import com.example.neuv3.features.social.domain.model.UserAccount
import com.example.neuv3.features.social.domain.model.UserProfile
import com.example.neuv3.features.social.domain.repository.AuditEventRepository
import com.example.neuv3.features.social.domain.repository.BlockRepository
import com.example.neuv3.features.social.domain.repository.FollowRepository
import com.example.neuv3.features.social.domain.repository.FollowRequestRepository
import com.example.neuv3.features.social.domain.repository.PrivacySettingsRepository
import com.example.neuv3.features.social.domain.repository.ProfileCounterRepository
import com.example.neuv3.features.social.domain.repository.UserProfileRepository
import com.example.neuv3.features.social.domain.valueobject.Cursor
import com.example.neuv3.features.social.domain.valueobject.Handle
import com.example.neuv3.features.social.domain.valueobject.RelationshipPair
import com.example.neuv3.features.social.domain.valueobject.RequestId
import com.example.neuv3.features.social.domain.valueobject.Timestamp
import com.example.neuv3.features.social.domain.valueobject.UserId
import com.example.neuv3.features.social.shared.util.Page
import com.example.neuv3.features.social.shared.util.PageRequest

class InMemoryUserProfileRepository(
    private val database: InMemorySocialGraphDatabase,
) : UserProfileRepository {
    override suspend fun getAccount(userId: UserId): UserAccount? =
        database.state.accountsById[userId]

    override suspend fun saveAccount(account: UserAccount) {
        database.state.accountsById[account.id] = account
    }

    override suspend fun getProfile(userId: UserId): UserProfile? =
        database.state.profilesByUserId[userId]

    override suspend fun findProfileByHandle(handle: Handle): UserProfile? {
        val userId = database.state.profileUserIdByHandle[handle.value] ?: return null
        return database.state.profilesByUserId[userId]
    }

    override suspend fun saveProfile(profile: UserProfile) {
        database.state.profilesByUserId[profile.userId]?.let { old ->
            database.state.profileUserIdByHandle.remove(old.handle.value)
        }
        database.state.profilesByUserId[profile.userId] = profile
        database.state.profileUserIdByHandle[profile.handle.value] = profile.userId
    }
}

class InMemoryPrivacySettingsRepository(
    private val database: InMemorySocialGraphDatabase,
) : PrivacySettingsRepository {
    override suspend fun get(userId: UserId): PrivacySettings? =
        database.state.privacyByUserId[userId]

    override suspend fun save(settings: PrivacySettings) {
        database.state.privacyByUserId[settings.userId] = settings
    }
}

class InMemoryFollowRepository(
    private val database: InMemorySocialGraphDatabase,
) : FollowRepository {
    override suspend fun get(followerId: UserId, followeeId: UserId): FollowRelation? =
        if (followerId == followeeId) {
            null
        } else {
            database.state.followsByPair[RelationshipPair(followerId, followeeId)]
        }

    override suspend fun exists(followerId: UserId, followeeId: UserId): Boolean =
        get(followerId, followeeId) != null

    override suspend fun insert(relation: FollowRelation): Boolean {
        val existing = database.state.followsByPair.putIfAbsent(relation.pair, relation)
        return existing == null
    }

    override suspend fun delete(followerId: UserId, followeeId: UserId): Boolean =
        if (followerId == followeeId) {
            false
        } else {
            database.state.followsByPair.remove(RelationshipPair(followerId, followeeId)) != null
        }

    override suspend fun deleteBothDirections(firstUserId: UserId, secondUserId: UserId): Int {
        if (firstUserId == secondUserId) return 0
        var deleted = 0
        if (delete(firstUserId, secondUserId)) deleted++
        if (delete(secondUserId, firstUserId)) deleted++
        return deleted
    }

    override suspend fun listFollowers(userId: UserId, pageRequest: PageRequest): Page<FollowRelation> =
        database.state.followsByPair.values
            .filter { it.followeeId == userId }
            .sortedWith(compareBy<FollowRelation> { it.createdAt.epochMillis }.thenBy { it.followerId.value })
            .page(pageRequest)

    override suspend fun listFollowing(userId: UserId, pageRequest: PageRequest): Page<FollowRelation> =
        database.state.followsByPair.values
            .filter { it.followerId == userId }
            .sortedWith(compareBy<FollowRelation> { it.createdAt.epochMillis }.thenBy { it.followeeId.value })
            .page(pageRequest)

    override suspend fun countFollowers(userId: UserId): Int =
        database.state.followsByPair.values.count { it.followeeId == userId }

    override suspend fun countFollowing(userId: UserId): Int =
        database.state.followsByPair.values.count { it.followerId == userId }
}

class InMemoryFollowRequestRepository(
    private val database: InMemorySocialGraphDatabase,
) : FollowRequestRepository {
    override suspend fun get(requestId: RequestId): FollowRequest? =
        database.state.requestsById[requestId]

    override suspend fun findPending(requesterId: UserId, targetId: UserId): FollowRequest? =
        if (requesterId == targetId) {
            null
        } else {
            database.state.pendingRequestIdByPair[RelationshipPair(requesterId, targetId)]
                ?.let { database.state.requestsById[it] }
        }

    override suspend fun insert(request: FollowRequest): Boolean {
        if (database.state.requestsById.containsKey(request.id)) return false
        if (request.isPending && database.state.pendingRequestIdByPair.containsKey(request.pair)) return false
        database.state.requestsById[request.id] = request
        if (request.isPending) {
            database.state.pendingRequestIdByPair[request.pair] = request.id
        }
        return true
    }

    override suspend fun update(request: FollowRequest): Boolean {
        val existing = database.state.requestsById[request.id] ?: return false
        if (existing.isPending) {
            database.state.pendingRequestIdByPair.remove(existing.pair)
        }
        if (request.isPending) {
            val pendingId = database.state.pendingRequestIdByPair[request.pair]
            if (pendingId != null && pendingId != request.id) return false
            database.state.pendingRequestIdByPair[request.pair] = request.id
        }
        database.state.requestsById[request.id] = request
        return true
    }

    override suspend fun invalidatePendingBetweenBothDirections(
        firstUserId: UserId,
        secondUserId: UserId,
        at: Timestamp,
    ): List<FollowRequest> {
        if (firstUserId == secondUserId) return emptyList()
        val pairs = setOf(
            RelationshipPair(firstUserId, secondUserId),
            RelationshipPair(secondUserId, firstUserId),
        )
        return pairs.mapNotNull { pair ->
            val requestId = database.state.pendingRequestIdByPair.remove(pair) ?: return@mapNotNull null
            val request = database.state.requestsById[requestId] ?: return@mapNotNull null
            val invalidated = request.resolve(FollowRequestStatus.InvalidatedByBlock, at)
            database.state.requestsById[requestId] = invalidated
            invalidated
        }
    }

    override suspend fun listIncoming(
        targetId: UserId,
        status: FollowRequestStatus?,
        pageRequest: PageRequest,
    ): Page<FollowRequest> =
        database.state.requestsById.values
            .filter { it.targetId == targetId && (status == null || it.status == status) }
            .sortedWith(compareBy<FollowRequest> { it.createdAt.epochMillis }.thenBy { it.id.value })
            .page(pageRequest)

    override suspend fun listOutgoing(
        requesterId: UserId,
        status: FollowRequestStatus?,
        pageRequest: PageRequest,
    ): Page<FollowRequest> =
        database.state.requestsById.values
            .filter { it.requesterId == requesterId && (status == null || it.status == status) }
            .sortedWith(compareBy<FollowRequest> { it.createdAt.epochMillis }.thenBy { it.id.value })
            .page(pageRequest)

    override suspend fun countIncomingPending(userId: UserId): Int =
        database.state.requestsById.values.count { it.targetId == userId && it.status == FollowRequestStatus.Pending }

    override suspend fun countOutgoingPending(userId: UserId): Int =
        database.state.requestsById.values.count { it.requesterId == userId && it.status == FollowRequestStatus.Pending }
}

class InMemoryBlockRepository(
    private val database: InMemorySocialGraphDatabase,
) : BlockRepository {
    override suspend fun get(blockerId: UserId, blockedId: UserId): BlockRelation? =
        if (blockerId == blockedId) {
            null
        } else {
            database.state.blocksByPair[RelationshipPair(blockerId, blockedId)]
        }

    override suspend fun exists(blockerId: UserId, blockedId: UserId): Boolean =
        get(blockerId, blockedId) != null

    override suspend fun existsAnyDirection(firstUserId: UserId, secondUserId: UserId): Boolean =
        if (firstUserId == secondUserId) {
            false
        } else {
            exists(firstUserId, secondUserId) || exists(secondUserId, firstUserId)
        }

    override suspend fun upsert(block: BlockRelation): Boolean {
        val existing = database.state.blocksByPair.putIfAbsent(block.pair, block)
        return existing == null
    }

    override suspend fun delete(blockerId: UserId, blockedId: UserId): Boolean =
        if (blockerId == blockedId) {
            false
        } else {
            database.state.blocksByPair.remove(RelationshipPair(blockerId, blockedId)) != null
        }

    override suspend fun countBlockedBy(userId: UserId): Int =
        database.state.blocksByPair.values.count { it.blockerId == userId }
}

class InMemoryProfileCounterRepository(
    private val database: InMemorySocialGraphDatabase,
) : ProfileCounterRepository {
    override suspend fun getSnapshot(userId: UserId): ProfileCounters? =
        database.state.countersByUserId[userId]

    override suspend fun saveSnapshot(counters: ProfileCounters) {
        database.state.countersByUserId[counters.userId] = counters
    }
}

class InMemoryAuditEventRepository(
    private val database: InMemorySocialGraphDatabase,
) : AuditEventRepository {
    override suspend fun append(event: AuditEvent) {
        database.state.auditEvents += event
    }

    override suspend fun listBySubject(userId: UserId, pageRequest: PageRequest): Page<AuditEvent> =
        database.state.auditEvents
            .filter { it.actorId == userId || it.subjectId == userId }
            .sortedWith(compareBy<AuditEvent> { it.occurredAt.epochMillis }.thenBy { it.id.value })
            .page(pageRequest)
}

private fun <T> List<T>.page(pageRequest: PageRequest): Page<T> {
    val start = pageRequest.cursor?.value?.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val boundedStart = start.coerceAtMost(size)
    val endExclusive = (boundedStart + pageRequest.limit).coerceAtMost(size)
    val next = if (endExclusive < size) Cursor(endExclusive.toString()) else null
    return Page(
        items = subList(boundedStart, endExclusive),
        nextCursor = next,
    )
}

private fun <K, V> MutableMap<K, V>.putIfAbsent(key: K, value: V): V? {
    val existing = this[key]
    if (existing == null) {
        this[key] = value
    }
    return existing
}

