package com.example.neuv3.features.social.domain.policy

import com.example.neuv3.features.social.domain.model.AudienceRule
import com.example.neuv3.features.social.domain.model.PrivacySettings
import com.example.neuv3.features.social.domain.model.ProfileVisibility
import com.example.neuv3.features.social.domain.model.ProfileVisibilityDecision
import com.example.neuv3.features.social.domain.model.RelationshipFacts
import com.example.neuv3.features.social.domain.valueobject.UserId

class ProfileVisibilityPolicy {
    fun evaluate(
        viewerId: UserId,
        ownerId: UserId,
        settings: PrivacySettings,
        facts: RelationshipFacts,
    ): ProfileVisibilityDecision {
        val isSelf = viewerId == ownerId
        val blocked = facts.hasAnyBlock
        val viewerFollowsOwner = facts.viewerFollowsOwner != null
        val ownerFollowsViewer = facts.ownerFollowsViewer != null
        val mutual = viewerFollowsOwner && ownerFollowsViewer

        val canViewFullProfile = when {
            isSelf -> true
            blocked -> false
            settings.profileVisibility == ProfileVisibility.Public -> true
            else -> viewerFollowsOwner
        }

        return ProfileVisibilityDecision(
            canViewFullProfile = canViewFullProfile,
            canViewFollowers = settings.followersVisibility.allows(
                isSelf = isSelf,
                canViewFullProfile = canViewFullProfile,
                blocked = blocked,
                viewerFollowsOwner = viewerFollowsOwner,
                mutual = mutual,
            ),
            canViewFollowing = settings.followingVisibility.allows(
                isSelf = isSelf,
                canViewFullProfile = canViewFullProfile,
                blocked = blocked,
                viewerFollowsOwner = viewerFollowsOwner,
                mutual = mutual,
            ),
            canMessage = settings.messagePermission.allows(
                isSelf = isSelf,
                canViewFullProfile = canViewFullProfile,
                blocked = blocked,
                viewerFollowsOwner = viewerFollowsOwner,
                mutual = mutual,
            ),
        )
    }

    private fun AudienceRule.allows(
        isSelf: Boolean,
        canViewFullProfile: Boolean,
        blocked: Boolean,
        viewerFollowsOwner: Boolean,
        mutual: Boolean,
    ): Boolean {
        if (isSelf) return true
        if (blocked || !canViewFullProfile) return false
        return when (this) {
            AudienceRule.Everyone -> true
            AudienceRule.Followers -> viewerFollowsOwner
            AudienceRule.Mutuals -> mutual
            AudienceRule.OnlySelf -> false
        }
    }
}

