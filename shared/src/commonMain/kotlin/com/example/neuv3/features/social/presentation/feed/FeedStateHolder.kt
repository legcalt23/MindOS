package com.example.neuv3.features.social.presentation.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.neuv3.features.social.domain.publishing.FeedItem
import com.example.neuv3.features.social.domain.publishing.GetFeedUseCase
import com.example.neuv3.features.social.domain.valueobject.Cursor
import com.example.neuv3.features.social.domain.valueobject.UserId
import com.example.neuv3.features.social.presentation.publisher.PublisherOptimisticPost
import com.example.neuv3.features.social.shared.util.DomainResult
import com.example.neuv3.features.social.shared.util.PageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

interface FeedLocalStore {
    suspend fun savePage(viewerId: UserId, items: List<FeedItem>, clear: Boolean)
    suspend fun loadCachedFeed(viewerId: UserId): List<FeedItem>
    fun observeCachedFeed(viewerId: UserId): Flow<List<FeedItem>>
    suspend fun clear(viewerId: UserId)
}

class InMemoryFeedLocalStore : FeedLocalStore {
    private val cache = mutableMapOf<UserId, List<FeedItem>>()
    private val flows = mutableMapOf<UserId, MutableStateFlow<List<FeedItem>>>()

    override suspend fun savePage(viewerId: UserId, items: List<FeedItem>, clear: Boolean) {
        val current = if (clear) emptyList() else cache[viewerId] ?: emptyList()
        val combined = (current + items).distinctBy { it.postId }
        cache[viewerId] = combined
        getFlow(viewerId).value = combined
    }

    override suspend fun loadCachedFeed(viewerId: UserId): List<FeedItem> {
        return cache[viewerId] ?: emptyList()
    }

    override fun observeCachedFeed(viewerId: UserId): Flow<List<FeedItem>> {
        return getFlow(viewerId)
    }

    override suspend fun clear(viewerId: UserId) {
        cache.remove(viewerId)
        getFlow(viewerId).value = emptyList()
    }

    private fun getFlow(viewerId: UserId): MutableStateFlow<List<FeedItem>> {
        return flows.getOrPut(viewerId) { MutableStateFlow(emptyList()) }
    }
}

data class FeedUiState(
    val items: List<FeedItem> = emptyList(),
    val optimisticItems: List<PublisherOptimisticPost> = emptyList(),
    val isLoading: Boolean = false,
    val isPaginating: Boolean = false,
    val reachedEnd: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null,
)

@Stable
class FeedStateHolder(
    private val viewerId: UserId,
    private val getFeed: GetFeedUseCase,
    private val localStore: FeedLocalStore,
    private val scope: CoroutineScope,
) {
    var uiState by mutableStateOf(FeedUiState())
        private set

    private var nextCursor: Cursor? = null
    private var loadJob: Job? = null

    init {
        localStore.observeCachedFeed(viewerId).onEach { cached ->
            if (!uiState.isLoading && uiState.items.isEmpty()) {
                uiState = uiState.copy(items = cached)
            }
        }.launchIn(scope)
    }

    fun loadInitial() {
        if (uiState.isLoading || uiState.refreshing) return

        loadJob?.cancel()
        loadJob = scope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            val cached = localStore.loadCachedFeed(viewerId)
            if (cached.isNotEmpty()) {
                uiState = uiState.copy(items = cached, isLoading = false)
            }
            fetchPage(clear = true)
        }
    }

    fun refresh() {
        if (uiState.refreshing) return

        loadJob?.cancel()
        loadJob = scope.launch {
            uiState = uiState.copy(refreshing = true, error = null)
            fetchPage(clear = true)
        }
    }

    fun loadMore() {
        if (uiState.isLoading || uiState.isPaginating || uiState.refreshing || uiState.reachedEnd) return
        if (nextCursor == null) return

        loadJob = scope.launch {
            uiState = uiState.copy(isPaginating = true, error = null)
            fetchPage(clear = false, cursor = nextCursor)
        }
    }

    fun updateOptimisticPosts(posts: List<PublisherOptimisticPost>) {
        val feedPostIds = uiState.items.map { it.postId }.toSet()
        val filtered = posts.filter { it.postId !in feedPostIds }
        uiState = uiState.copy(optimisticItems = filtered)
    }

    private suspend fun fetchPage(clear: Boolean, cursor: Cursor? = null) {
        val request = PageRequest(limit = 20, cursor = cursor)
        when (val result = getFeed.getFeed(viewerId, request)) {
            is DomainResult.Success -> {
                val page = result.value
                val newItems = page.items
                nextCursor = page.nextCursor

                localStore.savePage(viewerId, newItems, clear = clear)
                val allItems = localStore.loadCachedFeed(viewerId)

                val feedPostIds = allItems.map { it.postId }.toSet()
                val remainingOptimistic = uiState.optimisticItems.filter { it.postId !in feedPostIds }

                uiState = uiState.copy(
                    items = allItems,
                    optimisticItems = remainingOptimistic,
                    isLoading = false,
                    isPaginating = false,
                    refreshing = false,
                    reachedEnd = newItems.isEmpty() || page.nextCursor == null,
                    error = null
                )
            }
            is DomainResult.Failure -> {
                uiState = uiState.copy(
                    isLoading = false,
                    isPaginating = false,
                    refreshing = false,
                    error = result.error.message
                )
            }
        }
    }
}

@Composable
fun rememberFeedLocalStore(): FeedLocalStore {
    return remember { InMemoryFeedLocalStore() }
}

@Composable
fun rememberFeedStateHolder(
    viewerId: UserId,
    getFeed: GetFeedUseCase,
    localStore: FeedLocalStore,
    scope: CoroutineScope,
): FeedStateHolder {
    return remember(viewerId, getFeed, localStore, scope) {
        FeedStateHolder(viewerId, getFeed, localStore, scope)
    }
}
