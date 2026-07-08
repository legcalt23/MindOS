@file:Suppress("MagicNumber", "MaxLineLength")

package com.mindos.app.features.mindboost.speed.domain.exercise

import com.mindos.app.features.mindboost.speed.domain.SpeedChoice
import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** MC9-C · E5 Visual Scan Race — the pure, deterministic visual-search generator + its content invariants. */
class VisualScanRaceGeneratorTest {

    private val config = VisualScanConfig.DEFAULT

    // #1,#2,#3,#4,#5,#12,#13 — exactly one target, on the correct side; the other side has none; pools disjoint.
    @Test
    fun every_trial_has_exactly_one_target_on_the_correct_side() {
        val gen = VisualScanRaceGenerator(config)
        val rng = XorShiftSpeedRng(42L)
        repeat(500) {
            val t = gen.trial(rng)
            val hits = t.leftItems.count { it == t.target } + t.rightItems.count { it == t.target }
            assertEquals(1, hits, "exactly one target across both panels (never 0, never 2)")
            val correctSide = if (t.correct == SpeedChoice.LEFT) t.leftItems else t.rightItems
            val otherSide = if (t.correct == SpeedChoice.LEFT) t.rightItems else t.leftItems
            assertEquals(1, correctSide.count { it == t.target }, "correct side holds the target")
            assertEquals(0, otherSide.count { it == t.target }, "incorrect side never holds the target")
            assertEquals(config.itemsPerSide, t.leftItems.size)
            assertEquals(config.itemsPerSide, t.rightItems.size)
            (t.leftItems + t.rightItems).filter { it != t.target }.forEach {
                assertFalse(it in config.targetPool, "distractor '$it' must not be a target symbol")
            }
        }
    }

    // #6 — the prompt cue states the target and points the correct side, but never spells the side in words.
    @Test
    fun prompt_target_is_on_the_correct_side_only_and_the_cue_hides_the_side() {
        val gen = VisualScanRaceGenerator(config)
        val rng = XorShiftSpeedRng(11L)
        repeat(300) {
            val p = gen.next(rng)
            assertTrue(p.cue.startsWith("Busca "), "cue states the search target")
            val target = p.cue.removePrefix("Busca ")
            val inLeft = p.leftLabel.split(" ").count { it == target }
            val inRight = p.rightLabel.split(" ").count { it == target }
            assertEquals(1, inLeft + inRight, "the target appears on exactly one panel")
            val expected = if (inLeft == 1) SpeedChoice.LEFT else SpeedChoice.RIGHT
            assertEquals(expected, p.correctChoice, "correctChoice points to the panel holding the target")
            listOf("izquierda", "derecha", "left", "right").forEach {
                assertFalse(p.cue.lowercase().contains(it), "cue leaks the side: '${p.cue}'")
            }
        }
    }

    // #7 — the target side is reasonably balanced left/right over many trials with a fixed seed.
    @Test
    fun the_target_side_is_reasonably_balanced() {
        val gen = VisualScanRaceGenerator(config)
        val rng = XorShiftSpeedRng(99L)
        val left = (0 until 1000).count { gen.trial(rng).correct == SpeedChoice.LEFT }
        assertTrue(left in 350..650, "target side balance out of range: $left/1000")
    }

    // #8 — same seed + same config ⇒ same sequence.
    @Test
    fun same_seed_and_config_produce_the_same_sequence() {
        val g1 = VisualScanRaceGenerator(config)
        val g2 = VisualScanRaceGenerator(config)
        val r1 = XorShiftSpeedRng(123L)
        val r2 = XorShiftSpeedRng(123L)
        repeat(80) { assertEquals(g1.next(r1), g2.next(r2)) }
    }

    // #9 — invalid config fails with a clear message.
    @Test
    fun invalid_config_fails_with_a_clear_message() {
        assertFailsWith<IllegalArgumentException> { VisualScanConfig(itemsPerSide = 99) }
        assertFailsWith<IllegalArgumentException> { VisualScanConfig(targetPool = emptyList()) }
        assertFailsWith<IllegalArgumentException> { VisualScanConfig(distractorPool = emptyList()) }
        // #11 — overlapping pools are rejected (a distractor equal to a target would be unjudgeable).
        assertFailsWith<IllegalArgumentException> { VisualScanConfig(targetPool = listOf("●")) }
        // no repeats but the pool is too small to fill a side with distinct distractors.
        assertFailsWith<IllegalArgumentException> {
            VisualScanConfig(allowRepeatedDistractors = false, distractorPool = listOf("●", "▲"))
        }
    }

    // #10 — difficulty scales with itemsPerSide (higher visual load ⇒ more cells per panel).
    @Test
    fun larger_itemsPerSide_increases_visual_load() {
        val small = VisualScanRaceGenerator(VisualScanConfig(itemsPerSide = 3)).trial(XorShiftSpeedRng(5L))
        val big = VisualScanRaceGenerator(VisualScanConfig(itemsPerSide = 6)).trial(XorShiftSpeedRng(5L))
        assertEquals(3, small.leftItems.size)
        assertEquals(6, big.leftItems.size)
        assertTrue(big.leftItems.size > small.leftItems.size, "more items per side = higher visual load")
    }

    // #11 — every similarity family keeps target/distractor pools disjoint and produces valid trials.
    @Test
    fun similarity_families_have_disjoint_pools_and_valid_trials() {
        VisualSimilarity.entries.forEach { level ->
            val c = VisualScanConfig.forSimilarity(level)
            assertTrue(c.targetPool.none { it in c.distractorPool }, "$level pools overlap")
            val t = VisualScanRaceGenerator(c).trial(XorShiftSpeedRng(3L))
            assertEquals(1, (t.leftItems + t.rightItems).count { it == t.target }, "$level trial must have one target")
        }
    }

    @Test
    fun exercise_id_is_e5_and_distinct_distractors_do_not_repeat_on_a_side() {
        assertEquals(SpeedExerciseId.E5_BUSQUEDA, VisualScanRaceGenerator().exerciseId)
        val cfg = VisualScanConfig(
            itemsPerSide = 4,
            allowRepeatedDistractors = false,
            distractorPool = listOf("●", "▲", "◆", "■", "□", "◇"),
        )
        val gen = VisualScanRaceGenerator(cfg)
        val rng = XorShiftSpeedRng(8L)
        repeat(100) {
            val t = gen.trial(rng)
            val plainSide = if (t.correct == SpeedChoice.LEFT) t.rightItems else t.leftItems
            assertEquals(plainSide.size, plainSide.toSet().size, "distinct distractors must not repeat on a side")
        }
    }

    // MC9-C-R — mixed-load: a run probes several visual-load levels (enables load-cost telemetry).
    @Test
    fun mixed_load_config_varies_items_per_side_across_trials() {
        val gen = VisualScanRaceGenerator(VisualScanConfig.mixedLoad())
        val rng = XorShiftSpeedRng(17L)
        val sizes = (0 until 200).map { gen.trial(rng).leftItems.size }.toSet()
        assertTrue(sizes.size >= 2, "a mixed-load run must probe multiple load levels: $sizes")
        sizes.forEach { assertTrue(it in 3..6, "each level within min..max: $it") }
    }

    @Test
    fun mixed_load_still_keeps_exactly_one_target_and_equal_panels() {
        val gen = VisualScanRaceGenerator(VisualScanConfig.mixedLoad(VisualSimilarity.HIGH))
        val rng = XorShiftSpeedRng(23L)
        repeat(200) {
            val t = gen.trial(rng)
            assertEquals(1, (t.leftItems + t.rightItems).count { it == t.target })
            assertEquals(t.leftItems.size, t.rightItems.size, "both panels share the trial's load level")
        }
    }

    @Test
    fun invalid_load_levels_fail() {
        assertFailsWith<IllegalArgumentException> { VisualScanConfig(loadLevels = emptyList()) }
        assertFailsWith<IllegalArgumentException> { VisualScanConfig(loadLevels = listOf(2)) } // below minItemsPerSide
        assertFailsWith<IllegalArgumentException> { VisualScanConfig(loadLevels = listOf(7)) } // above maxItemsPerSide
    }

    // A multi-target pool still yields exactly one (pool) target on one side (target variability is a difficulty axis).
    @Test
    fun a_multi_target_pool_still_produces_one_valid_target() {
        val cfg = VisualScanConfig(targetPool = listOf("★", "✚"), distractorPool = listOf("●", "▲", "◆", "■"))
        val gen = VisualScanRaceGenerator(cfg)
        val rng = XorShiftSpeedRng(21L)
        repeat(200) {
            val t = gen.trial(rng)
            assertTrue(t.target in cfg.targetPool, "target drawn from the pool")
            assertEquals(1, t.leftItems.count { it == t.target } + t.rightItems.count { it == t.target })
        }
    }
}
