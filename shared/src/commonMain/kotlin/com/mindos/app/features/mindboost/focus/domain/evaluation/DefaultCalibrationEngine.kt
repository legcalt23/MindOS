package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricSet
import com.mindos.app.features.mindboost.focus.domain.metrics.RtStatistics
import com.mindos.app.features.mindboost.focus.domain.model.AxisLevel
import com.mindos.app.features.mindboost.focus.domain.model.BaselineSnapshot
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyAxis
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseDefinition
import com.mindos.app.features.mindboost.focus.domain.model.IndividualThresholds
import com.mindos.app.features.mindboost.focus.domain.model.SubjectiveState
import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome
import com.mindos.app.features.mindboost.focus.domain.model.TrialResult
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Default calibration. Personalizes the lapse cutoff from the user's own RT
 * distribution (a fixed 500ms is wrong for a much faster/slower person), records
 * their baseline RT-CV for relative-spike detection, and seeds an initial
 * difficulty in the productive accuracy zone. Falls back to population defaults
 * when there are too few samples to personalize honestly.
 */
class DefaultCalibrationEngine : CalibrationEngine {

    override fun calibrate(samples: List<TrialResult>): IndividualThresholds {
        val hitRts = samples
            .filter { it.outcome == TrialOutcome.HIT }
            .mapNotNull { it.reactionTime?.value?.toDouble() }
            .filter { it >= DEFAULT_ANTICIPATION_FLOOR_MS }
        if (hitRts.size < MIN_SAMPLES) return IndividualThresholds()

        val median = RtStatistics.median(hitRts)
        val mad = RtStatistics.scaledMad(hitRts)
        val personalizedLapse = (median + LAPSE_K * mad).roundToLong()
        val lapseRtMs = max(DEFAULT_LAPSE_MS, personalizedLapse)
        val sd = RtStatistics.standardDeviation(hitRts)
        val baselineRtCv = if (median > 0.0) sd / median else null

        return IndividualThresholds(
            lapseRtMs = lapseRtMs,
            anticipationFloorMs = DEFAULT_ANTICIPATION_FLOOR_MS,
            baselineRtCv = baselineRtCv,
        )
    }

    override fun initialDifficulty(exercise: ExerciseDefinition, samples: List<TrialResult>): DifficultyVector {
        val valid = samples.count { it.outcome != TrialOutcome.ANTICIPATION }
        val correct = samples.count {
            it.outcome == TrialOutcome.HIT || it.outcome == TrialOutcome.CORRECT_REJECTION
        }
        val accuracy = if (valid == 0) 0.0 else correct.toDouble() / valid
        val axis = calibrationAxis(exercise)
        val level = when {
            accuracy >= ACC_VERY_HIGH -> LEVEL_HARD
            accuracy >= ACC_HIGH -> LEVEL_MEDIUM
            accuracy >= ACC_OK -> LEVEL_EASY
            else -> LEVEL_MIN
        }
        return DifficultyVector.EASIEST.adjust(axis, AxisLevel(level))
    }

    override fun baselineSnapshot(
        userId: String,
        metrics: MetricSet,
        capturedAtMs: Long,
        state: SubjectiveState?,
        deviceFingerprint: String?,
    ): BaselineSnapshot = BaselineSnapshot(
        userId = userId,
        perMetric = metrics.filterValues { it.confident }.mapValues { it.value.value },
        capturedAtMs = capturedAtMs,
        state = state,
        deviceFingerprint = deviceFingerprint,
    )

    /** The axis that most drives in-block difficulty — perceptual load when relevant. */
    private fun calibrationAxis(exercise: ExerciseDefinition): DifficultyAxis =
        if (DifficultyAxis.PERCEPTUAL_LOAD in exercise.relevantAxes) {
            DifficultyAxis.PERCEPTUAL_LOAD
        } else {
            exercise.governingAxis
        }

    private companion object {
        const val DEFAULT_LAPSE_MS = 500L
        const val DEFAULT_ANTICIPATION_FLOOR_MS = 150L
        const val LAPSE_K = 2.5
        const val MIN_SAMPLES = 8

        const val ACC_VERY_HIGH = 0.90
        const val ACC_HIGH = 0.80
        const val ACC_OK = 0.70
        const val LEVEL_HARD = 0.50
        const val LEVEL_MEDIUM = 0.35
        const val LEVEL_EASY = 0.20
        const val LEVEL_MIN = 0.0
    }
}
