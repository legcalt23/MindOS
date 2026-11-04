@file:Suppress("MagicNumber") // Probe sizes and seeds ARE the contract under test.

package com.mindos.app.features.mindboost.emotions.domain.exercise

import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Ola 4 · P2 — the held-out probe. These tests exist to stop the probe from quietly degrading into something
 * that still returns a number but no longer means "unseen items, balanced, chance = 1/4".
 */
class EmotionHoldoutProbeTest {

    // Scoped to the WHOLE bank (authored seed + generated extension), which is what the probe draws from.
    // Pinning the authored seed alone would let a generated holdout item look like "a trained item" here.
    private val evalPhrases = SeparatorItemFactory.ALL.filter { it.isEval }.map { it.phrase }.toSet()

    @Test
    fun a_probe_is_balanced_drawn_only_from_held_out_items() {
        val draw = EmotionHoldoutProbe.draw(servedItemIds = emptySet(), seed = 7L)
        val items = (draw as EmotionHoldoutProbe.Draw.Served).items

        assertEquals(EmotionHoldoutProbe.PROBE_SIZE, items.size)
        assertTrue(items.all { it.phrase in evalPhrases }, "a probe must never show a trained item")
        EmotionCategory.entries.forEach { category ->
            assertEquals(
                EmotionHoldoutProbe.ITEMS_PER_CATEGORY,
                items.count { it.category == category },
                "category $category is unbalanced, so chance is no longer ${EmotionHoldoutProbe.CHANCE_LEVEL}",
            )
        }
    }

    @Test
    fun the_same_seed_and_history_always_draw_the_same_probe_in_the_same_order() {
        val first = EmotionHoldoutProbe.draw(emptySet(), seed = 42L) as EmotionHoldoutProbe.Draw.Served
        val again = EmotionHoldoutProbe.draw(emptySet(), seed = 42L) as EmotionHoldoutProbe.Draw.Served
        assertEquals(first.items.map { it.phrase }, again.items.map { it.phrase })

        val other = EmotionHoldoutProbe.draw(emptySet(), seed = 43L) as EmotionHoldoutProbe.Draw.Served
        assertTrue(
            first.items.map { it.phrase } != other.items.map { it.phrase },
            "a different seed must be able to produce a different probe",
        )
    }

    @Test
    fun presentation_order_is_not_the_fixed_category_order() {
        // One item per category taken in declaration order would make the correct targets run A, B, C, D every
        // time — learnable without reading a single phrase. At least one seed must break that ordering.
        val declarationOrder = EmotionCategory.entries.toList()
        val anyShuffled = (1L..10L).any { seed ->
            val items = (EmotionHoldoutProbe.draw(emptySet(), seed) as EmotionHoldoutProbe.Draw.Served).items
            items.map { it.category } != declarationOrder
        }
        assertTrue(anyShuffled, "probe order must be shuffled, not the category declaration order")
    }

    @Test
    fun an_item_already_answered_is_never_served_again() {
        val first = (EmotionHoldoutProbe.draw(emptySet(), seed = 1L) as EmotionHoldoutProbe.Draw.Served).items
        val served = first.map { it.phrase }.toSet()
        val second = (EmotionHoldoutProbe.draw(served, seed = 1L) as EmotionHoldoutProbe.Draw.Served).items

        assertTrue(
            second.none { it.phrase in served },
            "re-serving a burned item measures memory of the probe, not the ability",
        )
    }

    @Test
    fun the_holdout_exhausts_after_the_bank_can_no_longer_fill_a_balanced_probe() {
        // The measurement ceiling of the authored bank, made explicit: a finite number of complete probes exist,
        // and past that the draw refuses rather than serving a shorter or unbalanced one.
        val served = mutableSetOf<String>()
        var probes = 0
        var stranded = -1
        while (stranded < 0) {
            when (val draw = EmotionHoldoutProbe.draw(served, seed = probes.toLong())) {
                is EmotionHoldoutProbe.Draw.Served -> {
                    served += draw.items.map { it.phrase }
                    probes++
                }
                is EmotionHoldoutProbe.Draw.Exhausted -> stranded = draw.freshItemsLeft
            }
        }

        assertTrue(probes > 0, "the authored bank must support at least one probe")
        assertEquals(EmotionHoldoutProbe.MAX_PROBES, probes, "complete probes available must match the bank")
        assertEquals(probes * EmotionHoldoutProbe.PROBE_SIZE, served.size, "every probe burns a full set of items")
        assertEquals(evalPhrases.size - served.size, stranded, "stranded items must be reported, not silently served")
    }

    @Test
    fun probe_trials_are_flagged_as_evaluation() {
        val items = (EmotionHoldoutProbe.draw(emptySet(), seed = 3L) as EmotionHoldoutProbe.Draw.Served).items
        val stimuli = EmotionHoldoutProbe.stimuli(items, responseWindowMs = 2500L, interStimulusDelayMs = 500L)

        assertEquals(items.size, stimuli.size)
        stimuli.forEach { stimulus ->
            val payload = stimulus.payload as EmotionStimulusPayload.Classification
            assertTrue(payload.isEval, "an unflagged probe trial would be counted as training")
        }
    }
}
