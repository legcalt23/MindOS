package com.example.neuv3.features.social.presentation.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.neuv3.core.session.AuthenticatedUser
import com.example.neuv3.features.social.domain.feed.GlobalFeedPageSize
import com.example.neuv3.features.social.domain.feed.GlobalFeedPost
import com.example.neuv3.features.social.domain.feed.GlobalFeedRepository
import com.example.neuv3.features.social.shared.util.DomainResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class GlobalFeedUiState(
    val posts: List<GlobalFeedPost> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

@Stable
class GlobalFeedStateHolder(
    private val repository: GlobalFeedRepository,
    private val scope: CoroutineScope,
) {
    var uiState by mutableStateOf(GlobalFeedUiState())
        private set

    private var loadJob: Job? = null

    fun load() {
        if (uiState.isLoading) return
        loadJob?.cancel()
        loadJob = scope.launch {
            uiState = uiState.copy(isLoading = uiState.posts.isEmpty(), isRefreshing = uiState.posts.isNotEmpty(), errorMessage = null)
            when (val result = repository.loadGlobalFeed(GlobalFeedPageSize)) {
                is DomainResult.Success -> uiState = uiState.copy(posts = result.value, isLoading = false, isRefreshing = false, errorMessage = null)
                is DomainResult.Failure -> uiState = uiState.copy(isLoading = false, isRefreshing = false, errorMessage = result.error.message)
            }
        }
    }

    fun refresh() = load()
}

@Composable
fun rememberGlobalFeedStateHolder(
    user: AuthenticatedUser,
    repository: GlobalFeedRepository,
    scope: CoroutineScope,
): GlobalFeedStateHolder =
    remember(user.uid, repository, scope) { GlobalFeedStateHolder(repository = repository, scope = scope) }
