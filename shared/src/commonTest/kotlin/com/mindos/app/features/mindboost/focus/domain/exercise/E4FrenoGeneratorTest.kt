package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.metrics.DefaultMetricsEngine
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.IndividualThresholds
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome
import com.mindos.app.features.mindboost.focus.domain.model.TrialResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class E4FrenoGeneratorTest {

    private val generator = E4FrenoGenerator()
    private val spec = BlockSpec(targetTrialCount = 60, mode = Mode.TRAINING)
    private val engine = DefaultMetricsEngine()

    @Test
    fun generation_isDeterministic_andGuaranteesAStopTrial() {
        val a = generator.generate(DifficultyVector.EASIEST, spec, seed = 6)
        val b = generator.generate(DifficultyVector.EASIEST, spec, seed = 6)
        assertEquals(a, b)
        assertTrue(a.any { it.requiresStop })
    }

    @Test
    fun ssdStaircase_convergesTowardFiftyPercentRespond() {
        // Virtual subject: fixed go-RT 500ms, true SSRT 200ms. They fail to stop
        // (respond) iff the stop signal arrives too late: goRt < ssd + ssrt → ssd > 300.
        val trueSsrt = 200L
        val goRt = 500L
        val tracker = generator.tracker(DifficultyVector.EASIEST)
        var respondedInLastHalf = 0
        val total = 200
        repeat(total) { i ->
            val ssd = tracker.currentSsdMs
            val responded = goRt < ssd + trueSsrt
            if (i >= total / 2 && responded) respondedInLastHalf++
            tracker.onStopOutcome(successfulStop = !responded)
        }
        val pRespond = respondedInLastHalf.toDouble() / (total / 2)
        assertTrue(pRespond in 0.35..0.65, "p(respond) should converge near 0.5, was $pRespond")
        // SSD stabilizes near the crossover (~300ms).
        assertTrue(tracker.currentSsdMs in 250..350, "SSD should settle near 300, was ${tracker.currentSsdMs}")
    }

    @Test
    fun ssrtMetric_isComputable_forBalancedStaircase() {
        val ssrt = ssrtMetric(stopFailures = 5)
        assertTrue(ssrt?.confident == true)
        assertTrue((ssrt?.value ?: 0.0) > 0.0)
    }

    @Test
    fun ssrtMetric_flagsGaming_whenStaircaseBrokeDown() {
        // p(respond|signal) = 0.1 → strategic slowing; SSRT must be marked not confident.
        assertEquals(false, ssrtMetric(stopFailures = 1)?.confident)
    }

    private fun ssrtMetric(stopFailures: Int): MetricValue? {
        val block = stopSignalBlock(stopFailures)
        return engine.blockMetrics(block, ExerciseCatalog.E4_FRENO, IndividualThresholds())[MetricKey.SSRT]
    }

    /** 10 go hits + 10 stop trials (SSD 200ms), [stopFailures] of which were responded to. */
    private fun stopSignalBlock(stopFailures: Int): List<TrialResult> {
        val go = (0 until 10).map {
            FocusTestFixtures.trial(
                it, TrialOutcome.HIT, rtMs = (300 + it * 10).toLong(), exerciseId = ExerciseId.E4_FRENO,
            )
        }
        val stops = (0 until 10).map { i ->
            val responded = i < stopFailures
            FocusTestFixtures.trial(
                index = 100 + i,
                outcome = if (responded) TrialOutcome.FALSE_ALARM else TrialOutcome.CORRECT_REJECTION,
                rtMs = if (responded) 250 else null,
                isTarget = true,
                requiredStop = true,
                stopSignalDelayMs = 200,
                exerciseId = ExerciseId.E4_FRENO,
            )
        }
        return go + stops
    }
}
