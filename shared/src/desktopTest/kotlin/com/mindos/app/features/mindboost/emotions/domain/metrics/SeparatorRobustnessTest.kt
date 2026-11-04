@file:Suppress("MagicNumber", "MaxLineLength") // The counts ARE the fixtures; the arithmetic reads best whole.

package com.mindos.app.features.mindboost.emotions.domain.metrics

import com.mindos.app.features.mindboost.core.scoring.MinSampleGate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Ola 3 · R3 · P4 — the robustness index and, just as much, its refusals.
 *
 * The tests that matter here are the pairs: the same two accuracies with four times the data flip the verdict,
 * which is what proves a refusal is about sample size rather than about the effect being absent.
 */
class SeparatorRobustnessTest {

    private fun cells(calmCorrect: Int, calmTotal: Int, pressureCorrect: Int, pressureTotal: Int) =
        PressureCells(calmCorrect, calmTotal, pressureCorrect, pressureTotal)

    private fun measured(cells: PressureCells): SeparatorRobustness.Measured =
        SeparatorRobustnessIndex.evaluate(cells).let { it as? SeparatorRobustness.Measured ?: fail("expected an index, got $it") }

    private fun gapOf(cells: PressureCells): RobustnessGap =
        SeparatorRobustnessIndex.evaluate(cells).let { (it as? SeparatorRobustness.Unavailable ?: fail("expected a refusal, got $it")).reason }

    // ---- the refusals ------------------------------------------------------------------------------------------

    @Test
    fun a_session_that_never_served_pressure_has_no_index_rather_than_a_flattering_one() {
        assertEquals(RobustnessGap.NO_PRESSURE_SERVED, gapOf(cells(36, 40, 0, 0)))
    }

    /**
     * The measurement that decided the geometry, kept as a test so it cannot quietly stop being true.
     *
     * A user whose accuracy really does fall from .90 to .70 gets NO verdict from one session — not at today's
     * ten calm trials and not at the sixteen R3 sketches. Pool four sessions of the SAME accuracies and the
     * verdict appears. Nothing about the effect changed between these three rows; only how much of it was seen.
     */
    @Test
    fun one_session_cannot_resolve_a_real_pressure_cost_at_either_candidate_geometry() {
        assertEquals(RobustnessGap.TOO_FEW_TRIALS, gapOf(cells(9, 10, 14, 20)), "3x10, one session")
        assertEquals(RobustnessGap.TOO_FEW_TRIALS, gapOf(cells(14, 16, 22, 32)), "3x16, one session")

        val pooled = measured(cells(36, 40, 56, 80))
        assertEquals(RobustnessVerdict.PRESSURE_COSTS_ACCURACY, pooled.verdict)
        assertTrue(pooled.interval.high < 1.0, "pooled interval ${pooled.interval} should sit below 1")
    }

    /**
     * A difference is judged by its SMALLER cell. Forty calm trials against two pressure ones are forty-two
     * trials and no comparison — and a gate that summed them would call that sufficient, which is why the
     * discriminating half below uses FEWER total trials and passes.
     */
    @Test
    fun the_floor_reads_the_smaller_cell_and_never_the_sum() {
        val lopsided = cells(36, 40, 2, 2)
        assertTrue(lopsided.calmTotal + lopsided.pressureTotal > 40, "the fixture must out-total the balanced one")
        assertEquals(RobustnessGap.TOO_FEW_TRIALS, gapOf(lopsided))

        val balanced = cells(18, 20, 14, 20)
        assertTrue(balanced.calmTotal + balanced.pressureTotal < lopsided.calmTotal + lopsided.pressureTotal)
        measured(balanced)
    }

    /**
     * The boundary sits where the central gate says it does today, checked against the gate rather than against
     * a number written here — so this suite has no second copy of the threshold to drift from.
     *
     * **Declared ceiling: this does NOT prove the kernel reads the gate at runtime.** Verified by mutation —
     * replacing the gate call with a literal of the same value leaves every test in this file green, because
     * agreement with the gate is exactly what a correct copy of it also produces. No behavioural test can
     * separate the two; what keeps them together is that the number appears nowhere in the kernel, so inlining
     * it is a visible diff rather than a silent divergence.
     */
    @Test
    fun the_floor_sits_where_the_central_gate_puts_it() {
        val floor = MinSampleGate.specFor(SeparatorRobustnessIndex.GOVERNING_METRIC).nMinPerCell
        assertEquals(RobustnessGap.TOO_FEW_TRIALS, gapOf(cells(floor - 1, floor - 1, floor - 2, floor - 1)))
        measured(cells(floor, floor, floor - 4, floor))
    }

    /**
     * The degenerate points, decided from RAW COUNTS and left as refusals rather than smoothed away. A
     * correction applied here would hand back a confident number where the estimator has none.
     */
    @Test
    fun a_cell_with_nothing_correct_yields_no_index() {
        assertEquals(RobustnessGap.UNDEFINED_AT_CELL_BOUNDARY, gapOf(cells(0, 30, 15, 30)), "calm 0 makes the ratio undefined")
        assertEquals(RobustnessGap.UNDEFINED_AT_CELL_BOUNDARY, gapOf(cells(25, 30, 0, 30)), "pressure 0 has no interval")

        // Discriminating half: one correct answer is enough to leave the boundary, so nothing was clamped wholesale.
        measured(cells(1, 30, 1, 30))
    }

    // ---- the readings ------------------------------------------------------------------------------------------

    /**
     * The direction claim comes from the INTERVAL, never from the point estimate — the rule the charter states
     * for exactly this shape of reading, and the one a suite is likeliest to leave untested because both
     * versions agree everywhere except in the band that matters.
     *
     * Both rows below share an index of .778. The first is at the floor, where the interval still reaches past 1
     * and the honest answer is that nothing was resolved; the second has five times the evidence and the same
     * index, and only there does the claim become sayable. A kernel that judged by the index alone would call
     * them both a cost.
     */
    @Test
    fun a_direction_is_claimed_from_the_interval_and_not_from_the_index() {
        val atTheFloor = measured(cells(18, 20, 14, 20))
        assertTrue(atTheFloor.index < 1.0, "the fixture must point downward, or this test proves nothing")
        assertTrue(atTheFloor.interval.high > 1.0, "the fixture's interval must still contain 1")
        assertEquals(RobustnessVerdict.NO_DETECTABLE_COST, atTheFloor.verdict)

        val resolved = measured(cells(90, 100, 70, 100))
        assertEquals(atTheFloor.index, resolved.index, 1e-12, "the two rows must hold the index constant")
        assertEquals(RobustnessVerdict.PRESSURE_COSTS_ACCURACY, resolved.verdict)
    }

    @Test
    fun an_index_of_one_with_plenty_of_data_still_says_no_detectable_cost_and_not_robust() {
        val flat = measured(cells(51, 60, 51, 60))
        assertEquals(1.0, flat.index, 1e-12)
        assertEquals(RobustnessVerdict.NO_DETECTABLE_COST, flat.verdict)
        assertTrue(flat.interval.low < 1.0 && flat.interval.high > 1.0, "a null effect's interval must contain 1")
    }

    @Test
    fun the_index_is_pressure_accuracy_over_calm_accuracy() {
        assertEquals(0.75 / 0.9, measured(cells(36, 40, 60, 80)).index, 1e-12)
    }

    /**
     * The interval is multiplicative, which is the shape a ratio actually has: symmetric bounds around .8 with a
     * wide standard error reach below zero, and a proportion ratio cannot go there.
     */
    @Test
    fun the_interval_is_log_symmetric_and_never_reaches_zero() {
        val reading = measured(cells(36, 40, 56, 80))
        assertTrue(reading.interval.low > 0.0, "lower bound ${reading.interval.low} must stay positive")
        assertEquals(
            reading.index / reading.interval.low,
            reading.interval.high / reading.index,
            1e-9,
            "the bounds are not the same multiplicative distance from the index",
        )
    }

    /** More of the same evidence must narrow the interval; if it does not, the sample size is decorative. */
    @Test
    fun more_evidence_narrows_the_interval() {
        val small = measured(cells(18, 20, 14, 20))
        val large = measured(cells(90, 100, 70, 100))
        assertEquals(small.index, large.index, 1e-12, "the fixtures must hold the accuracies constant")
        assertTrue(
            large.interval.high - large.interval.low < small.interval.high - small.interval.low,
            "five times the data did not narrow ${small.interval} to anything tighter than ${large.interval}",
        )
    }

    @Test
    fun pooling_adds_cells_rather_than_averaging_them() {
        val a = cells(9, 10, 14, 20)
        val b = cells(8, 10, 13, 20)
        assertEquals(cells(17, 20, 27, 40), a + b)
        assertEquals(a, PressureCells.EMPTY + a)
    }
}
