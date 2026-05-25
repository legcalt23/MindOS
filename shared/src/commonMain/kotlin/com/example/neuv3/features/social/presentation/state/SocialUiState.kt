package com.example.neuv3.features.social.presentation.state

import com.example.neuv3.features.social.domain.error.DomainError
import com.example.neuv3.features.social.domain.model.FollowRequest
import com.example.neuv3.features.social.domain.model.PrivacySettings
import com.example.neuv3.features.social.domain.model.ProfileVisualConfig
import com.example.neuv3.features.social.domain.model.UserProfile
import com.example.neuv3.features.social.domain.usecase.ProfileDetails
import com.example.neuv3.features.social.presentation.profile.ProfileEditorUiState

sealed interface ProfileActionState {
    data object Idle : ProfileActionState
    data object Running : ProfileActionState
    data class Succeeded(val message: String) : ProfileActionState
    data class Failed(val message: String) : ProfileActionState
}

data class ProfileUiState(
    val isLoading: Boolean = false,
    val details: ProfileDetails? = null,
    val visualConfig: ProfileVisualConfig? = null,
    val editor: ProfileEditorUiState = ProfileEditorUiState(),
    val actionState: ProfileActionState = ProfileActionState.Idle,
    val errorMessage: String? = null,
)

data class SocialGraphUiState(
    val isLoading: Boolean = false,
    val followers: List<UserProfile> = emptyList(),
    val following: List<UserProfile> = emptyList(),
    val errorMessage: String? = null,
)

data class FollowRequestsUiState(
    val isLoading: Boolean = false,
    val incoming: List<FollowRequest> = emptyList(),
    val outgoing: List<FollowRequest> = emptyList(),
    val actionState: ProfileActionState = ProfileActionState.Idle,
    val errorMessage: String? = null,
)

data class PrivacySettingsUiState(
    val isLoading: Boolean = false,
    val settings: PrivacySettings? = null,
    val actionState: ProfileActionState = ProfileActionState.Idle,
    val errorMessage: String? = null,
)

fun DomainError.toUiMessage(): String = message
