package com.mindos.app.features.mindboost.focus2.simulation

import com.mindos.app.features.mindboost.focus2.domain.scoring.AbsenceReason
import com.mindos.app.features.mindboost.focus2.domain.scoring.ConditionId
import com.mindos.app.features.mindboost.focus2.domain.scoring.Descriptive
import com.mindos.app.features.mindboost.focus2.domain.scoring.DescriptiveScorer
import com.mindos.app.features.mindboost.focus2.domain.scoring.InvalidationReason
import com.mindos.app.features.mindboost.focus2.domain.scoring.Response
import com.mindos.app.features.mindboost.focus2.domain.scoring.SecondaryTiming
import com.mindos.app.features.mindboost.focus2.domain.scoring.SessionDescriptive
import com.mindos.app.features.mindboost.focus2.domain.scoring.TrialRecord
import com.mindos.app.features.mindboost.focus2.domain.scoring.Verdict

import com.mindos.app.features.mindboost.core.scoring.MinSampleGate
import com.mindos.app.features.mindboost.core.scoring.ScoreMetricId
import com.mindos.app.features.mindboost.focus2.simulation.SyntheticTrialStreams.E01_SPEC
import com.mindos.app.features.mindboost.focus2.simulation.SyntheticTrialStreams.E07_SPEC
import com.mindos.app.features.mindboost.focus2.simulation.SyntheticTrialStreams.Profile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Focus V5 · S2 · tarea (5) — A1..A6 against the CANDIDATE scorer, in SIMULATION.
 *
 * The plan's mandate: "si A1-A6 no pasan EN SIMULACION, la formula candidata se corrige AQUI, no en S3".
 * That has already happened once — see `verdictOf`'s note on the loglinear artifact under unequal cells,
 * which this suite surfaced before any engine was committed. That is what the slice is for.
 *
 * **These assertions are in SPEC DIRECTION.** The v1 oracle asserts the DEFECT (a correct scorer fails it on
 * purpose, as documentation). Reusing those assertion bodies here would produce guaranteed failures that
 * mean "the candidate is right", which is a reading error waiting to happen. Nothing is reused from it.
 *
 * **The failure mode this suite is built against is a VACUOUS green.** The scorer's honest behaviour when a
 * cell is under-powered is to return `Absent` — and an all-`Absent` scorer satisfies A1, A2, A3, A5 and A6
 * trivially, because comparisons over missing operands never execute. Five of six green, nothing measured.
 * Two mechanical defences: every ordering assertion routes its operands through [requirePresent], which
 * FAILS on absence instead of skipping; and A4 asserts the sample counts themselves, so the suite cannot go
 * green until the trial counts actually clear the floor.
 */
class FocusV5CandidateOracleTest {

    // ---------------------------------------------------------------------------------------------------
    // A4 FIRST. It is the discriminating half: without it, a scorer that returns Absent (or 0.0) for
    // everything passes every other assertion in this file.
    // ---------------------------------------------------------------------------------------------------

    @Test
    fun a4_an_honest_filterer_produces_a_real_measurement_in_every_condition() {
        val result = DescriptiveScorer.score(SyntheticTrialStreams.e07(Profile.HONEST), E07_SPEC)
        assertEquals(Verdict.Valid, result.verdict, "An honest performer must yield a VALID session.")

        result.perCondition.forEach { (condition, conditionResult) ->
            val d = requirePresent(conditionResult.dPrime, "HONEST/$condition d'")
            assertTrue(d > HONEST_DPRIME_FLOOR, "HONEST/$condition d'=$d must clear $HONEST_DPRIME_FLOOR")
            // The anti-vacuity teeth: the cells must actually be powered, or every other assertion here is
            // comparing nothing to nothing. This is what forces the n=55 -> n=112 correction to be real.
            assertTrue(
                conditionResult.cell.signalTrials >= DescriptiveScorer.minNPerCell,
                "$condition signal cell has ${conditionResult.cell.signalTrials}, floor is " +
                    "${DescriptiveScorer.minNPerCell} — the assertion would be vacuous",
            )
            assertTrue(
                conditionResult.cell.noiseTrials >= DescriptiveScorer.minNPerCell,
                "$condition noise cell has ${conditionResult.cell.noiseTrials}, floor is " +
                    "${DescriptiveScorer.minNPerCell} — the assertion would be vacuous",
            )
        }
    }

    @Test
    fun a4_the_floor_this_suite_enforces_is_the_repos_own_and_not_a_local_invention() {
        assertEquals(
            MinSampleGate.specFor(ScoreMetricId.DPRIME).nMinPerCell,
            DescriptiveScorer.minNPerCell,
            "The simulation's per-cell floor must track the central gate, not drift from it.",
        )
    }

    @Test
    fun a4_being_good_is_not_penalised() {
        // Red-team row 4 / row 36: a near-ceiling performer must not lose ground to an ordinary one. A
        // scorer that penalises low variability would fail here, and low variability is exactly what
        // expertise looks like.
        val expert = requirePresent(primary(Profile.EXPERT), "EXPERT")
        val honest = requirePresent(primary(Profile.HONEST), "HONEST")
        assertTrue(expert >= honest, "EXPERT d'=$expert must not fall below HONEST d'=$honest")
    }

    // ---------------------------------------------------------------------------------------------------
    // A1 — the headline ordering.
    // ---------------------------------------------------------------------------------------------------

    @Test
    fun a1_an_honest_filterer_outscores_a_key_masher() {
        val honest = requirePresent(primary(Profile.HONEST), "HONEST")
        val masher = requirePresent(primary(Profile.MASHER), "MASHER")
        assertTrue(
            honest > masher + A1_MARGIN,
            "A1 FAILS: HONEST d'=$honest vs MASHER d'=$masher (margin $A1_MARGIN). " +
                "This is the v1 defect the slice exists to repair — v1 scored the masher 73.8 vs 45.2.",
        )
    }

    @Test
    fun a1_holds_on_the_vigilance_tracer_too() {
        val honest = requirePresent(primaryE01(Profile.HONEST), "HONEST/E01")
        val masher = requirePresent(primaryE01(Profile.MASHER), "MASHER/E01")
        assertTrue(honest > masher + A1_MARGIN, "A1/E01 FAILS: HONEST=$honest vs MASHER=$masher")
    }

    // ---------------------------------------------------------------------------------------------------
    // A2 — the masher does not reach a valid measurement at all.
    // ---------------------------------------------------------------------------------------------------

    @Test
    fun a2_a_key_masher_yields_no_valid_measurement_and_the_reason_is_earned() {
        listOf(
            "E07" to DescriptiveScorer.score(SyntheticTrialStreams.e07(Profile.MASHER), E07_SPEC),
            "E01" to DescriptiveScorer.score(SyntheticTrialStreams.e01(Profile.MASHER), E01_SPEC),
        ).forEach { (task, result) ->
            val verdict = assertIs<Verdict.Invalidated>(result.verdict, "$task: masher must not be VALID")
            // Earned, not asserted: the record itself shows a single distinct response throughout.
            assertEquals(InvalidationReason.DEGENERATE_RESPONDING, verdict.reason, task)
        }
    }

    @Test
    fun a2_the_masher_verdict_is_reached_without_reading_latency() {
        // The obvious signature of a masher is a near-constant RT. Using it would violate the ratified rule
        // that RT is secondary. Proof that it is not used: destroy the RT signal entirely and the verdict
        // is unchanged.
        val stream = SyntheticTrialStreams.e07(Profile.MASHER)
        val scrambled = stream.mapIndexed { i, t ->
            t.copy(timing = t.timing?.copy(rtMs = 300L + (i * 37L) % 600L))
        }
        assertEquals(
            DescriptiveScorer.score(stream, E07_SPEC).verdict,
            DescriptiveScorer.score(scrambled, E07_SPEC).verdict,
        )
    }

    // ---------------------------------------------------------------------------------------------------
    // A3 — deadline-riders and one-sided responders land below an honest performer.
    // ---------------------------------------------------------------------------------------------------

    @Test
    fun a3_a_deadline_rider_and_a_one_sided_responder_both_land_below_an_honest_filterer() {
        val honest = requirePresent(primary(Profile.HONEST), "HONEST")
        val waiter = requirePresent(primary(Profile.WAITER), "WAITER")
        val sideBias = requirePresent(primary(Profile.SIDE_BIAS), "SIDE_BIAS")
        assertTrue(honest > waiter + A3_MARGIN, "HONEST=$honest must exceed WAITER=$waiter")
        assertTrue(honest > sideBias + A3_MARGIN, "HONEST=$honest must exceed SIDE_BIAS=$sideBias")
    }

    @Test
    fun a3_a_point_estimate_above_zero_is_not_a_demonstrated_discrimination() {
        // Found BY this simulation and fixed here rather than in S3, which is the whole purpose of the
        // slice. The deadline-rider chooses sides by coin flip, yet sampling noise handed it d' = 0.50 and
        // 0.25 — comfortably positive point estimates. With 28+28 trials the standard error is around 0.4,
        // so neither is distinguishable from zero, and the first draft of the verdict (a point-vs-threshold
        // comparison) called that session VALID. `MinSampleGate.specFor(DPRIME).requiresConfidenceInterval`
        // was already true in this repo; the scorer simply was not honouring it.
        val result = DescriptiveScorer.score(SyntheticTrialStreams.e07(Profile.WAITER), E07_SPEC)
        val verdict = assertIs<Verdict.Invalidated>(
            result.verdict,
            "A coin-flipper must not reach a valid measurement on the strength of noise.",
        )
        assertEquals(InvalidationReason.NO_DISCRIMINATION, verdict.reason)
        result.perCondition.forEach { (condition, r) ->
            assertTrue(
                !DescriptiveScorer.discriminated(r),
                "$condition: interval must include zero, d'=${r.dPrime} se=${r.dPrimeStandardError}",
            )
        }
    }

    @Test
    fun a3_and_the_interval_rule_does_not_swallow_a_genuine_performer() {
        // The discriminating half of the rule above. Without this, tightening the verdict until nothing
        // passes would look like a fix.
        val result = DescriptiveScorer.score(SyntheticTrialStreams.e07(Profile.HONEST), E07_SPEC)
        assertEquals(Verdict.Valid, result.verdict)
        result.perCondition.forEach { (condition, r) ->
            assertTrue(DescriptiveScorer.discriminated(r), "$condition: HONEST must clear zero")
        }
    }

    @Test
    fun a3_a_one_sided_responder_is_caught_by_criterion_which_is_what_criterion_is_for() {
        // The scientific reason the yes/no framing was chosen over the 2AFC shortcut: the 2AFC formula
        // assumes an unbiased observer, which is precisely what this profile violates, and it would fold the
        // bias into sensitivity. Here d' and criterion separate cleanly.
        val result = DescriptiveScorer.score(SyntheticTrialStreams.e07(Profile.SIDE_BIAS), E07_SPEC)
        result.perCondition.forEach { (condition, conditionResult) ->
            val c = requirePresent(conditionResult.criterion, "SIDE_BIAS/$condition criterion")
            assertTrue(c < -1.0, "$condition: an always-LEFT responder must show a strong yes-bias, got c=$c")
        }
    }

    @Test
    fun a3_side_bias_is_declared_inapplicable_to_vigilance_rather_than_silently_skipped() {
        // A go-only single-tap task has ONE response, so there is no side to be biased toward. Four of the
        // five profiles run on E01. Fabricating a fifth would invent an adversary the paradigm cannot have.
        assertFailsWith<IllegalArgumentException> { SyntheticTrialStreams.e01(Profile.SIDE_BIAS) }
    }

    @Test
    fun a3_the_masher_and_the_one_sided_responder_are_indistinguishable_here_and_that_is_correct() {
        // Both answer LEFT on every trial, so their 2x2 cells are identical and so is their d'. They differ
        // ONLY in latency, which the primary statistic must not consume. The descriptive scorer cannot and
        // should not separate them; that is the response-entropy detector's job, and that detector is NOT
        // wired — a pure-JVM simulation cannot claim detector coverage it does not have.
        val masher = DescriptiveScorer.score(SyntheticTrialStreams.e07(Profile.MASHER), E07_SPEC)
        val sideBias = DescriptiveScorer.score(SyntheticTrialStreams.e07(Profile.SIDE_BIAS), E07_SPEC)
        assertEquals(
            masher.perCondition.mapValues { it.value.dPrime },
            sideBias.perCondition.mapValues { it.value.dPrime },
            "Declared limit of the S2 simulation, pinned so nobody later claims it separates them.",
        )
    }

    // ---------------------------------------------------------------------------------------------------
    // A5 — the primary statistic consumes no latency.
    // ---------------------------------------------------------------------------------------------------

    @Test
    fun a5_tripling_every_latency_leaves_the_measurement_bit_identical() {
        val stream = SyntheticTrialStreams.e07(Profile.HONEST)
        val baseline = DescriptiveScorer.score(stream, E07_SPEC)
        // Precondition, or the perturbation proves nothing: no trial sits near the anticipation floor, so
        // slowing everything down cannot reclassify anything. If this fails, the test below is hollow.
        baseline.perCondition.values.forEach { assertEquals(0, it.cell.anticipations) }

        val slowed = stream.map { trial -> trial.copy(timing = trial.timing?.let { it.copy(rtMs = it.rtMs * 3) }) }
        assertEquals(
            baseline.perCondition,
            DescriptiveScorer.score(slowed, E07_SPEC).perCondition,
            "Exact equality, not a tolerance: one ULP of difference means latency leaked into the primary.",
        )
    }

    // DELIBERATELY NOT A TEST: "the primary path has nothing RT-shaped to read".
    //
    // That guarantee is the TYPE SIGNATURE. `TrialRecord` carries no rtCv, no lapse count and no `MetricSet`,
    // so the six retired difference-scores are not merely unused — they are unreachable, and the compiler
    // enforces it on every edit. A first draft asserted it by reflection over the class members; it needed
    // kotlin-reflect (absent from this classpath) and, more to the point, it would only have restated in
    // slower form what the compiler already refuses to build. The behavioural regression above
    // (`a5_tripling_every_latency...`) is the part that can actually fail, and it is kept.

    @Test
    fun a5_the_scorer_reads_latency_for_exactly_one_purpose_and_it_is_declared() {
        // Full disclosure rather than a claim of total blindness: the scorer DOES look at rtMs once, to
        // separate anticipations from decisions. Claiming "RT is never read" would be an overclaim, so the
        // one read is pinned here instead — a response below the floor must land in `anticipations` and
        // NOWHERE else, never as a hit and never as a false alarm.
        val premature = (0 until 60).map {
            TrialRecord(
                index = it,
                conditionId = ConditionId.VIGILANCE,
                isSignal = it % 2 == 0,
                response = Response.TAP,
                timing = SecondaryTiming(rtMs = 40L, windowMs = 1500L),
            )
        }
        val cell = DescriptiveScorer
            .score(premature, E01_SPEC)
            .perCondition.getValue(ConditionId.VIGILANCE).cell
        assertEquals(60, cell.anticipations)
        assertEquals(0, cell.hits)
        assertEquals(0, cell.falseAlarms)
    }

    // ---------------------------------------------------------------------------------------------------
    // A6 — chance performance yields a typed absence, never a fabricated zero.
    // ---------------------------------------------------------------------------------------------------

    @Test
    fun a6_performance_at_chance_is_reported_as_absent_never_as_a_perfect_zero() {
        // This is the direct repair of the v1 defect the plan names: raw accuracy with no chance floor
        // (DefaultMetricsEngine.kt:43-57) hands a coin-flipper the same 0.50 it hands a real discriminator.
        val result = DescriptiveScorer.score(SyntheticTrialStreams.e07(Profile.MASHER), E07_SPEC)
        result.perCondition.forEach { (condition, conditionResult) ->
            assertEquals(
                Descriptive.Absent(AbsenceReason.AT_OR_BELOW_CHANCE),
                conditionResult.accuracyChanceCorrected,
                "$condition: at chance, corrected accuracy is NOT a measurement.",
            )
        }
        assertTrue(
            result.perCondition.values.none {
                val a = it.accuracyChanceCorrected
                a is Descriptive.Present && a.value == 0.0
            },
            "A clamped 0.0 would be the same lie in the other direction: it reports 'zero ability measured' " +
                "where the truth is 'nothing was measured'.",
        )
    }

    @Test
    fun a6_a_paradigm_without_a_defined_guessing_probability_says_so_instead_of_inventing_one() {
        // Go-only vigilance: the chance of a correct "no" depends on how willing the person is to respond
        // at all, which is what criterion measures. There is no chance level to correct against.
        val result = DescriptiveScorer.score(SyntheticTrialStreams.e01(Profile.HONEST), E01_SPEC)
        assertEquals(
            Descriptive.Absent(AbsenceReason.NOT_APPLICABLE),
            result.perCondition.getValue(ConditionId.VIGILANCE).accuracyChanceCorrected,
        )
    }

    @Test
    fun a6_an_empty_session_is_insufficient_data_and_never_a_zero() {
        val result = DescriptiveScorer.score(emptyList(), E07_SPEC)
        assertEquals(Verdict.InsufficientData, result.verdict)
        assertEquals(Descriptive.Absent(AbsenceReason.NO_SIGNAL_TRIALS), result.primaryStatistic)
        result.perCondition.values.forEach {
            assertIs<Descriptive.Absent>(it.dPrime)
            assertIs<Descriptive.Absent>(it.accuracyRaw)
        }
    }

    /**
     * Takes [n] trials from EVERY (condition, signal) cell.
     *
     * A plain `.take(n)` does not do this. The stream is generated condition by condition and signal before
     * noise, so `take(19)` yields nineteen CONGRUENT signal trials and nothing else — which trips
     * `NO_NOISE_TRIALS` and `NO_SIGNAL_TRIALS` long before the sample floor is ever consulted.
     */
    private fun balancedCells(n: Int): List<TrialRecord> {
        val all = SyntheticTrialStreams.e07(Profile.HONEST)
        return E07_SPEC.conditions.flatMap { condition ->
            listOf(true, false).flatMap { signal ->
                all.filter { it.conditionId == condition && it.isSignal == signal }.take(n)
            }
        }
    }

    /**
     * The honest answer to a small sample is "not enough", never a d' computed from it.
     *
     * **This test was VACUOUS until the S3 exit gate caught it** (2026-07-20). It used `.take(19)`, whose
     * comment claimed "19 per cell", and the absence it observed came from two empty cells rather than from
     * the floor — the `signalTrials < minNPerCell` branch it exists to protect never executed, and deleting
     * that branch would have left the test green. It now asserts the REASON, not just the verdict, and carries
     * the discriminating half below.
     */
    @Test
    fun an_under_powered_session_reports_absence_rather_than_a_confident_small_number() {
        val floor = DescriptiveScorer.minNPerCell
        val result = DescriptiveScorer.score(balancedCells(floor - 1), E07_SPEC)

        assertEquals(Verdict.InsufficientData, result.verdict)
        result.perCondition.forEach { (condition, outcome) ->
            assertEquals(
                Descriptive.Absent(AbsenceReason.INSUFFICIENT_N),
                outcome.dPrime,
                "$condition must be absent BECAUSE of the sample floor, not because a cell was empty",
            )
            assertTrue(outcome.cell.signalTrials > 0 && outcome.cell.noiseTrials > 0, "both cells must be populated")
        }
    }

    @Test
    fun one_more_trial_per_cell_clears_the_floor_and_produces_a_statistic() {
        // The discriminating half. Without it, a scorer that reported absence for EVERYTHING would satisfy the
        // test above, and the floor would be indistinguishable from a permanent refusal to measure.
        val result = DescriptiveScorer.score(balancedCells(DescriptiveScorer.minNPerCell), E07_SPEC)
        assertIs<Descriptive.Present<Double>>(result.primaryStatistic)
    }

    /**
     * The verdict ladder must report "we could not measure" BEFORE "the measurement says nothing" — the
     * rung-1-before-rung-2 ordering the scorer's KDoc argues for and that the S3 certification pass (both
     * technical lenses, 2026-07-21) found was the ONE ordering the mutation pass had left unpinned.
     *
     * Here CONGRUENT is under the floor AND every response is a single type ("yes"): both rung 1 (a condition
     * cannot be measured) and rung 2 (only one distinct response) fire. An under-sampled session is not a
     * demonstration of degenerate responding — it is a failure to gather enough data — so the honest verdict
     * is InsufficientData. Swapping the two rungs turns this red.
     */
    @Test
    fun an_under_powered_single_response_session_is_insufficient_data_before_it_is_degenerate() {
        val trials = allYes(ConditionId.CONGRUENT, signal = 10, noise = 10) +
            allYes(ConditionId.INCONGRUENT, signal = 25, noise = 25)
        assertEquals(Verdict.InsufficientData, DescriptiveScorer.score(trials, E07_SPEC).verdict)
    }

    @Test
    fun a_fully_powered_single_response_session_is_degenerate_the_discriminating_half() {
        // Same single-response profile, but every cell now clears the floor, so rung 1 no longer fires and the
        // verdict falls through to rung 2. Without this half, the test above could pass simply because the
        // scorer never reaches DEGENERATE_RESPONDING at all.
        val trials = allYes(ConditionId.CONGRUENT, signal = 25, noise = 25) +
            allYes(ConditionId.INCONGRUENT, signal = 25, noise = 25)
        assertEquals(
            Verdict.Invalidated(InvalidationReason.DEGENERATE_RESPONDING),
            DescriptiveScorer.score(trials, E07_SPEC).verdict,
        )
    }

    /** Every trial answered "yes" (LEFT): a hit on signal, a false alarm on noise, and no "no" anywhere. */
    private fun allYes(condition: ConditionId, signal: Int, noise: Int): List<TrialRecord> =
        List(signal) { TrialRecord(it, condition, isSignal = true, Response.LEFT, timing = null) } +
            List(noise) { TrialRecord(it, condition, isSignal = false, Response.LEFT, timing = null) }

    // ---------------------------------------------------------------------------------------------------
    // Reporting: the numbers themselves, so a green run is never reported as a bare "PASS".
    // ---------------------------------------------------------------------------------------------------

    @Test
    fun the_simulation_reports_its_actual_counts_and_not_merely_a_verdict() {
        val rows = buildList {
            Profile.entries.forEach { p ->
                add(row("E07", p, DescriptiveScorer.score(SyntheticTrialStreams.e07(p), E07_SPEC)))
                if (p != Profile.SIDE_BIAS) {
                    add(row("E01", p, DescriptiveScorer.score(SyntheticTrialStreams.e01(p), E01_SPEC)))
                }
            }
        }
        // Printed so the slice report can carry real numbers. A gate reported as "PASS" without its counts
        // is the same class of false green as a BUILD SUCCESSFUL over an UP-TO-DATE test task.
        rows.forEach { println(it) }
        assertEquals(9, rows.size, "5 profiles on E07 + 4 on E01 (SIDE_BIAS is inapplicable to E01).")
    }

    private fun row(task: String, profile: Profile, result: SessionDescriptive): String {
        val cells = result.perCondition.entries.joinToString("; ") { (condition, r) ->
            val c = r.cell
            "$condition n=${c.signalTrials}/${c.noiseTrials} hits=${c.hits} fa=${c.falseAlarms} " +
                "om=${c.omissions} d'=${fmt(r.dPrime)}+-${fmt(r.dPrimeStandardError)} c=${fmt(r.criterion)} " +
                "discriminated=${DescriptiveScorer.discriminated(r)}"
        }
        return "$task ${profile.name.padEnd(9)} verdict=${result.verdict} | $cells"
    }

    private fun fmt(d: Descriptive<Double>): String = when (d) {
        is Descriptive.Present -> ((d.value * 1000).toInt() / 1000.0).toString()
        is Descriptive.Absent -> "ABSENT(${d.reason})"
    }

    private fun primary(profile: Profile): Descriptive<Double> =
        DescriptiveScorer.score(SyntheticTrialStreams.e07(profile), E07_SPEC).primaryStatistic

    private fun primaryE01(profile: Profile): Descriptive<Double> =
        DescriptiveScorer.score(SyntheticTrialStreams.e01(profile), E01_SPEC).primaryStatistic

    /**
     * The anti-vacuity gate. An `Absent` operand FAILS the assertion instead of quietly satisfying an
     * inequality it never evaluated.
     */
    private fun requirePresent(d: Descriptive<Double>, who: String): Double = when (d) {
        is Descriptive.Present -> {
            assertTrue(
                d.n >= DescriptiveScorer.minNPerCell,
                "$who: n=${d.n} is below the floor — this comparison would be vacuous",
            )
            d.value
        }
        is Descriptive.Absent ->
            fail("$who: Absent(${d.reason}). A missing operand is a VACUOUS comparison, not a passing one.")
    }

    private companion object {
        /**
         * Margins in d' units. [PROPOSED] — they are recorded in the tracer dossiers rather than being
         * silent constants, because a margin is a claim about how far apart two performers must land.
         */
        const val A1_MARGIN = 1.0
        const val A3_MARGIN = 0.5
        const val HONEST_DPRIME_FLOOR = 1.5
    }
}
