package com.mindos.app.features.mindboost.emotions.presentation

import com.mindos.app.features.mindboost.core.calibration.BlockPrediction
import com.mindos.app.features.mindboost.core.calibration.BlockPredictionOutcome
import com.mindos.app.features.mindboost.core.calibration.BlockPredictionReader
import com.mindos.app.features.mindboost.core.calibration.CalibrationJudgment
import com.mindos.app.features.mindboost.core.calibration.CalibrationKernel
import com.mindos.app.features.mindboost.core.calibration.CalibrationReading
import com.mindos.app.features.mindboost.core.calibration.CalibrationVerdict
import com.mindos.app.features.mindboost.core.calibration.ConfidenceInterval
import com.mindos.app.features.mindboost.core.calibration.ConfidenceLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Ola 1 · N1 · P5 — the readout the user actually sees.
 *
 * The tests are mostly about what must NOT appear: no percentage before the floor, no percentage for a level that
 * was barely used, and no "you are well calibrated" when all that happened is that the interval failed to resolve
 * a direction.
 */
class EmotionCalibrationCardBuilderTest {

    private val fourWay = 0.25
    private val noForecasts = BlockPredictionReader.read(emptyList())

    private fun judgments(level: ConfidenceLevel, n: Int, correct: Int) =
        List(n) { CalibrationJudgment(level, wasCorrect = it < correct, chanceLevel = fourWay) }

    @Test
    fun below_the_floor_it_reports_the_counts_and_states_no_percentage_at_all() {
        val reading = CalibrationKernel.read(judgments(ConfidenceLevel.HIGH, n = 12, correct = 9))
        assertTrue(reading is CalibrationReading.Absent)

        val model = EmotionCalibrationCardBuilder.build(reading, noForecasts)
        assertFalse(model.hasReading)
        assertTrue(model.lines.isEmpty(), "there is nothing to say per level yet")
        assertNull(model.forecastNote)
        assertTrue(model.headline.contains("12") && model.headline.contains("${CalibrationKernel.judgmentsRequired}"))
        assertFalse(model.headline.contains("%"), "a percentage before the floor is exactly what must not happen")
    }

    @Test
    fun a_well_used_level_states_its_accuracy_and_a_barely_used_one_says_it_cannot() {
        // HIGH: 30 judgments, 18 right → 60 %. MEDIUM: 25, 10 right → 40 %. LOW: 3 → below the per-level floor.
        val reading = CalibrationKernel.read(
            judgments(ConfidenceLevel.HIGH, 30, 18) +
                judgments(ConfidenceLevel.MEDIUM, 25, 10) +
                judgments(ConfidenceLevel.LOW, 3, 0),
        )
        assertTrue(reading is CalibrationReading.Present)

        val model = EmotionCalibrationCardBuilder.build(reading, noForecasts)
        assertTrue(model.hasReading)
        assertEquals(3, model.lines.size)

        val high = model.lines.single { it.readout.contains(EmotionStrings.CONFIDENCE_HIGH) }
        assertTrue(high.hasReading)
        assertTrue(high.readout.contains("60 %"), "expected the real rate, got: ${high.readout}")

        val medium = model.lines.single { it.readout.contains(EmotionStrings.CONFIDENCE_MEDIUM) }
        assertTrue(medium.readout.contains("40 %"))

        val low = model.lines.single { it.readout.contains(EmotionStrings.CONFIDENCE_LOW) }
        assertFalse(low.hasReading, "3 uses cannot support a rate")
        assertFalse(low.readout.contains("%"))
    }

    @Test
    fun the_neutral_verdict_never_congratulates_the_user() {
        val neutral = EmotionCalibrationCardBuilder.build(
            CalibrationReading.Present(
                n = 40,
                brier = 0.2,
                overconfidence = 0.01,
                overconfidenceCi = ConfidenceInterval(-0.05, 0.07),
                verdict = CalibrationVerdict.NO_DETECTABLE_BIAS,
                resolution = 0.1,
                buckets = emptyList(),
            ),
            noForecasts,
        )
        assertEquals(EmotionStrings.CALIBRATION_NO_BIAS, neutral.headline)
        assertFalse(neutral.headline.contains("bien"), "'no detectable' is not 'well calibrated'")

        // Discriminating half: a resolved direction IS stated, so the neutral wording is not just blanket silence.
        val biased = EmotionCalibrationCardBuilder.build(
            CalibrationReading.Present(
                n = 40,
                brier = 0.3,
                overconfidence = 0.2,
                overconfidenceCi = ConfidenceInterval(0.1, 0.3),
                verdict = CalibrationVerdict.OVERCONFIDENT,
                resolution = 0.1,
                buckets = emptyList(),
            ),
            noForecasts,
        )
        assertEquals(EmotionStrings.CALIBRATION_OVERCONFIDENT, biased.headline)
    }

    @Test
    fun the_forecast_note_appears_only_once_there_are_enough_blocks() {
        val reading = CalibrationKernel.read(judgments(ConfidenceLevel.HIGH, 40, 25))
        val twoBlocks = listOf(
            BlockPredictionOutcome(BlockPrediction(8, 10), actualCorrect = 6),
            BlockPredictionOutcome(BlockPrediction(8, 10), actualCorrect = 6),
        )
        assertNull(EmotionCalibrationCardBuilder.build(reading, BlockPredictionReader.read(twoBlocks)).forecastNote)

        // Three blocks off by 2, 2 and 4 of 10 → mean absolute error 0.2667 → 27 %.
        val threeBlocks = twoBlocks + BlockPredictionOutcome(BlockPrediction(9, 10), actualCorrect = 5)
        val note = EmotionCalibrationCardBuilder.build(reading, BlockPredictionReader.read(threeBlocks)).forecastNote
        assertEquals(EmotionStrings.calibrationForecast(27), note)
    }
}
