package com.mindos.app.features.mindboost.focus.sim

import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.domain.adaptive.AdaptiveDecision
import com.mindos.app.features.mindboost.focus.domain.adaptive.AdaptiveInput
import com.mindos.app.features.mindboost.focus.domain.adaptive.DefaultAdaptiveEngine
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.exercise.E1VigiaGenerator
import com.mindos.app.features.mindboost.focus.domain.exercise.E3GatilloGenerator
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.valueOf
import com.mindos.app.features.mindboost.focus.domain.model.BlockResult
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SimulatorLoopTest {

    private val engine = DefaultAdaptiveEngine()

    private fun decideFor(block: BlockResult, definition: ExerciseDefinition) = engine.decide(
        AdaptiveInput(
            exercise = definition,
            current = DifficultyVector.EASIEST,
            recentBlock = block.metrics,
            recentTrials = block.trials.takeLast(15),
            perAxisMastery = emptyMap(),
            fatigue = FocusTestFixtures.noFatigueMeasured,
            blocksSinceChange = Int.MAX_VALUE,
        ),
    )

    @Test
    fun impulsiveUser_triggersContainImpulsivity() {
        val block = SimulatedLoop.runBlock(
            generator = E3GatilloGenerator(),
            simulator = Simulators.IMPULSIVE,
            definition = ExerciseCatalog.E3_GATILLO,
            difficulty = DifficultyVector.EASIEST,
            trialCount = 80,
            seed = 1,
        )
        assertTrue(block.metrics.valueOf(MetricKey.FALSE_ALARMS) >= 2.0)
        assertIs<AdaptiveDecision.ContainImpulsivity>(decideFor(block, ExerciseCatalog.E3_GATILLO).decision)
    }

    @Test
    fun advancedUser_mastersAndStepsUp() {
        val block = SimulatedLoop.runBlock(
            generator = E1VigiaGenerator(),
            simulator = Simulators.ADVANCED,
            definition = ExerciseCatalog.E1_VIGIA,
            difficulty = DifficultyVector.EASIEST,
            trialCount = 60,
            seed = 2,
        )
        assertTrue(block.metrics.valueOf(MetricKey.ACCURACY) >= 0.85)
        assertTrue(block.metrics.valueOf(MetricKey.RT_CV) < 0.20)
        assertIs<AdaptiveDecision.StepUp>(decideFor(block, ExerciseCatalog.E1_VIGIA).decision)
    }

    @Test
    fun slowButAccurateUser_hasHighAccuracyAndSlowRt() {
        val block = SimulatedLoop.runBlock(
            generator = E1VigiaGenerator(),
            simulator = Simulators.SLOW_BUT_ACCURATE,
            definition = ExerciseCatalog.E1_VIGIA,
            difficulty = DifficultyVector.EASIEST,
            trialCount = 60,
            seed = 3,
        )
        assertTrue(block.metrics.valueOf(MetricKey.ACCURACY) >= 0.9)
        assertTrue(block.metrics.valueOf(MetricKey.RT_MEDIAN) > 400.0)
    }

    @Test
    fun fatiguedUser_accumulatesLapses() {
        val block = SimulatedLoop.runBlock(
            generator = E1VigiaGenerator(),
            simulator = Simulators.FATIGUED,
            definition = ExerciseCatalog.E1_VIGIA,
            difficulty = DifficultyVector.EASIEST,
            trialCount = 80,
            seed = 4,
        )
        // Late slow responses exceed the lapse threshold (500ms).
        assertTrue(block.metrics.valueOf(MetricKey.LAPSES) > 0.0)
    }

    @Test
    fun gamerOnStopSignal_breaksSsrtEstimate() {
        val metrics = SimulatedLoop.runStopSignalMetrics(
            simulator = Simulators.GAMER_E4,
            difficulty = DifficultyVector.EASIEST,
            trialCount = 80,
            seed = 5,
        )
        // Strategic slowing → p(respond|signal) far from 0.5 → SSRT not trustworthy.
        assertEquals(false, metrics[MetricKey.SSRT]?.confident)
    }
}
