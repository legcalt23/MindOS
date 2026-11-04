package com.mindos.app.features.mindboost.emotions.domain.exercise

import com.mindos.app.features.mindboost.core.staircase.AccuracyStaircase
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionAxis
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionDifficultyVector
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionResponseKey
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulus
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import kotlin.random.Random

/** A generator whose difficulty is held at a target ERROR RATE, trial by trial, by the user's own responses. */
interface EmotionAccuracyStaircaseGenerator {
    /** A fresh staircase configured for [difficulty]; the runtime drives it across the whole session. */
    fun staircase(difficulty: EmotionDifficultyVector): AccuracyStaircase
}

/**
 * Ola 3 · R2 — RECUPERACIÓN TRAS ERROR, the fusion of EM2 (recuperación), EM11 (continuidad tras feedback
 * negativo) and EM15 (recuperación encadenada).
 *
 * The three ran the SAME two-alternative perceptual host and differed only in how many near-threshold trials
 * they planted: EM2 one in five, EM11 one in three, EM15 in runs. Planting is the wrong instrument for all
 * three. A planted trial is only error-LIKELY, so how many errors a session produced — and therefore whether the
 * exercise could measure anything at all — was decided by how good the user already was. The strongest users got
 * no errors and no reading, which the module then reported as `unavailable`; that looks like honesty and is a
 * design failure, because the exercise had simply declined to ask them the question.
 *
 * The staircase replaces planting: difficulty follows the user until the error rate SITS at its target, so every
 * user — including the strongest — reaches the post-error pairs the reading needs. The error rate is the thing
 * being held constant, which is also why this is the module's strongest frustration induction and carries an
 * explicit ceiling in the runtime (a run of consecutive errors lowers the target, the streak cost is always
 * recoverable, and the exit stays one tap away).
 *
 * `POST_ERROR_LOAD` moves the target error rate across a narrow band ([ERROR_RATE_MIN] .. +[ERROR_RATE_SPAN]),
 * which is how the audit's "longer error chains" is delivered by a staircase: chains are emergent, `p^k`, so a
 * higher target makes them longer without the exercise ever engineering one. The audit's literal reading — hold
 * the level after an error so a chain of a fixed length forms — was REJECTED and the rejection is the point:
 * making difficulty depend on the immediately preceding outcome is precisely the confound
 * [AccuracyStaircase]'s one-trial lag exists to remove, and it would corrupt this exercise's primary metric.
 *
 * The generator therefore emits no difficulty of its own worth trusting: [EmotionStimulusPayload.TwoAltPerceptual.discriminability]
 * carries the block's STARTING level and the runtime overwrites it per trial with the live staircase level, the
 * same contract R1 uses for its stop-signal delay. What it does own is the correct side, drawn from the seeded
 * RNG so the answer can never be predicted from the trial index.
 */
class EM2RecuperacionGenerator : EmotionTrialGenerator, EmotionAccuracyStaircaseGenerator {

    override val exerciseId: EmotionExerciseId = EmotionExerciseId.EM2_RECUPERACION

    override fun staircase(difficulty: EmotionDifficultyVector): AccuracyStaircase {
        val errorRate = ERROR_RATE_MIN + ERROR_RATE_SPAN * difficulty.level(EmotionAxis.POST_ERROR_LOAD)
        return AccuracyStaircase(
            targetAccuracy = 1.0 - errorRate,
            stepDown = STEP_DOWN,
            initialLevel = INITIAL_DISCRIMINABILITY,
            minLevel = MIN_DISCRIMINABILITY,
            maxLevel = MAX_DISCRIMINABILITY,
        )
    }

    override fun generate(
        difficulty: EmotionDifficultyVector,
        spec: EmotionBlockSpec,
        seed: Long,
    ): List<EmotionStimulus> {
        val rng = Random(seed)
        return (0 until spec.targetTrialCount).map { index ->
            val correct = if (rng.nextBoolean()) EmotionResponseKey.LEFT else EmotionResponseKey.RIGHT
            EmotionStimulus(
                index = index,
                correctResponseKey = correct,
                payload = EmotionStimulusPayload.TwoAltPerceptual(
                    correct = correct,
                    // Placeholder: the runtime stamps the live staircase level before the trial is shown. It is
                    // the block's starting level rather than a "safe" easy value, so a path that somehow skipped
                    // the stamp would present a plausible difficulty instead of a trivial one that quietly
                    // manufactures a perfect block.
                    discriminability = INITIAL_DISCRIMINABILITY,
                ),
                interStimulusDelayMs = INTER_STIMULUS_MS,
                responseWindowMs = spec.responseWindowMs,
            )
        }
    }

    companion object {
        /** Target error rate at the bottom and across the span of `POST_ERROR_LOAD`; the audit's 0.30 is mid-band. */
        const val ERROR_RATE_MIN = 0.25
        const val ERROR_RATE_SPAN = 0.10

        /**
         * The error rate the runtime falls back to when the induction ceiling fires. The audit's own overload
         * response for this paradigm: a smaller induction, never a longer session and never a fabricated success.
         */
        const val RELIEF_ERROR_RATE = 0.20

        /** Consecutive errors after which the induction ceiling fires. Fixed by the audit, not by the axis. */
        const val MAX_CONSECUTIVE_ERRORS = 3

        /** Neutral post-error marker. Brief on purpose: it marks the cost, it does not dwell on it. */
        const val ERROR_MARKER_MS = 250L

        private const val STEP_DOWN = 0.03
        private const val INITIAL_DISCRIMINABILITY = 0.55
        private const val MIN_DISCRIMINABILITY = 0.12
        private const val MAX_DISCRIMINABILITY = 0.90
        private const val INTER_STIMULUS_MS = 500L
    }
}
