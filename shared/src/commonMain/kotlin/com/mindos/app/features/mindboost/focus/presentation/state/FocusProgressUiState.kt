package com.mindos.app.features.mindboost.focus.presentation.state

import com.mindos.app.features.mindboost.focus.domain.reporting.FocusScore
import com.mindos.app.features.mindboost.focus.domain.reporting.MetricTrend
import com.mindos.app.features.mindboost.focus.domain.reporting.TransferReport

/** State of the focus progress view: an honest composite score + per-metric trends + near-transfer. */
sealed interface FocusProgressUiState {
    data object Loading : FocusProgressUiState

    data class Ready(
        val totalSessions: Int,
        val focusScore: FocusScore,
        val trends: List<MetricTrend>,
        /**
         * Near-transfer report from the latest reserved-probe retest vs the baseline, or null until an
         * EVALUATION retest exists. Reliable changes (RCI) are the strongest honest evidence; everything
         * else is preliminary. Never claims far transfer.
         */
        val transfer: TransferReport? = null,
    ) : FocusProgressUiState
}
