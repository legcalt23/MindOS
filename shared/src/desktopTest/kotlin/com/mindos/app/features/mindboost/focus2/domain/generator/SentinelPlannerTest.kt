package com.mindos.app.features.mindboost.focus2.domain.generator

import com.mindos.app.features.mindboost.core.runtime.DeterministicRng
import com.mindos.app.features.mindboost.core.runtime.RngVersion
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.core.scoring.MinSampleGate
import com.mindos.app.features.mindboost.core.scoring.ScoreMetricId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Focus V5 · S4 · tarea (1) — the vigilance planner's guarantees.
 *
 * Two tests carry the weight, and both exist because a quota assertion on its own is unfalsifiable praise:
 *
 *  - [a_bernoulli_target_draw_would_starve_the_signal_cell_below_the_floor] measures the ALTERNATIVE — sampling
 *    rare targets by coin flip instead of constructing them — and shows it dropping the signal cell under the
 *    d' floor. Without it, "the signal cell always holds 24" passes just as happily under a policy that merely
 *    averages 24, and nothing tells the reader the constructed quota is doing any work.
 *  - [the_composition_matches_the_independently_pinned_draw_primitive] is the determinism anchor. It does NOT
 *    re-pin splitmix64 or the shuffle — those are pinned against an EXTERNAL sequence by `VectorFilterPlannerTest`
 *    (`the_plan_permutation_is_pinned_against_an_independently_computed_sequence`), and this planner reuses that
 *    exact `DeterministicPlanDraw`. What is NEW to the vigilance planner, and therefore what this pins, is its
 *    COMPOSITION of that anchored primitive: the signal-first quota layout, the draw ORDER (shuffle first, then
 *    one jitter draw per trial), and the jitter arithmetic. A planner that drew jitter before shuffling, or laid
 *    the quota noise-first, or used a different jitter formula, turns this red — while the algorithm underneath
 *    stays covered by the flanker's external anchor.
 */
class SentinelPlannerTest {

    private fun seed(value: Long) = SessionSeed(value, RngVersion.V1)

    @Test
    fun every_signal_and_noise_count_is_exactly_the_constructed_quota_for_every_seed_tried() {
        val spec = SentinelSpec()
        repeat(SEED_SWEEP) { i ->
            val plan = SentinelPlanner.plan(spec, seed(BASE_SEED + i))
            val counts = plan.cellCounts()
            assertEquals(spec.signalTrials, counts[true], "seed $i, signal cell")
            assertEquals(spec.noiseTrials, counts[false], "seed $i, noise cell")
            assertEquals(spec.totalTrials, plan.trials.size, "seed $i, total")
        }
    }

    /**
     * The discriminating half — the reason the quota is constructed rather than sampled.
     *
     * Rare targets at a 20% base rate drawn by an independent coin flip put the SIGNAL cell below
     * `MinSampleGate.specFor(DPRIME).nMinPerCell` a meaningful fraction of the time — and every such session
     * returns `Absent(INSUFFICIENT_N)` from the scorer, producing no vigilance measurement at all, for a reason
     * the participant neither sees nor caused. The assertion is deliberately weak ("at least once in the sweep")
     * so it is not hostage to the RNG; the point is only that the rate is not zero.
     */
    @Test
    fun a_bernoulli_target_draw_would_starve_the_signal_cell_below_the_floor() {
        val floor = MinSampleGate.specFor(ScoreMetricId.DPRIME).nMinPerCell
        val spec = SentinelSpec()
        val targetRate = spec.signalTrials.toDouble() / spec.totalTrials
        var starved = 0
        repeat(SEED_SWEEP) { i ->
            val rng = DeterministicRng.forSeed(seed(BASE_SEED + i))
            val signals = (0 until spec.totalTrials).count { rng.nextDouble() < targetRate }
            if (signals < floor) starved++
        }
        assertTrue(
            starved > 0,
            "In $SEED_SWEEP seeds a Bernoulli target draw never starved the signal cell below $floor. Either " +
                "the floor moved or the target count grew; re-derive whether the constructed quota is still " +
                "necessary before weakening it.",
        )
        // And the constructed quota starves it in NONE of them, over the identical sweep.
        val quotaStarved = (0 until SEED_SWEEP).count { i ->
            (SentinelPlanner.plan(spec, seed(BASE_SEED + i)).cellCounts()[true] ?: 0) < floor
        }
        assertEquals(0, quotaStarved, "the constructed quota must never starve the signal cell")
    }

    @Test
    fun every_inter_stimulus_gap_stays_within_the_jitter_band_and_stays_positive() {
        val spec = SentinelSpec()
        val low = spec.baseIsiMs - spec.isiJitterMs
        val high = spec.baseIsiMs + spec.isiJitterMs
        repeat(SEED_SWEEP) { i ->
            SentinelPlanner.plan(spec, seed(BASE_SEED + i)).trials.forEach { trial ->
                assertTrue(
                    trial.interStimulusDelayMs in low..high && trial.interStimulusDelayMs > 0,
                    "seed $i, trial ${trial.index}: gap ${trial.interStimulusDelayMs} outside [$low, $high]",
                )
            }
        }
    }

    /**
     * Unpredictable ISI is the whole confound answer, so a jitter that is secretly a constant would defeat the
     * exercise while every bound check still passed. This proves the gaps actually take several distinct values.
     */
    @Test
    fun the_gaps_actually_vary_rather_than_being_a_constant_dressed_as_jitter() {
        val distinctGaps = SentinelPlanner.plan(SentinelSpec(), seed(PINNED_SEED))
            .trials.map { it.interStimulusDelayMs }.toSet()
        assertTrue(distinctGaps.size > MIN_DISTINCT_GAPS, "only ${distinctGaps.size} distinct gaps — jitter is flat")
    }

    /** The determinism anchor. See the class KDoc for why this pins the COMPOSITION, not the algorithm. */
    @Test
    fun the_composition_matches_the_independently_pinned_draw_primitive() {
        val spec = SentinelSpec()
        // The expected sequence is built by applying the independently-anchored DeterministicPlanDraw in the
        // order this planner documents: shuffle the signal-first quota, then draw one jitter per trial.
        val refRng = DeterministicRng.forSeed(seed(PINNED_SEED))
        val quota = List(spec.signalTrials) { true } + List(spec.noiseTrials) { false }
        val expectedOrder = DeterministicPlanDraw.shuffle(quota, refRng)
        val span = (2 * spec.isiJitterMs + 1).toInt()
        val expectedGaps = expectedOrder.map { spec.baseIsiMs + DeterministicPlanDraw.boundedInt(refRng, span) - spec.isiJitterMs }

        val plan = SentinelPlanner.plan(spec, seed(PINNED_SEED))
        assertEquals(expectedOrder, plan.trials.map { it.isSignal }, "the signal/noise order drifted from the pinned draw")
        assertEquals(expectedGaps, plan.trials.map { it.interStimulusDelayMs }, "the jitter draw order or formula drifted")
    }

    @Test
    fun the_same_seed_reproduces_the_plan_and_a_different_seed_reorders_it() {
        val spec = SentinelSpec()
        val a = SentinelPlanner.plan(spec, seed(PINNED_SEED))
        val b = SentinelPlanner.plan(spec, seed(PINNED_SEED))
        assertEquals(a, b, "same seed must reproduce the plan exactly")

        val c = SentinelPlanner.plan(spec, seed(PINNED_SEED + 1))
        assertTrue(a.trials.map { it.isSignal } != c.trials.map { it.isSignal }, "a different seed must reorder")
        assertEquals(a.cellCounts(), c.cellCounts(), "counts are a guarantee, not a consequence of the seed")
    }

    @Test
    fun a_spec_that_cannot_be_scheduled_is_refused_at_construction() {
        listOf(
            { SentinelSpec(signalTrials = 0) },
            { SentinelSpec(noiseTrials = 0) },
            { SentinelSpec(baseIsiMs = 0) },
            // jitter must be strictly less than the base, or a drawn gap could go non-positive.
            { SentinelSpec(baseIsiMs = 500, isiJitterMs = 500) },
            { SentinelSpec(isiJitterMs = -1) },
            { SentinelSpec(responseWindowMs = 0) },
        ).forEachIndexed { i, build ->
            assertTrue(runCatching { build() }.isFailure, "invalid spec #$i was accepted")
        }
    }

    private companion object {
        const val SEED_SWEEP = 200
        const val BASE_SEED = 0x1000L
        const val PINNED_SEED = 0x5EEDF0C25L
        const val MIN_DISTINCT_GAPS = 20
    }
}
