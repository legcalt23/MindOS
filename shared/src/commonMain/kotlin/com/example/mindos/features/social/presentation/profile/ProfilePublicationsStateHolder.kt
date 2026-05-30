package com.example.mindos.features.social.presentation.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.mindos.features.social.domain.feed.GlobalFeedPost
import com.example.mindos.features.social.domain.feed.GlobalFeedRepository
import com.example.mindos.features.social.shared.util.DomainResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class ProfilePublicationsUiState(
    val posts: List<GlobalFeedPost> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@Stable
class ProfilePublicationsStateHolder(
    private val repository: GlobalFeedRepository,
    private val scope: CoroutineScope,
) {
    var uiState by mutableStateOf(ProfilePublicationsUiState())
        private set

    private var currentOwnerId: String? = null
    private var loadJob: Job? = null

    fun load(ownerId: String, limit: Int = 9) {
        if (ownerId.isBlank()) return
        val ownerChanged = currentOwnerId != ownerId
        currentOwnerId = ownerId
        loadJob?.cancel()
        loadJob = scope.launch {
            uiState = if (ownerChanged) {
                ProfilePublicationsUiState(isLoading = true)
            } else {
                uiState.copy(
                    isLoading = uiState.posts.isEmpty(),
                    errorMessage = null,
                )
            }
            when (val result = repository.loadPostsByUser(ownerId, limit)) {
                is DomainResult.Success -> uiState = ProfilePublicationsUiState(
                    posts = result.value.sortedByDescending { it.createdAtMillis }.take(limit),
                )
                is DomainResult.Failure -> uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = result.error.message,
                )
            }
        }
    }

    fun onPostPublished(post: GlobalFeedPost, limit: Int = 9) {
        if (post.userId != currentOwnerId) return
        uiState = uiState.copy(
            posts = listOf(post) + uiState.posts.filterNot { it.id == post.id }
                .sortedByDescending { it.createdAtMillis }
                .take((limit - 1).coerceAtLeast(0)),
            isLoading = false,
            errorMessage = null,
        )
    }
}

@Composable
fun rememberProfilePublicationsStateHolder(
    repository: GlobalFeedRepository,
    scope: CoroutineScope,
): ProfilePublicationsStateHolder =
    remember(repository, scope) {
        ProfilePublicationsStateHolder(
            repository = repository,
            scope = scope,
        )
    }
