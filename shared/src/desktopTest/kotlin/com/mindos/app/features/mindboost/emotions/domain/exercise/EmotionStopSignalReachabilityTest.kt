@file:Suppress("MagicNumber") // The session shape and the cell floor ARE the contract under test.

package com.mindos.app.features.mindboost.emotions.domain.exercise

import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionAxis
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionDifficultyVector
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseCatalog
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.metrics.DefaultEmotionMetricsEngine
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionResponseKey
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import com.mindos.app.features.mindboost.emotions.presentation.emotionBlocksFor
import com.mindos.app.features.mindboost.emotions.presentation.emotionTrialsPerBlock
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Ola 3 · R1 — is the affective SSRT delta REACHABLE, computed on the real generator at the real session shape?
 *
 * The failure mode this closes has a long history in this repo: a gate whose threshold no realistic session can
 * cross reads as honesty ("not enough data") while actually being a design defect, and worse, whether it can be
 * crossed drifts with difficulty, so the adaptive engine silently decides whether a capacity claim is eligible.
 * The reading has to be reachable at the BOTTOM of the axis, not only at the top, or every user the engine has
 * not yet pushed to maximum demand is quietly excluded from it.
 */
class EmotionStopSignalReachabilityTest {

    private val blocks = emotionBlocksFor(EmotionExerciseId.EM1_PAUSA)
    private val trialsPerBlock = emotionTrialsPerBlock(
        EmotionExerciseId.EM1_PAUSA,
        EmotionExerciseCatalog.definition(EmotionExerciseId.EM1_PAUSA).governingMetric,
    )

    /** Stop trials per tone cell across a whole session at [level] of the stop-share axis, worst tone cell. */
    private fun smallestToneCell(level: Double, seed: Long): Int {
        val difficulty = EmotionDifficultyVector.initial(EmotionAxis.entries.toSet(), start = level)
        val spec = EmotionBlockSpec(targetTrialCount = trialsPerBlock, responseWindowMs = 1_200)
        val stops = (0 until blocks).flatMap { block ->
            EM1PausaGenerator().generate(difficulty, spec, seed + block)
                .filter { it.correctResponseKey == EmotionResponseKey.NONE }
                .map { (it.payload as EmotionStimulusPayload.AffectiveGoNoGo).negativeWord }
        }
        return minOf(stops.count { it }, stops.count { !it })
    }

    @Test
    fun the_affective_delta_is_reachable_at_every_level_of_the_stop_share_axis() {
        val floor = DefaultEmotionMetricsEngine.MIN_SSRT_CELL
        listOf(0.0, 0.25, 0.5, 0.75, 1.0).forEach { level ->
            val cell = smallestToneCell(level, seed = 11L)
            assertTrue(
                cell >= floor,
                "at stop-share level $level a full session yields only $cell stop trials in its smaller tone " +
                    "cell, below the floor of $floor — the affective reading would exist only for users the " +
                    "engine had already driven to a higher demand",
            )
        }
    }

    /** The quota, not a coin flip: the cell size must not move with the seed. */
    @Test
    fun the_cell_size_does_not_depend_on_the_seed() {
        val sizes = (1L..40L).map { smallestToneCell(level = 0.0, seed = it) }.toSet()
        assertTrue(
            sizes.size == 1,
            "the smaller tone cell varies across seeds ($sizes) — a Bernoulli split would leave the " +
                "eligibility of the affective reading to chance",
        )
    }

    /** More demand may only ADD stop trials; the axis must never shrink the sample it is meant to grow. */
    @Test
    fun raising_the_demand_never_shrinks_the_measurement() {
        val low = smallestToneCell(level = 0.0, seed = 5L)
        val high = smallestToneCell(level = 1.0, seed = 5L)
        assertTrue(high >= low, "raising the stop share cut the tone cell from $low to $high")
    }
}
