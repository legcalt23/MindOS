package com.mindos.app.features.social.presentation.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mindos.app.features.social.domain.repository.ProfileVisitRepository
import com.mindos.app.features.social.domain.valueobject.UserId
import com.mindos.app.features.social.shared.util.AppClock

data class ProfileEntryUiState(
    val isPreparing: Boolean = true,
    val isOwner: Boolean = false,
    val initialPage: Int = 0,
    val hasRecordedVisit: Boolean = false,
    val showSettings: Boolean = false,
    val isPersistingVisit: Boolean = false,
    val errorMessage: String? = null,
)

@Stable
class ProfileEntryStateHolder(
    private val visitRepository: ProfileVisitRepository,
    private val clock: AppClock,
) {
    var uiState by mutableStateOf(ProfileEntryUiState())
        private set

    private var viewerId: UserId? = null
    private var ownerId: UserId? = null
    private var visitStampedForCurrentLoad: Boolean = false

    suspend fun load(
        viewerId: UserId,
        ownerId: UserId,
    ) {
        this.viewerId = viewerId
        this.ownerId = ownerId
        visitStampedForCurrentLoad = false
        val isOwner = viewerId == ownerId
        uiState = ProfileEntryUiState(isPreparing = true, isOwner = isOwner)
        if (isOwner) {
            visitStampedForCurrentLoad = true
            uiState = ProfileEntryUiState(
                isPreparing = false,
                isOwner = true,
                initialPage = 0,
                hasRecordedVisit = true,
            )
            return
        }

        runCatching {
            visitRepository.getVisit(viewerUserId = viewerId, profileOwnerId = ownerId)
        }.fold(
            onSuccess = { visit ->
                val hasVisited = visit?.hasVisitedProfile == true
                uiState = ProfileEntryUiState(
                    isPreparing = false,
                    isOwner = false,
                    initialPage = if (hasVisited) 1 else 0,
                    hasRecordedVisit = hasVisited,
                )
            },
            onFailure = { error ->
                uiState = ProfileEntryUiState(
                    isPreparing = false,
                    isOwner = false,
                    initialPage = 0,
                    hasRecordedVisit = false,
                    errorMessage = "No se pudo sincronizar la visita del perfil: ${error.message.orEmpty()}",
                )
            },
        )
    }

    suspend fun markVisitedIfNeeded() {
        val viewer = viewerId ?: return
        val owner = ownerId ?: return
        if (viewer == owner || visitStampedForCurrentLoad || uiState.isPersistingVisit) return
        uiState = uiState.copy(isPersistingVisit = true, errorMessage = null)
        runCatching {
            visitRepository.markVisited(
                viewerUserId = viewer,
                profileOwnerId = owner,
                visitedAt = clock.now(),
            )
        }.fold(
            onSuccess = {
                visitStampedForCurrentLoad = true
                uiState = uiState.copy(
                    hasRecordedVisit = true,
                    initialPage = 1,
                    isPersistingVisit = false,
                )
            },
            onFailure = { error ->
                uiState = uiState.copy(
                    isPersistingVisit = false,
                    errorMessage = "No se pudo registrar la visita: ${error.message.orEmpty()}",
                )
            },
        )
    }

    fun openSettings() {
        if (!uiState.isOwner) return
        uiState = uiState.copy(showSettings = true)
    }

    fun closeSettings() {
        uiState = uiState.copy(showSettings = false)
    }

    fun clearError() {
        if (uiState.errorMessage == null) return
        uiState = uiState.copy(errorMessage = null)
    }
}

@Composable
fun rememberProfileEntryStateHolder(
    visitRepository: ProfileVisitRepository,
    clock: AppClock,
): ProfileEntryStateHolder = remember(visitRepository, clock) {
    ProfileEntryStateHolder(
        visitRepository = visitRepository,
        clock = clock,
    )
}
