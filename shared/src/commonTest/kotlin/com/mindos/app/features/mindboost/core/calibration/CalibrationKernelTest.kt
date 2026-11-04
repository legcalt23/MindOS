@file:Suppress("MagicNumber", "MaxLineLength") // Chance levels and judgment counts ARE the fixtures under test.

package com.mindos.app.features.mindboost.core.calibration

import com.mindos.app.features.mindboost.core.scoring.MinSampleGate
import com.mindos.app.features.mindboost.core.scoring.ScoreMetricId
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Ola 1 · N1 — the calibration kernel.
 *
 * The property that carries the whole layer is that a reading is only ever produced from enough judgments, and
 * that a direction is only ever stated when the interval resolves its sign. Everything else here exists to stop
 * the two obvious ways of satisfying that cheaply: returning Absent forever, and returning NO_DETECTABLE_BIAS
 * forever.
 */
class CalibrationKernelTest {

    private val fourWay = 0.25
    private val twoAlt = 0.5

    private fun judgments(
        level: ConfidenceLevel,
        correct: Int,
        wrong: Int,
        chance: Double = fourWay,
    ): List<CalibrationJudgment> =
        List(correct) { CalibrationJudgment(level, wasCorrect = true, chanceLevel = chance) } +
            List(wrong) { CalibrationJudgment(level, wasCorrect = false, chanceLevel = chance) }

    // ---------------------------------------------------------------- probability mapping

    @Test
    fun the_lowest_level_is_read_as_chance_not_as_zero() {
        val low = CalibrationJudgment(ConfidenceLevel.LOW, wasCorrect = true, chanceLevel = fourWay)
        assertTrue(
            low.statedProbability() > fourWay,
            "the lowest bin's midpoint must sit above chance, not at or below it (was ${low.statedProbability()})",
        )
        assertTrue(
            low.statedProbability() < 0.5,
            "and it must stay in the bottom third of the usable range (was ${low.statedProbability()})",
        )
    }

    @Test
    fun the_highest_level_never_claims_absolute_certainty() {
        // A stated probability of exactly 1.0 hands a single mistake the maximum possible Brier penalty and
        // asserts a certainty no one has.
        ConfidenceLevel.entries.forEach { level ->
            listOf(fourWay, twoAlt).forEach { chance ->
                val p = CalibrationJudgment(level, wasCorrect = true, chanceLevel = chance).statedProbability()
                assertTrue(p > chance && p < 1.0, "$level at chance $chance mapped to $p, outside (chance, 1)")
            }
        }
    }

    @Test
    fun the_same_spoken_confidence_means_different_probabilities_on_different_tasks() {
        // This is the property that makes the layer transversal. Without it, pooling a four-way classification
        // with a two-alternative choice manufactures a bias out of which exercise the user picked.
        val onFourWay = CalibrationJudgment(ConfidenceLevel.LOW, wasCorrect = true, chanceLevel = fourWay)
        val onTwoAlt = CalibrationJudgment(ConfidenceLevel.LOW, wasCorrect = true, chanceLevel = twoAlt)
        assertTrue(
            onTwoAlt.statedProbability() > onFourWay.statedProbability(),
            "saying 'poco seguro' on a coin-flip task claims more than saying it on a four-way one",
        )
    }

    @Test
    fun a_chance_level_outside_zero_to_one_is_rejected() {
        assertFailsWith<IllegalArgumentException> {
            CalibrationJudgment(ConfidenceLevel.HIGH, wasCorrect = true, chanceLevel = 0.0)
        }
        assertFailsWith<IllegalArgumentException> {
            CalibrationJudgment(ConfidenceLevel.HIGH, wasCorrect = true, chanceLevel = 1.0)
        }
    }

    // ---------------------------------------------------------------- sample gate

    @Test
    fun the_floor_is_read_from_the_central_table_and_not_restated() {
        assertEquals(
            MinSampleGate.specFor(ScoreMetricId.BRIER).nMinPerCell,
            CalibrationKernel.judgmentsRequired,
            "the kernel must read its floor from MinSampleGate; a local copy would drift from it in silence",
        )
    }

    @Test
    fun one_judgment_short_of_the_floor_is_absent_not_a_provisional_number() {
        val n = CalibrationKernel.judgmentsRequired - 1
        val reading = CalibrationKernel.read(judgments(ConfidenceLevel.HIGH, correct = n, wrong = 0))
        assertTrue(reading is CalibrationReading.Absent, "expected Absent at n=$n, got $reading")
        assertEquals(n, reading.nHave)
        assertEquals(CalibrationKernel.judgmentsRequired, reading.nNeeded)
    }

    @Test
    fun exactly_the_floor_produces_a_reading() {
        // The discriminating half of the gate test. Without it, returning Absent unconditionally passes above.
        val n = CalibrationKernel.judgmentsRequired
        val reading = CalibrationKernel.read(judgments(ConfidenceLevel.MEDIUM, correct = n / 2, wrong = n - n / 2))
        assertTrue(reading is CalibrationReading.Present, "expected Present at exactly the floor, got $reading")
    }

    @Test
    fun no_judgments_at_all_is_absent() {
        assertTrue(CalibrationKernel.read(emptyList()) is CalibrationReading.Absent)
    }

    // ---------------------------------------------------------------- direction

    @Test
    fun a_user_who_is_always_sure_and_usually_wrong_is_reported_overconfident() {
        val reading = CalibrationKernel.read(judgments(ConfidenceLevel.HIGH, correct = 10, wrong = 40))
        val present = reading as CalibrationReading.Present
        assertEquals(CalibrationVerdict.OVERCONFIDENT, present.verdict)
        assertTrue(present.overconfidence > 0.0, "overconfidence must be signed positive here")
        assertTrue(present.overconfidenceCi.excludesZero(), "the interval must resolve the sign to claim a direction")
    }

    @Test
    fun a_user_who_is_never_sure_and_usually_right_is_reported_underconfident() {
        val reading = CalibrationKernel.read(judgments(ConfidenceLevel.LOW, correct = 48, wrong = 2))
        val present = reading as CalibrationReading.Present
        assertEquals(CalibrationVerdict.UNDERCONFIDENT, present.verdict)
        assertTrue(present.overconfidence < 0.0)
    }

    @Test
    fun a_bias_the_interval_cannot_resolve_is_reported_as_no_detectable_bias_not_as_well_calibrated() {
        // A well-matched user: HIGH stands for 0.875 on a four-way task, so ~7 of 8 correct is the honest match.
        val matched = judgments(ConfidenceLevel.HIGH, correct = 42, wrong = 6)
        val present = CalibrationKernel.read(matched) as CalibrationReading.Present
        assertEquals(CalibrationVerdict.NO_DETECTABLE_BIAS, present.verdict)
        assertTrue(
            !present.overconfidenceCi.excludesZero(),
            "a verdict of NO_DETECTABLE_BIAS must come from an interval that spans zero, not from a small mean",
        )
    }

    @Test
    fun the_verdict_is_driven_by_the_interval_and_not_by_the_point_estimate() {
        // The mutation guard for the confidence-interval requirement. Both fixtures have a POSITIVE mean
        // overconfidence; only the one whose interval clears zero may be called overconfident.
        val decisive = CalibrationKernel.read(judgments(ConfidenceLevel.HIGH, correct = 10, wrong = 40)) as CalibrationReading.Present
        val slight = CalibrationKernel.read(judgments(ConfidenceLevel.HIGH, correct = 41, wrong = 7)) as CalibrationReading.Present
        assertTrue(decisive.overconfidence > 0.0 && slight.overconfidence > 0.0, "both fixtures must lean the same way")
        assertEquals(CalibrationVerdict.OVERCONFIDENT, decisive.verdict)
        assertEquals(
            CalibrationVerdict.NO_DETECTABLE_BIAS,
            slight.verdict,
            "a positive point estimate whose interval spans zero must NOT be sold to the user as overconfidence",
        )
    }

    // ---------------------------------------------------------------- brier

    @Test
    fun brier_rewards_the_matched_user_over_the_confidently_wrong_one() {
        val matched = CalibrationKernel.read(judgments(ConfidenceLevel.HIGH, correct = 42, wrong = 6)) as CalibrationReading.Present
        val confidentlyWrong = CalibrationKernel.read(judgments(ConfidenceLevel.HIGH, correct = 6, wrong = 42)) as CalibrationReading.Present
        assertTrue(
            matched.brier < confidentlyWrong.brier,
            "Brier is LOWER_IS_BETTER: ${matched.brier} should beat ${confidentlyWrong.brier}",
        )
    }

    @Test
    fun brier_is_computed_from_the_stated_probabilities_and_not_from_the_raw_level_index() {
        // Pinned value. A HIGH judgment on a four-way task stands for 0.875, so an all-correct run scores
        // (1 - 0.875)^2 = 0.015625 exactly. This breaks if the bin mapping is changed.
        val allRight = CalibrationKernel.read(judgments(ConfidenceLevel.HIGH, correct = 50, wrong = 0)) as CalibrationReading.Present
        assertTrue(
            abs(allRight.brier - 0.015625) < 1e-9,
            "expected the pinned Brier of a perfectly-confident correct run, got ${allRight.brier}",
        )
    }

    // ---------------------------------------------------------------- resolution

    @Test
    fun resolution_is_positive_when_confidence_tracks_correctness() {
        val discriminating =
            judgments(ConfidenceLevel.HIGH, correct = 25, wrong = 0) + judgments(ConfidenceLevel.LOW, correct = 0, wrong = 25)
        val present = CalibrationKernel.read(discriminating) as CalibrationReading.Present
        val resolution = assertNotNull(present.resolution)
        assertTrue(resolution > 0.0, "a user who is sure exactly when they are right must show positive resolution")
    }

    @Test
    fun resolution_is_negative_when_confidence_runs_backwards() {
        val inverted =
            judgments(ConfidenceLevel.HIGH, correct = 0, wrong = 25) + judgments(ConfidenceLevel.LOW, correct = 25, wrong = 0)
        val present = CalibrationKernel.read(inverted) as CalibrationReading.Present
        assertTrue(assertNotNull(present.resolution) < 0.0)
    }

    @Test
    fun a_flawless_run_has_no_resolution_rather_than_a_resolution_of_zero() {
        // Zero would read as "this user cannot tell their hits from their misses", which is a real and damning
        // claim. Having no misses to compare against is a different thing entirely.
        val flawless = CalibrationKernel.read(judgments(ConfidenceLevel.HIGH, correct = 50, wrong = 0)) as CalibrationReading.Present
        assertNull(flawless.resolution, "with no incorrect answers there is nothing to discriminate against")
    }

    // ---------------------------------------------------------------- buckets

    @Test
    fun an_under_powered_bucket_reports_no_accuracy() {
        val floor = MinSampleGate.specFor(ScoreMetricId.ACCURACY_RT_BASE).nMinPerCell
        val skewed = judgments(ConfidenceLevel.HIGH, correct = 45, wrong = 0) +
            judgments(ConfidenceLevel.LOW, correct = 2, wrong = 3)
        val present = CalibrationKernel.read(skewed) as CalibrationReading.Present
        val low = present.buckets.first { it.level == ConfidenceLevel.LOW }
        val high = present.buckets.first { it.level == ConfidenceLevel.HIGH }
        assertTrue(low.n < floor && high.n >= floor, "fixture must straddle the bucket floor")
        assertNull(low.accuracy, "a bucket of ${low.n} judgments cannot support a percentage shown to the user")
        assertNotNull(high.accuracy, "but the well-sampled bucket must still report one, or the feedback is empty")
    }

    @Test
    fun a_bucket_the_user_never_used_has_no_stated_probability_rather_than_zero() {
        val present = CalibrationKernel.read(judgments(ConfidenceLevel.HIGH, correct = 25, wrong = 25)) as CalibrationReading.Present
        val unused = present.buckets.first { it.level == ConfidenceLevel.MEDIUM }
        assertEquals(0, unused.n)
        assertNull(unused.statedProbability, "0.0 would mean the user declared themselves certainly wrong")
        assertNull(unused.accuracy)
    }

    @Test
    fun every_confidence_level_is_represented_in_the_buckets() {
        val present = CalibrationKernel.read(judgments(ConfidenceLevel.HIGH, correct = 25, wrong = 25)) as CalibrationReading.Present
        assertEquals(ConfidenceLevel.entries.toSet(), present.buckets.map { it.level }.toSet())
    }

    // ---------------------------------------------------------------- cross-task pooling

    @Test
    fun judgments_from_different_host_tasks_pool_into_one_reading() {
        // The gate marks Brier cross-session, and each judgment carries its own chance level, so a reading may be
        // assembled from several exercises. This is what lets the layer reach its floor at all.
        val mixed = judgments(ConfidenceLevel.HIGH, correct = 20, wrong = 5, chance = fourWay) +
            judgments(ConfidenceLevel.MEDIUM, correct = 15, wrong = 10, chance = twoAlt)
        val present = CalibrationKernel.read(mixed) as CalibrationReading.Present
        assertEquals(50, present.n)
    }
}
