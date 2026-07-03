package com.mindos.app.features.mindboost.focus.presentation.state

import com.mindos.app.features.mindboost.focus.domain.evaluation.EvaluationResult
import com.mindos.app.features.mindboost.focus.domain.reporting.TransferReport

/**
 * Live model while a fixed-difficulty measurement block runs (baseline calibration or evaluation
 * probe). Mirrors [FocusRunningModel] but is block-scoped: it also reports which block of the
 * sequence is running so the UI can show "Bloque 2 de 4".
 */
data class FocusMeasureRunningModel(
    val title: String,
    val blockLabel: String,
    val field: FocusFieldState,
    val blockIndex: Int,
    val blockCount: Int,
    /** 0.0..1.0 progress within the current block. */
    val progress: Float,
    val trialsDone: Int,
    val trialsTotal: Int,
    val hits: Int,
    val lapses: Int,
    val falseAlarms: Int,
    val lastReactionMs: Long?,
)

/** Honest summary shown after a baseline is established. Numbers are calibration facts, not a diagnosis. */
data class FocusBaselineSummary(
    val lapseRtMs: Long,
    val baselineRtCv: Double?,
    val calibratedExercises: Int,
    val probeMetrics: Int,
    val note: String,
)

/** State of the initial baseline flow (calibration blocks + one reserved probe). */
sealed interface FocusBaselineUiState {
    data class Intro(
        val estimatedMinutes: Int,
        val blockCount: Int,
        val instructions: List<String>,
    ) : FocusBaselineUiState

    data class Running(val model: FocusMeasureRunningModel) : FocusBaselineUiState

    data class Finished(val summary: FocusBaselineSummary) : FocusBaselineUiState

    data class Aborted(val reason: String) : FocusBaselineUiState
}

/** State of an evaluation retest (one reserved probe in EVALUATION mode, compared against the baseline). */
sealed interface FocusEvaluationUiState {
    /** No baseline yet — a retest needs a pre-snapshot to compare against. */
    data class NeedsBaseline(val reason: String) : FocusEvaluationUiState

    data class Intro(
        val estimatedMinutes: Int,
        val probeName: String,
        val instructions: List<String>,
    ) : FocusEvaluationUiState

    data class Running(val model: FocusMeasureRunningModel) : FocusEvaluationUiState

    data class Finished(
        val result: EvaluationResult,
        val transfer: TransferReport,
    ) : FocusEvaluationUiState

    data class Aborted(val reason: String) : FocusEvaluationUiState
}
