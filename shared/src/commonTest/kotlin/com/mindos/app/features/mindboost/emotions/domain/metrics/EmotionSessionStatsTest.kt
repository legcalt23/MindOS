@file:Suppress("MagicNumber", "MaxLineLength") // Fixtures are the data under test.

package com.mindos.app.features.mindboost.emotions.domain.metrics

import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionResponseKey
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulus
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionTrialOutcome
import com.mindos.app.features.mindboost.emotions.domain.model.RecordedEmotionTrial
import kotlin.test.Test
import kotlin.test.assertEquals

/** MC8u-C §3: explicit session counts + RT aggregates. */
class EmotionSessionStatsTest {

    private fun trial(outcome: EmotionTrialOutcome, rt: Long?, key: EmotionResponseKey): ClassifiedEmotionTrial {
        val stim = EmotionStimulus(0, null, EmotionStimulusPayload.Classification("x", EmotionCategory.INTERNAL_STATE, false), 0, 1000)
        return ClassifiedEmotionTrial(RecordedEmotionTrial(stim, rt, key), outcome)
    }

    @Test
    fun counts_and_reaction_times_are_computed_explicitly() {
        val trials = listOf(
            trial(EmotionTrialOutcome.CORRECT, 500, EmotionResponseKey.CATEGORY_A),
            trial(EmotionTrialOutcome.CORRECT, 700, EmotionResponseKey.CATEGORY_B),
            trial(EmotionTrialOutcome.INCORRECT, 900, EmotionResponseKey.CATEGORY_C),
            trial(EmotionTrialOutcome.NO_RESPONSE, null, EmotionResponseKey.NONE),
        )
        val s = EmotionSessionStatsCalculator.from(trials)
        assertEquals(4, s.totalAnswers)
        assertEquals(2, s.correctAnswers)
        assertEquals(1, s.incorrectAnswers)
        assertEquals(1, s.omissions)
        assertEquals(0.5, s.accuracy, 1e-9)
        assertEquals(700.0, s.averageReactionTimeMs, 1e-9)
        assertEquals(500.0, s.bestReactionTimeMs, 1e-9)
        assertEquals(900.0, s.worstReactionTimeMs, 1e-9)
    }

    @Test
    fun empty_session_is_all_zero_no_nan() {
        val s = EmotionSessionStatsCalculator.from(emptyList())
        assertEquals(0, s.totalAnswers)
        assertEquals(0.0, s.accuracy, 1e-9)
        assertEquals(0.0, s.reactionTimeVariability, 1e-9)
        assertEquals(0.0, s.bestReactionTimeMs, 1e-9)
    }
}
