package com.mindos.app.features.mindboost.focus.domain.metrics

import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.model.IndividualThresholds
import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The charter's `null != 0` rule, applied where it was inverted.
 *
 * `RtStatistics.median` returns `0.0` for an empty list, so a block with no clean reaction times produced
 * `rtCv = 0.0`, which fed `BLOCK_STABILITY = (1 - 0) = 1.0` (perfect steadiness) and
 * `SUSTAINED_FOCUS_INDEX = 100 * 1.0 * 1.0 = 100.0` (a perfect score). A user who never responded was rated
 * maximally focused. The charter warns against fabricating a low value from absent data; this was its inversion,
 * which is worse — a fabricated 0 understates, a fabricated 100 flatters, and flattery is what a training
 * product is structurally tempted to ship.
 *
 * Found by an adversarial efficacy review of the Focus metrics engine and verified against the code before the
 * fix. `confident = false` was the only guard, and a guard a caller may ignore is not a guard.
 */
class FocusIndexHonestyTest {

    private val engine = DefaultMetricsEngine()
    private val e1 = ExerciseCatalog.E1_VIGIA
    private val thresholds = IndividualThresholds()

    /** A user who was present but never responded: every target missed, no reaction times at all. */
    private fun noResponseBlock() = List(12) { FocusTestFixtures.trial(it, TrialOutcome.MISS) }

    @Test
    fun a_block_with_no_responses_does_not_report_perfect_stability() {
        val metrics = engine.blockMetrics(noResponseBlock(), e1, thresholds)
        val stability = metrics[MetricKey.BLOCK_STABILITY]

        assertFalse(
            stability != null && stability.confident,
            "BLOCK_STABILITY must not be confident with zero reaction times",
        )
        val reported = stability?.value ?: 0.0
        assertFalse(
            reported >= 1.0,
            "zero reaction times fabricated PERFECT stability ($reported): absent data must not read as steady",
        )
    }

    @Test
    fun a_block_with_no_responses_does_not_report_a_perfect_focus_index() {
        val metrics = engine.blockMetrics(noResponseBlock(), e1, thresholds)
        val focus = metrics[MetricKey.SUSTAINED_FOCUS_INDEX]

        assertFalse(
            focus != null && focus.confident,
            "SUSTAINED_FOCUS_INDEX must not be confident with zero reaction times",
        )
        val reported = focus?.value ?: 0.0
        assertFalse(
            reported >= 100.0,
            "a user who never responded scored $reported/100 on sustained focus — absent data fabricated a " +
                "perfect score",
        )
    }

    /**
     * The discriminating half: the fix must not be "clamp the indices to zero". A real block with real
     * responses must still produce its real, confident value — otherwise the test above passes by breaking
     * the metric for everyone.
     */
    @Test
    fun a_real_block_still_reports_a_real_confident_index() {
        val trials = FocusTestFixtures.hits(listOf(250, 260, 255, 245, 250, 265, 255, 260))
        val metrics = engine.blockMetrics(trials, e1, thresholds)

        val stability = metrics[MetricKey.BLOCK_STABILITY]
        val focus = metrics[MetricKey.SUSTAINED_FOCUS_INDEX]
        assertTrue(stability != null && stability.confident, "a real 8-hit block must yield confident stability")
        assertTrue(focus != null && focus.confident, "a real 8-hit block must yield a confident focus index")
        // Tightly clustered RTs and no lapses: genuinely steady, genuinely high — and earned.
        assertTrue(stability.value > 0.9, "steady real block should read steady, was ${stability.value}")
        assertTrue(focus.value > 90.0, "steady real block should score high, was ${focus.value}")
        assertEquals(8, focus.n, "focus index sample size must be the real hit count")
    }
}
