package com.example.mindos.features.social.domain.usecase

import com.example.mindos.features.social.domain.error.ActiveFollowAlreadyExists
import com.example.mindos.features.social.domain.error.FollowRequestNotFound
import com.example.mindos.features.social.domain.error.PendingFollowRequestAlreadyExists
import com.example.mindos.features.social.domain.error.SelfFollowNotAllowed
import com.example.mindos.features.social.domain.model.AuditEventType
import com.example.mindos.features.social.domain.model.FollowRelation
import com.example.mindos.features.social.domain.model.FollowRequest
import com.example.mindos.features.social.domain.model.FollowRequestStatus
import com.example.mindos.features.social.domain.policy.FollowPermission
import com.example.mindos.features.social.domain.policy.PolicyDecision
import com.example.mindos.features.social.domain.policy.RequestResolutionPermission
import com.example.mindos.features.social.domain.valueobject.RequestId
import com.example.mindos.features.social.domain.valueobject.UserId
import com.example.mindos.features.social.shared.util.DomainResult

sealed interface FollowUserResult {
    data class Following(val relation: FollowRelation) : FollowUserResult
    data class Requested(val request: FollowRequest) : FollowUserResult
    data class AlreadyFollowing(val relation: FollowRelation) : FollowUserResult
    data class RequestAlreadyPending(val request: FollowRequest) : FollowUserResult
}

sealed interface UnfollowUserResult {
    data class Unfollowed(val relation: FollowRelation) : UnfollowUserResult
    data object NotFollowing : UnfollowUserResult
}

sealed interface FollowRequestResolutionResult {
    data class Accepted(val request: FollowRequest, val relation: FollowRelation) : FollowRequestResolutionResult
    data class Denied(val request: FollowRequest) : FollowRequestResolutionResult
    data class Canceled(val request: FollowRequest) : FollowRequestResolutionResult
    data class AlreadyResolved(val request: FollowRequest) : FollowRequestResolutionResult
}

class FollowUserUseCase(
    private val dependencies: SocialGraphDependencies,
) {
    suspend operator fun invoke(actorId: UserId, targetId: UserId): DomainResult<FollowUserResult> =
        dependencies.transactions.write {
            dependencies.requireParticipant(actorId).ifFailure { return@write it }
            dependencies.requireParticipant(targetId).ifFailure { return@write it }

            val facts = dependencies.collectFacts(actorId, targetId)
            val targetPrivacy = dependencies.privacy.settingsOrDefault(targetId)
            when (val decision = dependencies.followPermissionPolicy.evaluate(actorId, targetId, targetPrivacy, facts)) {
                is PolicyDecision.Denied -> DomainResult.failure(decision.error)
                is PolicyDecision.Allowed -> when (decision.value) {
                    FollowPermission.AlreadyFollowing -> DomainResult.success(
                        FollowUserResult.AlreadyFollowing(
                            requireNotNull(facts.viewerFollowsOwner) { "AlreadyFollowing requires existing relation" },
                        ),
                    )
                    FollowPermission.RequestAlreadyPending -> DomainResult.success(
                        FollowUserResult.RequestAlreadyPending(
                            requireNotNull(facts.outgoingRequestPending) { "RequestAlreadyPending requires existing request" },
                        ),
                    )
                    FollowPermission.CreateFollow -> createFollow(actorId, targetId)
                    FollowPermission.CreateRequest -> createRequest(actorId, targetId)
                }
            }
        }

    private suspend fun createFollow(actorId: UserId, targetId: UserId): DomainResult<FollowUserResult> {
        val relation = FollowRelation(actorId, targetId, dependencies.clock.now())
        return if (!dependencies.follows.insert(relation)) {
            DomainResult.failure(ActiveFollowAlreadyExists)
        } else {
            dependencies.audit(AuditEventType.FollowCreated, actorId, targetId)
            DomainResult.success(FollowUserResult.Following(relation))
        }
    }

    private suspend fun createRequest(actorId: UserId, targetId: UserId): DomainResult<FollowUserResult> {
        val request = FollowRequest(
            id = dependencies.requestIds.nextRequestId(),
            requesterId = actorId,
            targetId = targetId,
            createdAt = dependencies.clock.now(),
        )
        return if (!dependencies.requests.insert(request)) {
            DomainResult.failure(PendingFollowRequestAlreadyExists)
        } else {
            dependencies.audit(
                type = AuditEventType.FollowRequestCreated,
                actorId = actorId,
                subjectId = targetId,
                metadata = mapOf("requestId" to request.id.value),
            )
            DomainResult.success(FollowUserResult.Requested(request))
        }
    }
}

class UnfollowUserUseCase(
    private val dependencies: SocialGraphDependencies,
) {
    suspend operator fun invoke(actorId: UserId, targetId: UserId): DomainResult<UnfollowUserResult> =
        dependencies.transactions.write {
            if (actorId == targetId) return@write DomainResult.failure(SelfFollowNotAllowed)
            dependencies.requireParticipant(actorId).ifFailure { return@write it }
            dependencies.requireParticipant(targetId).ifFailure { return@write it }

            val existing = dependencies.follows.get(actorId, targetId)
                ?: return@write DomainResult.success(UnfollowUserResult.NotFollowing)
            dependencies.follows.delete(actorId, targetId)
            dependencies.audit(AuditEventType.FollowRemoved, actorId, targetId)
            DomainResult.success(UnfollowUserResult.Unfollowed(existing))
        }
}

class AcceptFollowRequestUseCase(
    private val dependencies: SocialGraphDependencies,
) {
    suspend operator fun invoke(actorId: UserId, requestId: RequestId): DomainResult<FollowRequestResolutionResult> =
        dependencies.transactions.write {
            val request = dependencies.requests.get(requestId)
                ?: return@write DomainResult.failure(FollowRequestNotFound(requestId))
            val facts = dependencies.collectFacts(request.requesterId, request.targetId)
            when (val decision = dependencies.requestResolutionPolicy.canAccept(actorId, request, facts)) {
                is PolicyDecision.Denied -> DomainResult.failure(decision.error)
                is PolicyDecision.Allowed -> when (decision.value) {
                    RequestResolutionPermission.AlreadyResolved -> DomainResult.success(
                        FollowRequestResolutionResult.AlreadyResolved(request),
                    )
                    RequestResolutionPermission.Resolve -> {
                        val resolved = request.resolve(FollowRequestStatus.Accepted, dependencies.clock.now())
                        dependencies.requests.update(resolved)
                        val relation = dependencies.follows.get(request.requesterId, request.targetId)
                            ?: FollowRelation(
                                followerId = request.requesterId,
                                followeeId = request.targetId,
                                createdAt = dependencies.clock.now(),
                            ).also { dependencies.follows.insert(it) }
                        dependencies.audit(
                            type = AuditEventType.FollowRequestAccepted,
                            actorId = actorId,
                            subjectId = request.requesterId,
                            metadata = mapOf("requestId" to request.id.value),
                        )
                        DomainResult.success(FollowRequestResolutionResult.Accepted(resolved, relation))
                    }
                }
            }
        }
}

class DenyFollowRequestUseCase(
    private val dependencies: SocialGraphDependencies,
) {
    suspend operator fun invoke(actorId: UserId, requestId: RequestId): DomainResult<FollowRequestResolutionResult> =
        dependencies.transactions.write {
            val request = dependencies.requests.get(requestId)
                ?: return@write DomainResult.failure(FollowRequestNotFound(requestId))
            val facts = dependencies.collectFacts(request.requesterId, request.targetId)
            when (val decision = dependencies.requestResolutionPolicy.canDeny(actorId, request, facts)) {
                is PolicyDecision.Denied -> DomainResult.failure(decision.error)
                is PolicyDecision.Allowed -> when (decision.value) {
                    RequestResolutionPermission.AlreadyResolved -> DomainResult.success(
                        FollowRequestResolutionResult.AlreadyResolved(request),
                    )
                    RequestResolutionPermission.Resolve -> {
                        val resolved = request.resolve(FollowRequestStatus.Denied, dependencies.clock.now())
                        dependencies.requests.update(resolved)
                        dependencies.audit(
                            type = AuditEventType.FollowRequestDenied,
                            actorId = actorId,
                            subjectId = request.requesterId,
                            metadata = mapOf("requestId" to request.id.value),
                        )
                        DomainResult.success(FollowRequestResolutionResult.Denied(resolved))
                    }
                }
            }
        }
}

class CancelFollowRequestUseCase(
    private val dependencies: SocialGraphDependencies,
) {
    suspend operator fun invoke(actorId: UserId, requestId: RequestId): DomainResult<FollowRequestResolutionResult> =
        dependencies.transactions.write {
            val request = dependencies.requests.get(requestId)
                ?: return@write DomainResult.failure(FollowRequestNotFound(requestId))
            when (val decision = dependencies.requestResolutionPolicy.canCancel(actorId, request)) {
                is PolicyDecision.Denied -> DomainResult.failure(decision.error)
                is PolicyDecision.Allowed -> when (decision.value) {
                    RequestResolutionPermission.AlreadyResolved -> DomainResult.success(
                        FollowRequestResolutionResult.AlreadyResolved(request),
                    )
                    RequestResolutionPermission.Resolve -> {
                        val resolved = request.resolve(FollowRequestStatus.Canceled, dependencies.clock.now())
                        dependencies.requests.update(resolved)
                        dependencies.audit(
                            type = AuditEventType.FollowRequestCanceled,
                            actorId = actorId,
                            subjectId = request.targetId,
                            metadata = mapOf("requestId" to request.id.value),
                        )
                        DomainResult.success(FollowRequestResolutionResult.Canceled(resolved))
                    }
                }
            }
        }
}
