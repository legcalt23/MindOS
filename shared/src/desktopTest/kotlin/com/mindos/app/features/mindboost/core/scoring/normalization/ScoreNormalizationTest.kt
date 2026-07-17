package com.mindos.app.features.mindboost.core.scoring.normalization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Fase A · Known input→output verification of the pure normalization kernel, including bounds/overflow,
 * the Rule-No-Speed hard gate, and minimal synthetic adversaries (robot / spammer / honest) confirming the
 * kernel does not reward gaming.
 */
class ScoreNormalizationTest {

    private val tol = 1e-9

    // --- winsorizeRt ---

    @Test fun winsorize_clamps_below_floor() {
        assertEquals(120.0, ScoreNormalization.winsorizeRt(50.0), tol)
    }

    @Test fun winsorize_clamps_aberrant_negative_to_floor() {
        assertEquals(120.0, ScoreNormalization.winsorizeRt(-30.0), tol)
    }

    @Test fun winsorize_clamps_above_ceiling() {
        assertEquals(2500.0, ScoreNormalization.winsorizeRt(9000.0), tol)
    }

    @Test fun winsorize_passes_value_within_bounds() {
        assertEquals(300.0, ScoreNormalization.winsorizeRt(300.0), tol)
    }

    @Test fun winsorize_rejects_inverted_bounds() {
        assertFailsWith<IllegalArgumentException> {
            ScoreNormalization.winsorizeRt(300.0, minPhysMs = 500.0, maxPhysMs = 100.0)
        }
    }

    // --- sigmoid ---

    @Test fun sigmoid_at_center_is_half() {
        assertEquals(0.5, ScoreNormalization.sigmoid(0.0, x0 = 0.0, k = 1.0), tol)
    }

    @Test fun sigmoid_is_monotonic_for_positive_k() {
        val low = ScoreNormalization.sigmoid(0.2, x0 = 0.5, k = 6.0)
        val high = ScoreNormalization.sigmoid(0.8, x0 = 0.5, k = 6.0)
        assertTrue(high > low, "sigmoid must increase with x for positive k")
    }

    @Test fun sigmoid_negative_k_inverts_direction() {
        val low = ScoreNormalization.sigmoid(0.2, x0 = 0.5, k = -6.0)
        val high = ScoreNormalization.sigmoid(0.8, x0 = 0.5, k = -6.0)
        assertTrue(low > high, "negative k (lower is better) must score lower x higher")
    }

    @Test fun sigmoid_extremes_stay_bounded_without_nan() {
        val hi = ScoreNormalization.sigmoid(1e6, x0 = 0.0, k = 50.0)
        val lo = ScoreNormalization.sigmoid(-1e6, x0 = 0.0, k = 50.0)
        assertTrue(hi in 0.0..1.0, "sigmoid must stay in [0,1] at +extreme")
        assertTrue(lo in 0.0..1.0, "sigmoid must stay in [0,1] at -extreme")
        assertTrue(hi > 0.99, "large positive argument -> ~1")
        assertTrue(lo < 0.01, "large negative argument -> ~0")
    }

    @Test fun sigmoid_rejects_non_finite_input() {
        assertFailsWith<IllegalArgumentException> {
            ScoreNormalization.sigmoid(Double.NaN, x0 = 0.0, k = 1.0)
        }
    }

    // --- difficultyAdjust ---

    @Test fun difficulty_trivial_earns_half_credit() {
        assertEquals(0.5, ScoreNormalization.difficultyAdjust(1.0, difficultyActual = 0.0, difficultyMax = 10.0), tol)
    }

    @Test fun difficulty_max_earns_full_credit() {
        assertEquals(1.0, ScoreNormalization.difficultyAdjust(1.0, difficultyActual = 10.0, difficultyMax = 10.0), tol)
    }

    @Test fun difficulty_ratio_above_range_is_clamped() {
        assertEquals(1.0, ScoreNormalization.difficultyAdjust(1.0, difficultyActual = 99.0, difficultyMax = 10.0), tol)
    }

    @Test fun difficulty_is_monotonic_in_difficulty() {
        val easy = ScoreNormalization.difficultyAdjust(0.8, difficultyActual = 2.0, difficultyMax = 10.0)
        val hard = ScoreNormalization.difficultyAdjust(0.8, difficultyActual = 8.0, difficultyMax = 10.0)
        assertTrue(hard > easy, "same performance at higher difficulty must earn more credit")
    }

    @Test fun difficulty_rejects_nonpositive_max() {
        assertFailsWith<IllegalArgumentException> {
            ScoreNormalization.difficultyAdjust(1.0, difficultyActual = 5.0, difficultyMax = 0.0)
        }
    }

    // --- blockSessionScore + Rule-No-Speed ---

    private val weights = BlockScoreWeights(
        accuracy = 0.3, speed = 0.3, difficulty = 0.2, consistency = 0.1, errorControl = 0.1,
    ) // baseSum = 1.0

    @Test fun rule_no_speed_suppresses_speed_below_amin() {
        val comps = BlockScoreComponents(
            accuracy = 0.4, speed = 1.0, difficulty = 0.5, consistency = 1.0, errorControl = 0.5,
        )
        val out = ScoreNormalization.blockSessionScore(comps, weights, minAccuracyForSpeed = 0.6)
        assertTrue(out.speedSuppressed, "accuracy below aMin must suppress speed")
        assertEquals(0.0, out.appliedWeights.speed, tol)
        // 0.3*0.4 + 0*1.0 + 0.2*0.5 + 0.1*1.0 + 0.1*0.5 = 0.12+0.10+0.10+0.05 = 0.37 ; /1.0
        assertEquals(0.37, out.score, tol)
    }

    @Test fun speed_counts_above_amin() {
        val comps = BlockScoreComponents(
            accuracy = 0.9, speed = 1.0, difficulty = 0.5, consistency = 1.0, errorControl = 0.5,
        )
        val out = ScoreNormalization.blockSessionScore(comps, weights, minAccuracyForSpeed = 0.6)
        assertFalse(out.speedSuppressed, "accuracy above aMin keeps speed")
        // 0.3*0.9 + 0.3*1.0 + 0.2*0.5 + 0.1*1.0 + 0.1*0.5 = 0.27+0.30+0.10+0.10+0.05 = 0.82
        assertEquals(0.82, out.score, tol)
    }

    @Test fun score_is_bounded_at_all_ones() {
        val comps = BlockScoreComponents(1.0, 1.0, 1.0, 1.0, 1.0)
        val out = ScoreNormalization.blockSessionScore(comps, weights, minAccuracyForSpeed = 0.0)
        assertEquals(1.0, out.score, tol)
    }

    @Test fun score_is_zero_at_all_zeros() {
        val comps = BlockScoreComponents(0.0, 0.0, 0.0, 0.0, 0.0)
        val out = ScoreNormalization.blockSessionScore(comps, weights, minAccuracyForSpeed = 0.0)
        assertEquals(0.0, out.score, tol)
    }

    @Test fun weights_reject_speed_only_configuration() {
        assertFailsWith<IllegalArgumentException> {
            BlockScoreWeights(accuracy = 0.0, speed = 1.0, difficulty = 0.0, consistency = 0.0, errorControl = 0.0)
        }
    }

    @Test fun weights_reject_negative_weight() {
        assertFailsWith<IllegalArgumentException> {
            BlockScoreWeights(accuracy = -0.1, speed = 0.3, difficulty = 0.2, consistency = 0.1, errorControl = 0.1)
        }
    }

    @Test fun components_reject_out_of_range() {
        assertFailsWith<IllegalArgumentException> {
            BlockScoreComponents(accuracy = 1.5, speed = 0.0, difficulty = 0.0, consistency = 0.0, errorControl = 0.0)
        }
    }

    // --- synthetic adversaries: the kernel must not reward gaming ---

    @Test fun robot_sub_physiological_rt_is_capped_by_winsorize() {
        // A robot answering at 40 ms is winsorized to the 120 ms floor: no sub-physiological speed credit.
        assertEquals(120.0, ScoreNormalization.winsorizeRt(40.0), tol)
    }

    @Test fun spammer_scores_below_honest_user() {
        // Spammer: chance-level accuracy, ultra-fast, artificially stable (constant-rate) consistency.
        val spammer = BlockScoreComponents(
            accuracy = 0.25, speed = 1.0, difficulty = 0.5, consistency = 1.0, errorControl = 0.2,
        )
        // Honest: high accuracy, moderate speed/consistency.
        val honest = BlockScoreComponents(
            accuracy = 0.9, speed = 0.6, difficulty = 0.5, consistency = 0.7, errorControl = 0.8,
        )
        val aMin = 0.6
        val spammerOut = ScoreNormalization.blockSessionScore(spammer, weights, minAccuracyForSpeed = aMin)
        val honestOut = ScoreNormalization.blockSessionScore(honest, weights, minAccuracyForSpeed = aMin)
        assertTrue(spammerOut.speedSuppressed, "spammer speed must be suppressed")
        assertFalse(honestOut.speedSuppressed, "honest user keeps speed")
        assertTrue(
            honestOut.score > spammerOut.score,
            "kernel must not reward the spammer over the honest user (honest=${honestOut.score}, spammer=${spammerOut.score})",
        )
    }
}
