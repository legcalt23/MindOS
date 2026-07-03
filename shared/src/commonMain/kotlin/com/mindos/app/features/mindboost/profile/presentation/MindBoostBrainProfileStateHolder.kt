package com.mindos.app.features.mindboost.profile.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mindos.app.features.mindboost.profile.domain.application.MindBoostProfileService
import com.mindos.app.features.mindboost.profile.presentation.state.BrainProfileUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Drives the brain-profile screen. Shows the last cached snapshot instantly (if any), then recomputes
 * from live module signals. Pure presentation: all gating/aggregation lives in the domain; this only
 * loads and maps to the [BrainProfileUiModel].
 */
class MindBoostBrainProfileStateHolder(
    private val service: MindBoostProfileService,
    private val userId: String,
    private val scope: CoroutineScope,
) {
    var uiModel by mutableStateOf<BrainProfileUiModel?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    /** Loads the cached snapshot (if present) for an instant first paint, then refreshes. */
    fun start() {
        scope.launch {
            service.cached(userId)?.let { cached ->
                uiModel = BrainProfileUiModelBuilder.build(cached, service.recommend(cached))
            }
            refreshInternal()
        }
    }

    fun refresh() {
        scope.launch { refreshInternal() }
    }

    private suspend fun refreshInternal() {
        isLoading = true
        val profile = service.refresh(userId)
        uiModel = BrainProfileUiModelBuilder.build(profile, service.recommend(profile))
        isLoading = false
    }
}
