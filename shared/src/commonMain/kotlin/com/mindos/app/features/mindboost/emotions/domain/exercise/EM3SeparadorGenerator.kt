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
 * higher difficulty draws more deliberately-confusable items ([SeparatorDifficulty.select]) and shortens the
 * inter-stimulus reset gap. Difficulty NEVER adds clinical/high-intensity content — it only makes the clean
 * discriminations harder. Deterministic for a fixed seed.
 *
 * Ola 3 · R3 · P2 — the axis no longer tightens the RESPONSE window: the block's calm window is served unchanged.
 * R3 makes time pressure a mode rather than a knob, and the old ramp also let phrase length act as a hidden second
 * difficulty dimension (see [SeparatorDifficulty]).
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
        val training = SeparatorItemFactory.ALL.filter { !it.isEval }
        val picked = SeparatorDifficulty.select(training, spec.targetTrialCount, level, rng, spec.recentPhrases)
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
                responseWindowMs = spec.responseWindowMs,
            )
        }
    }

    companion object {
        private const val INTER_STIMULUS_MS = 700L
        private const val ISI_SPAN = 0.40
        private const val MIN_ISI_MS = 300L
    }
}
