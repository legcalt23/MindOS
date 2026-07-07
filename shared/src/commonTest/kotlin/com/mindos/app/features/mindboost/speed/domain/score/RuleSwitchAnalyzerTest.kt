@file:Suppress("MagicNumber", "MaxLineLength")

package com.mindos.app.features.mindboost.speed.domain.score

import com.mindos.app.features.mindboost.speed.domain.exercise.RuleSwitchConfig
import com.mindos.app.features.mindboost.speed.domain.exercise.RuleSwitchGenerator
import com.mindos.app.features.mindboost.speed.domain.exercise.XorShiftSpeedRng
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** MC9-B · E4 — switch metrics computed purely from the ordered run trials (rt + correctness + index). */
class RuleSwitchAnalyzerTest {

    private fun outcome(rtMs: Long, correct: Boolean) = RuleSwitchOutcome(rtNanos = rtMs * 1_000_000L, isCorrect = correct)

    @Test
    fun computes_switch_cost_post_switch_accuracy_and_recovery() {
        val trialsPerRule = 3
        // idx: 0,1,2 (rule A) · 3,4,5 (switch@3) · 6,7,8 (switch@6). POST_SWITCH_WINDOW=2 ⇒ post={3,4,6,7}, stable={0,1,2,5,8}.
        val outcomes = listOf(
            outcome(200, true), outcome(200, true), outcome(200, true),
            outcome(500, false), outcome(500, true), outcome(200, true),
            outcome(500, false), outcome(500, true), outcome(200, true),
        )
        val m = RuleSwitchAnalyzer.analyze(outcomes, trialsPerRule)
        assertEquals(2, m.switchCount)
        assertEquals(300.0, m.switchCostMs, "post-switch median 500ms − stable median 200ms")
        assertEquals(0.5, m.postSwitchAccuracy, "2 of 4 post-switch trials correct")
        assertEquals(1.0, m.stableAccuracy, "all stable trials correct")
        assertEquals(1.0, m.recoveryTrialsMedian, "1 trial from each switch to the first correct")
    }

    @Test
    fun a_costly_switch_shows_a_positive_switch_cost() {
        val outcomes = (0 until 12).map { i ->
            val isSwitchWindow = i % 4 == 0 && i > 0
            outcome(rtMs = if (isSwitchWindow) 600 else 250, correct = true)
        }
        val m = RuleSwitchAnalyzer.analyze(outcomes, 4)
        assertTrue((m.switchCostMs ?: 0.0) > 0.0, "slower post-switch RT ⇒ positive switch cost")
    }

    @Test
    fun metrics_are_null_when_there_are_no_switches() {
        val outcomes = List(3) { outcome(200, true) } // 3 trials, trialsPerRule=5 ⇒ never switches
        val m = RuleSwitchAnalyzer.analyze(outcomes, 5)
        assertEquals(0, m.switchCount)
        assertNull(m.switchCostMs)
        assertNull(m.postSwitchAccuracy)
        assertNull(m.recoveryTrialsMedian)
        assertEquals(1.0, m.stableAccuracy, "with no switch every trial is stable")
    }

    @Test
    fun an_empty_run_yields_all_null_metrics() {
        val m = RuleSwitchAnalyzer.analyze(emptyList(), 5)
        assertEquals(0, m.switchCount)
        assertNull(m.switchCostMs)
        assertNull(m.stableAccuracy)
    }

    @Test
    fun the_analyzer_switch_schedule_matches_the_generators_real_switches() {
        // Cross-check the live-feedback invariant: the analyzer indexes trials 0..n and derives switches by index;
        // that MUST equal the rule switches the generator actually produced over the same trial count.
        val config = RuleSwitchConfig.DEFAULT
        val gen = RuleSwitchGenerator(config)
        val rng = XorShiftSpeedRng(1L)
        val ruleLabels = (0 until 4 * config.trialsPerRule).map { gen.next(rng).cue.substringBefore(" · ") }
        val actualSwitches = (1 until ruleLabels.size).count { ruleLabels[it] != ruleLabels[it - 1] }
        val outcomes = List(ruleLabels.size) { RuleSwitchOutcome(200_000_000L, true) }
        assertEquals(actualSwitches, RuleSwitchAnalyzer.analyze(outcomes, config.trialsPerRule).switchCount)
    }
}
