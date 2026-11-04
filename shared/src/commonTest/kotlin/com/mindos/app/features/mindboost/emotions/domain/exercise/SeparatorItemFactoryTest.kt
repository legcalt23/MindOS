@file:Suppress("MagicNumber") // The sizes, shares and ceilings ARE the contract under test.

package com.mindos.app.features.mindboost.emotions.domain.exercise

import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.domain.safety.EmotionTerminologyScanner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Ola 2 · R3 · P1 — the bank's design contract.
 *
 * These tests exist to make one specific regression impossible: a future edit that "simplifies" the templates
 * into one opening per category. That version would compile, would still produce hundreds of items, and would
 * turn the exercise into a prefix detector. [openings_that_carry_a_confusion_axis_do_not_decide_the_category]
 * and [reading_only_the_first_word_cannot_carry_the_bank] both go red on it.
 */
class SeparatorItemFactoryTest {

    private val bank = SeparatorItemFactory.ALL

    /** Openings deliberately shared across categories — one per confusion axis, plus the subject family. */
    private val axisOpenings = listOf("siento", "noto", "esto", "el", "la")

    private fun firstWord(phrase: String) = phrase.substringBefore(' ').lowercase()

    private fun majorityShare(items: List<EmotionContent.SeparatorItem>): Double =
        items.groupingBy { it.category }.eachCount().values.max().toDouble() / items.size

    // ------------------------------------------------------------------------------------- size

    @Test
    fun the_bank_reaches_the_design_size_by_extending_the_authored_seed() {
        assertTrue(
            bank.size >= SeparatorItemFactory.DESIGN_MIN_BANK_SIZE,
            "bank is ${bank.size}, design floor is ${SeparatorItemFactory.DESIGN_MIN_BANK_SIZE}",
        )
        // The authored items were hand-validated. They must survive verbatim and stay at the front, so an
        // exposure history keyed by phrase keeps pointing at the same content.
        assertEquals(EmotionContent.SEPARATOR_ITEMS, bank.take(EmotionContent.SEPARATOR_ITEMS.size))
    }

    @Test
    fun no_phrase_appears_twice_or_under_two_categories() {
        val phrases = bank.map { it.phrase }
        assertEquals(phrases.size, phrases.toSet().size, "duplicate phrases: an item cannot be shown twice as new")
        val ambiguous = bank.groupBy { it.phrase }.filterValues { rows -> rows.map { it.category }.toSet().size > 1 }
        assertTrue(ambiguous.isEmpty(), "phrases with no correct answer: ${ambiguous.keys}")
    }

    @Test
    fun no_phrase_is_blank_or_too_long_to_read_inside_the_response_window() {
        // A phrase that cannot be read inside the window would be measuring reading speed, not separation.
        val offenders = bank.filter { it.phrase.isBlank() || it.phrase.length > 48 }
        assertTrue(offenders.isEmpty(), "unreadable items: ${offenders.map { it.phrase }}")
    }

    /**
     * Ola 3 · R3 · P2 — the length spread the timing design has to survive, measured rather than assumed.
     *
     * Items run 2 to 7 words today. That spread is the whole reason the calm window stopped varying with
     * difficulty: one window for every length spends a different share of a short item's reading time than of a
     * long one's, so tightening it was a second difficulty dimension the engine could not see. It is also the
     * domain any length-scaled pressure window has to be safe over, so the ceiling is pinned here rather than
     * discovered later by an item nobody could read in time.
     */
    @Test
    fun every_phrase_is_between_two_and_seven_words() {
        val words = bank.map { it.phrase.trim().split(" ").size }
        val offenders = bank.filter { it.phrase.trim().split(" ").size !in MIN_WORDS..MAX_WORDS }

        assertTrue(words.toSet().size > 1, "every phrase is the same length — this test is measuring nothing")
        assertTrue(
            offenders.isEmpty(),
            "items outside $MIN_WORDS..$MAX_WORDS words (observed ${words.min()}..${words.max()}): " +
                offenders.map { it.phrase },
        )
    }

    // ------------------------------------------- the memorization defence (the point of this phase)

    @Test
    fun openings_that_carry_a_confusion_axis_do_not_decide_the_category() {
        axisOpenings.forEach { opening ->
            val group = bank.filter { firstWord(it.phrase) == opening }
            assertTrue(group.size >= 8, "'$opening' has only ${group.size} items — too few to be a real axis")
            assertTrue(
                group.map { it.category }.toSet().size >= 2,
                "'$opening' is diagnostic: every item under it is ${group.first().category}",
            )
            val share = majorityShare(group)
            assertTrue(
                share <= 0.80,
                "guessing the majority category after reading '$opening' scores ${(share * 100).toInt()}%",
            )
        }
    }

    @Test
    fun every_action_verb_appears_as_both_an_urge_and_something_already_done() {
        // This is the impulse↔fact axis made mechanical: if the verb were only ever an urge, the verb alone
        // would answer the item and the axis would be decorative.
        // Scoped to the generated half: the authored seed was written by hand and one of its verbs is unique
        // to an urge, which is fine. The guarantee being pinned is the one the templates are supposed to give.
        val generated = SeparatorItemFactory.GENERATED
        val facts = generated.filter { it.category == EmotionCategory.FACT }.map { it.phrase }
        val actions = generated.filter { it.category == EmotionCategory.IMPULSE }
            .mapNotNull { item -> item.phrase.substringAfter("Quiero ", "").takeIf { it.isNotEmpty() } }
        assertTrue(actions.size >= 8, "only ${actions.size} generated `Quiero …` items — the check below is thin")
        val stranded = actions.filterNot { action -> facts.any { it.contains(action) } }
        assertTrue(stranded.isEmpty(), "verbs that only ever appear as an urge: $stranded")
    }

    @Test
    fun reading_only_the_first_word_cannot_carry_the_bank() {
        // Best case for a user who learned every opening→category mapping and never reads further.
        val ceiling = bank.groupBy { firstWord(it.phrase) }
            .values
            .sumOf { group -> group.groupingBy { it.category }.eachCount().values.max() }
            .toDouble() / bank.size
        assertTrue(ceiling <= 0.85, "a first-word strategy scores ${(ceiling * 100).toInt()}% of the bank")

        // And the shared openings must not be a rounding error: they have to cover a real share of the bank,
        // or the ceiling above could be met by a handful of odd items while the bulk stays a giveaway.
        val underShared = bank.count { item ->
            bank.filter { firstWord(it.phrase) == firstWord(item.phrase) }
                .map { it.category }
                .toSet()
                .size >= 2
        }
        assertTrue(
            underShared.toDouble() / bank.size >= 0.50,
            "only $underShared/${bank.size} items sit under an opening shared by two categories",
        )
    }

    // ------------------------------------------------------------------------ axes and difficulty

    @Test
    fun boundary_items_name_their_axis_and_prototypical_ones_do_not() {
        bank.filter { it.ambiguity == 1 }.forEach {
            assertNotNull(it.axis, "unlabelled boundary item: ${it.phrase}")
        }
        bank.filter { it.ambiguity == 0 }.forEach {
            assertNull(it.axis, "prototypical item claims an axis: ${it.phrase}")
        }
    }

    @Test
    fun all_three_axes_have_items_on_both_of_their_sides() {
        val sides = mapOf(
            EmotionContent.ConfusionAxis.STATE_THOUGHT to
                setOf(EmotionCategory.INTERNAL_STATE, EmotionCategory.THOUGHT),
            EmotionContent.ConfusionAxis.IMPULSE_FACT to
                setOf(EmotionCategory.IMPULSE, EmotionCategory.FACT),
            EmotionContent.ConfusionAxis.THOUGHT_FACT to
                setOf(EmotionCategory.THOUGHT, EmotionCategory.FACT),
        )
        sides.forEach { (axis, expected) ->
            val present = bank.filter { it.axis == axis }.map { it.category }.toSet()
            assertEquals(expected, present, "$axis is one-sided: it only has items in $present")
        }
    }

    @Test
    fun categories_are_balanced_enough_for_a_four_way_task() {
        val counts = EmotionCategory.entries.map { c -> bank.count { it.category == c } }
        val ratio = counts.max().toDouble() / counts.min()
        assertTrue(ratio <= 1.35, "category counts $counts are too skewed for a 4-way task (ratio $ratio)")
    }

    // ------------------------------------------------------------------------------------ holdout

    @Test
    fun the_holdout_is_balanced_and_carries_the_same_difficulty_mix_as_training() {
        EmotionCategory.entries.forEach { category ->
            val group = bank.filter { it.category == category }
            val held = group.filter { it.isEval }
            val trained = group.filter { !it.isEval }
            assertTrue(held.size >= 12, "$category holds out only ${held.size} items")
            assertTrue(trained.size >= 12, "$category trains on only ${trained.size} items")

            val heldMix = held.count { it.ambiguity == 1 }.toDouble() / held.size
            val trainedMix = trained.count { it.ambiguity == 1 }.toDouble() / trained.size
            // A holdout made mostly of boundary items would be a harder task than training, and the gap would
            // read as a transfer failure that is really a difficulty difference.
            val gap = kotlin.math.abs(heldMix - trainedMix)
            assertTrue(gap <= 0.15, "$category holdout is a different task: boundary share $heldMix vs $trainedMix")
        }
    }

    @Test
    fun training_and_holdout_never_share_a_phrase() {
        val held = bank.filter { it.isEval }.map { it.phrase }.toSet()
        val trained = bank.filter { !it.isEval }.map { it.phrase }.toSet()
        assertTrue(held.intersect(trained).isEmpty(), "leaked into the holdout: ${held.intersect(trained)}")
    }

    // ------------------------------------------------------------------------------------ content

    @Test
    fun generation_is_deterministic() {
        // No RNG is involved, so the whole bank is a golden. Exposure history is keyed by phrase: a bank that
        // reshuffled between runs would silently re-point every stored exposure at different content.
        assertEquals(bank, SeparatorItemFactory.ALL)
        assertEquals(bank.map { it.phrase }, SeparatorItemFactory.ALL.map { it.phrase })
    }

    @Test
    fun every_phrase_stays_inside_the_mild_induction_ceiling() {
        // Assembled from fragments so this source file never holds a contiguous forbidden word — the emotions
        // terminology gate scans it too.
        val forbidden = listOf(
            "ansi" + "edad", "estr" + "es", "depres" + "ion", "diagnost" + "ico", "terap" + "ia",
            "tratamient" + "o", "trau" + "ma", "trastorn" + "o", "patolog" + "ia", "pani" + "co",
            "suicid" + "io", "autolesi" + "on", "cris" + "is", "cur" + "ar", "enfermedad ment" + "al",
        )
        val hits = EmotionTerminologyScanner.scan(bank.joinToString("\n") { it.phrase }, forbidden)
        assertTrue(hits.isEmpty(), "clinical terminology reached the item bank: ${hits.map { it.term }}")
    }

    private companion object {
        const val MIN_WORDS = 2
        const val MAX_WORDS = 7
    }
}
