package com.mindos.app.features.social.data.mapper

import com.mindos.app.features.social.domain.error.PersistenceFailure
import com.mindos.app.features.social.domain.model.BlockRelation
import com.mindos.app.features.social.domain.model.FollowRelation
import com.mindos.app.features.social.domain.model.FollowRequest
import com.mindos.app.features.social.domain.model.FollowRequestStatus
import com.mindos.app.features.social.domain.model.PrivacySettings
import com.mindos.app.features.social.domain.model.ProfileVisibility
import com.mindos.app.features.social.domain.valueobject.RequestId
import com.mindos.app.features.social.domain.valueobject.Timestamp
import com.mindos.app.features.social.domain.valueobject.UserId
import com.mindos.app.features.social.shared.util.DomainResult

data class PrivacySettingsRecord(
    val userId: String,
    val profileVisibility: String,
    val followersVisibility: String,
    val followingVisibility: String,
    val messagePermission: String,
    val allowMentionsFrom: String,
    val discoverable: Boolean,
    val showActivityStatus: Boolean,
    val updatedAtMillis: Long,
)

data class FollowRelationRecord(
    val followerId: String,
    val followeeId: String,
    val createdAtMillis: Long,
)

data class FollowRequestRecord(
    val id: String,
    val requesterId: String,
    val targetId: String,
    val status: String,
    val createdAtMillis: Long,
    val resolvedAtMillis: Long?,
)

data class BlockRelationRecord(
    val blockerId: String,
    val blockedId: String,
    val createdAtMillis: Long,
)

fun PrivacySettings.toRecord(): PrivacySettingsRecord =
    PrivacySettingsRecord(
        userId = userId.value,
        profileVisibility = profileVisibility.name,
        followersVisibility = followersVisibility.name,
        followingVisibility = followingVisibility.name,
        messagePermission = messagePermission.name,
        allowMentionsFrom = allowMentionsFrom.name,
        discoverable = discoverable,
        showActivityStatus = showActivityStatus,
        updatedAtMillis = updatedAt.epochMillis,
    )

fun PrivacySettingsRecord.toDomain(): DomainResult<PrivacySettings> =
    mapRecord("PrivacySettingsRecord") {
        PrivacySettings(
            userId = UserId(userId),
            profileVisibility = ProfileVisibility.valueOf(profileVisibility),
            followersVisibility = com.mindos.app.features.social.domain.model.AudienceRule.valueOf(followersVisibility),
            followingVisibility = com.mindos.app.features.social.domain.model.AudienceRule.valueOf(followingVisibility),
            messagePermission = com.mindos.app.features.social.domain.model.AudienceRule.valueOf(messagePermission),
            allowMentionsFrom = com.mindos.app.features.social.domain.model.AudienceRule.valueOf(allowMentionsFrom),
            discoverable = discoverable,
            showActivityStatus = showActivityStatus,
            updatedAt = Timestamp(updatedAtMillis),
        )
    }

fun FollowRelation.toRecord(): FollowRelationRecord =
    FollowRelationRecord(
        followerId = followerId.value,
        followeeId = followeeId.value,
        createdAtMillis = createdAt.epochMillis,
    )

fun FollowRelationRecord.toDomain(): DomainResult<FollowRelation> =
    mapRecord("FollowRelationRecord") {
        FollowRelation(
            followerId = UserId(followerId),
            followeeId = UserId(followeeId),
            createdAt = Timestamp(createdAtMillis),
        )
    }

fun FollowRequest.toRecord(): FollowRequestRecord =
    FollowRequestRecord(
        id = id.value,
        requesterId = requesterId.value,
        targetId = targetId.value,
        status = status.name,
        createdAtMillis = createdAt.epochMillis,
        resolvedAtMillis = resolvedAt?.epochMillis,
    )

fun FollowRequestRecord.toDomain(): DomainResult<FollowRequest> =
    mapRecord("FollowRequestRecord") {
        FollowRequest(
            id = RequestId(id),
            requesterId = UserId(requesterId),
            targetId = UserId(targetId),
            status = FollowRequestStatus.valueOf(status),
            createdAt = Timestamp(createdAtMillis),
            resolvedAt = resolvedAtMillis?.let(::Timestamp),
        )
    }

fun BlockRelation.toRecord(): BlockRelationRecord =
    BlockRelationRecord(
        blockerId = blockerId.value,
        blockedId = blockedId.value,
        createdAtMillis = createdAt.epochMillis,
    )

fun BlockRelationRecord.toDomain(): DomainResult<BlockRelation> =
    mapRecord("BlockRelationRecord") {
        BlockRelation(
            blockerId = UserId(blockerId),
            blockedId = UserId(blockedId),
            createdAt = Timestamp(createdAtMillis),
        )
    }

private inline fun <T> mapRecord(
    recordName: String,
    block: () -> T,
): DomainResult<T> =
    try {
        DomainResult.success(block())
    } catch (exception: IllegalArgumentException) {
        DomainResult.failure(PersistenceFailure("$recordName contains invalid persisted data: ${exception.message}"))
    }

