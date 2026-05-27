package com.example.mindos.features.social.domain.error

import com.example.mindos.features.social.domain.valueobject.RequestId
import com.example.mindos.features.social.domain.valueobject.UserId

sealed interface DomainError {
    val code: String
    val message: String
}

data class InvalidValue(
    val field: String,
    val reason: String,
) : DomainError {
    override val code: String = "invalid_value"
    override val message: String = "$field is invalid: $reason"
}

data class UserNotFound(
    val userId: UserId,
) : DomainError {
    override val code: String = "user_not_found"
    override val message: String = "User ${userId.value} was not found."
}

data class ProfileNotFound(
    val userId: UserId,
) : DomainError {
    override val code: String = "profile_not_found"
    override val message: String = "Profile for user ${userId.value} was not found."
}

data class FollowRequestNotFound(
    val requestId: RequestId,
) : DomainError {
    override val code: String = "follow_request_not_found"
    override val message: String = "Follow request ${requestId.value} was not found."
}

data object SelfFollowNotAllowed : DomainError {
    override val code: String = "self_follow_not_allowed"
    override val message: String = "A user cannot follow themselves."
}

data object SelfFollowRequestNotAllowed : DomainError {
    override val code: String = "self_follow_request_not_allowed"
    override val message: String = "A user cannot request to follow themselves."
}

data object SelfBlockNotAllowed : DomainError {
    override val code: String = "self_block_not_allowed"
    override val message: String = "A user cannot block themselves."
}

data class BlockedRelationship(
    val actorId: UserId,
    val targetId: UserId,
) : DomainError {
    override val code: String = "blocked_relationship"
    override val message: String = "The relationship between ${actorId.value} and ${targetId.value} is blocked."
}

data class ActionNotAuthorized(
    override val message: String,
) : DomainError {
    override val code: String = "action_not_authorized"
}

data class ProfileVisibilityDenied(
    val viewerId: UserId,
    val ownerId: UserId,
    val surface: String,
) : DomainError {
    override val code: String = "profile_visibility_denied"
    override val message: String = "User ${viewerId.value} cannot view $surface for ${ownerId.value}."
}

data object ActiveFollowAlreadyExists : DomainError {
    override val code: String = "active_follow_already_exists"
    override val message: String = "An active follow already exists for this ordered pair."
}

data object PendingFollowRequestAlreadyExists : DomainError {
    override val code: String = "pending_follow_request_already_exists"
    override val message: String = "A pending follow request already exists for this ordered pair."
}

data object FollowAndPendingRequestConflict : DomainError {
    override val code: String = "follow_and_pending_request_conflict"
    override val message: String = "An active follow and a pending request cannot coexist for the same ordered pair."
}

data class RequestAlreadyResolved(
    val requestId: RequestId,
) : DomainError {
    override val code: String = "request_already_resolved"
    override val message: String = "Follow request ${requestId.value} is already resolved."
}

data class IntegrityViolation(
    override val message: String,
) : DomainError {
    override val code: String = "integrity_violation"
}

data class PersistenceFailure(
    override val message: String,
) : DomainError {
    override val code: String = "persistence_failure"
}

