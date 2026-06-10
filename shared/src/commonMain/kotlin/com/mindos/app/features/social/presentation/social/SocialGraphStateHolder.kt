package com.mindos.app.features.social.presentation.social

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mindos.app.features.social.domain.usecase.ListFollowersUseCase
import com.mindos.app.features.social.domain.usecase.ListFollowingUseCase
import com.mindos.app.features.social.domain.valueobject.UserId
import com.mindos.app.features.social.presentation.state.SocialGraphUiState
import com.mindos.app.features.social.presentation.state.toUiMessage
import com.mindos.app.features.social.shared.util.DomainResult

@Stable
class SocialGraphStateHolder(
    private val listFollowers: ListFollowersUseCase,
    private val listFollowing: ListFollowingUseCase,
) {
    var uiState by mutableStateOf(SocialGraphUiState())
        private set

    suspend fun load(viewerId: UserId, ownerId: UserId) {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        val followers = listFollowers(viewerId, ownerId)
        val following = listFollowing(viewerId, ownerId)
        uiState = when {
            followers is DomainResult.Failure -> SocialGraphUiState(errorMessage = followers.error.toUiMessage())
            following is DomainResult.Failure -> SocialGraphUiState(errorMessage = following.error.toUiMessage())
            followers is DomainResult.Success && following is DomainResult.Success -> SocialGraphUiState(
                followers = followers.value.items,
                following = following.value.items,
            )
            else -> SocialGraphUiState(errorMessage = "Unable to load social graph.")
        }
    }
}

@Composable
fun rememberSocialGraphStateHolder(
    listFollowers: ListFollowersUseCase,
    listFollowing: ListFollowingUseCase,
): SocialGraphStateHolder =
    remember(listFollowers, listFollowing) {
        SocialGraphStateHolder(listFollowers, listFollowing)
    }

