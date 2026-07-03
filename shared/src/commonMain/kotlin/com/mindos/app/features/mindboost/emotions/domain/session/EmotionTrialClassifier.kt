package com.mindos.app.features.mindboost.emotions.domain.session

import com.mindos.app.features.mindboost.emotions.domain.model.EmotionResponseKey
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionTrialOutcome
import com.mindos.app.features.mindboost.emotions.domain.model.RecordedEmotionTrial

/**
 * Classifies a recorded trial by paradigm. This is pure and deterministic; the session assembler is the
 * SOLE authority that runs it over the recorded trials (live UI counters are display-only).
 */
object EmotionTrialClassifier {

    fun classify(trial: RecordedEmotionTrial): EmotionTrialOutcome = when (val payload = trial.stimulus.payload) {
        is EmotionStimulusPayload.AffectiveGoNoGo -> classifyGoNoGo(payload, trial)
        is EmotionStimulusPayload.TwoAltPerceptual -> classifyChoice(payload, trial)
        is EmotionStimulusPayload.Classification -> classifyCategory(payload, trial)
    }

    private fun classifyGoNoGo(
        payload: EmotionStimulusPayload.AffectiveGoNoGo,
        trial: RecordedEmotionTrial,
    ): EmotionTrialOutcome {
        val responded = trial.responseKey == EmotionResponseKey.PRIMARY
        return when {
            payload.frameOpen && responded -> EmotionTrialOutcome.HIT
            payload.frameOpen -> EmotionTrialOutcome.MISS
            responded -> EmotionTrialOutcome.FALSE_ALARM
            else -> EmotionTrialOutcome.CORRECT_REJECTION
        }
    }

    private fun classifyChoice(
        payload: EmotionStimulusPayload.TwoAltPerceptual,
        trial: RecordedEmotionTrial,
    ): EmotionTrialOutcome = when {
        trial.responseKey == payload.correct -> EmotionTrialOutcome.CORRECT
        trial.responseKey.isDirectional -> EmotionTrialOutcome.INCORRECT
        else -> EmotionTrialOutcome.NO_RESPONSE
    }

    private fun classifyCategory(
        payload: EmotionStimulusPayload.Classification,
        trial: RecordedEmotionTrial,
    ): EmotionTrialOutcome = when (trial.chosenCategory) {
        payload.truth -> EmotionTrialOutcome.CORRECT
        null -> EmotionTrialOutcome.NO_RESPONSE
        else -> EmotionTrialOutcome.INCORRECT
    }
}
