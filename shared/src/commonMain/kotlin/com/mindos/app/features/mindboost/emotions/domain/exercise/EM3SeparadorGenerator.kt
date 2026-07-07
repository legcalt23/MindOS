package com.mindos.app.features.mindboost.emotions.domain.exercise

import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionAxis
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionDifficultyVector
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulus
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import kotlin.random.Random

/**
 * EM3 Separador — draws training items (never the held-out eval pool) from the fixed bank. The four-category answer
 * is captured by the renderer/holder and scored by the assembler's confusion matrix; the stimulus carries no
 * `correctResponseKey` (the truth lives in the payload). Eval items stay reserved for the Ola-4 holdout probe.
 *
 * MC8u-B §2 — REAL difficulty on the CATEGORY_AMBIGUITY axis, applied to the actual trials (not a decorative number):
 * higher difficulty (1) draws more deliberately-confusable items ([SeparatorDifficulty.select]), (2) tightens the
 * response window, and (3) shortens the inter-stimulus reset gap. Difficulty NEVER adds clinical/high-intensity
 * content — it only makes the clean discriminations harder. Deterministic for a fixed seed.
 */
class EM3SeparadorGenerator : EmotionTrialGenerator {

    override val exerciseId: EmotionExerciseId = EmotionExerciseId.EM3_SEPARADOR

    override fun generate(
        difficulty: EmotionDifficultyVector,
        spec: EmotionBlockSpec,
        seed: Long,
    ): List<EmotionStimulus> {
        val rng = Random(seed)
        val level = difficulty.level(EmotionAxis.CATEGORY_AMBIGUITY)
        val training = EmotionContent.SEPARATOR_ITEMS.filter { !it.isEval }
        val picked = SeparatorDifficulty.select(training, spec.targetTrialCount, level, rng, spec.recentPhrases)
        val window = SeparatorDifficulty.window(spec.responseWindowMs, level, BASE_PRESSURE, WINDOW_SPAN, MIN_WINDOW_MS)
        val isi = SeparatorDifficulty.interStimulus(INTER_STIMULUS_MS, level, ISI_SPAN, MIN_ISI_MS)
        return picked.mapIndexed { index, item ->
            EmotionStimulus(
                index = index,
                correctResponseKey = null,
                payload = EmotionStimulusPayload.Classification(
                    phrase = item.phrase,
                    truth = item.category,
                    isEval = false,
                ),
                interStimulusDelayMs = isi,
                responseWindowMs = window,
            )
        }
    }

    companion object {
        private const val INTER_STIMULUS_MS = 700L
        private const val BASE_PRESSURE = 0.0 // EM3 has no baseline time pressure; only ambiguity/window scale it up
        private const val WINDOW_SPAN = 0.30
        private const val MIN_WINDOW_MS = 800L
        private const val ISI_SPAN = 0.40
        private const val MIN_ISI_MS = 300L
    }
}
