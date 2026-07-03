package com.mindos.app.features.mindboost.emotions.domain.exercise

import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionAxis
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionDifficultyVector
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionResponseKey
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulus
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import kotlin.random.Random

/**
 * EM2 Recuperación — a neutral 2-alternative perceptual host. Difficulty lowers discriminability until
 * NORMAL (never unfair) errors appear. The generator deterministically PLANTS at least
 * [MIN_POST_ERROR] near-threshold "error-likely" trials so post-error recovery is measurable without
 * unfair induction; the post-error window itself is detected at runtime by the session assembler.
 */
class EM2RecuperacionGenerator : EmotionTrialGenerator {

    override val exerciseId: EmotionExerciseId = EmotionExerciseId.EM2_RECUPERACION

    override fun generate(
        difficulty: EmotionDifficultyVector,
        spec: EmotionBlockSpec,
        seed: Long,
    ): List<EmotionStimulus> {
        val rng = Random(seed)
        val n = spec.targetTrialCount
        val load = difficulty.level(EmotionAxis.POST_ERROR_LOAD)
        val baseDiscriminability = (EASY_DISC - DISC_SPAN * load).coerceIn(HARD_DISC, EASY_DISC)
        val hardCount = maxOf(MIN_POST_ERROR, n / HARD_EVERY)
        val hardIndices = plantIndices(n, hardCount)

        return (0 until n).map { index ->
            val harder = index in hardIndices
            val correct = if (rng.nextBoolean()) EmotionResponseKey.LEFT else EmotionResponseKey.RIGHT
            EmotionStimulus(
                index = index,
                correctResponseKey = correct,
                payload = EmotionStimulusPayload.TwoAltPerceptual(
                    correct = correct,
                    discriminability = if (harder) HARD_DISC else baseDiscriminability,
                    harder = harder,
                ),
                interStimulusDelayMs = INTER_STIMULUS_MS,
                responseWindowMs = spec.responseWindowMs,
            )
        }
    }

    /** Evenly spread [count] planted hard trials across the block (deterministic, no RNG). */
    private fun plantIndices(n: Int, count: Int): Set<Int> {
        if (count <= 0 || n <= 0) return emptySet()
        val step = (n / count).coerceAtLeast(1)
        return (0 until n step step).take(count).toSet()
    }

    companion object {
        private const val EASY_DISC = 0.90
        private const val HARD_DISC = 0.45
        private const val DISC_SPAN = 0.35
        private const val MIN_POST_ERROR = 3
        private const val HARD_EVERY = 5
        private const val INTER_STIMULUS_MS = 500L
    }
}
