package com.example.mindos.features.social.domain.usecase

import com.example.mindos.features.social.domain.error.ActionNotAuthorized
import com.example.mindos.features.social.domain.model.AuditEventType
import com.example.mindos.features.social.domain.model.BlockRelation
import com.example.mindos.features.social.domain.model.PrivacySettings
import com.example.mindos.features.social.domain.policy.BlockPermission
import com.example.mindos.features.social.domain.policy.PolicyDecision
import com.example.mindos.features.social.domain.valueobject.UserId
import com.example.mindos.features.social.shared.util.DomainResult

sealed interface BlockUserResult {
    data class Blocked(
        val relation: BlockRelation,
        val removedFollowCount: Int,
        val invalidatedRequestCount: Int,
        val alreadyBlocked: Boolean,
    ) : BlockUserResult
}

sealed interface UnblockUserResult {
    data class Unblocked(val relation: BlockRelation) : UnblockUserResult
    data object WasNotBlocked : UnblockUserResult
}

class BlockUserUseCase(
    private val dependencies: SocialGraphDependencies,
) {
    suspend operator fun invoke(actorId: UserId, targetId: UserId): DomainResult<BlockUserResult> =
        dependencies.transactions.write {
            dependencies.requireParticipant(actorId).ifFailure { return@write it }
            dependencies.requireParticipant(targetId).ifFailure { return@write it }

            val facts = dependencies.collectFacts(actorId, targetId)
            when (val decision = dependencies.blockPolicy.evaluateBlock(actorId, targetId, facts)) {
                is PolicyDecision.Denied -> DomainResult.failure(decision.error)
                is PolicyDecision.Allowed -> {
                    val relation = facts.blockedByViewer ?: BlockRelation(actorId, targetId, dependencies.clock.now())
                    val inserted = when (decision.value) {
                        BlockPermission.AlreadyBlocked -> false
                        BlockPermission.CreateBlock -> dependencies.blocks.upsert(relation)
                    }
                    val removedFollows = dependencies.follows.deleteBothDirections(actorId, targetId)
                    val invalidatedRequests = dependencies.requests.invalidatePendingBetweenBothDirections(
                        firstUserId = actorId,
                        secondUserId = targetId,
                        at = dependencies.clock.now(),
                    )

                    if (inserted) {
                        dependencies.audit(
                            type = AuditEventType.BlockCreated,
                            actorId = actorId,
                            subjectId = targetId,
                            metadata = mapOf(
                                "removedFollowCount" to removedFollows.toString(),
                                "invalidatedRequestCount" to invalidatedRequests.size.toString(),
                            ),
                        )
                    }
                    invalidatedRequests.forEach { request ->
                        dependencies.audit(
                            type = AuditEventType.FollowRequestInvalidated,
                            actorId = actorId,
                            subjectId = request.targetId,
                            metadata = mapOf("requestId" to request.id.value),
                        )
                    }

                    DomainResult.success(
                        BlockUserResult.Blocked(
                            relation = relation,
                            removedFollowCount = removedFollows,
                            invalidatedRequestCount = invalidatedRequests.size,
                            alreadyBlocked = !inserted,
                        ),
                    )
                }
            }
        }
}

class UnblockUserUseCase(
    private val dependencies: SocialGraphDependencies,
) {
    suspend operator fun invoke(actorId: UserId, targetId: UserId): DomainResult<UnblockUserResult> =
        dependencies.transactions.write {
            when (val decision = dependencies.blockPolicy.evaluateUnblock(actorId, targetId)) {
                is PolicyDecision.Denied -> DomainResult.failure(decision.error)
                is PolicyDecision.Allowed -> {
                    val existing = dependencies.blocks.get(actorId, targetId)
                        ?: return@write DomainResult.success(UnblockUserResult.WasNotBlocked)
                    dependencies.blocks.delete(actorId, targetId)
                    dependencies.audit(AuditEventType.BlockRemoved, actorId, targetId)
                    DomainResult.success(UnblockUserResult.Unblocked(existing))
                }
            }
        }
}

class UpdatePrivacySettingsUseCase(
    private val dependencies: SocialGraphDependencies,
) {
    suspend operator fun invoke(actorId: UserId, settings: PrivacySettings): DomainResult<PrivacySettings> =
        dependencies.transactions.write {
            if (actorId != settings.userId) {
                return@write DomainResult.failure(
                    ActionNotAuthorized("Users can only update their own privacy settings."),
                )
            }
            dependencies.requireParticipant(actorId).ifFailure { return@write it }
            val updated = settings.copy(updatedAt = dependencies.clock.now())
            dependencies.privacy.save(updated)
            dependencies.audit(
                type = AuditEventType.PrivacyUpdated,
                actorId = actorId,
                subjectId = actorId,
                metadata = mapOf("profileVisibility" to updated.profileVisibility.name),
            )
            DomainResult.success(updated)
        }
}

