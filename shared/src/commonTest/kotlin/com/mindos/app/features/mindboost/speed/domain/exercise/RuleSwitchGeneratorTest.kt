@file:Suppress("MaxLineLength")

package com.mindos.app.features.mindboost.speed.domain.exercise

import com.mindos.app.features.mindboost.speed.domain.SpeedChoice
import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** MC9-B · E4 Rule Switch Sprint — the color/shape rule model + the stateful generator. */
class RuleSwitchGeneratorTest {

    private val config = RuleSwitchConfig.DEFAULT

    // 1/3 — the mapping under each rule (COLOR: azul→izq / naranja→der; FORMA: círculo→izq / triángulo→der).
    @Test
    fun the_correct_side_follows_the_active_rule() {
        val blueCircle = RuleSwitchStimulus(colorIsBlue = true, shapeIsCircle = true)
        val orangeTriangle = RuleSwitchStimulus(colorIsBlue = false, shapeIsCircle = false)
        assertEquals(SpeedChoice.LEFT, RuleSwitchLogic.correctChoice(SpeedRule.COLOR, blueCircle))
        assertEquals(SpeedChoice.RIGHT, RuleSwitchLogic.correctChoice(SpeedRule.COLOR, orangeTriangle))
        assertEquals(SpeedChoice.LEFT, RuleSwitchLogic.correctChoice(SpeedRule.SHAPE, blueCircle))
        assertEquals(SpeedChoice.RIGHT, RuleSwitchLogic.correctChoice(SpeedRule.SHAPE, orangeTriangle))
        // Same stimulus, different rule ⇒ different correct side when the two dimensions disagree.
        val blueTriangle = RuleSwitchStimulus(colorIsBlue = true, shapeIsCircle = false)
        assertEquals(SpeedChoice.LEFT, RuleSwitchLogic.correctChoice(SpeedRule.COLOR, blueTriangle))
        assertEquals(SpeedChoice.RIGHT, RuleSwitchLogic.correctChoice(SpeedRule.SHAPE, blueTriangle))
    }

    // 4 — the rule alternates every trialsPerRule; switch trials are the first of each rule block.
    @Test
    fun the_rule_alternates_and_switches_at_the_expected_frequency() {
        val n = config.trialsPerRule
        (0 until n).forEach { assertEquals(SpeedRule.COLOR, RuleSwitchLogic.ruleAt(it, n)) }
        (n until 2 * n).forEach { assertEquals(SpeedRule.SHAPE, RuleSwitchLogic.ruleAt(it, n)) }
        assertFalse(RuleSwitchLogic.isSwitch(0, n), "trial 0 (session start) is never a switch")
        assertFalse(RuleSwitchLogic.isSwitch(n - 1, n), "last trial of the 1st rule block is not a switch")
        assertTrue(RuleSwitchLogic.isSwitch(n, n), "first trial of the 2nd rule block is a switch")
        assertFalse(RuleSwitchLogic.isSwitch(n + 1, n), "mid-block trial is not a switch")
        assertTrue(RuleSwitchLogic.isSwitch(2 * n, n))
    }

    // MC9-B-R §3 — the E4 prompt is onset-gated: its panel labels/cue reveal the rule, so they must not
    // appear before the stimulus. The generator flags every prompt so the UI hides them during the foreperiod.
    @Test
    fun the_generator_prompts_are_onset_gated() {
        val gen = RuleSwitchGenerator(config)
        val rng = XorShiftSpeedRng(11L)
        repeat(30) { assertTrue(gen.next(rng).revealLabelsOnlyAtStimulus, "E4 prompts must be onset-gated") }
    }

    // 6 — perseveration: a wrong response that matches the PREVIOUS rule's answer, only on a real switch.
    @Test
    fun perseveration_is_detected_only_when_the_response_follows_the_previous_rule() {
        // blueTriangle: COLOR⇒LEFT, SHAPE⇒RIGHT. On a COLOR→SHAPE switch, correct=RIGHT; choosing LEFT = previous rule.
        val s = RuleSwitchStimulus(colorIsBlue = true, shapeIsCircle = false)
        assertTrue(RuleSwitchLogic.isPerseveration(SpeedRule.SHAPE, SpeedRule.COLOR, s, SpeedChoice.LEFT), "followed the old rule")
        assertFalse(RuleSwitchLogic.isPerseveration(SpeedRule.SHAPE, SpeedRule.COLOR, s, SpeedChoice.RIGHT), "correct answer is not perseveration")
        assertFalse(RuleSwitchLogic.isPerseveration(SpeedRule.SHAPE, SpeedRule.SHAPE, s, SpeedChoice.LEFT), "no switch ⇒ no perseveration")
    }

    // 2/3 — the generator emits both stimulus dimensions in the cue and the correct side matches the rule model.
    @Test
    fun the_generator_emits_valid_two_dimension_prompts() {
        val gen = RuleSwitchGenerator(config)
        val rng = XorShiftSpeedRng(42L)
        repeat(400) {
            val p = gen.next(rng)
            val ruleLabel = p.cue.substringBefore(" · ")
            val stim = p.cue.substringAfter(" · ")
            val rule = if (ruleLabel == "COLOR") SpeedRule.COLOR else SpeedRule.SHAPE
            val stimulus = RuleSwitchStimulus(colorIsBlue = stim.endsWith("azul"), shapeIsCircle = stim.startsWith("●"))
            assertEquals(RuleSwitchLogic.correctChoice(rule, stimulus), p.correctChoice, "cue '${p.cue}'")
            val (l, r) = if (rule == SpeedRule.COLOR) "Azul" to "Naranja" else "Círculo" to "Triángulo"
            assertEquals(l, p.leftLabel)
            assertEquals(r, p.rightLabel)
        }
    }

    // 4 — the generator's rule sequence matches the schedule.
    @Test
    fun the_generator_switches_rule_on_schedule() {
        val gen = RuleSwitchGenerator(config)
        val rng = XorShiftSpeedRng(7L)
        val labels = (0 until 3 * config.trialsPerRule).map { gen.next(rng).cue.substringBefore(" · ") }
        labels.forEachIndexed { i, label ->
            val expected = if (RuleSwitchLogic.ruleAt(i, config.trialsPerRule) == SpeedRule.COLOR) "COLOR" else "FORMA"
            assertEquals(expected, label, "trial $i")
        }
    }

    // reset returns to the first rule and reproduces the first trial for a fresh rng.
    @Test
    fun reset_returns_to_the_first_rule() {
        val gen = RuleSwitchGenerator(config)
        val first = gen.next(XorShiftSpeedRng(1L))
        gen.next(XorShiftSpeedRng(2L)); gen.next(XorShiftSpeedRng(3L))
        gen.reset()
        val afterReset = gen.next(XorShiftSpeedRng(1L))
        assertEquals(first, afterReset, "reset ⇒ index 0 ⇒ same trial for the same fresh rng")
        assertTrue(afterReset.cue.startsWith("COLOR"), "the first rule is COLOR")
    }

    @Test
    fun is_deterministic_for_a_given_seed() {
        val g1 = RuleSwitchGenerator(config)
        val g2 = RuleSwitchGenerator(config)
        val r1 = XorShiftSpeedRng(123L)
        val r2 = XorShiftSpeedRng(123L)
        repeat(60) { assertEquals(g1.next(r1), g2.next(r2)) }
    }

    @Test
    fun exercise_id_is_e4_and_config_is_validated() {
        assertEquals(SpeedExerciseId.E4_CAMBIO, RuleSwitchGenerator().exerciseId)
        assertFailsWith<IllegalArgumentException> { RuleSwitchConfig(trialsPerRule = 1) }
    }
}
