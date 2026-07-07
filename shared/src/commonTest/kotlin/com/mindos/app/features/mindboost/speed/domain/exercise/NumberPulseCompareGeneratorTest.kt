@file:Suppress("MaxLineLength")

package com.mindos.app.features.mindboost.speed.domain.exercise

import com.mindos.app.features.mindboost.speed.domain.SpeedChoice
import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** MC9-A · E2 Number Pulse Compare — the magnitude-comparison generator. */
class NumberPulseCompareGeneratorTest {

    private val config = NumberPulseConfig.DEFAULT

    @Test
    fun generates_valid_distinct_pairs_within_range_and_gap_band() {
        val gen = NumberPulseCompareGenerator(config)
        val rng = XorShiftSpeedRng(42L)
        repeat(1_000) {
            val p = gen.pair(rng)
            assertTrue(p.left in config.minValue..config.maxValue, "left in range: ${p.left}")
            assertTrue(p.right in config.minValue..config.maxValue, "right in range: ${p.right}")
            assertTrue(p.left != p.right, "the two magnitudes must differ: ${p.left}/${p.right}")
            assertTrue(abs(p.left - p.right) in config.minGap..config.maxGap, "gap in band: ${abs(p.left - p.right)}")
        }
    }

    @Test
    fun the_correct_side_is_always_the_larger_magnitude() {
        val gen = NumberPulseCompareGenerator(config)
        val rng = XorShiftSpeedRng(7L)
        repeat(1_000) {
            val p = gen.pair(rng)
            val expected = if (p.left > p.right) SpeedChoice.LEFT else SpeedChoice.RIGHT
            assertEquals(expected, p.correct, "correct is the larger side for ${p.left}/${p.right}")
        }
    }

    @Test
    fun the_magnitudes_appear_only_in_the_onset_cue_not_the_panel_labels() {
        val gen = NumberPulseCompareGenerator(config)
        // Same seed ⇒ next() consumes the same draws as pair(), so they describe the same trial.
        val expected = gen.pair(XorShiftSpeedRng(99L))
        val prompt = gen.next(XorShiftSpeedRng(99L))
        assertEquals(expected.correct, prompt.correctChoice)
        assertTrue(
            prompt.cue.contains(expected.left.toString()) && prompt.cue.contains(expected.right.toString()),
            "both magnitudes must be in the Stimulus cue: '${prompt.cue}'",
        )
        // Panel labels stay generic (no digits) so the comparison isn't leaked during the foreperiod.
        assertTrue(prompt.leftLabel.none { it.isDigit() } && prompt.rightLabel.none { it.isDigit() }, "labels carry no magnitudes")
    }

    @Test
    fun is_deterministic_for_a_given_seed() {
        val g1 = NumberPulseCompareGenerator(config)
        val g2 = NumberPulseCompareGenerator(config)
        val r1 = XorShiftSpeedRng(123L)
        val r2 = XorShiftSpeedRng(123L)
        repeat(100) { assertEquals(g1.pair(r1), g2.pair(r2)) }
    }

    @Test
    fun closeness_config_controls_the_gap_band() {
        val hard = NumberPulseCompareGenerator(NumberPulseConfig(minValue = 1, maxValue = 9, minGap = 1, maxGap = 1))
        val rng = XorShiftSpeedRng(5L)
        repeat(300) { assertEquals(1, abs(hard.pair(rng).let { it.left - it.right }), "min gap ⇒ hardest discrimination") }
    }

    @Test
    fun exercise_id_is_e2_compara() {
        assertEquals(SpeedExerciseId.E2_COMPARA, NumberPulseCompareGenerator().exerciseId)
    }

    @Test
    fun an_invalid_config_is_rejected() {
        assertFailsWith<IllegalArgumentException> { NumberPulseConfig(minValue = 5, maxValue = 5) }
        assertFailsWith<IllegalArgumentException> { NumberPulseConfig(minValue = 1, maxValue = 9, minGap = 0) }
        assertFailsWith<IllegalArgumentException> { NumberPulseConfig(minValue = 1, maxValue = 9, minGap = 5, maxGap = 3) }
    }
}
