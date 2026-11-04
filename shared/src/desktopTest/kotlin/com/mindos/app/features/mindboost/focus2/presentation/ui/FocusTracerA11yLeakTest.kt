package com.mindos.app.features.mindboost.focus2.presentation.ui

import com.mindos.app.features.mindboost.focus2.domain.generator.PlannedTrial
import com.mindos.app.features.mindboost.focus2.domain.scoring.ConditionId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Focus V5 · S5 · R3.1 · T-A11Y-NO-ANSWER-LEAK (RA-05 / MB-FOCUS-0051).
 *
 * FocusTracerScreen used to announce the centre element's direction (the correct flanker response) and whether
 * the flankers matched it (the congruency manipulation) to the screen reader. Under a screen reader that voids
 * the conflict and hands a trivial 100%, so "accuracy is modality-fair" was false. The description must be
 * answer-blind.
 *
 * The discriminating check is INVARIANCE, not a substring blocklist (which a paraphrase could dodge): the four
 * direction x congruency combinations, holding the array size fixed, must produce the IDENTICAL description. If
 * the text encoded either the direction or the congruency - as the old code did - two of these would differ
 * and this test would be RED. The substring test is a second, cheaper guard against the exact old tokens.
 */
class FocusTracerA11yLeakTest {

    private fun trial(centrePointsLeft: Boolean, congruent: Boolean) = PlannedTrial(
        index = 0,
        conditionId = if (congruent) ConditionId.CONGRUENT else ConditionId.INCONGRUENT,
        centrePointsLeft = centrePointsLeft,
        congruent = congruent,
        flankerCount = 4,
        separation = 1.0,
        similarity = 1.0,
        responseWindowMs = 1_500L,
        interStimulusDelayMs = 500L,
    )

    @Test
    fun stimulus_description_is_identical_across_direction_and_congruency() {
        val descriptions = setOf(
            stimulusContentDescription(trial(centrePointsLeft = true, congruent = true)),
            stimulusContentDescription(trial(centrePointsLeft = true, congruent = false)),
            stimulusContentDescription(trial(centrePointsLeft = false, congruent = true)),
            stimulusContentDescription(trial(centrePointsLeft = false, congruent = false)),
        )
        assertEquals(
            1,
            descriptions.size,
            "The stimulus description leaks the answer: it differs across direction/congruency. Got $descriptions",
        )
    }

    @Test
    fun stimulus_description_never_names_the_response_direction_or_congruency() {
        val text = stimulusContentDescription(trial(centrePointsLeft = true, congruent = false)).lowercase()
        listOf("izquierda", "derecha", "iguales", "distintos").forEach { leak ->
            assertFalse(text.contains(leak), "stimulus description must not contain '$leak' (leaks the answer): $text")
        }
    }
}
