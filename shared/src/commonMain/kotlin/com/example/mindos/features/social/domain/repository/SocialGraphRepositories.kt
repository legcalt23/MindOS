package com.example.mindos.features.social.domain.repository

import com.example.mindos.features.social.domain.model.AuditEvent
import com.example.mindos.features.social.domain.model.BlockRelation
import com.example.mindos.features.social.domain.model.FollowRelation
import com.example.mindos.features.social.domain.model.FollowRequest
import com.example.mindos.features.social.domain.model.FollowRequestStatus
import com.example.mindos.features.social.domain.model.PrivacySettings
import com.example.mindos.features.social.domain.model.ProfileCounters
import com.example.mindos.features.social.domain.model.UserAccount
import com.example.mindos.features.social.domain.model.UserProfile
import com.example.mindos.features.social.domain.valueobject.Handle
import com.example.mindos.features.social.domain.valueobject.RelationshipPair
import com.example.mindos.features.social.domain.valueobject.RequestId
import com.example.mindos.features.social.domain.valueobject.Timestamp
import com.example.mindos.features.social.domain.valueobject.UserId
import com.example.mindos.features.social.shared.util.Page
import com.example.mindos.features.social.shared.util.PageRequest

interface SocialGraphTransactionRunner {
    suspend fun <T> read(block: suspend () -> T): T
    suspend fun <T> write(block: suspend () -> T): T
}

interface UserProfileRepository {
    suspend fun getAccount(userId: UserId): UserAccount?
    suspend fun saveAccount(account: UserAccount)
    suspend fun getProfile(userId: UserId): UserProfile?
    suspend fun findProfileByHandle(handle: Handle): UserProfile?
    suspend fun saveProfile(profile: UserProfile)
}

interface PrivacySettingsRepository {
    suspend fun get(userId: UserId): PrivacySettings?
    suspend fun save(settings: PrivacySettings)
}

interface FollowRepository {
    suspend fun get(followerId: UserId, followeeId: UserId): FollowRelation?
    suspend fun exists(followerId: UserId, followeeId: UserId): Boolean
    suspend fun insert(relation: FollowRelation): Boolean
    suspend fun delete(followerId: UserId, followeeId: UserId): Boolean
    suspend fun deleteBothDirections(firstUserId: UserId, secondUserId: UserId): Int
    suspend fun listFollowers(userId: UserId, pageRequest: PageRequest): Page<FollowRelation>
    suspend fun listFollowing(userId: UserId, pageRequest: PageRequest): Page<FollowRelation>
    suspend fun countFollowers(userId: UserId): Int
    suspend fun countFollowing(userId: UserId): Int
}

interface FollowRequestRepository {
    suspend fun get(requestId: RequestId): FollowRequest?
    suspend fun findPending(requesterId: UserId, targetId: UserId): FollowRequest?
    suspend fun insert(request: FollowRequest): Boolean
    suspend fun update(request: FollowRequest): Boolean
    suspend fun invalidatePendingBetweenBothDirections(
        firstUserId: UserId,
        secondUserId: UserId,
        at: Timestamp,
    ): List<FollowRequest>
    suspend fun listIncoming(
        targetId: UserId,
        status: FollowRequestStatus?,
        pageRequest: PageRequest,
    ): Page<FollowRequest>
    suspend fun listOutgoing(
        requesterId: UserId,
        status: FollowRequestStatus?,
        pageRequest: PageRequest,
    ): Page<FollowRequest>
    suspend fun countIncomingPending(userId: UserId): Int
    suspend fun countOutgoingPending(userId: UserId): Int
}

interface BlockRepository {
    suspend fun get(blockerId: UserId, blockedId: UserId): BlockRelation?
    suspend fun exists(blockerId: UserId, blockedId: UserId): Boolean
    suspend fun existsAnyDirection(firstUserId: UserId, secondUserId: UserId): Boolean
    suspend fun upsert(block: BlockRelation): Boolean
    suspend fun delete(blockerId: UserId, blockedId: UserId): Boolean
    suspend fun countBlockedBy(userId: UserId): Int
}

interface ProfileCounterRepository {
    suspend fun getSnapshot(userId: UserId): ProfileCounters?
    suspend fun saveSnapshot(counters: ProfileCounters)
}

interface AuditEventRepository {
    suspend fun append(event: AuditEvent)
    suspend fun listBySubject(userId: UserId, pageRequest: PageRequest): Page<AuditEvent>
}

fun RelationshipPair.reverse(): RelationshipPair =
    RelationshipPair(targetId, actorId)

