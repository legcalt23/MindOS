package com.mindos.app.features.mindboost.focus2.simulation

import com.mindos.app.features.mindboost.focus2.domain.scoring.ConditionId
import com.mindos.app.features.mindboost.focus2.domain.scoring.Descriptive
import com.mindos.app.features.mindboost.focus2.domain.scoring.DescriptiveScorer
import com.mindos.app.features.mindboost.core.scoring.Probit
import com.mindos.app.features.mindboost.focus2.domain.scoring.Response
import com.mindos.app.features.mindboost.focus2.domain.scoring.TrialRecord
import com.mindos.app.features.mindboost.focus2.domain.scoring.Verdict

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Focus V5 · S2 · tarea (5) — goldens for the inverse normal and for the loglinear correction.
 *
 * **Where the expected values come from, and why that matters.** They are published quantiles of the standard
 * normal, cross-checked in a different language by an independent bisection over an erf-based CDF (agreement
 * to ~1e-8, limited by that erf, not by the quantiles). They were NOT read off `zScore`. A golden whose
 * expectations come from its own subject is a tautology that passes against any implementation, including a
 * wrong one — the failure mode documented on 2026-07-15 and found twice more since.
 *
 * These are LITERAL pins. Range checks and self-consistency checks would survive an algorithm swap and
 * therefore pin nothing.
 */
class CandidateScorerGoldenTest {

    @Test
    fun the_inverse_normal_matches_independently_sourced_quantiles() {
        // p to z(p). Published values; independently recomputed before being written here.
        val golden = listOf(
            0.5 to 0.0,
            0.75 to 0.6744897501960817,
            0.9 to 1.2815515655446004,
            0.95 to 1.6448536269514722,
            0.975 to 1.959963984540054,
            0.99 to 2.3263478740408408,
            0.999 to 3.090232306167813,
            0.025 to -1.959963984540054,
            0.001 to -3.090232306167813,
        )
        golden.forEach { (p, expected) ->
            val actual = Probit.zScore(p)
            assertTrue(
                abs(actual - expected) < TOLERANCE,
                "z($p) = $actual, expected $expected (tolerance $TOLERANCE)",
            )
        }
    }

    @Test
    fun the_inverse_normal_is_antisymmetric_about_one_half() {
        // Not a golden — a structural property, kept because a coefficient typo in ONE branch (the tails are
        // computed by a different branch than the centre) shows up here even if the pinned points miss it.
        listOf(0.01, 0.1, 0.3, 0.45).forEach { p ->
            val lower = Probit.zScore(p)
            val upper = Probit.zScore(1.0 - p)
            assertTrue(abs(lower + upper) < TOLERANCE, "z($p)=$lower and z(${1.0 - p})=$upper must mirror")
        }
    }

    @Test
    fun probabilities_outside_the_open_unit_interval_are_refused_rather_than_returning_infinity() {
        listOf(0.0, 1.0, -0.1, 1.1).forEach { p ->
            assertFailsWith<IllegalArgumentException>("z($p) must be refused") {
                Probit.zScore(p)
            }
        }
    }

    @Test
    fun the_loglinear_correction_keeps_d_prime_finite_at_both_extremes() {
        // The reason the correction is unconditional: without it, a perfect or a floor performance sends z to
        // infinity. With it there is NO non-finite path, so the fabricated-zero trap that catches
        // DefaultMetricsEngine.kt:147 (non-finite silently mapped to 0.0) cannot be reached at all.
        val perfect = (0 until 60).map {
            TrialRecord(it, ConditionId.VIGILANCE, isSignal = it % 2 == 0, response = correctFor(it), timing = null)
        }
        val result = DescriptiveScorer.score(perfect, SyntheticTrialStreams.E01_SPEC)
        val d = result.perCondition.getValue(ConditionId.VIGILANCE).dPrime
        val present = d as? Descriptive.Present ?: error("a perfect performer must still produce a number")
        assertTrue(present.value.isFinite(), "d' must be finite at ceiling, got ${present.value}")
        assertTrue(present.value > 3.0, "a flawless record must yield a high d', got ${present.value}")
    }

    @Test
    fun the_correction_shrinks_toward_zero_and_never_inflates() {
        // The disclosed, direction-consistent bias. Stated as a test so nobody later "fixes" it into a
        // conditional correction that would flatter ceiling performers instead.
        val h = (30 + 0.5) / (30 + 1.0)
        val f = (0 + 0.5) / (30 + 1.0)
        val corrected = Probit.zScore(h) - Probit.zScore(f)
        // Uncorrected this cell would be z(1.0) - z(0.0) = +inf. The correction yields a large but finite
        // value, which is the whole point.
        assertTrue(corrected.isFinite() && corrected in 3.0..8.0, "expected a large finite d', got $corrected")
    }

    private fun correctFor(index: Int): Response =
        if (index % 2 == 0) Response.TAP else Response.NONE

    @Test
    fun the_scorer_never_emits_a_present_value_carrying_a_zero_sample() {
        val result = DescriptiveScorer.score(emptyList(), SyntheticTrialStreams.E07_SPEC)
        result.perCondition.values.forEach { conditionResult ->
            listOf(
                conditionResult.dPrime,
                conditionResult.criterion,
                conditionResult.accuracyRaw,
                conditionResult.accuracyChanceCorrected,
            ).forEach { descriptive ->
                assertTrue(
                    descriptive !is Descriptive.Present,
                    "An empty session must produce absences, never a Present carrying a fabricated number.",
                )
            }
        }
        assertEquals(Verdict.InsufficientData, result.verdict)
    }

    private companion object {
        /**
         * Acklam's own relative error is ~1.15e-9, so the tolerance must sit above it or the golden would
         * fail on a correct implementation.
         *
         * **What this golden does and does not catch, established by running both mutations rather than by
         * assuming.** A single-digit typo in a coefficient (2.5066... -> 2.6066...) turns it RED — verified.
         * A perturbation of ~1e-12 does NOT — also verified, and stated here because the first draft of this
         * comment claimed the test caught "a coefficient change" without qualifying the size. It catches the
         * error modes that actually happen (a typo, a swapped implementation, a transcription slip), not
         * last-bit drift. Claiming otherwise would be the overclaim this suite exists to avoid.
         */
        const val TOLERANCE = 1e-6
    }
}
