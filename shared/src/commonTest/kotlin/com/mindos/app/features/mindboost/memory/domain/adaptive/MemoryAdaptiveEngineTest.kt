package com.mindos.app.features.mindboost.memory.domain.adaptive

import com.mindos.app.features.mindboost.memory.domain.catalog.MemoryExerciseCatalog
import com.mindos.app.features.mindboost.memory.domain.metrics.MemoryMetricKey
import com.mindos.app.features.mindboost.memory.domain.metrics.MemoryMetricSet
import com.mindos.app.features.mindboost.memory.domain.metrics.MemoryMetricUnit
import com.mindos.app.features.mindboost.memory.domain.metrics.MemoryMetricValue
import com.mindos.app.features.mindboost.memory.domain.model.AxisLevel
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyAxis
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyVector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MemoryAdaptiveEngineTest {

    private val engine = DefaultMemoryAdaptiveEngine()
    private val ex3 = MemoryExerciseCatalog.EX3_LISTA_ESTRATEGIA // governing LOAD; DELAY & INTERFERENCE relevant
    private val midDifficulty = MemoryDifficultyVector.EASIEST.adjust(MemoryDifficultyAxis.LOAD, AxisLevel(0.5))

    /** Base input; tests vary it via `.copy(...)` to keep call sites small. */
    private val base = MemoryAdaptiveInput(
        exercise = ex3,
        current = midDifficulty,
        recentBlock = emptyMap(),
    )

    private fun entry(key: MemoryMetricKey, value: Double, unit: MemoryMetricUnit) =
        key to MemoryMetricValue(key, value, n = 12, confident = true, unit = unit)

    private fun block(
        immediate: Double? = null,
        delayed: Double? = null,
        intrusion: Double? = null,
        rtMs: Double? = null,
    ): MemoryMetricSet = listOfNotNull(
        immediate?.let { entry(MemoryMetricKey.IMMEDIATE_RECALL_ACCURACY, it, MemoryMetricUnit.PROPORTION) },
        delayed?.let { entry(MemoryMetricKey.DELAYED_RECALL_ACCURACY, it, MemoryMetricUnit.PROPORTION) },
        intrusion?.let { entry(MemoryMetricKey.INTRUSION_RATE, it, MemoryMetricUnit.PROPORTION) },
        rtMs?.let { entry(MemoryMetricKey.RETRIEVAL_SPEED, it, MemoryMetricUnit.MILLISECONDS) },
    ).toMap()

    @Test
    fun sustainedFrustration_stopsSession() {
        val r = engine.decide(base.copy(recentBlock = block(immediate = 0.2), consecutiveFrustrationBlocks = 3))
        assertIs<MemoryAdaptiveDecision.StopSession>(r.decision)
    }

    @Test
    fun emergingFatigue_proposesRest() {
        val r = engine.decide(
            base.copy(
                recentBlock = block(immediate = 0.7),
                fatigue = MemoryFatigueSignal(MemoryFatigueLevel.MODERATE, emerging = true, sustained = false),
            ),
        )
        assertIs<MemoryAdaptiveDecision.ProposeRest>(r.decision)
    }

    @Test
    fun poorCalibration_activatesMetacognition() {
        val r = engine.decide(base.copy(recentBlock = block(immediate = 0.7), confidenceBias = 0.5))
        assertIs<MemoryAdaptiveDecision.ActivateMetacognition>(r.decision)
    }

    @Test
    fun frustrationZone_stepsDownBig_reducingLastRaisedAxisFirst() {
        val r = engine.decide(
            base.copy(
                recentBlock = block(immediate = 0.2),
                current = midDifficulty.adjust(MemoryDifficultyAxis.DELAY, AxisLevel(0.5)),
                lastRaisedAxis = MemoryDifficultyAxis.DELAY,
            ),
        )
        val decision = r.decision
        assertIs<MemoryAdaptiveDecision.StepDown>(decision)
        assertEquals(MemoryDifficultyAxis.DELAY, decision.axis)
        assertTrue(r.nextDifficulty.delay.value < 0.5, "DELAY should be reduced.")
    }

    @Test
    fun suboptimalLow_stepsDownSmallOnGoverning() {
        val r = engine.decide(base.copy(recentBlock = block(immediate = 0.46)))
        val decision = r.decision
        assertIs<MemoryAdaptiveDecision.StepDown>(decision)
        assertEquals(MemoryDifficultyAxis.LOAD, decision.axis)
    }

    @Test
    fun optimalNoWeakness_holds() {
        val r = engine.decide(base.copy(recentBlock = block(immediate = 0.70)))
        assertIs<MemoryAdaptiveDecision.Hold>(r.decision)
        assertEquals(MemoryZone.OPTIMAL, r.zone)
    }

    @Test
    fun optimalWithIntrusions_raisesInterference() {
        val r = engine.decide(base.copy(recentBlock = block(immediate = 0.70, intrusion = 0.30)))
        val decision = r.decision
        assertIs<MemoryAdaptiveDecision.StepUp>(decision)
        assertEquals(MemoryDifficultyAxis.INTERFERENCE, decision.axis)
    }

    @Test
    fun optimalWithDelayedLag_raisesDelay() {
        val r = engine.decide(base.copy(recentBlock = block(immediate = 0.90, delayed = 0.60)))
        val decision = r.decision
        assertIs<MemoryAdaptiveDecision.StepUp>(decision)
        assertEquals(MemoryDifficultyAxis.DELAY, decision.axis)
    }

    @Test
    fun accurateButSlow_worksSpeedWithoutChangingDifficulty() {
        val r = engine.decide(base.copy(recentBlock = block(immediate = 0.80, rtMs = 3900.0)))
        assertIs<MemoryAdaptiveDecision.WorkSpeed>(r.decision)
        assertEquals(midDifficulty, r.nextDifficulty, "WorkSpeed must not change difficulty.")
    }

    @Test
    fun tooEasy_stepsUpBig() {
        val r = engine.decide(base.copy(recentBlock = block(immediate = 1.0, rtMs = 500.0)))
        assertEquals(MemoryZone.TOO_EASY, r.zone)
        assertIs<MemoryAdaptiveDecision.StepUp>(r.decision)
        assertTrue(r.nextDifficulty.load.value > midDifficulty.load.value)
    }

    @Test
    fun hysteresis_holdsStepUpRightAfterAChange() {
        // High zone that would step up, but a change just happened → hold.
        val r = engine.decide(base.copy(recentBlock = block(immediate = 0.85), blocksSinceChange = 0))
        assertIs<MemoryAdaptiveDecision.Hold>(r.decision)
    }

    @Test
    fun stepDownBypassesHysteresisAsSafety() {
        // Frustration must step down even right after a change (safety > hysteresis).
        val r = engine.decide(base.copy(recentBlock = block(immediate = 0.2), blocksSinceChange = 0))
        assertIs<MemoryAdaptiveDecision.StepDown>(r.decision)
    }
}
