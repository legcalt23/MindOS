package com.mindos.app.features.mindboost.focus.presentation.state

import com.mindos.app.features.mindboost.focus.domain.reporting.FocusScore
import com.mindos.app.features.mindboost.focus.domain.reporting.MetricTrend

/** State of the focus progress view: an honest composite score + per-metric trends. */
sealed interface FocusProgressUiState {
    data object Loading : FocusProgressUiState

    data class Ready(
        val totalSessions: Int,
        val focusScore: FocusScore,
        val trends: List<MetricTrend>,
    ) : FocusProgressUiState
}
