@file:Suppress("MagicNumber") // Minimum bank/category counts are the spec under test.

package com.mindos.app.features.mindboost.emotions.domain.exercise

import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Pins the EM3 bank's measurement properties (decision E3: ≥30, balanced, expandable to 80) and the
 * holdout-separation invariant: training and eval item pools are disjoint, and every category has both.
 */
class EmotionSeparatorBankTest {

    private val bank = EmotionContent.SEPARATOR_ITEMS

    @Test
    fun bank_is_large_enough_and_balanced() {
        assertTrue(bank.size >= 32, "bank must ship ≥32 items (target 80); was ${bank.size}")
        EmotionCategory.entries.forEach { category ->
            assertTrue(bank.count { it.category == category } >= 8, "category $category needs ≥8 items")
        }
    }

    @Test
    fun training_and_eval_pools_are_disjoint_and_both_present() {
        val train = bank.filter { !it.isEval }.map { it.phrase }.toSet()
        val eval = bank.filter { it.isEval }.map { it.phrase }.toSet()
        assertTrue(train.isNotEmpty(), "training pool must be non-empty")
        assertTrue(eval.isNotEmpty(), "eval (holdout) pool must be non-empty")
        assertTrue(train.intersect(eval).isEmpty(), "train and eval pools must be disjoint by phrase")
        assertTrue(bank.map { it.phrase }.toSet().size == bank.size, "no duplicate phrases in the bank")
    }

    @Test
    fun every_category_has_a_held_out_eval_item() {
        EmotionCategory.entries.forEach { category ->
            assertTrue(
                bank.any { it.category == category && it.isEval },
                "category $category needs a held-out eval item for the holdout probe",
            )
        }
    }
}
