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
        val trends: List<TrendLine>,
        /**
         * Near-transfer report from the latest reserved-probe retest vs the baseline, or null until an
         * EVALUATION retest exists. Reliable changes (RCI) are the strongest honest evidence; everything
         * else is preliminary. Never claims far transfer.
         */
        val transfer: TransferReport? = null,
    ) : FocusProgressUiState
}

/**
 * One trend row, carrying its own user-facing [label] — the mirror of `TransferLine`, which has carried
 * its label since R13. The name travels WITH the row, so no lookup can miss it and no second table can
 * drift out of step with the set of tracked metrics.
 *
 * These names deliberately do NOT carry the "(prueba)" marker that transfer rows use: that marker means
 * the reserved probe, while a trend is computed over the user's own training sessions. Both row kinds
 * appear on the same screen, so merging their names would mislabel where the number came from.
 */
data class TrendLine(
    val label: String,
    val trend: MetricTrend,
)
