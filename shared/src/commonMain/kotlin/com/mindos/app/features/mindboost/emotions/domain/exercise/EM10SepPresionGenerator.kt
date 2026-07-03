package com.mindos.app.features.mindboost.emotions.domain.exercise

import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionDifficultyVector
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulus
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import kotlin.random.Random

/**
 * EM10 Separación bajo presión leve — the EM3 four-category classification with a SHORTER response
 * window baked into each stimulus (the "presión" is real time pressure, carried by the stimulus so the
 * runtime honors it). Draws training items only (eval pool stays held out). Reuses the EM3 renderer.
 */
class EM10SepPresionGenerator : EmotionTrialGenerator {

    override val exerciseId: EmotionExerciseId = EmotionExerciseId.EM10_SEP_PRESION

    @Suppress("UnusedParameter") // Difficulty (temporal pressure) is applied via the shortened window
    // below and by the adaptive engine; item selection itself is from the fixed, pre-balanced bank.
    override fun generate(
        difficulty: EmotionDifficultyVector,
        spec: EmotionBlockSpec,
        seed: Long,
    ): List<EmotionStimulus> {
        val rng = Random(seed)
        val window = (spec.responseWindowMs * PRESSURE_FACTOR).toLong().coerceAtLeast(MIN_WINDOW_MS)
        val training = EmotionContent.SEPARATOR_ITEMS.filter { !it.isEval }
        val picked = training.shuffled(rng).take(spec.targetTrialCount.coerceAtMost(training.size))
        return picked.mapIndexed { index, item ->
            EmotionStimulus(
                index = index,
                correctResponseKey = null,
                payload = EmotionStimulusPayload.Classification(item.phrase, item.category, isEval = false),
                interStimulusDelayMs = INTER_STIMULUS_MS,
                responseWindowMs = window,
            )
        }
    }

    companion object {
        private const val PRESSURE_FACTOR = 0.65
        private const val MIN_WINDOW_MS = 900L
        private const val INTER_STIMULUS_MS = 500L
    }
}
