@file:Suppress("MaxLineLength") // Trial-construction fixtures are dense by nature.

package com.mindos.app.features.mindboost.emotions.domain.session

import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionResponseKey
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulus
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionTrialOutcome
import com.mindos.app.features.mindboost.emotions.domain.model.RecordedEmotionTrial
import kotlin.test.Test
import kotlin.test.assertEquals

/** The assembler-authority classifier: go/no-go, 2-alt choice, and 4-category outcomes. */
class EmotionTrialClassifierTest {

    private fun goNoGo(frameOpen: Boolean) = EmotionStimulus(
        index = 0,
        correctResponseKey = if (frameOpen) EmotionResponseKey.PRIMARY else EmotionResponseKey.NONE,
        payload = EmotionStimulusPayload.AffectiveGoNoGo(frameOpen, affectiveWord = "espera", congruent = true),
        interStimulusDelayMs = 0,
        responseWindowMs = 1000,
    )

    private fun choice(correct: EmotionResponseKey) = EmotionStimulus(
        index = 0,
        correctResponseKey = correct,
        payload = EmotionStimulusPayload.TwoAltPerceptual(correct, discriminability = 0.6),
        interStimulusDelayMs = 0,
        responseWindowMs = 1000,
    )

    private fun classification(truth: EmotionCategory) = EmotionStimulus(
        index = 0,
        correctResponseKey = null,
        payload = EmotionStimulusPayload.Classification(phrase = "He fallado tres rondas", truth = truth, isEval = false),
        interStimulusDelayMs = 0,
        responseWindowMs = 1000,
    )

    @Test
    fun go_no_go_outcomes() {
        assertEquals(
            EmotionTrialOutcome.HIT,
            EmotionTrialClassifier.classify(RecordedEmotionTrial(goNoGo(true), 400, EmotionResponseKey.PRIMARY)),
        )
        assertEquals(
            EmotionTrialOutcome.MISS,
            EmotionTrialClassifier.classify(RecordedEmotionTrial(goNoGo(true), null, EmotionResponseKey.NONE)),
        )
        assertEquals(
            EmotionTrialOutcome.FALSE_ALARM,
            EmotionTrialClassifier.classify(RecordedEmotionTrial(goNoGo(false), 300, EmotionResponseKey.PRIMARY)),
        )
        assertEquals(
            EmotionTrialOutcome.CORRECT_REJECTION,
            EmotionTrialClassifier.classify(RecordedEmotionTrial(goNoGo(false), null, EmotionResponseKey.NONE)),
        )
    }

    @Test
    fun two_alternative_choice_outcomes() {
        val stim = choice(EmotionResponseKey.LEFT)
        assertEquals(
            EmotionTrialOutcome.CORRECT,
            EmotionTrialClassifier.classify(RecordedEmotionTrial(stim, 500, EmotionResponseKey.LEFT)),
        )
        assertEquals(
            EmotionTrialOutcome.INCORRECT,
            EmotionTrialClassifier.classify(RecordedEmotionTrial(stim, 500, EmotionResponseKey.RIGHT)),
        )
        assertEquals(
            EmotionTrialOutcome.NO_RESPONSE,
            EmotionTrialClassifier.classify(RecordedEmotionTrial(stim, null, EmotionResponseKey.NONE)),
        )
    }

    @Test
    fun four_category_outcomes() {
        val stim = classification(EmotionCategory.FACT)
        assertEquals(
            EmotionTrialOutcome.CORRECT,
            EmotionTrialClassifier.classify(
                RecordedEmotionTrial(stim, 900, EmotionResponseKey.CATEGORY_D, chosenCategory = EmotionCategory.FACT),
            ),
        )
        assertEquals(
            EmotionTrialOutcome.INCORRECT,
            EmotionTrialClassifier.classify(
                RecordedEmotionTrial(stim, 900, EmotionResponseKey.CATEGORY_A, chosenCategory = EmotionCategory.THOUGHT),
            ),
        )
        assertEquals(
            EmotionTrialOutcome.NO_RESPONSE,
            EmotionTrialClassifier.classify(RecordedEmotionTrial(stim, null, EmotionResponseKey.NONE, chosenCategory = null)),
        )
    }
}
