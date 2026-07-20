package com.mindos.app.features.mindboost.focus2.simulation

import com.mindos.app.features.mindboost.core.runtime.DeterministicRng
import com.mindos.app.features.mindboost.core.runtime.RngVersion
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.focus2.domain.generator.SentinelPlan
import com.mindos.app.features.mindboost.focus2.domain.generator.SentinelPlanner
import com.mindos.app.features.mindboost.focus2.domain.generator.SentinelSpec
import com.mindos.app.features.mindboost.focus2.domain.runtime.SentinelTask
import com.mindos.app.features.mindboost.focus2.domain.scoring.ConditionId
import com.mindos.app.features.mindboost.focus2.domain.scoring.Descriptive
import com.mindos.app.features.mindboost.focus2.domain.scoring.DescriptiveScorer
import com.mindos.app.features.mindboost.focus2.domain.scoring.InvalidationReason
import com.mindos.app.features.mindboost.focus2.domain.scoring.Response
import com.mindos.app.features.mindboost.focus2.domain.scoring.SecondaryTiming
import com.mindos.app.features.mindboost.focus2.domain.scoring.SessionDescriptive
import com.mindos.app.features.mindboost.focus2.domain.scoring.TrialRecord
import com.mindos.app.features.mindboost.focus2.domain.scoring.Verdict
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Focus V5 · S4 · tarea (3) — the vigilance tracer's OWN adversarial suite (T-ADV B).
 *
 * The S2 simulation already ran a vigilance stream (`SyntheticTrialStreams.e01`) against MASHER and WAITER, but
 * that stream laid its targets on a fixed `index % 5 == 0` cadence of its own. This suite is stricter in the way
 * that matters for a tracer: every adversary responds to the ACTUAL plan this exercise's generator produced —
 * shuffled target positions, jittered gaps — so a periodicity exploit is tested against the real unpredictability
 * it claims to defeat (the "verify the live capture flow" rule).
 *
 * The load-bearing adversary is [a_rhythmic_responder_rarely_beats_the_shuffle_while_an_honest_one_almost_always_wins].
 * It is the one the flanker never faced: a person who taps on a fixed beat, blind to the screen. In a vigilance
 * task with predictable target timing that person would score, and the whole paradigm would be gameable. The
 * defence is statistical, not absolute — on a single unlucky shuffle a metronome CAN line up with the targets —
 * so the honest test is a seed sweep: the rhythmic responder discriminates only at the ~chance false-positive
 * rate, while an honest watcher on the same plans discriminates almost always. A single pinned seed would either
 * hide a real leak or fail on an unlucky-but-correct one; the distribution is the claim.
 */
class FocusV5SentinelOracleTest {

    private val pinnedPlan = SentinelPlanner.plan(SentinelSpec(), SessionSeed(PINNED_SEED, RngVersion.V1))

    // --- the honest and the excellent both pass (position-independent, so one plan suffices) --------------

    @Test
    fun an_honest_watcher_is_valid_and_discriminates() {
        val result = score(profileStream(pinnedPlan, Adversary.HONEST, rngFor(Adversary.HONEST)))
        assertEquals(Verdict.Valid, result.verdict)
        assertTrue(DescriptiveScorer.discriminated(result.perCondition.getValue(ConditionId.VIGILANCE)))
    }

    @Test
    fun an_expert_is_valid_and_is_not_penalised_for_being_good() {
        val honest = dPrime(score(profileStream(pinnedPlan, Adversary.HONEST, rngFor(Adversary.HONEST))))
        val expert = score(profileStream(pinnedPlan, Adversary.EXPERT, rngFor(Adversary.EXPERT)))
        assertEquals(Verdict.Valid, expert.verdict)
        assertTrue(dPrime(expert) > honest, "the expert must out-score the honest watcher, never be flagged for it")
    }

    // --- the vigilance adversaries fail, each by its OWN route --------------------------------------------

    @Test
    fun a_collapsed_criterion_yes_sayer_is_degenerate_not_merely_low() {
        // Taps at everything: one distinct response across every scorable trial. Not "poor discrimination" — NO
        // SECOND ANSWER to discriminate with, and the verdict says exactly that.
        assertEquals(
            Verdict.Invalidated(InvalidationReason.DEGENERATE_RESPONDING),
            score(profileStream(pinnedPlan, Adversary.YES_SAYER, rngFor(Adversary.YES_SAYER))).verdict,
        )
    }

    @Test
    fun a_waiter_who_responds_at_chance_does_not_discriminate() {
        assertEquals(
            Verdict.Invalidated(InvalidationReason.NO_DISCRIMINATION),
            score(profileStream(pinnedPlan, Adversary.WAITER, rngFor(Adversary.WAITER))).verdict,
        )
    }

    @Test
    fun a_rhythmic_responder_rarely_beats_the_shuffle_while_an_honest_one_almost_always_wins() {
        var rhythmicValid = 0
        var honestValid = 0
        var gapSum = 0.0
        repeat(SEED_SWEEP) { i ->
            val plan = SentinelPlanner.plan(SentinelSpec(), SessionSeed(SWEEP_BASE + i, RngVersion.V1))
            val rhythmic = score(profileStream(plan, Adversary.RHYTHMIC, rngFor(Adversary.RHYTHMIC)))
            if (rhythmic.verdict == Verdict.Valid) rhythmicValid++
            val cell = rhythmic.perCondition.getValue(ConditionId.VIGILANCE).cell
            gapSum += abs(cell.hits.toDouble() / cell.signalTrials - cell.falseAlarms.toDouble() / cell.noiseTrials)
            val honest = score(profileStream(plan, Adversary.HONEST, DeterministicRng.forSeed(SessionSeed(SWEEP_BASE + i, RngVersion.V1))))
            if (honest.verdict == Verdict.Valid) honestValid++
        }
        assertTrue(
            rhythmicValid <= SEED_SWEEP / 10,
            "a fixed rhythm beat the shuffle in $rhythmicValid/$SEED_SWEEP seeds — targets may not be random",
        )
        assertTrue(
            honestValid >= SEED_SWEEP * 9 / 10,
            "an honest watcher should discriminate on nearly every plan, was $honestValid/$SEED_SWEEP",
        )
        assertTrue(
            gapSum / SEED_SWEEP < RHYTHM_RATE_GAP,
            "the rhythmic hit-rate and false-alarm-rate should coincide on average; mean gap ${gapSum / SEED_SWEEP}",
        )
    }

    // --- stream construction ------------------------------------------------------------------------------

    private enum class Adversary { HONEST, EXPERT, YES_SAYER, WAITER, RHYTHMIC }

    private fun rngFor(adversary: Adversary) =
        DeterministicRng.forSeed(SessionSeed(PINNED_SEED xor adversary.ordinal.toLong(), RngVersion.V1))

    /** Turns a real plan into a response stream for [adversary]. Probabilistic profiles draw from [rng]. */
    private fun profileStream(plan: SentinelPlan, adversary: Adversary, rng: DeterministicRng): List<TrialRecord> =
        plan.trials.map { trial ->
            val taps = when (adversary) {
                Adversary.HONEST -> rng.nextDouble() < (if (trial.isSignal) HONEST_HIT else HONEST_FA)
                Adversary.EXPERT -> rng.nextDouble() < (if (trial.isSignal) EXPERT_HIT else EXPERT_FA)
                Adversary.YES_SAYER -> true // taps at everything, blind to the target
                Adversary.WAITER -> rng.nextDouble() < CHANCE_TAP // same rate on signal and noise: no discrimination
                Adversary.RHYTHMIC -> trial.index % RHYTHM_PERIOD == 0 // a fixed beat, blind to the target
            }
            TrialRecord(
                index = trial.index,
                conditionId = trial.conditionId,
                isSignal = trial.isSignal,
                response = if (taps) Response.TAP else Response.NONE,
                timing = if (taps) SecondaryTiming(RESPONSE_RT_MS, trial.responseWindowMs) else null,
            )
        }

    private fun score(stream: List<TrialRecord>) = DescriptiveScorer.score(stream, SentinelTask.SPEC)

    private fun dPrime(result: SessionDescriptive): Double =
        (result.perCondition.getValue(ConditionId.VIGILANCE).dPrime as Descriptive.Present).value

    private companion object {
        const val PINNED_SEED = 0x5EEDF0C25L
        const val SWEEP_BASE = 0x1000L
        const val SEED_SWEEP = 200
        const val HONEST_HIT = 0.92
        const val HONEST_FA = 0.06
        const val EXPERT_HIT = 0.97
        const val EXPERT_FA = 0.02
        const val CHANCE_TAP = 0.4
        const val RHYTHM_PERIOD = 3
        const val RHYTHM_RATE_GAP = 0.15
        const val RESPONSE_RT_MS = 430L // past the 150 ms anticipation floor, inside the window
    }
}
