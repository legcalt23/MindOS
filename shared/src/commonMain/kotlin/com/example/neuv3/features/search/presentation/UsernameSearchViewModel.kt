package com.example.neuv3.features.search.presentation

import com.example.neuv3.features.search.domain.UsernameSearchMinQueryLength
import com.example.neuv3.features.search.domain.UsernameSearchRepository
import com.example.neuv3.features.search.domain.UsernameSearchUser
import com.example.neuv3.features.search.domain.normalizeUsernameSearchQuery
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class UsernameSearchMode {
    Radar,
    ZeroUi,
}

sealed interface UsernameSearchStatus {
    data object Latent : UsernameSearchStatus
    data object Scanning : UsernameSearchStatus
    data object Ready : UsernameSearchStatus
    data object Empty : UsernameSearchStatus
    data class Failed(val message: String) : UsernameSearchStatus
}

data class UsernameSearchUiState(
    val query: String = "",
    val normalizedQuery: String = "",
    val mode: UsernameSearchMode = UsernameSearchMode.Radar,
    val status: UsernameSearchStatus = UsernameSearchStatus.Latent,
    val results: List<UsernameSearchUser> = emptyList(),
    val selectedUserId: String? = null,
    val selectionMessage: String? = null,
) {
    val canSearch: Boolean
        get() = normalizedQuery.length >= UsernameSearchMinQueryLength
}

@OptIn(FlowPreview::class)
class UsernameSearchViewModel(
    private val repository: UsernameSearchRepository,
    private val scope: CoroutineScope,
) {
    private val queryRequests = MutableStateFlow("")
    private val _uiState = MutableStateFlow(UsernameSearchUiState())

    val uiState: StateFlow<UsernameSearchUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            queryRequests
                .map(::normalizeUsernameSearchQuery)
                .distinctUntilChanged()
                .debounce(UsernameSearchDebounceMillis)
                .collectLatest(::search)
        }
    }

    fun onQueryChanged(input: String) {
        val clipped = input.take(42)
        val normalized = normalizeUsernameSearchQuery(clipped)
        _uiState.update { current ->
            current.copy(
                query = clipped,
                normalizedQuery = normalized,
                status = if (normalized.length >= UsernameSearchMinQueryLength) {
                    UsernameSearchStatus.Scanning
                } else {
                    UsernameSearchStatus.Latent
                },
                results = if (normalized.length >= UsernameSearchMinQueryLength) current.results else emptyList(),
                selectedUserId = null,
                selectionMessage = null,
            )
        }
        queryRequests.value = clipped
    }

    fun onModeChanged(mode: UsernameSearchMode) {
        _uiState.update { current -> current.copy(mode = mode) }
    }

    fun select(user: UsernameSearchUser, isCurrentUser: Boolean) {
        _uiState.update { current ->
            current.copy(
                selectedUserId = user.userId,
                selectionMessage = if (isCurrentUser) {
                    "Perfil propio enlazado."
                } else {
                    "Perfil remoto enlazado."
                },
            )
        }
    }

    private suspend fun search(normalizedQuery: String) {
        if (normalizedQuery.length < UsernameSearchMinQueryLength) {
            _uiState.update { current ->
                current.copy(
                    normalizedQuery = normalizedQuery,
                    status = UsernameSearchStatus.Latent,
                    results = emptyList(),
                    selectedUserId = null,
                    selectionMessage = null,
                )
            }
            return
        }

        _uiState.update { current ->
            current.copy(
                normalizedQuery = normalizedQuery,
                status = UsernameSearchStatus.Scanning,
                selectionMessage = null,
            )
        }

        try {
            val matches = repository.searchByUsername(normalizedQuery)
            _uiState.update { current ->
                current.copy(
                    status = if (matches.isEmpty()) UsernameSearchStatus.Empty else UsernameSearchStatus.Ready,
                    results = matches,
                    selectedUserId = null,
                )
            }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            _uiState.update { current ->
                current.copy(
                    status = UsernameSearchStatus.Failed(
                        message = error.message?.takeIf { it.isNotBlank() }
                            ?: "No se pudo completar la busqueda.",
                    ),
                    results = emptyList(),
                    selectedUserId = null,
                )
            }
        }
    }
}

private const val UsernameSearchDebounceMillis = 320L
