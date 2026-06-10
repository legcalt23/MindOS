package com.mindos.app.features.mindboost.memory.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MemoryDifficultyVectorTest {

    @Test
    fun adjust_changesOnlyTargetedAxis() {
        val base = MemoryDifficultyVector.EASIEST
        val adjusted = base.adjust(MemoryDifficultyAxis.DELAY, AxisLevel(0.5))
        assertEquals(0.5, adjusted.level(MemoryDifficultyAxis.DELAY).value)
        assertEquals(0.0, adjusted.level(MemoryDifficultyAxis.INTERFERENCE).value)
        // Original is untouched (immutability).
        assertEquals(0.0, base.level(MemoryDifficultyAxis.DELAY).value)
    }

    @Test
    fun stepUpDown_areInverseWithinRange() {
        val v = MemoryDifficultyVector.EASIEST.stepUp(MemoryDifficultyAxis.LOAD, 0.3)
        assertTrue(v.level(MemoryDifficultyAxis.LOAD).value > 0.0)
        val back = v.stepDown(MemoryDifficultyAxis.LOAD, 0.3)
        assertEquals(0.0, back.level(MemoryDifficultyAxis.LOAD).value, absoluteTolerance = 1e-9)
    }

    @Test
    fun cme_easiestIsRetrievalFloor() {
        // All axes 0 → loadFactor 1 · P_weight 1.0 · R_weight 0.7 · all (1+0) → 0.7.
        assertEquals(0.7, MemoryDifficultyVector.EASIEST.cme(), absoluteTolerance = 1e-9)
    }

    @Test
    fun cme_hardestWithDefaultSensitivityMatchesFormula() {
        val hardest = MemoryDifficultyVector(
            load = AxisLevel.MAX,
            interference = AxisLevel.MAX,
            delay = AxisLevel.MAX,
            similarity = AxisLevel.MAX,
            depth = AxisLevel.MAX,
            retrieval = AxisLevel.MAX,
            context = AxisLevel.MAX,
            concurrency = AxisLevel.MAX,
        )
        // 2 · 2 · 2 · 2 · 1.5 · 1.5 · 2 · 2 = 144.0
        assertEquals(144.0, hardest.cme(), absoluteTolerance = 1e-9)
    }

    @Test
    fun cme_isMonotonicInEveryMultiplicativeAxis() {
        val base = MemoryDifficultyVector.EASIEST.cme()
        MemoryDifficultyAxis.entries.forEach { axis ->
            val raised = MemoryDifficultyVector.EASIEST.adjust(axis, AxisLevel.MAX).cme()
            assertTrue(raised > base, "Raising $axis should increase CME ($raised !> $base).")
        }
    }

    @Test
    fun cme_higherSensitivityRaisesDifficultyOfMultiplicativeAxes() {
        val v = MemoryDifficultyVector.EASIEST.adjust(MemoryDifficultyAxis.INTERFERENCE, AxisLevel.MAX)
        val low = v.cme(CmeSensitivity(alpha = 0.5))
        val high = v.cme(CmeSensitivity(alpha = 2.0))
        assertTrue(high > low, "Higher alpha must raise CME for an interference-loaded trial.")
    }

    @Test
    fun cmeSensitivity_rejectsNegative() {
        assertFailsWith<IllegalArgumentException> { CmeSensitivity(alpha = -0.1) }
    }

    @Test
    fun overallLoad_isMeanOfAxes() {
        val v = MemoryDifficultyVector.EASIEST
            .adjust(MemoryDifficultyAxis.LOAD, AxisLevel.MAX)
            .adjust(MemoryDifficultyAxis.DELAY, AxisLevel.MAX)
        // Two of eight axes at 1.0 → mean 0.25.
        assertEquals(0.25, v.overallLoad(), absoluteTolerance = 1e-9)
    }
}
