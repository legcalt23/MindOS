package com.mindos.app.features.mindboost.focus.domain.adaptive

import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Verifies the hysteresis damps progression: even with a steady stream of
 * mastering blocks, difficulty does not step up every single block — the engine
 * holds for a beat after each change, preventing rapid oscillation.
 */
class AntiOscillationTest {

    private val engine = DefaultAdaptiveEngine()

    private fun masteringInput(blocksSinceChange: Int) = AdaptiveInput(
        exercise = ExerciseCatalog.E1_VIGIA,
        current = DifficultyVector.EASIEST,
        recentBlock = FocusTestFixtures.metricSet(
            MetricKey.ACCURACY to 0.95,
            MetricKey.RT_CV to 0.14,
            MetricKey.FALSE_ALARMS to 0.0,
        ),
        recentTrials = FocusTestFixtures.hits(listOf(300, 305, 310)),
        perAxisMastery = emptyMap(),
        fatigue = FocusTestFixtures.noFatigueMeasured,
        blocksSinceChange = blocksSinceChange,
    )

    @Test
    fun consecutiveMasteringBlocks_doNotStepUpEveryTime() {
        var blocksSinceChange = Int.MAX_VALUE
        var stepUps = 0
        val totalBlocks = 6
        repeat(totalBlocks) {
            val decision = engine.decide(masteringInput(blocksSinceChange)).decision
            if (decision is AdaptiveDecision.StepUp) {
                stepUps++
                blocksSinceChange = 0
            } else {
                blocksSinceChange++
            }
        }
        // Hysteresis halves the up-rate: progress, but not on every block.
        assertTrue(stepUps in 1 until totalBlocks, "expected damped step-ups, got $stepUps")
    }

    @Test
    fun rightAfterAChange_holdsInsteadOfSteppingUpAgain() {
        val decision = engine.decide(masteringInput(blocksSinceChange = 0)).decision
        assertTrue(decision is AdaptiveDecision.Hold)
    }
}
