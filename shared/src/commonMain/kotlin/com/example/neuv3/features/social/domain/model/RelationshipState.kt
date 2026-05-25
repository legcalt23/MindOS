package com.example.neuv3.features.social.domain.model

import com.example.neuv3.features.social.domain.valueobject.UserId

data class RelationshipFacts(
    val viewerFollowsOwner: FollowRelation? = null,
    val ownerFollowsViewer: FollowRelation? = null,
    val outgoingRequestPending: FollowRequest? = null,
    val incomingRequestPending: FollowRequest? = null,
    val blockedByViewer: BlockRelation? = null,
    val blockedViewer: BlockRelation? = null,
) {
    val hasAnyBlock: Boolean
        get() = blockedByViewer != null || blockedViewer != null
}

data class ProfileVisibilityDecision(
    val canViewFullProfile: Boolean,
    val canViewFollowers: Boolean,
    val canViewFollowing: Boolean,
    val canMessage: Boolean,
)

data class RelationshipState(
    val viewerId: UserId,
    val ownerId: UserId,
    val isSelf: Boolean,
    val isFollowing: Boolean,
    val followsYou: Boolean,
    val isMutual: Boolean,
    val outgoingRequestPending: Boolean,
    val incomingRequestPending: Boolean,
    val blockedByYou: Boolean,
    val blockedYou: Boolean,
    val canFollow: Boolean,
    val canRequestFollow: Boolean,
    val canUnfollow: Boolean,
    val canBlock: Boolean,
    val canUnblock: Boolean,
    val canViewFullProfile: Boolean,
    val canViewFollowers: Boolean,
    val canViewFollowing: Boolean,
    val canMessage: Boolean,
)

