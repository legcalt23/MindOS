@file:Suppress("MagicNumber") // The cell counts ARE the contract under test.

package com.mindos.app.features.mindboost.emotions.domain.session

import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Ola 2 · R3 · P4 — reading the matrix that nobody read.
 *
 * The risk this suite exists for is not a missing insight, it is a fabricated one: an exercise that tells a user
 * they confuse urges with facts because they slipped twice. So most of what is pinned here is the REFUSAL, and
 * every threshold has a case just below it and a case just above it.
 */
class EmotionConfusionMatrixReadingTest {

    /** Builds a matrix from `(truth, chosen) to count` cells; every other cell is zero. */
    private fun matrix(vararg cells: Pair<Pair<EmotionCategory, EmotionCategory>, Int>): EmotionConfusionMatrix {
        val counts = EmotionCategory.entries.associateWith { truth ->
            cells.filter { it.first.first == truth }.associate { it.first.second to it.second }
        }
        return EmotionConfusionMatrix(counts)
    }

    private val impulseAsFact = EmotionCategory.IMPULSE to EmotionCategory.FACT
    private val factAsImpulse = EmotionCategory.FACT to EmotionCategory.IMPULSE
    private val thoughtAsFact = EmotionCategory.THOUGHT to EmotionCategory.FACT

    @Test
    fun a_session_whose_mistakes_pile_onto_one_boundary_names_it_with_its_direction() {
        val reading = matrix(
            (EmotionCategory.IMPULSE to EmotionCategory.IMPULSE) to 4,
            impulseAsFact to 5,
            thoughtAsFact to 1,
        ).dominantConfusion()

        assertEquals(DominantConfusion(EmotionCategory.IMPULSE, EmotionCategory.FACT, 5, 6), reading)
    }

    @Test
    fun the_direction_is_not_collapsed_into_a_symmetric_pair() {
        // Calling facts urges is a different mistake from calling urges facts, and it takes a different check.
        // A symmetric "you mix up urges and facts" would be true of both and useful for neither.
        val reading = matrix(factAsImpulse to 5, thoughtAsFact to 1).dominantConfusion()
        assertEquals(EmotionCategory.FACT, reading?.truth)
        assertEquals(EmotionCategory.IMPULSE, reading?.chosen)
    }

    @Test
    fun a_handful_of_slips_is_not_a_pattern() {
        // Three mistakes, all in the same direction — a perfect 100 % share, and still not enough mistakes for
        // the session to have a shape. This is the case a naive "most common error" reader would happily name.
        assertNull(matrix(impulseAsFact to 3).dominantConfusion())
        // And the case just above the floor does produce one, so the refusal above is the threshold and not a
        // blanket silence.
        assertEquals(4, matrix(impulseAsFact to 4).dominantConfusion()?.count)
    }

    @Test
    fun mistakes_scattered_across_boundaries_name_none_of_them() {
        val scattered = matrix(
            impulseAsFact to 2,
            thoughtAsFact to 2,
            (EmotionCategory.INTERNAL_STATE to EmotionCategory.THOUGHT) to 2,
        )
        assertNull(scattered.dominantConfusion(), "6 errors spread three ways is not a dominant confusion")
    }

    @Test
    fun a_direction_that_only_ties_for_first_names_nothing() {
        // The discriminating half of "most common wins": two boundaries missed equally often is a session with
        // two problems, and picking whichever one the map happened to yield first would be arbitrary.
        val tied = matrix(impulseAsFact to 3, thoughtAsFact to 3)
        assertNull(tied.dominantConfusion(), "a tie must not be broken by iteration order")
    }

    @Test
    fun a_direction_that_is_ahead_but_is_not_half_the_mistakes_names_nothing() {
        // 4 of 10 is the most common error and still a minority of what went wrong; calling it THE confusion
        // would hide the other 6.
        val thin = matrix(impulseAsFact to 4, thoughtAsFact to 3, factAsImpulse to 3)
        assertNull(thin.dominantConfusion())

        // Same shape, same leader, but now it carries half the mistakes.
        val enough = matrix(impulseAsFact to 5, thoughtAsFact to 3, factAsImpulse to 2)
        assertEquals(5, enough.dominantConfusion()?.count)
    }

    @Test
    fun a_flawless_session_has_no_confusion_to_report() {
        val perfect = EmotionCategory.entries.associateWith { mapOf(it to 6) }
        assertNull(EmotionConfusionMatrix(perfect).dominantConfusion())
        assertNull(EmotionConfusionMatrix(emptyMap()).dominantConfusion())
    }

    @Test
    fun the_diagonal_is_never_read_as_a_confusion() {
        // Correct answers live on the diagonal and always outnumber the errors in a decent session. Reading the
        // largest cell instead of the largest OFF-diagonal cell would report "facts, as facts" as the problem.
        val mostlyRight = matrix(
            (EmotionCategory.FACT to EmotionCategory.FACT) to 40,
            impulseAsFact to 4,
        )
        assertEquals(EmotionCategory.IMPULSE, mostlyRight.dominantConfusion()?.truth)
        assertEquals(4, mostlyRight.dominantConfusion()?.errors, "the diagonal must not count as an error")
    }
}
