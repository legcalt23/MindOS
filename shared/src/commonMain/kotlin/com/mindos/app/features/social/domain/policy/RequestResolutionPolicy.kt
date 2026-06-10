package com.mindos.app.features.social.domain.policy

import com.mindos.app.features.social.domain.error.ActionNotAuthorized
import com.mindos.app.features.social.domain.error.BlockedRelationship
import com.mindos.app.features.social.domain.error.RequestAlreadyResolved
import com.mindos.app.features.social.domain.model.FollowRequest
import com.mindos.app.features.social.domain.model.FollowRequestStatus
import com.mindos.app.features.social.domain.model.RelationshipFacts
import com.mindos.app.features.social.domain.valueobject.UserId

sealed interface RequestResolutionPermission {
    data object Resolve : RequestResolutionPermission
    data object AlreadyResolved : RequestResolutionPermission
}

class RequestResolutionPolicy {
    fun canAccept(
        actorId: UserId,
        request: FollowRequest,
        facts: RelationshipFacts,
    ): PolicyDecision<RequestResolutionPermission> =
        resolveByTarget(actorId, request, facts, idempotentStatus = FollowRequestStatus.Accepted)

    fun canDeny(
        actorId: UserId,
        request: FollowRequest,
        facts: RelationshipFacts,
    ): PolicyDecision<RequestResolutionPermission> =
        resolveByTarget(actorId, request, facts, idempotentStatus = FollowRequestStatus.Denied)

    fun canCancel(
        actorId: UserId,
        request: FollowRequest,
    ): PolicyDecision<RequestResolutionPermission> =
        when {
            actorId != request.requesterId -> PolicyDecision.Denied(
                ActionNotAuthorized("Only the requester can cancel a follow request."),
            )
            request.status == FollowRequestStatus.Canceled -> PolicyDecision.Allowed(
                RequestResolutionPermission.AlreadyResolved,
            )
            request.status != FollowRequestStatus.Pending -> PolicyDecision.Denied(RequestAlreadyResolved(request.id))
            else -> PolicyDecision.Allowed(RequestResolutionPermission.Resolve)
        }

    private fun resolveByTarget(
        actorId: UserId,
        request: FollowRequest,
        facts: RelationshipFacts,
        idempotentStatus: FollowRequestStatus,
    ): PolicyDecision<RequestResolutionPermission> =
        when {
            actorId != request.targetId -> PolicyDecision.Denied(
                ActionNotAuthorized("Only the target user can resolve an incoming follow request."),
            )
            request.status == idempotentStatus -> PolicyDecision.Allowed(
                RequestResolutionPermission.AlreadyResolved,
            )
            request.status != FollowRequestStatus.Pending -> PolicyDecision.Denied(RequestAlreadyResolved(request.id))
            facts.hasAnyBlock -> PolicyDecision.Denied(BlockedRelationship(request.requesterId, request.targetId))
            else -> PolicyDecision.Allowed(RequestResolutionPermission.Resolve)
        }
}

