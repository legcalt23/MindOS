package com.mindos.app.features.mindboost.emotions.domain.exercise

import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionDifficultyVector
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulus
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import kotlin.random.Random

/**
 * EM3 Separador — draws training items (never the held-out eval pool) from the fixed bank and shuffles
 * them deterministically. The four-category answer is captured by the renderer/holder and scored by
 * the assembler's confusion matrix; the stimulus carries no `correctResponseKey` (the truth lives in
 * the payload). Eval items stay reserved for the Ola-4 holdout probe — train/eval pools are disjoint.
 */
class EM3SeparadorGenerator : EmotionTrialGenerator {

    override val exerciseId: EmotionExerciseId = EmotionExerciseId.EM3_SEPARADOR

    @Suppress("UnusedParameter") // EM3 difficulty (category ambiguity) is applied at presentation
    // (the holder's response window), not at item generation; the bank is fixed and pre-balanced.
    override fun generate(
        difficulty: EmotionDifficultyVector,
        spec: EmotionBlockSpec,
        seed: Long,
    ): List<EmotionStimulus> {
        val rng = Random(seed)
        val training = EmotionContent.SEPARATOR_ITEMS.filter { !it.isEval }
        val picked = training.shuffled(rng).take(spec.targetTrialCount.coerceAtMost(training.size))
        return picked.mapIndexed { index, item ->
            EmotionStimulus(
                index = index,
                correctResponseKey = null,
                payload = EmotionStimulusPayload.Classification(
                    phrase = item.phrase,
                    truth = item.category,
                    isEval = false,
                ),
                interStimulusDelayMs = INTER_STIMULUS_MS,
                responseWindowMs = spec.responseWindowMs,
            )
        }
    }

    companion object {
        private const val INTER_STIMULUS_MS = 700L
    }
}
