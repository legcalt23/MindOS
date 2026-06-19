package com.mindos.app.features.mindboost.focus.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.repository.SessionHistoryRepository
import com.mindos.app.features.mindboost.focus.domain.reporting.DefaultFocusScoreCalculator
import com.mindos.app.features.mindboost.focus.domain.reporting.DefaultTrendAnalyzer
import com.mindos.app.features.mindboost.focus.domain.reporting.FocusScoreCalculator
import com.mindos.app.features.mindboost.focus.domain.reporting.TrendAnalyzer
import com.mindos.app.features.mindboost.focus.presentation.state.FocusProgressUiState

/**
 * Loads the honest progress view: a composite Focus Score (with its uncertainty band) from the most
 * recent session, plus per-metric trends across recent sessions. Read-only and platform-free.
 * Near-transfer (probe-based RCI evidence) is added once EVALUATION sessions exist.
 */
class FocusProgressStateHolder(
    private val historyRepository: SessionHistoryRepository,
    private val focusScoreCalculator: FocusScoreCalculator = DefaultFocusScoreCalculator(),
    private val trendAnalyzer: TrendAnalyzer = DefaultTrendAnalyzer(),
) {
    var uiState: FocusProgressUiState by mutableStateOf(FocusProgressUiState.Loading)
        private set

    suspend fun load(userId: String) {
        val recent = historyRepository.recent(userId, RECENT_LIMIT)
        val latestMetrics = recent.firstOrNull()?.sessionMetrics ?: emptyMap()
        val score = focusScoreCalculator.compute(latestMetrics)
        val trends = TRACKED_METRICS.map { trendAnalyzer.analyze(it, recent) }
        uiState = FocusProgressUiState.Ready(
            totalSessions = recent.size,
            focusScore = score,
            trends = trends,
        )
    }

    private companion object {
        const val RECENT_LIMIT = 30
        val TRACKED_METRICS = listOf(
            MetricKey.ACCURACY,
            MetricKey.RT_MEDIAN,
            MetricKey.RT_CV,
            MetricKey.SUSTAINED_FOCUS_INDEX,
        )
    }
}
