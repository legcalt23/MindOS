package com.mindos.app.features.mindboost.focus2.domain.scoring

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Focus V5 · S5 · R3.1 · T-R2-KOTLIN - the R2 falsification, executed against the PRODUCTION scorer.
 *
 * The reaudit reproduced the non-monotonicity in a JS harness (docs/engines/focus_v5/reaudit/
 * r2_falsification_harness.mjs, self-digest 2df300f3...), cross-checked by three independent probit
 * implementations agreeing to 1.3e-9. This test closes the one gap that harness could not: it runs the REAL
 * [DescriptiveScorer.scoreCells] + [Probit] on the exact cells the falsification uses and pins the d' each
 * produces. The expected values are the harness's independently-sourced numbers, NOT read off this scorer, so
 * this is a golden and not a tautology - the same discipline as [CandidateScorerGoldenTest].
 *
 * What it demonstrates: the falsified R2 gate compared an ADMITTED-subset d' (S_adm) against the full-session
 * d' (S_full). d' is NON-MONOTONE under nested exclusion - removing 5 misses RAISES it (D1), then additionally
 * removing 15 hits LOWERS it below the full session (D2). A gate keyed on that ordering is therefore
 * outcome-dependent selection, which is exactly why D-R3-01 retired it. This is the arithmetic on the real code.
 */
class DescriptiveScorerR2GoldenTest {

    private val spec = TaskSpec(
        taskId = "r2-golden",
        conditions = listOf(ConditionId.CONGRUENT),
        yesResponse = Response.LEFT,
        noResponse = Response.RIGHT,
        chanceLevel = 0.5,
        primaryCondition = ConditionId.CONGRUENT,
    )

    private fun dPrimeOf(hits: Int, misses: Int, falseAlarms: Int, correctRejections: Int): Double {
        val cell = SdtCell(
            hits = hits,
            misses = misses,
            falseAlarms = falseAlarms,
            correctRejections = correctRejections,
        )
        val result = DescriptiveScorer.scoreCells(mapOf(ConditionId.CONGRUENT to cell), spec)
        val d = result.perCondition.getValue(ConditionId.CONGRUENT).dPrime
        return (d as Descriptive.Present).value
    }

    @Test
    fun the_production_scorer_reproduces_the_harness_d_prime_to_1e_6() {
        // Base A 30H/10M/10FA/30CR and its two nested exclusions. Numbers are the harness's, independently sourced.
        assertClose(1.310847011, dPrimeOf(30, 10, 10, 30), "S_full (base A)")
        assertClose(1.680015899, dPrimeOf(30, 5, 10, 30), "D1 (base A, -5 misses)")
        assertClose(1.292907667, dPrimeOf(15, 5, 10, 30), "D2 (base A, -5 misses -15 hits)")
        // Base B 60/20/20/60 - the second base the reaudit already used.
        assertClose(1.329617456, dPrimeOf(60, 20, 20, 60), "S_full (base B)")
    }

    @Test
    fun d_prime_is_non_monotone_under_nested_exclusion_on_the_production_scorer() {
        val sFull = dPrimeOf(30, 10, 10, 30)
        val d1 = dPrimeOf(30, 5, 10, 30) // less exclusion (5 removed)
        val d2 = dPrimeOf(15, 5, 10, 30) // MORE exclusion (20 removed)
        assertTrue(d1 > sFull, "removing 5 misses must RAISE d' ($d1 vs $sFull)")
        assertTrue(d2 < sFull, "additionally removing 15 hits must LOWER d' below full ($d2 vs $sFull)")
        // The flip, on real code: the LESS-excluded subset (D1) sits ABOVE the full, the MORE-excluded (D2)
        // BELOW it - an ordering no monotone gate on the amount of exclusion can respect. This is R2, falsified.
    }

    @Test
    fun an_empty_cell_yields_a_typed_absence_never_a_fabricated_zero() {
        val result = DescriptiveScorer.scoreCells(mapOf(ConditionId.CONGRUENT to SdtCell.EMPTY), spec)
        val d = result.perCondition.getValue(ConditionId.CONGRUENT).dPrime
        assertTrue(d is Descriptive.Absent, "an empty cell must be a typed Absent, not a Present carrying 0.0")
    }

    private fun assertClose(expected: Double, actual: Double, label: String) {
        assertTrue(abs(actual - expected) < TOLERANCE, "$label: expected $expected, got $actual (tol $TOLERANCE)")
    }

    private companion object {
        // Acklam's own relative error is ~1.15e-9; the harness cross-probit spread was 1.3e-9. 1e-6 sits safely
        // above both and still catches any real formula/transcription error, matching CandidateScorerGoldenTest.
        const val TOLERANCE = 1e-6
    }
}
