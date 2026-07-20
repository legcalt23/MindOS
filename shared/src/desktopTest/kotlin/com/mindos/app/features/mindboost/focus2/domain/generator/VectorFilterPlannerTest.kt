package com.mindos.app.features.mindboost.focus2.domain.generator

import com.mindos.app.features.mindboost.core.runtime.DeterministicRng
import com.mindos.app.features.mindboost.core.runtime.RngVersion
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.core.scoring.MinSampleGate
import com.mindos.app.features.mindboost.core.scoring.ScoreMetricId
import com.mindos.app.features.mindboost.focus2.domain.scoring.ConditionId
import com.mindos.app.features.mindboost.focus2.domain.scoring.Response
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Focus V5 · S3 · tarea (2) — the planner's guarantees.
 *
 * The load-bearing test here is [a_bernoulli_draw_policy_would_starve_cells_below_the_floor]. Without it, the
 * quota assertions are unfalsifiable praise: "every cell has 28" passes just as happily under a policy that
 * happens to average 28, and the reader has no way to tell whether the constructed quota is doing any work.
 * That test measures the alternative and shows it failing, which is what makes the rest of the file mean
 * something.
 */
class VectorFilterPlannerTest {

    private fun seed(value: Long) = SessionSeed(value, RngVersion.V1)

    @Test
    fun every_cell_holds_exactly_the_quota_for_every_seed_tried() {
        val spec = VectorFilterSpec()
        repeat(SEED_SWEEP) { i ->
            val plan = VectorFilterPlanner.plan(spec, seed(BASE_SEED + i))
            val counts = plan.cellCounts()
            assertEquals(VectorFilterSpec.CELL_COUNT, counts.size, "seed $i produced ${counts.size} cells")
            counts.forEach { (cell, n) ->
                assertEquals(spec.trialsPerCell, n, "seed $i, cell $cell held $n trials")
            }
        }
    }

    /**
     * The discriminating half — the reason the quota exists.
     *
     * This replays the v1 draw policy verbatim (`E2FiltroGenerator.kt:66-67`: an independent coin flip for
     * direction and an independent Bernoulli for congruency) over the same trial budget, and counts how often a
     * cell lands below `MinSampleGate.specFor(DPRIME).nMinPerCell`. Every such session would return
     * `Absent(INSUFFICIENT_N)` from the scorer and produce no measurement at all.
     *
     * The assertion is deliberately weak — "it happens at least once in 200 seeds" — because the exact rate is
     * not the point and pinning it would make this test a hostage to the RNG. The point is that the rate is not
     * zero, so the constructed quota is removing a real failure mode rather than decorating an already-safe one.
     */
    @Test
    fun a_bernoulli_draw_policy_would_starve_cells_below_the_floor() {
        val floor = MinSampleGate.specFor(ScoreMetricId.DPRIME).nMinPerCell
        val spec = VectorFilterSpec()
        var starved = 0
        repeat(SEED_SWEEP) { i ->
            val rng = DeterministicRng.forSeed(seed(BASE_SEED + i))
            val counts = mutableMapOf<Pair<ConditionId, Boolean>, Int>()
            repeat(spec.totalTrials) {
                val signal = rng.nextBoolean()
                val condition =
                    if (rng.nextDouble() >= HALF) ConditionId.CONGRUENT else ConditionId.INCONGRUENT
                counts[condition to signal] = (counts[condition to signal] ?: 0) + 1
            }
            if (counts.size < VectorFilterSpec.CELL_COUNT || counts.values.any { it < floor }) starved++
        }
        assertTrue(
            starved > 0,
            "In $SEED_SWEEP seeds the Bernoulli policy never starved a cell below $floor. Either the floor " +
                "moved or the budget grew; re-derive whether the constructed quota is still necessary before " +
                "weakening it.",
        )
        // And the constructed quota starves none of them, over the identical seed sweep.
        val quotaStarved = (0 until SEED_SWEEP).count { i ->
            VectorFilterPlanner.plan(spec, seed(BASE_SEED + i)).cellCounts().values.any { it < floor }
        }
        assertEquals(0, quotaStarved, "the constructed quota must never starve a cell")
    }

    /**
     * Golden. The expected permutation was computed by an INDEPENDENT implementation — splitmix64, the bounded
     * rejection draw and Fisher-Yates re-written from the spec in another language, in arbitrary-precision
     * integers — and never read off this planner. A golden whose expectations come from its own subject is a
     * tautology that passes against any implementation, including a wrong one.
     *
     * These are LITERALS. A range check or a self-consistency check would survive an algorithm swap and would
     * therefore pin nothing.
     */
    @Test
    fun the_plan_permutation_is_pinned_against_an_independently_computed_sequence() {
        val plan = VectorFilterPlanner.plan(VectorFilterSpec(), seed(PINNED_SEED))
        val actual = plan.trials.take(PINNED_PREFIX).map { trial ->
            val condition = if (trial.conditionId == ConditionId.CONGRUENT) "C" else "I"
            condition + if (trial.isSignal) "S" else "N"
        }
        val expected = listOf("IS", "CN", "CS", "CN", "CN", "IN", "IS", "CN", "IN", "CS", "CN", "IS")
        assertEquals(expected, actual, "the shuffle changed; bump GENERATOR_VERSION or restore the algorithm")
    }

    @Test
    fun the_same_seed_reproduces_the_plan_and_a_different_seed_reorders_it() {
        val spec = VectorFilterSpec()
        val a = VectorFilterPlanner.plan(spec, seed(PINNED_SEED))
        val b = VectorFilterPlanner.plan(spec, seed(PINNED_SEED))
        assertEquals(a, b, "same seed must reproduce the plan exactly")

        val c = VectorFilterPlanner.plan(spec, seed(PINNED_SEED + 1))
        assertTrue(
            a.trials.map { it.conditionId to it.isSignal } != c.trials.map { it.conditionId to it.isSignal },
            "a different seed must produce a different ORDER (the counts stay identical by construction)",
        )
        assertEquals(a.cellCounts(), c.cellCounts(), "counts are a guarantee, not a consequence of the seed")
    }

    @Test
    fun the_correct_response_is_derived_from_the_centre_and_never_contradicts_the_signal_axis() {
        val plan = VectorFilterPlanner.plan(VectorFilterSpec(), seed(PINNED_SEED))
        plan.trials.forEach { trial ->
            val expected = if (trial.centrePointsLeft) Response.LEFT else Response.RIGHT
            assertEquals(expected, trial.correctResponse, "trial ${trial.index}")
            assertEquals(trial.centrePointsLeft, trial.isSignal, "signal axis must be the centre direction")
        }
        // Both response sides are actually used, or the "derivation" would be vacuously consistent.
        assertEquals(
            setOf(Response.LEFT, Response.RIGHT),
            plan.trials.map { it.correctResponse }.toSet(),
            "a plan that only ever asks for one side cannot measure discrimination",
        )
    }

    @Test
    fun the_quota_clears_the_central_sample_floor_with_margin() {
        val floor = MinSampleGate.specFor(ScoreMetricId.DPRIME).nMinPerCell
        assertTrue(
            VectorFilterSpec.DEFAULT_TRIALS_PER_CELL > floor,
            "the default quota (${VectorFilterSpec.DEFAULT_TRIALS_PER_CELL}) must exceed the d' floor ($floor)",
        )
    }

    @Test
    fun a_spec_that_cannot_be_measured_is_refused_at_construction() {
        listOf(
            { VectorFilterSpec(trialsPerCell = 0) },
            { VectorFilterSpec(flankerCount = 0) },
            { VectorFilterSpec(separation = 1.5) },
            { VectorFilterSpec(similarity = -0.1) },
            { VectorFilterSpec(responseWindowMs = 0) },
            { VectorFilterSpec(interStimulusDelayMs = -1) },
        ).forEachIndexed { i, build ->
            val threw = runCatching { build() }.isFailure
            assertTrue(threw, "invalid spec #$i was accepted")
        }
    }

    @Test
    fun a_trial_whose_congruency_contradicts_its_condition_cannot_be_constructed() {
        val threw = runCatching {
            PlannedTrial(
                index = 0,
                conditionId = ConditionId.INCONGRUENT,
                centrePointsLeft = true,
                congruent = true, // contradicts INCONGRUENT
                flankerCount = 4,
                separation = 0.5,
                similarity = 0.5,
                responseWindowMs = 1200L,
                interStimulusDelayMs = 600L,
            )
        }.isFailure
        assertTrue(threw, "a trial may not disagree with its own condition label")
    }

    private companion object {
        const val SEED_SWEEP = 200
        const val BASE_SEED = 0x1000L
        const val PINNED_SEED = 0x5EEDF0C25L
        const val PINNED_PREFIX = 12
        const val HALF = 0.5
    }
}
