package com.mindos.app.features.mindboost.focus.domain.adaptive

import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyAxis
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DefaultAdaptiveEngineTest {

    private val engine = DefaultAdaptiveEngine()
    private val e1 = ExerciseCatalog.E1_VIGIA
    private val e3 = ExerciseCatalog.E3_GATILLO

    @Suppress("LongParameterList") // Test builder mirroring AdaptiveInput's fields.
    private fun input(
        accuracy: Double,
        rtCv: Double = 0.20,
        falseAlarms: Double = 0.0,
        recentTrials: List<com.mindos.app.features.mindboost.focus.domain.model.TrialResult> =
            FocusTestFixtures.hits(listOf(300, 310, 305)),
        fatigue: FatigueSignal = FocusTestFixtures.noFatigueMeasured,
        blocksSinceChange: Int = Int.MAX_VALUE,
        exercise: com.mindos.app.features.mindboost.focus.domain.model.ExerciseDefinition = e1,
    ) = AdaptiveInput(
        exercise = exercise,
        current = DifficultyVector.EASIEST,
        recentBlock = FocusTestFixtures.metricSet(
            MetricKey.ACCURACY to accuracy,
            MetricKey.RT_CV to rtCv,
            MetricKey.FALSE_ALARMS to falseAlarms,
        ),
        recentTrials = recentTrials,
        perAxisMastery = emptyMap(),
        fatigue = fatigue,
        blocksSinceChange = blocksSinceChange,
    )

    @Test
    fun stepsUp_whenMasteringAndStable() {
        val result = engine.decide(input(accuracy = 0.90))
        val decision = assertIs<AdaptiveDecision.StepUp>(result.decision)
        assertEquals(DifficultyAxis.TIME_ON_TASK, decision.axis)
        assertTrue(result.nextDifficulty.level(DifficultyAxis.TIME_ON_TASK).value > 0.0)
    }

    @Test
    fun stepsDown_whenAccuracyLow() {
        val result = engine.decide(input(accuracy = 0.50))
        assertIs<AdaptiveDecision.StepDown>(result.decision)
    }

    @Test
    fun stepsDown_onThreeConsecutiveErrors_evenIfMeanAccuracyOk() {
        val trials = listOf(
            FocusTestFixtures.trial(0, TrialOutcome.HIT, 300),
            FocusTestFixtures.trial(1, TrialOutcome.MISS),
            FocusTestFixtures.trial(2, TrialOutcome.MISS),
            FocusTestFixtures.trial(3, TrialOutcome.FALSE_ALARM, rtMs = 280, isTarget = false),
        )
        val result = engine.decide(input(accuracy = 0.80, recentTrials = trials))
        assertIs<AdaptiveDecision.StepDown>(result.decision)
    }

    @Test
    fun holds_underHysteresis_evenWhenMastering() {
        val result = engine.decide(input(accuracy = 0.95, blocksSinceChange = 0))
        assertIs<AdaptiveDecision.Hold>(result.decision)
    }

    @Test
    fun containsImpulsivity_onFastCommissions() {
        val trials = listOf(
            FocusTestFixtures.trial(0, TrialOutcome.HIT, 300),
            FocusTestFixtures.trial(1, TrialOutcome.FALSE_ALARM, rtMs = 200, isTarget = false),
            FocusTestFixtures.trial(2, TrialOutcome.FALSE_ALARM, rtMs = 180, isTarget = false),
        )
        val result = engine.decide(
            input(accuracy = 0.70, falseAlarms = 3.0, recentTrials = trials, exercise = e3),
        )
        val decision = assertIs<AdaptiveDecision.ContainImpulsivity>(result.decision)
        assertEquals(DifficultyAxis.INHIBITORY_DEMAND, decision.axis)
    }

    @Test
    fun stopsSession_onSustainedHighFatigue() {
        val result = engine.decide(
            input(accuracy = 0.80, fatigue = FatigueSignal(FatigueLevel.HIGH, emerging = true, sustained = true)),
        )
        assertIs<AdaptiveDecision.StopSession>(result.decision)
    }

    @Test
    fun cooldown_onEmergingFatigue() {
        val result = engine.decide(
            input(accuracy = 0.80, fatigue = FatigueSignal(FatigueLevel.MODERATE, emerging = true, sustained = false)),
        )
        assertIs<AdaptiveDecision.Cooldown>(result.decision)
    }

    @Test
    fun alternatingPerformance_doesNotOscillate_whenWithinHysteresis() {
        // Immediately after any change, blocksSinceChange = 0 → engine must Hold,
        // regardless of a momentary high-accuracy block. This is the anti-oscillation guard.
        val high = engine.decide(input(accuracy = 0.95, blocksSinceChange = 0))
        assertIs<AdaptiveDecision.Hold>(high.decision)
    }
}
