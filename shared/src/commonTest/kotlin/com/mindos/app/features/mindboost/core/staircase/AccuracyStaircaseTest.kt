@file:Suppress("MagicNumber") // Targets, steps and run lengths ARE the contract under test.

package com.mindos.app.features.mindboost.core.staircase

import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Ola 3 · R2 — the staircase that replaced planting in the recovery exercise.
 *
 * Two properties are worth a test and one of them is worth the whole file. The first is ordinary: the walk must
 * actually sit at the error rate it claims, or the exercise is back to letting the user's own skill decide how
 * much evidence it collects. The second is the reason this class exists at all — the level a trial is presented
 * at must not be EASIER because the previous trial was an error, because this exercise's primary reading is
 * post-error accuracy and a staircase without the lag would manufacture that reading in the flattering direction
 * with its own difficulty knob.
 *
 * The observer below answers correctly with probability equal to the level, so easiness and accuracy share a
 * scale and the walk's fixed point is the target itself. That is a modelling convenience, not an assumption
 * about users: it makes the arithmetic checkable, and every assertion here is about the WALK, not about people.
 */
class AccuracyStaircaseTest {

    private fun staircase(target: Double, stepDown: Double = 0.03) = AccuracyStaircase(
        targetAccuracy = target,
        stepDown = stepDown,
        initialLevel = 0.55,
        minLevel = 0.02,
        maxLevel = 0.98,
    )

    /** One session of a synthetic observer whose chance of being right IS the presented level. */
    private fun run(
        staircase: AccuracyStaircase,
        trials: Int,
        seed: Long,
    ): List<Pair<Double, Boolean>> {
        val rng = Random(seed)
        return (0 until trials).map {
            val presented = staircase.currentLevel
            val correct = rng.nextDouble() < presented
            staircase.onOutcome(correct)
            presented to correct
        }
    }

    @Test
    fun the_up_step_is_derived_from_the_target_and_not_a_tuned_constant() {
        // p * down == (1 - p) * up is the stationarity condition; anything else converges somewhere else.
        listOf(0.65, 0.70, 0.75, 0.80).forEach { target ->
            val s = staircase(target)
            assertEquals(0.03 * target / (1.0 - target), s.stepUp, 1e-12, "up step is not derived at target $target")
        }
    }

    @Test
    fun retargeting_moves_the_up_step_with_the_target() {
        val s = staircase(0.75)
        val before = s.stepUp
        s.retargetTo(0.80)
        assertEquals(0.80, s.targetAccuracy, 1e-12)
        assertTrue(s.stepUp > before, "a higher target must demand a bigger correction after an error")
    }

    @Test
    fun the_walk_holds_the_error_rate_at_its_target() {
        listOf(0.65, 0.75, 0.80).forEach { target ->
            val outcomes = run(staircase(target), trials = 4_000, seed = 7L).drop(500) // drop the approach
            val realized = outcomes.count { it.second }.toDouble() / outcomes.size
            assertTrue(
                abs(realized - target) < 0.04,
                "target $target but the session realized $realized — the exercise would collect a different " +
                    "amount of evidence from every user, which is the defect the staircase replaced",
            )
        }
    }

    /**
     * THE test. A plain staircase eases immediately after an error, so its post-error trials are easier by
     * construction and post-error accuracy comes out inflated. The lagged walk must not do that.
     *
     * The discriminating half is the no-lag reference built in the same loop: without it, "post-error is not
     * easier" would also pass for a staircase that never moved at all, and the assertion would be pinning
     * nothing. With it, the contrast is the measurement: the reference separates the two cells by roughly a full
     * up-step in the flattering direction, and the real class does not.
     */
    @Test
    fun the_presented_level_is_never_easier_because_the_previous_trial_was_an_error() {
        val target = 0.75
        val lagged = run(staircase(target), trials = 6_000, seed = 21L)

        // Same observer, same seed, but the outcome is applied immediately — the confound in its pure form.
        val noLag = buildList {
            val rng = Random(21L)
            val up = 0.03 * target / (1.0 - target)
            var level = 0.55
            repeat(6_000) {
                val presented = level
                val correct = rng.nextDouble() < presented
                level = (if (correct) level - 0.03 else level + up).coerceIn(0.02, 0.98)
                add(presented to correct)
            }
        }

        fun easinessGap(session: List<Pair<Double, Boolean>>): Double {
            val afterError = session.zipWithNext().filter { !it.first.second }.map { it.second.first }
            val afterCorrect = session.zipWithNext().filter { it.first.second }.map { it.second.first }
            assertTrue(afterError.size > 500 && afterCorrect.size > 500, "not enough of both cells to compare")
            return afterError.average() - afterCorrect.average()
        }

        val referenceGap = easinessGap(noLag)
        assertTrue(
            referenceGap > 0.05,
            "the no-lag reference did not reproduce the confound (gap $referenceGap); without it this test " +
                "would pass for a staircase that simply never moves",
        )
        assertTrue(
            easinessGap(lagged) <= 0.0,
            "post-error trials are presented easier than post-correct ones (gap ${easinessGap(lagged)}); the " +
                "exercise would report a recovery produced by its own difficulty knob",
        )
    }

    @Test
    fun the_first_response_moves_nothing_because_there_is_nothing_held_back_yet() {
        val s = staircase(0.75)
        val start = s.currentLevel
        s.onOutcome(correct = false)
        assertEquals(start, s.currentLevel, 1e-12, "the first outcome has no predecessor to apply")
        s.onOutcome(correct = true)
        assertEquals(start + s.stepUp, s.currentLevel, 1e-12, "the second call must apply the FIRST outcome")
    }

    @Test
    fun the_level_never_leaves_its_bounds() {
        val floorWalk = AccuracyStaircase(0.75, stepDown = 0.30, initialLevel = 0.5, minLevel = 0.1, maxLevel = 0.9)
        repeat(20) { floorWalk.onOutcome(correct = true) }
        assertEquals(0.1, floorWalk.currentLevel, 1e-12)
        val ceilingWalk = AccuracyStaircase(0.75, stepDown = 0.30, initialLevel = 0.5, minLevel = 0.1, maxLevel = 0.9)
        repeat(20) { ceilingWalk.onOutcome(correct = false) }
        assertEquals(0.9, ceilingWalk.currentLevel, 1e-12)
    }

    @Test
    fun a_target_outside_zero_to_one_is_refused_rather_than_clamped() {
        // A clamped target would silently converge somewhere nobody chose; refusing says the caller is wrong.
        listOf(0.0, 1.0, -0.1, 1.2).forEach { bad ->
            assertFailsWith<IllegalArgumentException>("target $bad was accepted") { staircase(bad) }
            assertFailsWith<IllegalArgumentException>("retarget to $bad was accepted") {
                staircase(0.75).retargetTo(bad)
            }
        }
        assertFailsWith<IllegalArgumentException> { AccuracyStaircase(0.75, 0.0, 0.5, 0.1, 0.9) }
        assertFailsWith<IllegalArgumentException> { AccuracyStaircase(0.75, 0.03, 0.5, 0.9, 0.1) }
    }
}
