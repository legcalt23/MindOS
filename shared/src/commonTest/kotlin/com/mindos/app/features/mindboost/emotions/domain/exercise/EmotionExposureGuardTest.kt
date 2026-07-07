@file:Suppress("MagicNumber", "MaxLineLength") // Block sizes / seeds are the fixtures under test.

package com.mindos.app.features.mindboost.emotions.domain.exercise

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * MC8u-D §5 — the cross-session exposure guard reduces item repetition for the EM3/EM10 content bank so a repeated
 * session cannot inflate the DISPLAYED block score through false familiarity, while staying deterministic, honest
 * (only reorders the clean bank), and graceful when fresh content runs low.
 */
class EmotionExposureGuardTest {

    private val training = EmotionContent.SEPARATOR_ITEMS.filter { !it.isEval }
    private val cleanBank = training.map { it.phrase }.toSet()

    private fun phrases(items: List<EmotionContent.SeparatorItem>): List<String> = items.map { it.phrase }

    @Test
    fun fresh_first_moves_recent_items_after_fresh_ones_stably() {
        val recent = setOf(training[0].phrase, training[2].phrase)
        val reordered = EmotionExposureGuard.freshFirst(training, recent)
        val firstRecentPos = reordered.indexOfFirst { it.phrase in recent }
        val lastFreshPos = reordered.indexOfLast { it.phrase !in recent }
        assertTrue(firstRecentPos > lastFreshPos, "every fresh item must come before every recent one")
        assertEquals(training.toSet(), reordered.toSet(), "no item is dropped or invented")
    }

    @Test
    fun without_the_guard_the_same_seed_replays_the_exact_same_items() {
        val a = SeparatorDifficulty.select(training, 10, 0.0, Random(1), recent = emptySet())
        val b = SeparatorDifficulty.select(training, 10, 0.0, Random(1), recent = emptySet())
        assertEquals(phrases(a), phrases(b), "no guard + same seed ⇒ identical replay (the memorization risk)")
    }

    @Test
    fun recent_exposure_pushes_the_next_session_to_fresh_items() {
        val sessionA = SeparatorDifficulty.select(training, 10, 0.0, Random(1), recent = emptySet())
        val recent = phrases(sessionA).toSet()
        val sessionB = SeparatorDifficulty.select(training, 10, 0.0, Random(1), recent = recent)
        // 20 clear items, 10 shown in A → 10 fresh remain, enough to fill B with zero repeats.
        assertTrue(phrases(sessionA).toSet().intersect(phrases(sessionB).toSet()).isEmpty(), "session B must avoid session A's items when enough fresh content exists")
    }

    @Test
    fun selection_is_deterministic_for_the_same_seed_and_history() {
        val recent = setOf(training[1].phrase, training[5].phrase)
        val first = SeparatorDifficulty.select(training, 10, 0.5, Random(7), recent = recent)
        val second = SeparatorDifficulty.select(training, 10, 0.5, Random(7), recent = recent)
        assertEquals(phrases(first), phrases(second), "same seed + same history ⇒ identical selection")
    }

    @Test
    fun guard_falls_back_gracefully_when_almost_everything_is_recent() {
        val allRecent = training.map { it.phrase }.toSet()
        val picked = SeparatorDifficulty.select(training, 10, 0.0, Random(3), recent = allRecent)
        assertEquals(10, picked.size, "the block is never shrunk — it falls back to reusing seen items")
        assertTrue(picked.all { it.phrase in cleanBank }, "the guard only ever draws from the clean, pre-verified bank")
    }
}
