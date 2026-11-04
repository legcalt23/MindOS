package com.mindos.app.features.mindboost.focus.domain.metrics

import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.model.IndividualThresholds
import com.mindos.app.features.mindboost.focus.domain.model.TrialMarkers
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

    // ── FOC-P0-04 · `MB-FOCUS-0091`: accuracy fabricated by OMISSION ─────────────────────────────────────────
    //
    // Every non-target a silent user "correctly rejects" counts as a correct trial, so on a rare-target task a
    // block with ZERO responses reads as high accuracy — 0.94 at E1's maximum perceptual load — with
    // `confident = true`. That number is arithmetic about the stimulus schedule, not a measurement of the user.
    // A block nobody answered publishes ACCURACY as absent, with a typed reason, never as a value.

    /** E1 at maximum perceptual load (~6 % targets), never touched: 3 targets missed, 47 non-targets "rejected". */
    private fun rareTargetSilentBlock() = List(50) { i ->
        FocusTestFixtures.trial(i, if (i % 17 == 0) TrialOutcome.MISS else TrialOutcome.CORRECT_REJECTION)
    }

    @Test
    fun a_block_with_no_responses_does_not_publish_a_confident_accuracy() {
        val accuracy = engine.blockMetrics(rareTargetSilentBlock(), e1, thresholds)[MetricKey.ACCURACY]

        assertTrue(accuracy != null, "ACCURACY must still be present (as an absence), not silently dropped")
        assertFalse(accuracy.confident, "a silent block read as ${accuracy.value} confident accuracy")
        assertEquals(MetricAbsence.NO_RESPONSES, accuracy.absence, "the absence must carry its typed reason")
        assertEquals(50, accuracy.n, "the declared sample stays honest: 50 valid trials, none answered")
    }

    /** Discriminating half: the SAME schedule, actually answered, keeps its earned confident accuracy. */
    @Test
    fun the_same_rare_target_block_when_answered_still_reports_confident_accuracy() {
        val answered = List(50) { i ->
            when {
                i % 17 == 0 -> FocusTestFixtures.trial(i, TrialOutcome.HIT, rtMs = 300)
                i == 5 || i == 40 -> FocusTestFixtures.trial(i, TrialOutcome.FALSE_ALARM, rtMs = 280)
                else -> FocusTestFixtures.trial(i, TrialOutcome.CORRECT_REJECTION)
            }
        }
        val accuracy = engine.blockMetrics(answered, e1, thresholds)[MetricKey.ACCURACY]

        assertTrue(accuracy != null && accuracy.confident, "a genuinely answered block must stay confident")
        assertEquals(null, accuracy.absence)
        assertEquals(48.0 / 50, accuracy.value, 1e-9, "3 hits + 45 correct rejections out of 50")
    }

    /**
     * The same omission reaches the difference scores through the shared cell helper: two unanswered cells
     * compared as a CONFIDENT cost (0.5 here, from the target schedule alone). A cost nobody paid is not a
     * number — it is unmeasured.
     */
    @Test
    fun two_unanswered_cells_do_not_compare_as_a_confident_zero_dual_task_cost() {
        val silentDual = List(24) { i ->
            FocusTestFixtures.trial(
                i,
                if (i % 4 == 0) TrialOutcome.MISS else TrialOutcome.CORRECT_REJECTION,
                exerciseId = ExerciseCatalog.E8_DUO.id,
                markers = TrialMarkers(dualTask = i % 2 == 0),
            )
        }
        val cost = engine.blockMetrics(silentDual, ExerciseCatalog.E8_DUO, thresholds)[MetricKey.DUAL_TASK_COST]

        assertTrue(cost != null, "DUAL_TASK_COST must be published as an absence for E8")
        assertFalse(cost.confident, "two silent cells compared as a confident cost of ${cost.value}")
    }
}
