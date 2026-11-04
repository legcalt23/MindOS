package com.mindos.app.features.mindboost.emotions.domain.calibration

import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionResponseKey
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload

/**
 * Ola 1 · N1 — which host trials may carry a confidence judgment, and what "guessing" is worth on each.
 *
 * The calibration layer is transversal, but "transversal" is not the same as "everywhere". A confidence judgment
 * is only meaningful when there is a discrete answer the user gave and can be right or wrong about, and when the
 * ~600 ms the strip costs does not distort the construct the host task is measuring. Two exclusions follow, and
 * both are deliberate rather than incidental:
 *
 * **Go/no-go is excluded as a paradigm.** On a no-go trial the CORRECT response is to withhold, so the successful
 * answer is the absence of an action — there is no choice to rate. Asking "how sure were you?" after a
 * non-response either measures nothing or, worse, teaches the user to treat withholding as an answer, which is the
 * opposite of what an inhibition task trains. The host construct is also the speed of the go/no-go decision
 * itself, and interposing a metacognitive judgment after every trial is exactly the overload the design warns
 * about — the more so since these blocks are now 20 trials long.
 *
 * **A trial the user never answered is excluded, in every paradigm.** When the response window elapses in silence
 * there is no judgment to attach a confidence to. Scoring the omission as an incorrect answer the user was
 * "unsure" about would invent a metacognitive act that never happened.
 */
object EmotionCalibrationHost {

    /** Chance level of a four-way category classification. */
    private const val FOUR_WAY_CHANCE = 0.25

    /** Chance level of a two-alternative forced choice. */
    private const val TWO_ALTERNATIVE_CHANCE = 0.5

    /**
     * The probability of being right by guessing on this trial, or null when the trial may not carry a
     * confidence judgment at all.
     */
    fun chanceLevelOf(payload: EmotionStimulusPayload): Double? = when (payload) {
        is EmotionStimulusPayload.Classification -> FOUR_WAY_CHANCE
        is EmotionStimulusPayload.TwoAltPerceptual -> TWO_ALTERNATIVE_CHANCE
        is EmotionStimulusPayload.AffectiveGoNoGo -> null
    }

    /**
     * True when this trial should show the confidence strip: the paradigm supports it AND the user actually
     * answered.
     */
    fun acceptsJudgment(payload: EmotionStimulusPayload, responseKey: EmotionResponseKey): Boolean =
        chanceLevelOf(payload) != null && responseKey != EmotionResponseKey.NONE

    /**
     * Whether the answer given was the correct one, or null when this trial cannot be judged.
     *
     * Correctness is re-derived here from the stimulus rather than taken from the session's trial classifier on
     * purpose: the classifier answers "was this outcome a hit, a false alarm, an omission?", which is a richer
     * question with paradigm-specific categories, while calibration needs the single flat bit the user was
     * implicitly forecasting when they pressed the confidence button.
     */
    fun wasCorrect(
        payload: EmotionStimulusPayload,
        responseKey: EmotionResponseKey,
        category: EmotionCategory?,
    ): Boolean? = when (payload) {
        is EmotionStimulusPayload.Classification -> category?.let { it == payload.truth }
        is EmotionStimulusPayload.TwoAltPerceptual ->
            if (responseKey == EmotionResponseKey.NONE) null else responseKey == payload.correct
        is EmotionStimulusPayload.AffectiveGoNoGo -> null
    }
}
