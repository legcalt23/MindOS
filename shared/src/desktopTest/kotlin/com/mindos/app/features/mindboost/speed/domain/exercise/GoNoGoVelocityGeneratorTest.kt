@file:Suppress("MagicNumber", "MaxLineLength")

package com.mindos.app.features.mindboost.speed.domain.exercise

import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoTrialType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** MC9-D · E8 — the pure, deterministic Go/No-Go generator: balanced scheduler, guaranteed NO-GO, difficulty. */
class GoNoGoVelocityGeneratorTest {

    private fun run(config: GoNoGoConfig, seed: Long, n: Int): List<com.mindos.app.features.mindboost.speed.domain.score.GoNoGoStimulus> {
        val gen = GoNoGoVelocityGenerator(config)
        val rng = XorShiftSpeedRng(seed)
        return (0 until n).map { gen.next(rng) }
    }

    @Test
    fun a_run_contains_both_go_and_no_go_at_roughly_the_configured_ratio() {
        val stimuli = run(GoNoGoConfig(), seed = 7L, n = 40)
        val go = stimuli.count { it.trialType == GoNoGoTrialType.GO }
        val noGo = stimuli.count { it.trialType == GoNoGoTrialType.NO_GO }
        assertEquals(40, go + noGo)
        assertTrue(go in 28..32, "goRatio 0.75 over 40 trials ⇒ ~30 GO: got $go")
        assertTrue(noGo >= 8, "a run must probe enough NO-GO for inhibition: got $noGo")
    }

    @Test
    fun every_block_guarantees_the_minimum_no_go_sample() {
        val block = run(GoNoGoConfig(scheduleBlock = 8, minNoGoPerBlock = 2), seed = 3L, n = 8)
        assertEquals(2, block.count { it.trialType == GoNoGoTrialType.NO_GO }, "each block holds exactly the NO-GO quota")
    }

    @Test
    fun same_seed_and_config_produce_the_same_sequence() {
        val a = run(GoNoGoConfig.mixedDifficulty(), seed = 21L, n = 40)
        val b = run(GoNoGoConfig.mixedDifficulty(), seed = 21L, n = 40)
        assertEquals(a, b)
    }

    @Test
    fun reset_rebuilds_a_fresh_schedule() {
        val gen = GoNoGoVelocityGenerator(GoNoGoConfig.mixedDifficulty())
        val first = gen.next(XorShiftSpeedRng(9L))
        gen.reset()
        val afterReset = gen.next(XorShiftSpeedRng(9L))
        assertEquals(first, afterReset, "reset ⇒ fresh bags ⇒ same first stimulus for the same rng")
    }

    @Test
    fun invalid_config_fails_with_a_clear_message() {
        assertFailsWith<IllegalArgumentException> { GoNoGoConfig(goRatio = 0.0) }
        assertFailsWith<IllegalArgumentException> { GoNoGoConfig(goRatio = 1.0) }
        assertFailsWith<IllegalArgumentException> { GoNoGoConfig(scheduleBlock = 3) }
        assertFailsWith<IllegalArgumentException> { GoNoGoConfig(scheduleBlock = 8, minNoGoPerBlock = 8) }
        assertFailsWith<IllegalArgumentException> { GoNoGoConfig(baseDeadlineMs = 0L) }
        assertFailsWith<IllegalArgumentException> { GoNoGoConfig(foreperiodMinMs = 900L, foreperiodMaxMs = 500L) }
        assertFailsWith<IllegalArgumentException> { GoNoGoConfig(difficultyLevels = emptyList()) }
    }

    @Test
    fun higher_difficulty_shortens_the_deadline_and_mixed_runs_probe_several_levels() {
        val base = GoNoGoConfig()
        assertEquals(600L, GoNoGoVelocityGenerator(base).next(XorShiftSpeedRng(1L)).deadlineMs)
        val hard = GoNoGoConfig(baseDifficultyLevel = 2)
        assertEquals(440L, GoNoGoVelocityGenerator(hard).next(XorShiftSpeedRng(1L)).deadlineMs, "600 - 2*80 = 440")
        val mixed = run(GoNoGoConfig.mixedDifficulty(), seed = 5L, n = 30)
        assertTrue(mixed.map { it.difficultyLevel }.toSet().size >= 2, "mixed run varies difficulty")
        assertTrue(mixed.map { it.deadlineMs }.toSet().size >= 2, "difficulty changes the deadline")
    }

    @Test
    fun stimulus_metadata_is_structured_and_positioned() {
        val stimuli = run(GoNoGoConfig(), seed = 2L, n = 5)
        stimuli.forEachIndexed { i, s ->
            assertEquals(i, s.runPosition)
            assertEquals(GoNoGoVelocityGenerator.CONFIG_VERSION, s.trialConfigVersion)
            val expectedFamily = if (s.trialType == GoNoGoTrialType.GO) "go" else "nogo"
            assertEquals(expectedFamily, s.stimulusFamily)
        }
    }
}
