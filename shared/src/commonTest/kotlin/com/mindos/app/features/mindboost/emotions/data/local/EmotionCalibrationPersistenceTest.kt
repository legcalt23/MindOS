package com.mindos.app.features.mindboost.emotions.data.local

import com.mindos.app.features.mindboost.core.calibration.BlockPrediction
import com.mindos.app.features.mindboost.core.calibration.BlockPredictionOutcome
import com.mindos.app.features.mindboost.core.calibration.BlockPredictionReader
import com.mindos.app.features.mindboost.core.calibration.CalibrationJudgment
import com.mindos.app.features.mindboost.core.calibration.CalibrationKernel
import com.mindos.app.features.mindboost.core.calibration.CalibrationReading
import com.mindos.app.features.mindboost.core.calibration.ConfidenceLevel
import com.mindos.app.features.mindboost.emotions.data.dto.BlockForecastDto
import com.mindos.app.features.mindboost.emotions.data.dto.CalibrationBucketDto
import com.mindos.app.features.mindboost.emotions.data.dto.CalibrationSummaryDto
import com.mindos.app.features.mindboost.emotions.data.dto.DifficultySummaryDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionScoringVersions
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Ola 1 · N1 · P4 — the storage round trip and the cross-session reader.
 *
 * The claim under test is the one that makes counts an acceptable substitute for trials: reading a session back
 * from what was persisted must produce the SAME numbers as reading it live. If that ever stops holding, the
 * module is quietly reporting a different calibration than the one it measured.
 */
class EmotionCalibrationPersistenceTest {

    private val fourWay = 0.25

    /** A believable completed row. Variants are expressed with `.copy(...)` so the helper keeps one knob. */
    private fun summary(
        id: String = "sess_0000000000000000000000000000abcd",
        calibration: CalibrationSummaryDto? = null,
    ) = EmotionSessionSummaryDto(
        exerciseId = "EM3_SEPARADOR",
        startedAtMillis = 1_000L,
        durationMs = 200_000L,
        difficultySummary = DifficultySummaryDto("SPEED", 0.5, 2),
        metricsSummary = emptyList(),
        completed = true,
        abandoned = false,
        sessionId = id,
        endReason = "COMPLETED",
        scoringVersion = EmotionScoringVersions.SCORING,
        calibration = calibration,
    )

    /** A mixed, deliberately lopsided set: HIGH mostly right, LOW mostly wrong, MEDIUM in between. */
    private fun judgments(): List<CalibrationJudgment> =
        List(20) { CalibrationJudgment(ConfidenceLevel.HIGH, wasCorrect = it < 17, chanceLevel = fourWay) } +
            List(14) { CalibrationJudgment(ConfidenceLevel.MEDIUM, wasCorrect = it < 8, chanceLevel = fourWay) } +
            List(10) { CalibrationJudgment(ConfidenceLevel.LOW, wasCorrect = it < 2, chanceLevel = fourWay) }

    @Test
    fun persisted_counts_reproduce_the_live_reading_exactly() {
        val live = CalibrationKernel.read(judgments())
        val stored = EmotionCalibrationSummaries.summarize(judgments(), emptyList())
        val reloaded = CalibrationKernel.read(
            EmotionCalibrationSummaries.judgmentsOf(listOf(summary(calibration = stored))),
        )

        assertTrue(live is CalibrationReading.Present, "fixture must clear the floor or the test proves nothing")
        assertTrue(reloaded is CalibrationReading.Present)
        assertEquals(live.n, reloaded.n)
        assertEquals(live.brier, reloaded.brier)
        assertEquals(live.overconfidence, reloaded.overconfidence)
        assertEquals(live.overconfidenceCi, reloaded.overconfidenceCi)
        assertEquals(live.verdict, reloaded.verdict)
        assertEquals(live.resolution, reloaded.resolution)
        assertEquals(live.buckets, reloaded.buckets)
    }

    @Test
    fun a_session_that_captured_nothing_stores_nothing() {
        assertNull(EmotionCalibrationSummaries.summarize(emptyList(), emptyList()))
    }

    @Test
    fun a_legacy_row_contributes_no_judgments_rather_than_zero_ones() {
        val judgments = EmotionCalibrationSummaries.judgmentsOf(listOf(summary(calibration = null)))
        assertTrue(judgments.isEmpty())

        val reading = EmotionAggregateFactory.calibrationOf(listOf(summary(calibration = null)))
        assertTrue(reading is CalibrationReading.Absent)
        assertEquals(0, reading.nHave, "an absent row must read as no evidence, never as evidence of 0 accuracy")
    }

    @Test
    fun a_bucket_whose_level_cannot_be_resolved_is_dropped_not_guessed() {
        val stored = CalibrationSummaryDto(
            buckets = listOf(
                CalibrationBucketDto(level = "HIGH", chanceLevel = fourWay, n = 4, correct = 3),
                CalibrationBucketDto(level = "VERY_SURE_INDEED", chanceLevel = fourWay, n = 9, correct = 9),
                CalibrationBucketDto(level = "LOW", chanceLevel = 0.0, n = 7, correct = 0),
            ),
        )
        val judgments = EmotionCalibrationSummaries.judgmentsOf(listOf(summary(calibration = stored)))

        assertEquals(4, judgments.size, "only the interpretable bucket survives")
        assertTrue(judgments.all { it.level == ConfidenceLevel.HIGH })
        assertEquals(3, judgments.count { it.wasCorrect })
    }

    @Test
    fun evidence_accumulates_across_sessions_until_the_floor_is_met() {
        val perSession = List(15) { CalibrationJudgment(ConfidenceLevel.HIGH, wasCorrect = it < 12, fourWay) }
        val stored = EmotionCalibrationSummaries.summarize(perSession, emptyList())
        val rows = { count: Int -> List(count) { summary(id = "sess_row$it", calibration = stored) } }

        val afterTwo = EmotionAggregateFactory.calibrationOf(rows(2))
        assertTrue(afterTwo is CalibrationReading.Absent, "30 judgments is below the BRIER floor")
        assertEquals(30, afterTwo.nHave)
        assertEquals(CalibrationKernel.judgmentsRequired, afterTwo.nNeeded)

        val afterThree = EmotionAggregateFactory.calibrationOf(rows(3))
        assertTrue(afterThree is CalibrationReading.Present)
        assertEquals(45, afterThree.n)
    }

    @Test
    fun an_uninterpretable_or_gamed_row_never_contributes_while_a_safety_stop_still_does() {
        val stored = EmotionCalibrationSummaries.summarize(judgments(), emptyList())
        val good = summary(id = "sess_good", calibration = stored)
        val corrupt = summary("sess_corrupt", stored).copy(completed = false) // COMPLETED + completed=false
        val unidentified = summary(id = "", calibration = stored)
        val gamed = summary("sess_gamed", stored).copy(anomalyFlags = listOf("impossible_timing"))

        val onlyGood = EmotionAggregateFactory.calibrationOf(listOf(good, corrupt, unidentified, gamed))
        assertTrue(onlyGood is CalibrationReading.Present)
        assertEquals(44, onlyGood.n, "only the one believable row may contribute")

        // Discriminating half: the gate must not be "completed sessions only" — a safety stop's judgments were
        // still real metacognitive acts, and a short session's too.
        val stopped = summary("sess_stopped", stored)
            .copy(endReason = "SAFETY_STOP", completed = false, durationMs = 30_000L)
        val withStopped = EmotionAggregateFactory.calibrationOf(listOf(good, stopped))
        assertTrue(withStopped is CalibrationReading.Present)
        assertEquals(88, withStopped.n)
    }

    @Test
    fun block_forecasts_survive_the_round_trip_and_impossible_rows_are_dropped() {
        val outcomes = listOf(
            BlockPredictionOutcome(BlockPrediction(predictedCorrect = 8, blockSize = 10), actualCorrect = 5),
            BlockPredictionOutcome(BlockPrediction(predictedCorrect = 12, blockSize = 20), actualCorrect = 12),
            BlockPredictionOutcome(BlockPrediction(predictedCorrect = 3, blockSize = 10), actualCorrect = 6),
        )
        val stored = EmotionCalibrationSummaries.summarize(emptyList(), outcomes)
        val reloaded = EmotionCalibrationSummaries.forecastsOf(listOf(summary(calibration = stored)))
        assertEquals(outcomes, reloaded)

        val live = BlockPredictionReader.read(outcomes)
        val fromStore = EmotionAggregateFactory.blockForecastsOf(listOf(summary(calibration = stored)))
        assertEquals(live, fromStore)

        val impossible = CalibrationSummaryDto(
            forecasts = listOf(
                BlockForecastDto(predictedCorrect = 4, blockSize = 10, actualCorrect = 4),
                BlockForecastDto(predictedCorrect = 30, blockSize = 10, actualCorrect = 2),
                BlockForecastDto(predictedCorrect = 1, blockSize = 0, actualCorrect = 0),
            ),
        )
        val survivors = EmotionCalibrationSummaries.forecastsOf(listOf(summary(calibration = impossible)))
        assertEquals(1, survivors.size)
        assertEquals(4, survivors.single().prediction.predictedCorrect)
    }
}
