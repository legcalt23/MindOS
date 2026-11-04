@file:Suppress("MagicNumber") // The session shape and the pair floors ARE the contract under test.

package com.mindos.app.features.mindboost.emotions.domain.exercise

import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionAxis
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionDifficultyVector
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseCatalog
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.metrics.DefaultEmotionMetricsEngine
import com.mindos.app.features.mindboost.emotions.presentation.emotionTrialsPerBlock
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Ola 3 · R2 — are the post-error readings REACHABLE, computed on the real block size and the real staircase?
 *
 * Same failure mode R1 closed, one exercise over: a floor no realistic session can clear reads as honesty
 * ("not enough data") while being a design defect, and if whether it clears depends on the difficulty axis, the
 * adaptive engine has quietly become the thing that decides whether the exercise's own primary claim is
 * eligible. The binding case is the BOTTOM of `POST_ERROR_LOAD`, where the staircase asks for as few errors as
 * it ever asks for — sizing the block off the top would leave the reading available only to users the engine had
 * already pushed harder.
 *
 * The observer answers correctly with probability equal to the presented level, the same model
 * `AccuracyStaircaseTest` uses, and for the same reason: it makes the walk's fixed point the target, so what is
 * being measured here is the SESSION SHAPE rather than any claim about people.
 */
class EmotionPostErrorReachabilityTest {

    private val generator = EM2RecuperacionGenerator()
    private val trialsPerBlock = emotionTrialsPerBlock(
        EmotionExerciseId.EM2_RECUPERACION,
        EmotionExerciseCatalog.definition(EmotionExerciseId.EM2_RECUPERACION).governingMetric,
    )

    /** Post-error and post-correct pair counts of ONE block — the unit the floors are applied to. */
    private fun pairsInBlock(level: Double, trials: Int, seed: Long): Pair<Int, Int> {
        val difficulty = EmotionDifficultyVector.initial(EmotionAxis.entries.toSet(), start = level)
        val staircase = generator.staircase(difficulty)
        val rng = Random(seed)
        val outcomes = (0 until trials).map {
            val correct = rng.nextDouble() < staircase.currentLevel
            staircase.onOutcome(correct)
            correct
        }
        val postError = outcomes.zipWithNext().count { !it.first }
        val postCorrect = outcomes.zipWithNext().count { it.first }
        return postError to postCorrect
    }

    @Test
    fun both_cells_of_the_paired_reading_are_reachable_at_every_level_of_the_load_axis() {
        val errorFloor = DefaultEmotionMetricsEngine.MIN_POST_ERROR_CASES
        val correctFloor = DefaultEmotionMetricsEngine.MIN_POST_ERROR_FOLLOWUPS
        listOf(0.0, 0.25, 0.5, 0.75, 1.0).forEach { level ->
            val worstError = (1L..60L).minOf { pairsInBlock(level, trialsPerBlock, it).first }
            val worstCorrect = (1L..60L).minOf { pairsInBlock(level, trialsPerBlock, it).second }
            assertTrue(
                worstError >= errorFloor,
                "at load level $level the unluckiest of 60 blocks yields only $worstError post-error pairs, " +
                    "below the floor of $errorFloor — the primary reading would depend on the seed",
            )
            assertTrue(
                worstCorrect >= correctFloor,
                "at load level $level the unluckiest of 60 blocks yields only $worstCorrect post-correct " +
                    "baseline pairs, below the floor of $correctFloor — a post-error effect is worth its " +
                    "SMALLER cell, and raising the error rate is what starves this one",
            )
        }
    }

    /**
     * The discriminating half, and the arithmetic that justifies the block size rather than asserting it. At the
     * audit's nominal 20-trial block the same staircase does NOT clear the floor, so 48 is a derived number: if
     * a future edit shrinks the block back toward the module default, this fails instead of quietly returning
     * `unavailable` to every user.
     */
    @Test
    fun the_module_default_block_would_not_have_reached_the_floor() {
        val floor = DefaultEmotionMetricsEngine.MIN_POST_ERROR_CASES
        val worst = (1L..60L).minOf { pairsInBlock(level = 0.0, trials = 20, seed = it).first }
        assertTrue(
            worst < floor,
            "a 20-trial block already clears the floor in its worst case ($worst >= $floor); the 48-trial " +
                "block is then unexplained, and the comment deriving it is stale",
        )
        assertTrue(trialsPerBlock == 48, "block size moved away from the size its own arithmetic derived")
    }

    /** More load may only ADD errors; the axis must never shrink the cell it exists to grow. */
    @Test
    fun raising_the_load_never_shrinks_the_post_error_cell() {
        val low = (1L..40L).sumOf { pairsInBlock(0.0, trialsPerBlock, it).first }
        val high = (1L..40L).sumOf { pairsInBlock(1.0, trialsPerBlock, it).first }
        assertTrue(high >= low, "raising POST_ERROR_LOAD cut post-error pairs from $low to $high across 40 blocks")
    }
}
