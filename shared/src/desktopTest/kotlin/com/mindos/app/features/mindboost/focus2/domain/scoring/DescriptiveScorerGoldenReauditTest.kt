package com.mindos.app.features.mindboost.focus2.domain.scoring

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * S5 · R3 REAUDIT V2 · Phase 2.2 — PRODUCTION-SCORER PARITY GOLDEN.
 *
 * Pins the REAL [DescriptiveScorer] (loglinear Hautus +0.5/+1.0 correction + Acklam probit, [Probit.zScore])
 * to the three d′ values the cross-model reaudit mandate specified. The Node harness
 * (`docs/engines/focus_v5/reaudit/r2_falsification_harness.mjs`) is only `REIMPLEMENTATION_REPRODUCED`; THIS test
 * is the `EXECUTED_PRIMARY` evidence that the production Kotlin scorer produces the same numbers — it must fail if
 * anyone changes: the loglinear correction, the signal/noise-trial definitions, the probit, the rounding, the
 * order of operations, or the extreme-rate handling.
 *
 * d′ is computed purely from the cell COUNTS (no trial classification, no chanceLevel, no RT), which is exactly
 * the surface the R2 falsification exercises.
 */
class DescriptiveScorerGoldenReauditTest {

    private val spec = TaskSpec(
        taskId = "reaudit-golden",
        conditions = listOf(ConditionId.CONGRUENT),
        yesResponse = Response.LEFT,
        noResponse = Response.RIGHT,
        chanceLevel = 0.5,
        primaryCondition = ConditionId.CONGRUENT,
    )

    private fun dPrimeOf(hits: Int, misses: Int, fa: Int, cr: Int): Double {
        val cell = SdtCell(hits = hits, misses = misses, falseAlarms = fa, correctRejections = cr)
        val result = DescriptiveScorer.scoreCells(mapOf(ConditionId.CONGRUENT to cell), spec)
        val d = result.perCondition[ConditionId.CONGRUENT]?.dPrime
        assertTrue(d is Descriptive.Present, "d' must be Present for a sufficient cell, was $d")
        return (d as Descriptive.Present).value
    }

    @Test
    fun golden_dPrime_pins_the_production_scorer_formula() {
        // Tolerance 1e-6: Acklam's inverse-normal approximation error is ~1.15e-9, far below this bound.
        assertEquals(1.310847010469, dPrimeOf(30, 10, 10, 30), 1e-6, "S_full 30H/10M/10FA/30CR")
        assertEquals(1.680015897775, dPrimeOf(30, 5, 10, 30), 1e-6, "D1 exclude 5 misses (30H/5M/10FA/30CR)")
        assertEquals(1.292907666197, dPrimeOf(15, 5, 10, 30), 1e-6, "D2 exclude 5 misses + 15 hits (15H/5M/10FA/30CR)")
    }

    @Test
    fun golden_non_monotonic_flip_is_reproduced_on_the_real_scorer() {
        val sFull = dPrimeOf(30, 10, 10, 30)
        val d1 = dPrimeOf(30, 5, 10, 30)   // less exclusion
        val d2 = dPrimeOf(15, 5, 10, 30)   // MORE exclusion
        assertTrue(d1 > sFull, "D1 (less exclusion) d'=$d1 must exceed S_full=$sFull -> would gate Absent")
        assertTrue(d2 <= sFull, "D2 (more exclusion) d'=$d2 must be <= S_full=$sFull -> would gate Present (the flip)")
    }
}
