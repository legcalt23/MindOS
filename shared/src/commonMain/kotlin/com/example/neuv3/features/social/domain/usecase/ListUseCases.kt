package com.example.neuv3.features.social.domain.usecase

import com.example.neuv3.features.social.domain.error.ProfileNotFound
import com.example.neuv3.features.social.domain.error.ProfileVisibilityDenied
import com.example.neuv3.features.social.domain.error.SelfFollowNotAllowed
import com.example.neuv3.features.social.domain.model.AuditEventType
import com.example.neuv3.features.social.domain.model.FollowRequest
import com.example.neuv3.features.social.domain.model.FollowRequestStatus
import com.example.neuv3.features.social.domain.model.UserProfile
import com.example.neuv3.features.social.domain.valueobject.UserId
import com.example.neuv3.features.social.shared.util.DomainResult
import com.example.neuv3.features.social.shared.util.Page
import com.example.neuv3.features.social.shared.util.PageRequest

sealed interface RemoveFollowerResult {
    data class Removed(val relation: com.example.neuv3.features.social.domain.model.FollowRelation) : RemoveFollowerResult
    data object NotFollower : RemoveFollowerResult
}

class ListFollowersUseCase(
    private val dependencies: SocialGraphDependencies,
) {
    suspend operator fun invoke(
        viewerId: UserId,
        ownerId: UserId,
        pageRequest: PageRequest = PageRequest(),
    ): DomainResult<Page<UserProfile>> =
        dependencies.transactions.read {
            dependencies.profiles.getProfile(ownerId)
                ?: return@read DomainResult.failure(ProfileNotFound(ownerId))
            val state = dependencies.relationshipState(viewerId, ownerId)
            if (!state.canViewFollowers) {
                return@read DomainResult.failure(ProfileVisibilityDenied(viewerId, ownerId, "followers"))
            }
            val relations = dependencies.follows.listFollowers(ownerId, pageRequest)
            dependencies.profilePage(relations) { it.followerId }
        }
}

class ListFollowingUseCase(
    private val dependencies: SocialGraphDependencies,
) {
    suspend operator fun invoke(
        viewerId: UserId,
        ownerId: UserId,
        pageRequest: PageRequest = PageRequest(),
    ): DomainResult<Page<UserProfile>> =
        dependencies.transactions.read {
            dependencies.profiles.getProfile(ownerId)
                ?: return@read DomainResult.failure(ProfileNotFound(ownerId))
            val state = dependencies.relationshipState(viewerId, ownerId)
            if (!state.canViewFollowing) {
                return@read DomainResult.failure(ProfileVisibilityDenied(viewerId, ownerId, "following"))
            }
            val relations = dependencies.follows.listFollowing(ownerId, pageRequest)
            dependencies.profilePage(relations) { it.followeeId }
        }
}

class ListIncomingFollowRequestsUseCase(
    private val dependencies: SocialGraphDependencies,
) {
    suspend operator fun invoke(
        actorId: UserId,
        pageRequest: PageRequest = PageRequest(),
    ): DomainResult<Page<FollowRequest>> =
        dependencies.transactions.read {
            dependencies.requireParticipant(actorId).ifFailure { return@read it }
            DomainResult.success(
                dependencies.requests.listIncoming(actorId, FollowRequestStatus.Pending, pageRequest),
            )
        }
}

class ListOutgoingFollowRequestsUseCase(
    private val dependencies: SocialGraphDependencies,
) {
    suspend operator fun invoke(
        actorId: UserId,
        pageRequest: PageRequest = PageRequest(),
    ): DomainResult<Page<FollowRequest>> =
        dependencies.transactions.read {
            dependencies.requireParticipant(actorId).ifFailure { return@read it }
            DomainResult.success(
                dependencies.requests.listOutgoing(actorId, FollowRequestStatus.Pending, pageRequest),
            )
        }
}

class RemoveFollowerUseCase(
    private val dependencies: SocialGraphDependencies,
) {
    suspend operator fun invoke(ownerId: UserId, followerId: UserId): DomainResult<RemoveFollowerResult> =
        dependencies.transactions.write {
            if (ownerId == followerId) return@write DomainResult.failure(SelfFollowNotAllowed)
            dependencies.requireParticipant(ownerId).ifFailure { return@write it }
            dependencies.requireParticipant(followerId).ifFailure { return@write it }

            val relation = dependencies.follows.get(followerId, ownerId)
                ?: return@write DomainResult.success(RemoveFollowerResult.NotFollower)
            dependencies.follows.delete(followerId, ownerId)
            dependencies.audit(AuditEventType.FollowerRemoved, ownerId, followerId)
            DomainResult.success(RemoveFollowerResult.Removed(relation))
        }
}

