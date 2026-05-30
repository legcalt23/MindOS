package com.example.mindos.features.social.domain.usecase

import com.example.mindos.features.social.domain.error.ProfileNotFound
import com.example.mindos.features.social.domain.model.ProfileCounters
import com.example.mindos.features.social.domain.model.ProfileVisibility
import com.example.mindos.features.social.domain.model.RelationshipState
import com.example.mindos.features.social.domain.model.VisibleUserProfile
import com.example.mindos.features.social.domain.model.toVisibleProfile
import com.example.mindos.features.social.domain.valueobject.UserId
import com.example.mindos.features.social.shared.util.DomainResult

data class ProfileDetails(
    val profile: VisibleUserProfile?,
    val counters: ProfileCounters?,
    val relationshipState: RelationshipState,
)

class GetProfileUseCase(
    private val dependencies: SocialGraphDependencies,
) {
    suspend operator fun invoke(viewerId: UserId, ownerId: UserId): DomainResult<ProfileDetails> =
        dependencies.transactions.read {
            val ownerProfile = dependencies.profiles.getProfile(ownerId)
                ?: return@read DomainResult.failure(ProfileNotFound(ownerId))
            val ownerPrivacy = dependencies.privacy.settingsOrDefault(ownerId)
            val facts = dependencies.collectFacts(viewerId, ownerId)
            val relationshipState = dependencies.relationshipPolicyEngine.derive(
                viewerId = viewerId,
                ownerId = ownerId,
                ownerPrivacy = ownerPrivacy,
                facts = facts,
            )
            val isBlocked = relationshipState.blockedByYou || relationshipState.blockedYou
            val counters = if (isBlocked) null else dependencies.deriveCounters(ownerId)
            val visibleProfile = if (isBlocked) {
                null
            } else {
                ownerProfile.toVisibleProfile(
                    canViewFullProfile = relationshipState.canViewFullProfile,
                    isPrivate = ownerPrivacy.profileVisibility == ProfileVisibility.Private,
                )
            }
            DomainResult.success(ProfileDetails(visibleProfile, counters, relationshipState))
        }
}

class GetRelationshipStateUseCase(
    private val dependencies: SocialGraphDependencies,
) {
    suspend operator fun invoke(viewerId: UserId, ownerId: UserId): DomainResult<RelationshipState> =
        dependencies.transactions.read {
            dependencies.profiles.getProfile(ownerId)
                ?: return@read DomainResult.failure(ProfileNotFound(ownerId))
            DomainResult.success(dependencies.relationshipState(viewerId, ownerId))
        }
}

class GetProfileCountersUseCase(
    private val dependencies: SocialGraphDependencies,
) {
    suspend operator fun invoke(userId: UserId): DomainResult<ProfileCounters> =
        dependencies.transactions.read {
            dependencies.profiles.getProfile(userId)
                ?: return@read DomainResult.failure(ProfileNotFound(userId))
            DomainResult.success(dependencies.deriveCounters(userId))
        }
}

