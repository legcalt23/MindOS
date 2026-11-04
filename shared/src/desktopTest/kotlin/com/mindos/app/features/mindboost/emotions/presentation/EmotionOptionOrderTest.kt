package com.mindos.app.features.mindboost.emotions.presentation

import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionAxis
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionDifficultyVector
import com.mindos.app.features.mindboost.emotions.domain.exercise.EM3SeparadorGenerator
import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionBlockSpec
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulus
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionFieldState
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Ola 3 · R3 · P1 — position must carry no information about the answer.
 *
 * EM3 rendered the four categories in a FIXED 2×2 grid, so the map from category to quadrant was constant for the
 * whole product. That collapses two different strategies into one: "always tap the same quadrant" scores exactly
 * what "always answer the same category" scores, whatever that happens to be. The first test measures that number
 * over the real bank at the levels the engine actually serves, because the size of a defect is a fact to compute,
 * not one to assert from memory — and the number is 0.34 at the bottom of the ambiguity axis and 0.45 at the top,
 * against a chance of 0.25. The cheapest possible strategy pays BEST exactly where the engine sends a good user.
 *
 * Shuffling the buttons per trial fixes the position half and only that: tapping a place becomes worth chance no
 * matter how the bank is written, which rebalancing today's items would not (the next authored item can undo it).
 * The category skew that the first test measures is a separate defect of content selection, it survives this fix
 * untouched, and it is recorded as its own finding rather than quietly folded into a presentation change.
 *
 * The other half of the reason is not a scoring exploit at all. A constant mapping gets learned: after a few
 * sessions the user executes a trained stimulus-response map instead of reading four labels, and that shortcut pays
 * off most when there is no time to read. R3's robustness index is accuracy under pressure over accuracy in calm,
 * so a rehearsed mapping would inflate the numerator and read as robustness that is really motor practice.
 */
class EmotionOptionOrderTest {

    @Test
    fun a_fixed_grid_hands_a_position_strategy_whatever_skew_the_content_has() {
        // Anti-vacuity for the test below: if the served categories were uniform, pinning the POSITION distribution
        // would be measuring nothing, because the fixed grid would already have been worth chance.
        val shares = LEVELS.associateWith { bestCategoryShare(it) }
        val reading = shares.entries.joinToString { "${it.key}->${round3(it.value)}" }

        assertTrue(
            shares.values.max() > CHANCE + MEANINGFUL_SKEW,
            "the served category mix is close enough to uniform that a fixed grid was not exploitable; " +
                "this file's premise no longer holds: $reading",
        )
        assertTrue(
            shares.getValue(MAX_AMBIGUITY) > shares.getValue(MIN_AMBIGUITY),
            "the skew no longer grows with difficulty, so the recorded reason for the finding is wrong: $reading",
        )
    }

    @Test
    fun the_answer_does_not_sit_in_one_slot_once_the_options_are_drawn_per_trial() {
        val slotCounts = IntArray(SLOTS)
        var total = 0
        LEVELS.forEach { level ->
            val rng = Random(ORDER_SEED)
            repeat(SEEDS) { seed ->
                served(level, seed.toLong()).forEach { stimulus ->
                    val field = emotionFieldFor(stimulus, rng) as EmotionFieldState.FourCategory
                    val truth = (stimulus.payload as EmotionStimulusPayload.Classification).truth
                    slotCounts[field.options.indexOf(truth)]++
                    total++
                }
            }
        }

        // Goodness-of-fit against uniform, df = 3, alpha = 0.001. Deterministic RNG + fixed seeds, so it cannot
        // flake. A fixed grid fails this by construction: the answer's slot would follow the category mix above.
        val expected = total.toDouble() / SLOTS
        val chiSquare = slotCounts.sumOf { count ->
            val diff = count - expected
            diff * diff / expected
        }
        assertTrue(slotCounts.all { it > 0 }, "some slot never held the answer: ${slotCounts.toList()}")
        assertTrue(chiSquare < CHI_SQUARE_DF3_ALPHA_001, "the answer's slot is not uniform: chi2=$chiSquare")
    }

    @Test
    fun every_field_offers_the_four_categories_exactly_once() {
        val rng = Random(ORDER_SEED)
        served(MAX_AMBIGUITY, seed = 1L).forEach { stimulus ->
            val field = emotionFieldFor(stimulus, rng) as EmotionFieldState.FourCategory
            assertEquals(
                EmotionCategory.entries.toSet(),
                field.options.toSet(),
                "shuffling may reorder the options and nothing else — none added, dropped or repeated",
            )
            assertEquals(EmotionCategory.entries.size, field.options.size, "an option was duplicated")
        }
    }

    @Test
    fun the_order_is_deterministic_for_a_seed() {
        val stimuli = served(MAX_AMBIGUITY, seed = 3L)
        val first = orders(stimuli, Random(ORDER_SEED))
        val second = orders(stimuli, Random(ORDER_SEED))
        val other = orders(stimuli, Random(ORDER_SEED + 1))

        assertEquals(first, second, "the same seed must replay the same option orders")
        assertTrue(first != other, "different seeds produced identical orders")
        assertTrue(first.toSet().size > 1, "every trial in the block was served the same order")
    }

    private fun orders(stimuli: List<EmotionStimulus>, rng: Random): List<List<EmotionCategory>> =
        stimuli.map { (emotionFieldFor(it, rng) as EmotionFieldState.FourCategory).options }

    /** The share of served trials won by always answering the single best category, at difficulty [level]. */
    private fun bestCategoryShare(level: Double): Double {
        val counts = mutableMapOf<EmotionCategory, Int>()
        var total = 0
        repeat(SEEDS) { seed ->
            served(level, seed.toLong()).forEach { stimulus ->
                val truth = (stimulus.payload as EmotionStimulusPayload.Classification).truth
                counts[truth] = (counts[truth] ?: 0) + 1
                total++
            }
        }
        return counts.values.max().toDouble() / total
    }

    private fun served(level: Double, seed: Long): List<EmotionStimulus> {
        val difficulty = EmotionDifficultyVector(mapOf(EmotionAxis.CATEGORY_AMBIGUITY to level))
        val spec = EmotionBlockSpec(targetTrialCount = TRIALS, responseWindowMs = WINDOW_MS, recentPhrases = emptySet())
        return EM3SeparadorGenerator().generate(difficulty, spec, seed)
    }

    private fun round3(value: Double): Double = (value * 1_000).toInt() / 1_000.0

    private companion object {
        const val SEEDS = 200
        const val TRIALS = 10
        const val WINDOW_MS = 2_500L
        const val ORDER_SEED = 4_242L
        const val SLOTS = 4
        const val CHANCE = 0.25
        const val MEANINGFUL_SKEW = 0.05
        const val MIN_AMBIGUITY = 0.0
        const val MAX_AMBIGUITY = 1.0
        const val CHI_SQUARE_DF3_ALPHA_001 = 16.266
        val LEVELS = listOf(MIN_AMBIGUITY, 0.3, 0.5, 0.8, MAX_AMBIGUITY)
    }
}
