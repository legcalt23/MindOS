package com.example.mindos.features.social.domain.model

import com.example.mindos.features.social.domain.valueobject.Timestamp
import com.example.mindos.features.social.domain.valueobject.UserId

enum class ProfileVisibility {
    Public,
    Private,
}

enum class AudienceRule {
    Everyone,
    Followers,
    Mutuals,
    OnlySelf,
}

data class PrivacySettings(
    val userId: UserId,
    val profileVisibility: ProfileVisibility = ProfileVisibility.Public,
    val followersVisibility: AudienceRule = AudienceRule.Everyone,
    val followingVisibility: AudienceRule = AudienceRule.Everyone,
    val messagePermission: AudienceRule = AudienceRule.Mutuals,
    val allowMentionsFrom: AudienceRule = AudienceRule.Followers,
    val discoverable: Boolean = true,
    val showActivityStatus: Boolean = false,
    val updatedAt: Timestamp = Timestamp.Zero,
)

data class ProfileCounters(
    val userId: UserId,
    val followersCount: Int,
    val followingCount: Int,
    val incomingPendingRequestsCount: Int,
    val outgoingPendingRequestsCount: Int,
    val blockedUsersCount: Int,
    val derivedAt: Timestamp,
) {
    init {
        require(followersCount >= 0) { "followersCount cannot be negative." }
        require(followingCount >= 0) { "followingCount cannot be negative." }
        require(incomingPendingRequestsCount >= 0) { "incomingPendingRequestsCount cannot be negative." }
        require(outgoingPendingRequestsCount >= 0) { "outgoingPendingRequestsCount cannot be negative." }
        require(blockedUsersCount >= 0) { "blockedUsersCount cannot be negative." }
    }
}

