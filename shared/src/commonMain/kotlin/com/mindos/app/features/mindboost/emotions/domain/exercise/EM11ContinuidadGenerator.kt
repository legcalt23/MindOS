package com.mindos.app.features.mindboost.emotions.domain.exercise

import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionAxis
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionDifficultyVector
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionResponseKey
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulus
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import kotlin.random.Random

/**
 * EM11 Continuidad tras feedback negativo neutro — the EM2 2-alternative host with MORE planted
 * near-threshold trials (denser, but still normal, errors) so there is more post-error continuation to
 * practice and measure. Reuses the EM2 renderer/metrics (POST_ERROR_RECOVERY). Deterministic (seeded).
 */
class EM11ContinuidadGenerator : EmotionTrialGenerator {

    override val exerciseId: EmotionExerciseId = EmotionExerciseId.EM11_FEEDBACK_NEG

    override fun generate(
        difficulty: EmotionDifficultyVector,
        spec: EmotionBlockSpec,
        seed: Long,
    ): List<EmotionStimulus> {
        val rng = Random(seed)
        val n = spec.targetTrialCount
        val load = difficulty.level(EmotionAxis.POST_ERROR_LOAD)
        val baseDiscriminability = (EASY_DISC - DISC_SPAN * load).coerceIn(HARD_DISC, EASY_DISC)
        val hardCount = maxOf(MIN_HARD, n / HARD_EVERY)
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

    private fun plantIndices(n: Int, count: Int): Set<Int> {
        if (count <= 0 || n <= 0) return emptySet()
        val step = (n / count).coerceAtLeast(1)
        return (0 until n step step).take(count).toSet()
    }

    companion object {
        private const val EASY_DISC = 0.85
        private const val HARD_DISC = 0.42
        private const val DISC_SPAN = 0.35
        private const val MIN_HARD = 4
        private const val HARD_EVERY = 3
        private const val INTER_STIMULUS_MS = 500L
    }
}
