package com.example.mindos.features.social.domain.model

import com.example.mindos.features.social.domain.valueobject.RelationshipPair
import com.example.mindos.features.social.domain.valueobject.RequestId
import com.example.mindos.features.social.domain.valueobject.Timestamp
import com.example.mindos.features.social.domain.valueobject.UserId

data class FollowRelation(
    val followerId: UserId,
    val followeeId: UserId,
    val createdAt: Timestamp,
) {
    init {
        require(followerId != followeeId) { "A user cannot follow themselves." }
    }

    val pair: RelationshipPair
        get() = RelationshipPair(followerId, followeeId)
}

enum class FollowRequestStatus {
    Pending,
    Accepted,
    Denied,
    Canceled,
    InvalidatedByBlock,
}

data class FollowRequest(
    val id: RequestId,
    val requesterId: UserId,
    val targetId: UserId,
    val status: FollowRequestStatus = FollowRequestStatus.Pending,
    val createdAt: Timestamp,
    val resolvedAt: Timestamp? = null,
) {
    init {
        require(requesterId != targetId) { "A user cannot request to follow themselves." }
        require((status == FollowRequestStatus.Pending) == (resolvedAt == null)) {
            "Pending requests must not have resolvedAt, resolved requests must have resolvedAt."
        }
    }

    val pair: RelationshipPair
        get() = RelationshipPair(requesterId, targetId)

    val isPending: Boolean
        get() = status == FollowRequestStatus.Pending

    fun resolve(
        newStatus: FollowRequestStatus,
        at: Timestamp,
    ): FollowRequest {
        require(newStatus != FollowRequestStatus.Pending) { "Use a terminal status to resolve a request." }
        return copy(status = newStatus, resolvedAt = at)
    }
}

data class BlockRelation(
    val blockerId: UserId,
    val blockedId: UserId,
    val createdAt: Timestamp,
) {
    init {
        require(blockerId != blockedId) { "A user cannot block themselves." }
    }

    val pair: RelationshipPair
        get() = RelationshipPair(blockerId, blockedId)
}

