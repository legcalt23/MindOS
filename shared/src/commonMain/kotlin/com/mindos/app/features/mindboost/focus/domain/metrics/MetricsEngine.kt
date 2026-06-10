package com.mindos.app.features.mindboost.focus.domain.metrics

import com.mindos.app.features.mindboost.focus.domain.model.BlockResult
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseDefinition
import com.mindos.app.features.mindboost.focus.domain.model.IndividualThresholds
import com.mindos.app.features.mindboost.focus.domain.model.TrialResult

/**
 * Computes metrics from raw trials. Pure, deterministic, no side effects, no
 * platform. The honesty guarantees of the whole system live here.
 */
interface MetricsEngine {

    /** Metrics for a single block of trials of one exercise. */
    fun blockMetrics(
        trials: List<TrialResult>,
        definition: ExerciseDefinition,
        thresholds: IndividualThresholds,
    ): MetricSet

    /** Aggregated metrics across a session's blocks (incl. intra-session fatigue). */
    fun sessionMetrics(blocks: List<BlockResult>): MetricSet
}
