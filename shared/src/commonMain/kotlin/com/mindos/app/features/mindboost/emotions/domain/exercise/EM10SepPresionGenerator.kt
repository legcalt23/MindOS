package com.mindos.app.features.mindboost.emotions.domain.exercise

import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionAxis
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionDifficultyVector
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulus
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import kotlin.random.Random

/**
 * EM10 Separación bajo presión leve — the EM3 four-category classification under real time pressure. Draws training
 * items only (eval pool stays held out) and reuses the EM3 renderer.
 *
 * MC8u-B §2 — REAL difficulty on the TEMPORAL_PRESSURE axis, applied to the actual trials: (1) a constant baseline
 * pressure PLUS a difficulty-scaled tightening of the response window (always shorter than the base — that is the
 * "presión"), (2) a shortened inter-stimulus gap, and (3) more confusable items as pressure rises (contextual
 * complexity, [SeparatorDifficulty.select]). Difficulty never adds clinical/high-intensity content. Deterministic.
 */
class EM10SepPresionGenerator : EmotionTrialGenerator {

    override val exerciseId: EmotionExerciseId = EmotionExerciseId.EM10_SEP_PRESION

    override fun generate(
        difficulty: EmotionDifficultyVector,
        spec: EmotionBlockSpec,
        seed: Long,
    ): List<EmotionStimulus> {
        val rng = Random(seed)
        val level = difficulty.level(EmotionAxis.TEMPORAL_PRESSURE)
        val training = EmotionContent.SEPARATOR_ITEMS.filter { !it.isEval }
        val picked = SeparatorDifficulty.select(training, spec.targetTrialCount, level, rng, spec.recentPhrases)
        val window = SeparatorDifficulty.window(spec.responseWindowMs, level, BASE_PRESSURE, WINDOW_SPAN, MIN_WINDOW_MS)
        val isi = SeparatorDifficulty.interStimulus(INTER_STIMULUS_MS, level, ISI_SPAN, MIN_ISI_MS)
        return picked.mapIndexed { index, item ->
            EmotionStimulus(
                index = index,
                correctResponseKey = null,
                payload = EmotionStimulusPayload.Classification(item.phrase, item.category, isEval = false),
                interStimulusDelayMs = isi,
                responseWindowMs = window,
            )
        }
    }

    companion object {
        private const val INTER_STIMULUS_MS = 500L
        private const val BASE_PRESSURE = 0.25 // EM10 is always under pressure: even at level 0 the window is shorter
        private const val WINDOW_SPAN = 0.35
        private const val MIN_WINDOW_MS = 900L
        private const val ISI_SPAN = 0.40
        private const val MIN_ISI_MS = 250L
    }
}
