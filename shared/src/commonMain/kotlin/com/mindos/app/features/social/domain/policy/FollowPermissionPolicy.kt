package com.mindos.app.features.social.domain.policy

import com.mindos.app.features.social.domain.error.BlockedRelationship
import com.mindos.app.features.social.domain.error.FollowAndPendingRequestConflict
import com.mindos.app.features.social.domain.error.SelfFollowNotAllowed
import com.mindos.app.features.social.domain.model.PrivacySettings
import com.mindos.app.features.social.domain.model.ProfileVisibility
import com.mindos.app.features.social.domain.model.RelationshipFacts
import com.mindos.app.features.social.domain.valueobject.UserId

sealed interface FollowPermission {
    data object CreateFollow : FollowPermission
    data object CreateRequest : FollowPermission
    data object AlreadyFollowing : FollowPermission
    data object RequestAlreadyPending : FollowPermission
}

class FollowPermissionPolicy {
    fun evaluate(
        actorId: UserId,
        targetId: UserId,
        targetPrivacy: PrivacySettings,
        facts: RelationshipFacts,
    ): PolicyDecision<FollowPermission> =
        when {
            actorId == targetId -> PolicyDecision.Denied(SelfFollowNotAllowed)
            facts.hasAnyBlock -> PolicyDecision.Denied(BlockedRelationship(actorId, targetId))
            facts.viewerFollowsOwner != null && facts.outgoingRequestPending != null ->
                PolicyDecision.Denied(FollowAndPendingRequestConflict)
            facts.viewerFollowsOwner != null -> PolicyDecision.Allowed(FollowPermission.AlreadyFollowing)
            facts.outgoingRequestPending != null -> PolicyDecision.Allowed(FollowPermission.RequestAlreadyPending)
            targetPrivacy.profileVisibility == ProfileVisibility.Public ->
                PolicyDecision.Allowed(FollowPermission.CreateFollow)
            else -> PolicyDecision.Allowed(FollowPermission.CreateRequest)
        }
}

