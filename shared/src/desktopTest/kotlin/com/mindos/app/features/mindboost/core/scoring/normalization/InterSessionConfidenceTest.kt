package com.mindos.app.features.mindboost.core.scoring.normalization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Fase A · Extremes and monotonicity of the fixed inter-session confidence (CL). Confirms the plan's
 * defect (negative / divide-by-zero) is gone and the result always stays in `[0,1]`.
 */
class InterSessionConfidenceTest {

    private val tol = 1e-12

    @Test fun zero_sessions_is_zero() {
        assertEquals(0.0, InterSessionConfidence.interSessionConfidence(emptyList()), tol)
    }

    @Test fun one_session_is_zero() {
        assertEquals(0.0, InterSessionConfidence.interSessionConfidence(listOf(0.7)), tol)
    }

    @Test fun all_zero_scores_give_zero_not_blowup() {
        assertEquals(0.0, InterSessionConfidence.interSessionConfidence(listOf(0.0, 0.0, 0.0)), tol)
    }

    @Test fun high_cv_clamps_to_zero_never_negative() {
        // mean small, sd large -> CV > 1 -> dispersion term clamps to 0 -> CL 0 (the plan's negative-CL bug).
        val cl = InterSessionConfidence.interSessionConfidence(listOf(0.02, 0.10, 0.40))
        assertEquals(0.0, cl, tol)
    }

    @Test fun perfectly_stable_large_n_approaches_one() {
        val cl = InterSessionConfidence.interSessionConfidence(List(30) { 0.7 })
        assertTrue(cl > 0.98, "30 identical stable scores -> CL near 1 (was $cl)")
        assertTrue(cl <= 1.0, "CL never exceeds 1 (was $cl)")
    }

    @Test fun more_stable_sessions_increase_confidence() {
        val few = InterSessionConfidence.interSessionConfidence(List(3) { 0.6 })
        val many = InterSessionConfidence.interSessionConfidence(List(12) { 0.6 })
        assertTrue(many > few, "more stable sessions -> higher confidence (few=$few, many=$many)")
    }

    @Test fun result_always_stays_in_unit_interval() {
        val samples = listOf(
            listOf(0.5, 0.5),
            listOf(0.1, 0.9, 0.5),
            listOf(0.8, 0.82, 0.79, 0.81),
            listOf(0.05, 0.9),
            listOf(0.3, 0.3, 0.3, 0.31),
        )
        samples.forEach { scores ->
            val cl = InterSessionConfidence.interSessionConfidence(scores)
            assertTrue(cl in 0.0..1.0, "CL must stay in [0,1] for $scores (was $cl)")
        }
    }

    @Test fun rejects_non_finite_score() {
        assertFailsWith<IllegalArgumentException> {
            InterSessionConfidence.interSessionConfidence(listOf(0.5, Double.NaN))
        }
    }
}
