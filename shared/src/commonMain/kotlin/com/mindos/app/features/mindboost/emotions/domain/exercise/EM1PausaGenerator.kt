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
 * EM1 Pausa — affective go/no-go. The GO/NO-GO answer is the visual frame ([frameOpen]); the affective
 * word is only a distractor (tone never decides — no dishonest render). Deterministic (seeded). The
 * block guarantees ≥1 no-go and a minimum of BOTH congruent and incongruent GO cells so the
 * affective-interference-cost secondary metric can ever reach confidence. Difficulty raises the no-go
 * share via the inhibitory-demand axis.
 */
class EM1PausaGenerator : EmotionTrialGenerator {

    override val exerciseId: EmotionExerciseId = EmotionExerciseId.EM1_PAUSA

    override fun generate(
        difficulty: EmotionDifficultyVector,
        spec: EmotionBlockSpec,
        seed: Long,
    ): List<EmotionStimulus> {
        val rng = Random(seed)
        val n = spec.targetTrialCount
        val ratio = NO_GO_RATIO + INHIB_SPAN * difficulty.level(EmotionAxis.INHIBITORY_DEMAND)
        val noGo = (n * ratio).roundToInt().coerceIn(1, (n - 2 * MIN_CELL).coerceAtLeast(1))
        val go = n - noGo
        val incongruentGo = (go / 2).coerceIn(if (go >= 2 * MIN_CELL) MIN_CELL else 0, go)
        val congruentGo = go - incongruentGo

        val positives = EmotionContent.PAUSA_WORDS.filter { !it.negative }
        val negatives = EmotionContent.PAUSA_WORDS.filter { it.negative }

        val slots = ArrayList<EmotionStimulusPayload.AffectiveGoNoGo>(n)
        repeat(congruentGo) { slots.add(goSlot(rng, positives, negative = false)) }
        repeat(incongruentGo) { slots.add(goSlot(rng, negatives, negative = true)) }
        repeat(noGo) { slots.add(noGoSlot(rng, positives, negatives)) }
        slots.shuffle(rng)

        return slots.mapIndexed { index, payload ->
            EmotionStimulus(
                index = index,
                correctResponseKey = if (payload.frameOpen) EmotionResponseKey.PRIMARY else EmotionResponseKey.NONE,
                payload = payload,
                interStimulusDelayMs = INTER_STIMULUS_MS,
                responseWindowMs = spec.responseWindowMs,
            )
        }
    }

    private fun goSlot(rng: Random, words: List<EmotionContent.AffectiveWord>, negative: Boolean) =
        EmotionStimulusPayload.AffectiveGoNoGo(
            frameOpen = true,
            affectiveWord = words.random(rng).text,
            congruent = !negative,
        )

    private fun noGoSlot(
        rng: Random,
        positives: List<EmotionContent.AffectiveWord>,
        negatives: List<EmotionContent.AffectiveWord>,
    ): EmotionStimulusPayload.AffectiveGoNoGo {
        val negative = rng.nextBoolean()
        val word = (if (negative) negatives else positives).random(rng)
        // closed frame + negative tone is "congruent" (both push toward withholding).
        return EmotionStimulusPayload.AffectiveGoNoGo(
            frameOpen = false,
            affectiveWord = word.text,
            congruent = negative,
        )
    }

    companion object {
        private const val NO_GO_RATIO = 0.28
        private const val INHIB_SPAN = 0.12
        private const val MIN_CELL = 2
        private const val INTER_STIMULUS_MS = 600L
    }
}
