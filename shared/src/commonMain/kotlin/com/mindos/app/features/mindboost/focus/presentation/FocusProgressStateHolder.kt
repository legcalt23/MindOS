package com.mindos.app.features.mindboost.focus.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mindos.app.features.mindboost.focus.data.InMemoryBaselineRepository
import com.mindos.app.features.mindboost.focus.data.InMemoryProbeBankRepository
import com.mindos.app.features.mindboost.focus.domain.evaluation.FocusEvaluationService
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.reporting.DefaultFocusScoreCalculator
import com.mindos.app.features.mindboost.focus.domain.reporting.DefaultTrendAnalyzer
import com.mindos.app.features.mindboost.focus.domain.reporting.FocusScoreCalculator
import com.mindos.app.features.mindboost.focus.domain.reporting.TransferReport
import com.mindos.app.features.mindboost.focus.domain.reporting.TrendAnalyzer
import com.mindos.app.features.mindboost.focus.domain.repository.BaselineRepository
import com.mindos.app.features.mindboost.focus.domain.repository.ProbeBankRepository
import com.mindos.app.features.mindboost.focus.domain.repository.SessionHistoryRepository
import com.mindos.app.features.mindboost.focus.presentation.state.FocusProgressUiState

/**
 * Loads the honest progress view: a composite Focus Score (with its uncertainty band) from the most
 * recent session, plus per-metric trends across recent sessions, plus the **near-transfer** report once
 * an EVALUATION retest of a reserved probe exists. Read-only and platform-free.
 *
 * Near-transfer is the strongest honest evidence: it compares the latest reserved-probe retest against
 * the stored baseline. It is RCI-gated, so it surfaces "reliable change" only with enough evidence;
 * otherwise it shows preliminary deltas, and it never claims far transfer.
 */
class FocusProgressStateHolder(
    private val historyRepository: SessionHistoryRepository,
    private val baselineRepository: BaselineRepository = InMemoryBaselineRepository(),
    private val probeBankRepository: ProbeBankRepository = InMemoryProbeBankRepository(),
    private val evaluationService: FocusEvaluationService = FocusEvaluationService(),
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
            transfer = nearTransfer(userId),
        )
    }

    /** Near-transfer from the latest reserved-probe retest taken AFTER the baseline; null until one exists. */
    private suspend fun nearTransfer(userId: String): TransferReport? {
        val baseline = baselineRepository.load(userId) ?: return null
        val probes = probeBankRepository.probeHistory(userId, PROBE_LIMIT)
        // A real retest is a probe recorded strictly after the baseline snapshot (the baseline's own
        // probe run is not a retest). probeHistory is newest-first.
        return probes.firstOrNull { it.endedAtMs > baseline.capturedAtMs }?.let { retest ->
            val priorAdministrations = probes.count { it.endedAtMs < retest.endedAtMs }
            val evaluation = evaluationService.evaluate(
                baseline = baseline,
                retestMetrics = retest.sessionMetrics,
                priorAdministrations = priorAdministrations,
                capturedAtMs = retest.endedAtMs,
            )
            evaluationService.transferReport(evaluation)
        }
    }

    private companion object {
        const val RECENT_LIMIT = 30
        const val PROBE_LIMIT = 30
        val TRACKED_METRICS = listOf(
            MetricKey.ACCURACY,
            MetricKey.RT_MEDIAN,
            MetricKey.RT_CV,
            MetricKey.SUSTAINED_FOCUS_INDEX,
        )
    }
}
