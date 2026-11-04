package com.mindos.app.features.mindboost.emotions.presentation

import com.mindos.app.features.mindboost.core.calibration.BlockPredictionReader
import com.mindos.app.features.mindboost.core.calibration.CalibrationReading
import com.mindos.app.features.mindboost.core.calibration.CalibrationVerdict
import kotlin.math.roundToInt

/** One level's line. [readout] is always renderable; [hasReading] says whether it states a fact or a wait. */
data class EmotionCalibrationLine(val readout: String, val hasReading: Boolean)

/**
 * Ola 1 · N1 · P5 — what the user is told about their own confidence, and nothing more.
 *
 * [hasReading] is false while the evidence is below the floor, and in that state there is no headline verdict and
 * no percentage anywhere in the model — not a greyed-out one, not a provisional one. The card cannot render a
 * number it does not have, because the number is not in the model to render.
 */
data class EmotionCalibrationUiModel(
    val title: String,
    val headline: String,
    val hasReading: Boolean,
    val lines: List<EmotionCalibrationLine>,
    val forecastNote: String?,
)

/** Pure builder: [CalibrationReading] + the block-forecast reading in, user-facing copy out. */
object EmotionCalibrationCardBuilder {

    private const val PERCENT = 100.0

    fun build(
        calibration: CalibrationReading,
        forecasts: BlockPredictionReader.Reading,
    ): EmotionCalibrationUiModel = when (calibration) {
        is CalibrationReading.Absent -> EmotionCalibrationUiModel(
            title = EmotionStrings.CALIBRATION_TITLE,
            headline = EmotionStrings.calibrationCollecting(calibration.nHave, calibration.nNeeded),
            hasReading = false,
            lines = emptyList(),
            forecastNote = null,
        )

        is CalibrationReading.Present -> EmotionCalibrationUiModel(
            title = EmotionStrings.CALIBRATION_TITLE,
            headline = headlineOf(calibration.verdict),
            hasReading = true,
            lines = calibration.buckets.map { bucket ->
                val label = EmotionStrings.confidenceLabel(bucket.level)
                // `accuracy` is null exactly when the level was used too few times to state a rate. Saying so beats
                // a percentage that would swing wildly on the next judgment, and beats hiding the level entirely —
                // "you barely used this one" is itself information about how someone rates themselves.
                val accuracy = bucket.accuracy
                if (accuracy == null) {
                    EmotionCalibrationLine(EmotionStrings.calibrationBucketPending(label), hasReading = false)
                } else {
                    val percent = (accuracy * PERCENT).roundToInt()
                    EmotionCalibrationLine(EmotionStrings.calibrationBucket(label, percent), hasReading = true)
                }
            },
            forecastNote = forecastNoteOf(forecasts),
        )
    }

    private fun headlineOf(verdict: CalibrationVerdict): String = when (verdict) {
        CalibrationVerdict.OVERCONFIDENT -> EmotionStrings.CALIBRATION_OVERCONFIDENT
        CalibrationVerdict.UNDERCONFIDENT -> EmotionStrings.CALIBRATION_UNDERCONFIDENT
        CalibrationVerdict.NO_DETECTABLE_BIAS -> EmotionStrings.CALIBRATION_NO_BIAS
    }

    /** Null below the block floor: too few blocks to average is not a small average, it is no average. */
    private fun forecastNoteOf(forecasts: BlockPredictionReader.Reading): String? = when (forecasts) {
        is BlockPredictionReader.Reading.Absent -> null
        is BlockPredictionReader.Reading.Present ->
            EmotionStrings.calibrationForecast((forecasts.meanAbsoluteError * PERCENT).roundToInt())
    }
}
