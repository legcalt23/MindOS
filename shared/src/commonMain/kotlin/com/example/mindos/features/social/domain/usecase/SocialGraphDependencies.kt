package com.example.mindos.features.social.domain.usecase

import com.example.mindos.features.social.domain.error.ActionNotAuthorized
import com.example.mindos.features.social.domain.error.IntegrityViolation
import com.example.mindos.features.social.domain.error.ProfileNotFound
import com.example.mindos.features.social.domain.error.UserNotFound
import com.example.mindos.features.social.domain.model.AuditEvent
import com.example.mindos.features.social.domain.model.AuditEventType
import com.example.mindos.features.social.domain.model.FollowRelation
import com.example.mindos.features.social.domain.model.PrivacySettings
import com.example.mindos.features.social.domain.model.ProfileCounters
import com.example.mindos.features.social.domain.model.RelationshipFacts
import com.example.mindos.features.social.domain.model.RelationshipState
import com.example.mindos.features.social.domain.model.UserProfile
import com.example.mindos.features.social.domain.policy.BlockPolicy
import com.example.mindos.features.social.domain.policy.FollowPermissionPolicy
import com.example.mindos.features.social.domain.policy.RelationshipPolicyEngine
import com.example.mindos.features.social.domain.policy.RequestResolutionPolicy
import com.example.mindos.features.social.domain.ranking.HomeFeedRankingEngine
import com.example.mindos.features.social.domain.repository.AuditEventRepository
import com.example.mindos.features.social.domain.repository.BlockRepository
import com.example.mindos.features.social.domain.repository.FollowRepository
import com.example.mindos.features.social.domain.repository.FollowRequestRepository
import com.example.mindos.features.social.domain.repository.PrivacySettingsRepository
import com.example.mindos.features.social.domain.repository.ProfileCounterRepository
import com.example.mindos.features.social.domain.repository.SocialGraphTransactionRunner
import com.example.mindos.features.social.domain.repository.UserProfileRepository
import com.example.mindos.features.social.domain.valueobject.UserId
import com.example.mindos.features.social.shared.util.AppClock
import com.example.mindos.features.social.shared.util.AuditEventIdGenerator
import com.example.mindos.features.social.shared.util.DomainResult
import com.example.mindos.features.social.shared.util.Page
import com.example.mindos.features.social.shared.util.RequestIdGenerator

data class SocialGraphDependencies(
    val profiles: UserProfileRepository,
    val privacy: PrivacySettingsRepository,
    val follows: FollowRepository,
    val requests: FollowRequestRepository,
    val blocks: BlockRepository,
    val counters: ProfileCounterRepository,
    val auditEvents: AuditEventRepository,
    val transactions: SocialGraphTransactionRunner,
    val clock: AppClock,
    val requestIds: RequestIdGenerator,
    val auditIds: AuditEventIdGenerator,
    val relationshipPolicyEngine: RelationshipPolicyEngine = RelationshipPolicyEngine(),
    val followPermissionPolicy: FollowPermissionPolicy = FollowPermissionPolicy(),
    val requestResolutionPolicy: RequestResolutionPolicy = RequestResolutionPolicy(),
    val blockPolicy: BlockPolicy = BlockPolicy(),
    val rankingEngine: HomeFeedRankingEngine = HomeFeedRankingEngine(),
)

class SocialGraphUseCases(
    dependencies: SocialGraphDependencies,
) {
    val getProfile = GetProfileUseCase(dependencies)
    val getRelationshipState = GetRelationshipStateUseCase(dependencies)
    val followUser = FollowUserUseCase(dependencies)
    val unfollowUser = UnfollowUserUseCase(dependencies)
    val acceptFollowRequest = AcceptFollowRequestUseCase(dependencies)
    val denyFollowRequest = DenyFollowRequestUseCase(dependencies)
    val cancelFollowRequest = CancelFollowRequestUseCase(dependencies)
    val blockUser = BlockUserUseCase(dependencies)
    val unblockUser = UnblockUserUseCase(dependencies)
    val updatePrivacySettings = UpdatePrivacySettingsUseCase(dependencies)
    val listFollowers = ListFollowersUseCase(dependencies)
    val listFollowing = ListFollowingUseCase(dependencies)
    val listIncomingFollowRequests = ListIncomingFollowRequestsUseCase(dependencies)
    val listOutgoingFollowRequests = ListOutgoingFollowRequestsUseCase(dependencies)
    val removeFollower = RemoveFollowerUseCase(dependencies)
    val getProfileCounters = GetProfileCountersUseCase(dependencies)
}

internal suspend fun SocialGraphDependencies.requireParticipant(userId: UserId): DomainResult<UserProfile> {
    val account = profiles.getAccount(userId)
        ?: return DomainResult.failure(UserNotFound(userId))
    if (!account.canParticipateInSocialGraph) {
        return DomainResult.failure(ActionNotAuthorized("User ${userId.value} cannot participate in the social graph."))
    }
    val profile = profiles.getProfile(userId)
        ?: return DomainResult.failure(ProfileNotFound(userId))
    return DomainResult.success(profile)
}

internal suspend fun PrivacySettingsRepository.settingsOrDefault(userId: UserId): PrivacySettings =
    get(userId) ?: PrivacySettings(userId = userId)

internal suspend fun SocialGraphDependencies.collectFacts(
    viewerId: UserId,
    ownerId: UserId,
): RelationshipFacts =
    if (viewerId == ownerId) {
        RelationshipFacts()
    } else {
        RelationshipFacts(
            viewerFollowsOwner = follows.get(viewerId, ownerId),
            ownerFollowsViewer = follows.get(ownerId, viewerId),
            outgoingRequestPending = requests.findPending(viewerId, ownerId),
            incomingRequestPending = requests.findPending(ownerId, viewerId),
            blockedByViewer = blocks.get(viewerId, ownerId),
            blockedViewer = blocks.get(ownerId, viewerId),
        )
    }

internal suspend fun SocialGraphDependencies.relationshipState(
    viewerId: UserId,
    ownerId: UserId,
): RelationshipState =
    relationshipPolicyEngine.derive(
        viewerId = viewerId,
        ownerId = ownerId,
        ownerPrivacy = privacy.settingsOrDefault(ownerId),
        facts = collectFacts(viewerId, ownerId),
    )

internal suspend fun SocialGraphDependencies.deriveCounters(userId: UserId): ProfileCounters {
    val counters = ProfileCounters(
        userId = userId,
        followersCount = follows.countFollowers(userId),
        followingCount = follows.countFollowing(userId),
        incomingPendingRequestsCount = requests.countIncomingPending(userId),
        outgoingPendingRequestsCount = requests.countOutgoingPending(userId),
        blockedUsersCount = blocks.countBlockedBy(userId),
        derivedAt = clock.now(),
    )
    this.counters.saveSnapshot(counters)
    return counters
}

internal suspend fun SocialGraphDependencies.audit(
    type: AuditEventType,
    actorId: UserId,
    subjectId: UserId,
    metadata: Map<String, String> = emptyMap(),
) {
    auditEvents.append(
        AuditEvent(
            id = auditIds.nextAuditEventId(),
            type = type,
            actorId = actorId,
            subjectId = subjectId,
            occurredAt = clock.now(),
            metadata = metadata,
        ),
    )
}

internal inline fun <T> DomainResult<T>.ifFailure(block: (DomainResult.Failure) -> Nothing): T =
    when (this) {
        is DomainResult.Success -> value
        is DomainResult.Failure -> block(this)
    }

internal suspend inline fun SocialGraphDependencies.profilePage(
    relations: Page<FollowRelation>,
    crossinline userIdFromRelation: (FollowRelation) -> UserId,
): DomainResult<Page<UserProfile>> {
    val profiles = mutableListOf<UserProfile>()
    for (relation in relations.items) {
        val userId = userIdFromRelation(relation)
        val profile = this.profiles.getProfile(userId)
            ?: return DomainResult.failure(IntegrityViolation("Relation references missing profile ${userId.value}."))
        profiles += profile
    }
    return DomainResult.success(Page(items = profiles, nextCursor = relations.nextCursor))
}
