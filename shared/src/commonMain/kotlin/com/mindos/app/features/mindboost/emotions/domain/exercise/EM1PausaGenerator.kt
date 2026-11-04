package com.mindos.app.features.mindboost.emotions.domain.exercise

import com.mindos.app.features.mindboost.core.runtime.conditionSchedule
import com.mindos.app.features.mindboost.core.staircase.StopSignalTracker
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionAxis
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionDifficultyVector
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionResponseKey
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulus
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import kotlin.random.Random

/** A generator whose difficulty is resolved trial by trial from the user's own responses. */
interface EmotionStopSignalGenerator {
    /** A fresh staircase configured for [difficulty]; the runtime drives it across the whole session. */
    fun tracker(difficulty: EmotionDifficultyVector): StopSignalTracker
}

/**
 * Ola 3 · R1 — INHIBICIÓN AFECTIVA, the fusion of EM1 (pausa), EM8 (anti-impulso) and EM12 (espera).
 *
 * The three were one paradigm wearing three names: EM8 differed from EM1 in three constants and EM12 in one
 * more, so they occupied half the menu to train the same thing and inflated the module's apparent coverage.
 * They are now ONE exercise, and each contributes the part of itself that was real:
 *  - EM8's higher withhold demand becomes the top of the `INHIBITORY_DEMAND` axis (stop share 0.25 -> 0.33).
 *  - EM12's waiting becomes a variable foreperiod, so the go response cannot be timed to a metronome.
 *
 * What actually changes the measurement is neither of those: it is that the trial is no longer a NO-GO decided
 * at onset but a STOP SIGNAL. The frame opens, the response is already under way, and only then does the frame
 * close. That difference is what makes SSRT — how long the brake itself takes — estimable at all, and SSRT is
 * the reading a "pause before acting" exercise owes the user. A no-go decided at onset can only ever report how
 * often they pressed when they should not have, which confounds sensitivity with criterion.
 *
 * The stop-signal delay is ONLINE: a 1-up/1-down staircase ([tracker]) that lengthens the delay after a
 * successful stop and shortens it after a failed one, converging on p(respond|signal) ~ 0.5. This is also why
 * the exercise is immune to the two defects Ola 0 had to repair in the block-level engine: the staircase adapts
 * every single trial, so it cannot be starved by a session that is too short to reach a hysteresis threshold,
 * and it cannot deadlock on a metric that needs a difficulty it can only reach if that metric is already
 * trustworthy. The generator therefore leaves [EmotionStimulusPayload.AffectiveGoNoGo.stopAfterMs] null and the
 * runtime stamps the delay it presented.
 *
 * Both splits are QUOTAS, never coin flips ([conditionSchedule]): the stop trials are the sample of the SSRT
 * estimate and their tone split is the sample of the affective contrast, so leaving either cell size to chance
 * would leave the eligibility of those readings to the seed.
 */
class EM1PausaGenerator : EmotionTrialGenerator, EmotionStopSignalGenerator {

    override val exerciseId: EmotionExerciseId = EmotionExerciseId.EM1_PAUSA

    override fun tracker(difficulty: EmotionDifficultyVector): StopSignalTracker =
        StopSignalTracker(initialSsdMs = INITIAL_SSD_MS, stepMs = SSD_STEP_MS)

    override fun generate(
        difficulty: EmotionDifficultyVector,
        spec: EmotionBlockSpec,
        seed: Long,
    ): List<EmotionStimulus> {
        val rng = Random(seed)
        val n = spec.targetTrialCount
        val stopShare = STOP_SHARE_MIN + STOP_SHARE_SPAN * difficulty.level(EmotionAxis.INHIBITORY_DEMAND)
        val stops = conditionSchedule(n, stopShare, rng, atLeast = 1)

        // The tone cell is drawn over the STOP trials only, because the affective contrast lives on them; the go
        // trials keep their own balanced tone split so the legacy interference reading still has both cells.
        val stopCount = stops.count { it }
        val stopTones = conditionSchedule(stopCount, NEGATIVE_TONE_SHARE, rng).iterator()
        val goTones = conditionSchedule(n - stopCount, NEGATIVE_TONE_SHARE, rng).iterator()

        val positives = EmotionContent.PAUSA_WORDS.filter { !it.negative }
        val negatives = EmotionContent.PAUSA_WORDS.filter { it.negative }

        return stops.mapIndexed { index, isStop ->
            val negative = if (isStop) stopTones.next() else goTones.next()
            val word = (if (negative) negatives else positives).random(rng)
            EmotionStimulus(
                index = index,
                // Every trial starts as a go, so the prepotent response is real; a stop trial is aborted, not
                // withheld from the start, and the classifier reads that from `stopAfterMs`.
                correctResponseKey = if (isStop) EmotionResponseKey.NONE else EmotionResponseKey.PRIMARY,
                payload = EmotionStimulusPayload.AffectiveGoNoGo(
                    frameOpen = true,
                    affectiveWord = word.text,
                    // A stop trial's frame will close, so a negative tone "agrees" with what is about to be
                    // asked; on a go trial agreement is the other way round.
                    congruent = if (isStop) negative else !negative,
                    stopAfterMs = null, // stamped by the runtime from the staircase
                    negativeWord = negative,
                ),
                interStimulusDelayMs = foreperiodMs(rng),
                responseWindowMs = spec.responseWindowMs,
            )
        }
    }

    /** EM12's contribution: a variable wait, so the go response cannot be paced to a fixed rhythm. */
    private fun foreperiodMs(rng: Random): Long =
        FOREPERIOD_MIN_MS + rng.nextLong(FOREPERIOD_SPAN_MS + 1)

    companion object {
        /** Share of trials that carry a stop signal, at the bottom and top of `INHIBITORY_DEMAND`. */
        const val STOP_SHARE_MIN = 0.25
        const val STOP_SHARE_SPAN = 0.08

        /** Half the stop trials carry a negative word: an even split is the largest SMALLER cell a fixed n buys. */
        const val NEGATIVE_TONE_SHARE = 0.5

        private const val INITIAL_SSD_MS = 250L
        private const val SSD_STEP_MS = 50L
        private const val FOREPERIOD_MIN_MS = 450L
        private const val FOREPERIOD_SPAN_MS = 450L
    }
}
