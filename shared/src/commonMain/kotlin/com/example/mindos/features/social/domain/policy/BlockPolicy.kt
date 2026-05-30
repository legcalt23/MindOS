package com.example.mindos.features.social.domain.policy

import com.example.mindos.features.social.domain.error.SelfBlockNotAllowed
import com.example.mindos.features.social.domain.model.RelationshipFacts
import com.example.mindos.features.social.domain.valueobject.UserId

sealed interface BlockPermission {
    data object CreateBlock : BlockPermission
    data object AlreadyBlocked : BlockPermission
}

class BlockPolicy {
    fun evaluateBlock(
        actorId: UserId,
        targetId: UserId,
        facts: RelationshipFacts,
    ): PolicyDecision<BlockPermission> =
        when {
            actorId == targetId -> PolicyDecision.Denied(SelfBlockNotAllowed)
            facts.blockedByViewer != null -> PolicyDecision.Allowed(BlockPermission.AlreadyBlocked)
            else -> PolicyDecision.Allowed(BlockPermission.CreateBlock)
        }

    fun evaluateUnblock(
        actorId: UserId,
        targetId: UserId,
    ): PolicyDecision<Unit> =
        if (actorId == targetId) {
            PolicyDecision.Denied(SelfBlockNotAllowed)
        } else {
            PolicyDecision.Allowed(Unit)
        }
}

