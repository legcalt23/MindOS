package com.example.neuv3.features.social.presentation.social

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.neuv3.features.social.domain.usecase.AcceptFollowRequestUseCase
import com.example.neuv3.features.social.domain.usecase.CancelFollowRequestUseCase
import com.example.neuv3.features.social.domain.usecase.DenyFollowRequestUseCase
import com.example.neuv3.features.social.domain.usecase.ListIncomingFollowRequestsUseCase
import com.example.neuv3.features.social.domain.usecase.ListOutgoingFollowRequestsUseCase
import com.example.neuv3.features.social.domain.valueobject.RequestId
import com.example.neuv3.features.social.domain.valueobject.UserId
import com.example.neuv3.features.social.presentation.state.FollowRequestsUiState
import com.example.neuv3.features.social.presentation.state.ProfileActionState
import com.example.neuv3.features.social.presentation.state.toUiMessage
import com.example.neuv3.features.social.shared.util.DomainResult

@Stable
class FollowRequestsStateHolder(
    private val listIncoming: ListIncomingFollowRequestsUseCase,
    private val listOutgoing: ListOutgoingFollowRequestsUseCase,
    private val acceptRequest: AcceptFollowRequestUseCase,
    private val denyRequest: DenyFollowRequestUseCase,
    private val cancelRequest: CancelFollowRequestUseCase,
) {
    var uiState by mutableStateOf(FollowRequestsUiState())
        private set

    private var actorId: UserId? = null

    suspend fun load(actorId: UserId) {
        this.actorId = actorId
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        val incoming = listIncoming(actorId)
        val outgoing = listOutgoing(actorId)
        uiState = when {
            incoming is DomainResult.Failure -> FollowRequestsUiState(errorMessage = incoming.error.toUiMessage())
            outgoing is DomainResult.Failure -> FollowRequestsUiState(errorMessage = outgoing.error.toUiMessage())
            incoming is DomainResult.Success && outgoing is DomainResult.Success -> FollowRequestsUiState(
                incoming = incoming.value.items,
                outgoing = outgoing.value.items,
            )
            else -> FollowRequestsUiState(errorMessage = "Unable to load follow requests.")
        }
    }

    suspend fun accept(requestId: RequestId) =
        runAction("Request accepted.") { actor -> acceptRequest(actor, requestId) }

    suspend fun deny(requestId: RequestId) =
        runAction("Request denied.") { actor -> denyRequest(actor, requestId) }

    suspend fun cancel(requestId: RequestId) =
        runAction("Request canceled.") { actor -> cancelRequest(actor, requestId) }

    private suspend fun runAction(
        successMessage: String,
        action: suspend (UserId) -> DomainResult<*>,
    ) {
        val actor = actorId ?: return
        uiState = uiState.copy(actionState = ProfileActionState.Running)
        uiState = when (val result = action(actor)) {
            is DomainResult.Success -> uiState.copy(actionState = ProfileActionState.Succeeded(successMessage))
            is DomainResult.Failure -> uiState.copy(actionState = ProfileActionState.Failed(result.error.toUiMessage()))
        }
        load(actor)
    }
}

@Composable
fun rememberFollowRequestsStateHolder(
    listIncoming: ListIncomingFollowRequestsUseCase,
    listOutgoing: ListOutgoingFollowRequestsUseCase,
    acceptRequest: AcceptFollowRequestUseCase,
    denyRequest: DenyFollowRequestUseCase,
    cancelRequest: CancelFollowRequestUseCase,
): FollowRequestsStateHolder =
    remember(listIncoming, listOutgoing, acceptRequest, denyRequest, cancelRequest) {
        FollowRequestsStateHolder(listIncoming, listOutgoing, acceptRequest, denyRequest, cancelRequest)
    }

