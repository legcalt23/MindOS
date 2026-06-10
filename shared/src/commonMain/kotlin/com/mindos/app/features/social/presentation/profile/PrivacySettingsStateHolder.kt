package com.mindos.app.features.social.presentation.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mindos.app.features.social.domain.model.AudienceRule
import com.mindos.app.features.social.domain.model.PrivacySettings
import com.mindos.app.features.social.domain.model.ProfileVisibility
import com.mindos.app.features.social.domain.usecase.UpdatePrivacySettingsUseCase
import com.mindos.app.features.social.domain.valueobject.UserId
import com.mindos.app.features.social.presentation.state.PrivacySettingsUiState
import com.mindos.app.features.social.presentation.state.ProfileActionState
import com.mindos.app.features.social.presentation.state.toUiMessage
import com.mindos.app.features.social.shared.util.DomainResult

@Stable
class PrivacySettingsStateHolder(
    private val updatePrivacySettings: UpdatePrivacySettingsUseCase,
) {
    var uiState by mutableStateOf(PrivacySettingsUiState())
        private set

    fun load(settings: PrivacySettings) {
        uiState = PrivacySettingsUiState(settings = settings)
    }

    fun setProfileVisibility(visibility: ProfileVisibility) {
        mutate { it.copy(profileVisibility = visibility) }
    }

    fun setFollowersVisibility(rule: AudienceRule) {
        mutate { it.copy(followersVisibility = rule) }
    }

    fun setFollowingVisibility(rule: AudienceRule) {
        mutate { it.copy(followingVisibility = rule) }
    }

    fun setMessagePermission(rule: AudienceRule) {
        mutate { it.copy(messagePermission = rule) }
    }

    suspend fun save(actorId: UserId) {
        val settings = uiState.settings ?: return
        uiState = uiState.copy(actionState = ProfileActionState.Running)
        uiState = when (val result = updatePrivacySettings(actorId, settings)) {
            is DomainResult.Success -> PrivacySettingsUiState(
                settings = result.value,
                actionState = ProfileActionState.Succeeded("Privacy updated."),
            )
            is DomainResult.Failure -> uiState.copy(
                actionState = ProfileActionState.Failed(result.error.toUiMessage()),
            )
        }
    }

    private fun mutate(transform: (PrivacySettings) -> PrivacySettings) {
        val current = uiState.settings ?: return
        uiState = uiState.copy(settings = transform(current))
    }
}

@Composable
fun rememberPrivacySettingsStateHolder(
    updatePrivacySettings: UpdatePrivacySettingsUseCase,
): PrivacySettingsStateHolder =
    remember(updatePrivacySettings) {
        PrivacySettingsStateHolder(updatePrivacySettings)
    }

