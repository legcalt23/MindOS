package com.example.mindos.features.social.domain.model

import com.example.mindos.features.social.domain.valueobject.Bio
import com.example.mindos.features.social.domain.valueobject.DisplayName
import com.example.mindos.features.social.domain.valueobject.Handle
import com.example.mindos.features.social.domain.valueobject.ProfileId
import com.example.mindos.features.social.domain.valueobject.Timestamp
import com.example.mindos.features.social.domain.valueobject.UserId

enum class AccountStatus {
    Active,
    Suspended,
    Deleted,
}

data class UserAccount(
    val id: UserId,
    val handle: Handle,
    val status: AccountStatus = AccountStatus.Active,
    val createdAt: Timestamp,
    val updatedAt: Timestamp = createdAt,
) {
    val canParticipateInSocialGraph: Boolean
        get() = status == AccountStatus.Active
}

data class UserProfile(
    val id: ProfileId,
    val userId: UserId,
    val handle: Handle,
    val displayName: DisplayName,
    val bio: Bio = Bio.Empty,
    val avatarUrl: String? = null,
    val isVerified: Boolean = false,
    val createdAt: Timestamp,
    val updatedAt: Timestamp = createdAt,
    val usernameHistory: List<UsernameHistoryEntry> = emptyList(),
) {
    init {
        require(id == ProfileId.fromUserId(userId)) {
            "This profile model intentionally uses UserId as the canonical profile identity."
        }
    }
}

data class UsernameHistoryEntry(
    val previousHandle: Handle,
    val newHandle: Handle,
    val changedAt: Timestamp,
    val changedBy: UserId,
)

data class VisibleUserProfile(
    val userId: UserId,
    val handle: Handle,
    val displayName: DisplayName,
    val bio: Bio?,
    val avatarUrl: String?,
    val isVerified: Boolean,
    val isPrivate: Boolean,
    val usernameHistory: List<UsernameHistoryEntry>,
)

fun UserProfile.toVisibleProfile(
    canViewFullProfile: Boolean,
    isPrivate: Boolean,
): VisibleUserProfile =
    VisibleUserProfile(
        userId = userId,
        handle = handle,
        displayName = displayName,
        bio = if (canViewFullProfile) bio else null,
        avatarUrl = avatarUrl,
        isVerified = isVerified,
        isPrivate = isPrivate,
        usernameHistory = if (canViewFullProfile) usernameHistory else emptyList(),
    )

