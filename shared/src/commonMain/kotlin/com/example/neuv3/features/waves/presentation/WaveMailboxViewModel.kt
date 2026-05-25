package com.example.neuv3.features.waves.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.neuv3.features.waves.domain.service.WaveRequestService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WaveMailboxUiState(
    val pendingCount: Int = 0,
    val hasPendingRequests: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
)

class WaveMailboxViewModel(
    private val service: WaveRequestService,
    private val currentUserId: String,
    private val scope: CoroutineScope,
) {
    private val _uiState = MutableStateFlow(WaveMailboxUiState())
    val uiState: StateFlow<WaveMailboxUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        observeMailbox()
    }

    fun onMailboxClick() {
        // Reserved for wiring the future requests screen without moving navigation into the composable.
    }

    private fun observeMailbox() {
        observeJob?.cancel()
        _uiState.value = WaveMailboxUiState(isLoading = true)
        observeJob = scope.launch {
            try {
                service.observePendingRequestCountReceived(currentUserId).collect { count ->
                    _uiState.update {
                        it.copy(
                            pendingCount = count,
                            hasPendingRequests = count > 0,
                            isLoading = false,
                            error = null,
                        )
                    }
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message?.takeIf { message -> message.isNotBlank() } ?: "No se pudo cargar el buzón.",
                    )
                }
            }
        }
    }
}

@Composable
fun rememberWaveMailboxViewModel(
    service: WaveRequestService,
    currentUserId: String,
    scope: CoroutineScope = rememberCoroutineScope(),
): WaveMailboxViewModel =
    remember(service, currentUserId, scope) {
        WaveMailboxViewModel(service, currentUserId, scope)
    }
