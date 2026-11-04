package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.domain.adaptive.AdaptiveConfig
import com.mindos.app.features.mindboost.focus.domain.adaptive.AdaptiveDecision
import com.mindos.app.features.mindboost.focus.domain.adaptive.AdaptiveInput
import com.mindos.app.features.mindboost.focus.domain.adaptive.DefaultAdaptiveEngine
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AdaptiveModeGateTest {

    private val engine = DefaultAdaptiveEngine()

    // An input that WOULD step difficulty up if adaptation ran (mastering + stable).
    private fun masteringInput() = AdaptiveInput(
        exercise = ExerciseCatalog.E1_VIGIA,
        current = DifficultyVector.EASIEST,
        recentBlock = FocusTestFixtures.metricSet(
            MetricKey.ACCURACY to 0.95,
            MetricKey.RT_CV to 0.15,
            MetricKey.FALSE_ALARMS to 0.0,
        ),
        recentTrials = FocusTestFixtures.hits(listOf(300, 305, 310)),
        perAxisMastery = emptyMap(),
        fatigue = FocusTestFixtures.noFatigueMeasured,
        blocksSinceChange = Int.MAX_VALUE,
        config = AdaptiveConfig(),
    )

    @Test
    fun evaluationMode_neverAdapts() {
        val input = masteringInput()
        val result = AdaptiveModeGate.decide(Mode.EVALUATION, engine, input)
        assertIs<AdaptiveDecision.Hold>(result.decision)
        // Difficulty must be byte-for-byte unchanged in EVALUATION.
        assertEquals(input.current, result.nextDifficulty)
    }

    @Test
    fun calibrationMode_neverAdapts() {
        val input = masteringInput()
        val result = AdaptiveModeGate.decide(Mode.CALIBRATION, engine, input)
        assertIs<AdaptiveDecision.Hold>(result.decision)
        assertEquals(input.current, result.nextDifficulty)
    }

    @Test
    fun trainingMode_delegatesToEngine_andCanAdapt() {
        val input = masteringInput()
        val result = AdaptiveModeGate.decide(Mode.TRAINING, engine, input)
        // The same input drives a StepUp when adaptation is allowed.
        assertIs<AdaptiveDecision.StepUp>(result.decision)
    }
}
