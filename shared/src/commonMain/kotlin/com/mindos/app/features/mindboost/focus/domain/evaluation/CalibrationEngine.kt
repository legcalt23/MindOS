package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricSet
import com.mindos.app.features.mindboost.focus.domain.model.BaselineSnapshot
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseDefinition
import com.mindos.app.features.mindboost.focus.domain.model.IndividualThresholds
import com.mindos.app.features.mindboost.focus.domain.model.SubjectiveState
import com.mindos.app.features.mindboost.focus.domain.model.TrialResult

/**
 * Turns calibration samples into individualized thresholds and a per-exercise
 * starting difficulty, and folds a finished baseline into a [BaselineSnapshot]
 * (the pre side of any later pre/post comparison). See 06_ §3.
 */
interface CalibrationEngine {

    /** Derives individual thresholds (personalized lapse cutoff, baseline RT-CV) from samples. */
    fun calibrate(samples: List<TrialResult>): IndividualThresholds

    /** Picks a starting difficulty aiming for the productive ~0.80 accuracy zone. */
    fun initialDifficulty(exercise: ExerciseDefinition, samples: List<TrialResult>): DifficultyVector

    /** Builds the baseline snapshot from a baseline's computed metrics (confident values only). */
    fun baselineSnapshot(
        userId: String,
        metrics: MetricSet,
        capturedAtMs: Long,
        state: SubjectiveState?,
        deviceFingerprint: String?,
    ): BaselineSnapshot
}
