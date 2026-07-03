package com.mindos.app.features.mindboost.core.scoring

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** INFRA-06 · golden thresholds pinned (changing one breaks the test) + null!=0 (n<nMin ⇒ Insufficient). */
class MinSampleGateTest {

    @Test
    fun comparatives_requireAllCells_and20PerCell_withIc() {
        val comparatives = listOf(
            ScoreMetricId.CONGRUENCY_EFFECT,
            ScoreMetricId.SWITCH_COST,
            ScoreMetricId.PRESSURE_DELTA,
            ScoreMetricId.DUAL_TASK_COST,
            ScoreMetricId.POST_ERROR,
        )
        for (m in comparatives) {
            val s = MinSampleGate.specFor(m)
            assertEquals(20, s.nMinPerCell, "$m nMin")
            assertTrue(s.requiresAllCells, "$m requiresAllCells")
            assertTrue(s.requiresConfidenceInterval, "$m IC")
        }
    }

    @Test
    fun goldenThresholds_arePinned() {
        assertEquals(30, MinSampleGate.specFor(ScoreMetricId.SSRT).nMinPerCell)
        assertEquals(40, MinSampleGate.specFor(ScoreMetricId.BRIER).nMinPerCell)
        assertEquals(12, MinSampleGate.specFor(ScoreMetricId.BUR).nMinPerCell)
        assertEquals(2, MinSampleGate.specFor(ScoreMetricId.RCI).nMinPerCell)
        assertEquals(20, MinSampleGate.specFor(ScoreMetricId.DPRIME).nMinPerCell)
        assertEquals(20, MinSampleGate.specFor(ScoreMetricId.COWANS_K).nMinPerCell)
        assertEquals(20, MinSampleGate.specFor(ScoreMetricId.ACCURACY_RT_BASE).nMinPerCell)
    }

    @Test
    fun everyMetric_hasASpec() {
        for (m in ScoreMetricId.entries) MinSampleGate.specFor(m) // getValue throws if missing
    }

    @Test
    fun belowMinN_isInsufficientData_notZero() {
        val v = MinSampleGate.evaluate(ScoreMetricId.SSRT, nEffective = 29, cellsPresent = true)
        assertTrue(v is SampleVerdict.InsufficientData)
        assertEquals(30, v.nNeeded)
        assertEquals(29, v.nHave)
    }

    @Test
    fun comparativeMissingCell_isInsufficient_evenWhenNLargeEnough() {
        val v = MinSampleGate.evaluate(ScoreMetricId.CONGRUENCY_EFFECT, nEffective = 100, cellsPresent = false)
        assertTrue(v is SampleVerdict.InsufficientData)
    }

    @Test
    fun sufficient_whenMet_carriesIcFlag() {
        val v = MinSampleGate.evaluate(ScoreMetricId.SSRT, nEffective = 30, cellsPresent = true)
        assertTrue(v is SampleVerdict.Sufficient)
        assertTrue(v.requiresIc)
        assertEquals(30, v.nEffective)
    }
}
