package com.mindos.app.features.mindboost.language.presentation

import kotlin.random.Random

/**
 * Serve-time presentation of a closed item.
 *
 * Option **order** is not content: the answer key names an option by id, the screen renders only the
 * option text, and the diagnostic distractor map is keyed by id too — so the slot an option occupies
 * carries no meaning to anything except the user's eye. Left as authored it carried a great deal to the
 * user: four of the six Lenguaje banks placed the correct answer in slot "b" in every item and a
 * fifth in five of six — 32 of 36 closed items — and the ordering items were handed over already in
 * their key order. "Tap the second option" and "tap top to bottom" scored 89% and 100% while reading
 * nothing at all.
 *
 * Shuffling here rather than rebalancing the banks is deliberate. Rebalanced content fixes the items that
 * exist and guarantees nothing about the next one authored; a serve-time shuffle makes position
 * structurally uninformative for any bank, including future ones. Option-order randomization is also the
 * standard psychometric practice for exactly this reason.
 *
 * Evidence spans are deliberately NOT shuffled: they are quotations from the passage and their order is
 * the passage's, not a scoring artefact.
 */
object ItemPresentation {

    /** Returns [items] with each item's options in a seeded random order; ids, texts and keys untouched. */
    fun shuffleOptions(items: List<DemoItem>, rng: Random): List<DemoItem> = items.map { shuffleOptions(it, rng) }

    /** Returns [item] with its options in a seeded random order. */
    fun shuffleOptions(item: DemoItem, rng: Random): DemoItem =
        if (item.options.size < MIN_SHUFFLEABLE) item else item.copy(options = item.options.shuffled(rng))

    private const val MIN_SHUFFLEABLE = 2
}
