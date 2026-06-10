package com.mindos.app.features.social.domain.policy

import com.mindos.app.features.social.domain.model.PrivacySettings
import com.mindos.app.features.social.domain.model.RelationshipFacts
import com.mindos.app.features.social.domain.model.RelationshipState
import com.mindos.app.features.social.domain.valueobject.UserId

class RelationshipPolicyEngine(
    private val visibilityPolicy: ProfileVisibilityPolicy = ProfileVisibilityPolicy(),
    private val followPermissionPolicy: FollowPermissionPolicy = FollowPermissionPolicy(),
) {
    fun derive(
        viewerId: UserId,
        ownerId: UserId,
        ownerPrivacy: PrivacySettings,
        facts: RelationshipFacts,
    ): RelationshipState {
        val isSelf = viewerId == ownerId
        val isFollowing = facts.viewerFollowsOwner != null
        val followsYou = facts.ownerFollowsViewer != null
        val blockedByYou = facts.blockedByViewer != null
        val blockedYou = facts.blockedViewer != null
        val visibility = visibilityPolicy.evaluate(viewerId, ownerId, ownerPrivacy, facts)
        val followDecision = followPermissionPolicy.evaluate(viewerId, ownerId, ownerPrivacy, facts)
        val followPermission = (followDecision as? PolicyDecision.Allowed)?.value

        return RelationshipState(
            viewerId = viewerId,
            ownerId = ownerId,
            isSelf = isSelf,
            isFollowing = isFollowing,
            followsYou = followsYou,
            isMutual = isFollowing && followsYou,
            outgoingRequestPending = facts.outgoingRequestPending != null,
            incomingRequestPending = facts.incomingRequestPending != null,
            blockedByYou = blockedByYou,
            blockedYou = blockedYou,
            canFollow = followPermission == FollowPermission.CreateFollow,
            canRequestFollow = followPermission == FollowPermission.CreateRequest,
            canUnfollow = !isSelf && isFollowing && !facts.hasAnyBlock,
            canBlock = !isSelf && !blockedByYou,
            canUnblock = blockedByYou,
            canViewFullProfile = visibility.canViewFullProfile,
            canViewFollowers = visibility.canViewFollowers,
            canViewFollowing = visibility.canViewFollowing,
            canMessage = visibility.canMessage,
        )
    }
}

