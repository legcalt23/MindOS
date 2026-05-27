package com.example.mindos.features.social.presentation.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.mindos.features.social.domain.model.InAppNotification
import com.example.mindos.features.social.domain.repository.InAppNotificationRepository
import com.example.mindos.features.social.domain.valueobject.UserId
import com.example.mindos.features.social.presentation.state.toUiMessage
import com.example.mindos.features.social.shared.util.DomainResult

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val items: List<InAppNotification> = emptyList(),
    val errorMessage: String? = null,
)

@Stable
class NotificationsStateHolder(
    private val repository: InAppNotificationRepository,
) {
    var uiState by mutableStateOf(NotificationsUiState())
        private set

    suspend fun load(recipientUserId: UserId) {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        uiState = when (val result = repository.listForRecipient(recipientUserId)) {
            is DomainResult.Success -> NotificationsUiState(items = result.value)
            is DomainResult.Failure -> uiState.copy(
                isLoading = false,
                errorMessage = result.error.toUiMessage(),
            )
        }
    }
}

@Composable
fun rememberNotificationsStateHolder(
    repository: InAppNotificationRepository,
): NotificationsStateHolder =
    remember(repository) {
        NotificationsStateHolder(repository)
    }

