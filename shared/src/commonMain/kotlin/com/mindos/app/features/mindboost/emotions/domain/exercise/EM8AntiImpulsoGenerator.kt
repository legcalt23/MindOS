package com.mindos.app.features.mindboost.emotions.domain.exercise

import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionAxis
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionDifficultyVector
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionResponseKey
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulus
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * EM8 Anti-impulso — a go/no-go variant focused purely on inhibition: a HIGHER no-go share than EM1
 * (more "withhold" demand), no affective-interference balancing (its metric is commission, not
 * congruency). Reuses the EM1 renderer/classifier/metrics. Deterministic (seeded); guarantees ≥1 no-go.
 */
class EM8AntiImpulsoGenerator : EmotionTrialGenerator {

    override val exerciseId: EmotionExerciseId = EmotionExerciseId.EM8_ANTIIMPULSO

    override fun generate(
        difficulty: EmotionDifficultyVector,
        spec: EmotionBlockSpec,
        seed: Long,
    ): List<EmotionStimulus> {
        val rng = Random(seed)
        val n = spec.targetTrialCount
        val ratio = NO_GO_RATIO + INHIB_SPAN * difficulty.level(EmotionAxis.INHIBITORY_DEMAND)
        val noGo = (n * ratio).roundToInt().coerceIn(1, n - 1)
        val flags = (List(noGo) { false } + List(n - noGo) { true }).toMutableList()
        flags.shuffle(rng)
        return flags.mapIndexed { index, frameOpen ->
            val word = EmotionContent.PAUSA_WORDS.random(rng)
            EmotionStimulus(
                index = index,
                correctResponseKey = if (frameOpen) EmotionResponseKey.PRIMARY else EmotionResponseKey.NONE,
                payload = EmotionStimulusPayload.AffectiveGoNoGo(
                    frameOpen = frameOpen,
                    affectiveWord = word.text,
                    congruent = frameOpen != word.negative,
                ),
                interStimulusDelayMs = INTER_STIMULUS_MS,
                responseWindowMs = spec.responseWindowMs,
            )
        }
    }

    companion object {
        private const val NO_GO_RATIO = 0.40
        private const val INHIB_SPAN = 0.15
        private const val INTER_STIMULUS_MS = 550L
    }
}
