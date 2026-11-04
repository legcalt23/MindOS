package com.mindos.app.features.mindboost.emotions.data.local

import com.mindos.app.features.mindboost.core.calibration.BlockPrediction
import com.mindos.app.features.mindboost.core.calibration.BlockPredictionOutcome
import com.mindos.app.features.mindboost.core.calibration.CalibrationJudgment
import com.mindos.app.features.mindboost.core.calibration.ConfidenceLevel
import com.mindos.app.features.mindboost.emotions.data.dto.BlockForecastDto
import com.mindos.app.features.mindboost.emotions.data.dto.CalibrationBucketDto
import com.mindos.app.features.mindboost.emotions.data.dto.CalibrationSummaryDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto

/**
 * Ola 1 · N1 — the bridge between what a session captured and what gets stored, and back again.
 *
 * The round trip is lossless for every statistic the kernel computes, which is the property that makes storing
 * counts instead of trials legitimate. It is NOT lossless for the ORDER of judgments, and that is fine: Brier,
 * overconfidence, its interval, resolution and the per-level accuracies are all order-independent. If a
 * future metric ever needs sequence (a within-session drift, say), this shape stops being sufficient and the
 * decision has to be revisited rather than quietly worked around.
 */
object EmotionCalibrationSummaries {

    /** Collapses a session's judgments and forecasts into the storable summary. Null when it captured nothing. */
    fun summarize(
        judgments: List<CalibrationJudgment>,
        forecasts: List<BlockPredictionOutcome>,
    ): CalibrationSummaryDto? {
        if (judgments.isEmpty() && forecasts.isEmpty()) return null
        val buckets = judgments
            .groupBy { it.level to it.chanceLevel }
            .map { (key, group) ->
                CalibrationBucketDto(
                    level = key.first.name,
                    chanceLevel = key.second,
                    n = group.size,
                    correct = group.count { it.wasCorrect },
                )
            }
        return CalibrationSummaryDto(
            buckets = buckets,
            forecasts = forecasts.map {
                BlockForecastDto(
                    predictedCorrect = it.prediction.predictedCorrect,
                    blockSize = it.prediction.blockSize,
                    actualCorrect = it.actualCorrect,
                )
            },
        )
    }

    /**
     * Expands stored counts back into judgments so the shared kernel reads them exactly as it reads a live
     * session. Unknown level names are dropped rather than guessed: a bucket whose level cannot be resolved is
     * evidence we cannot interpret, and inventing a level for it would put a fabricated judgment into a
     * measurement.
     */
    fun judgmentsOf(summaries: List<EmotionSessionSummaryDto>): List<CalibrationJudgment> =
        summaries.flatMap { summary ->
            summary.calibration?.buckets.orEmpty().flatMap { bucket ->
                val level = ConfidenceLevel.entries.firstOrNull { it.name == bucket.level }
                if (level == null || bucket.chanceLevel <= 0.0 || bucket.chanceLevel >= 1.0) {
                    emptyList()
                } else {
                    val correct = bucket.correct.coerceIn(0, bucket.n)
                    List(bucket.n) { index ->
                        CalibrationJudgment(level, wasCorrect = index < correct, chanceLevel = bucket.chanceLevel)
                    }
                }
            }
        }

    /** Rebuilds the stored block forecasts. Rows that do not describe a possible block are dropped, not clamped. */
    fun forecastsOf(summaries: List<EmotionSessionSummaryDto>): List<BlockPredictionOutcome> =
        summaries.flatMap { summary ->
            summary.calibration?.forecasts.orEmpty().mapNotNull { dto ->
                val valid = dto.blockSize > 0 &&
                    dto.predictedCorrect in 0..dto.blockSize &&
                    dto.actualCorrect in 0..dto.blockSize
                if (valid) {
                    BlockPredictionOutcome(BlockPrediction(dto.predictedCorrect, dto.blockSize), dto.actualCorrect)
                } else {
                    null
                }
            }
        }
}
