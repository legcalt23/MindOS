@file:Suppress("MaxLineLength")

package com.mindos.app.features.mindboost.speed.presentation.ui

import com.mindos.app.features.mindboost.speed.domain.SpeedChoice
import com.mindos.app.features.mindboost.speed.domain.exercise.SpeedTrialPrompt
import com.mindos.app.features.mindboost.speed.presentation.SpeedPhase
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * MC9-A-R §3 — E2 · Comparación shows its magnitudes on the panels, but they must NOT be revealed before the
 * stimulus onset (else the comparison is decided during the foreperiod and the TR is contaminated). The reveal
 * predicate is the pure, testable guarantee; generic 2-way exercises are unaffected.
 */
class TwoChoiceLabelRevealTest {

    private fun comparison() = SpeedTrialPrompt(
        correctChoice = SpeedChoice.LEFT,
        cue = "¿Cuál es mayor?",
        leftLabel = "7",
        rightLabel = "3",
        revealLabelsOnlyAtStimulus = true,
    )

    @Test
    fun e2_magnitudes_are_hidden_until_the_stimulus() {
        val p = comparison()
        assertFalse(shouldRevealTwoChoiceLabels(p, SpeedPhase.Idle), "hidden before start")
        assertFalse(shouldRevealTwoChoiceLabels(p, SpeedPhase.Foreperiod), "hidden during the foreperiod")
        assertTrue(shouldRevealTwoChoiceLabels(p, SpeedPhase.Stimulus), "revealed at the onset")
        assertTrue(shouldRevealTwoChoiceLabels(p, SpeedPhase.Feedback), "still shown during feedback")
    }

    @Test
    fun generic_two_way_exercises_always_show_their_labels() {
        // Default reveal flag = false (E3/E6/E7): generic affordance labels stay visible in every phase.
        val generic = SpeedTrialPrompt(SpeedChoice.LEFT, "◀", "Izquierda", "Derecha")
        SpeedPhase.entries.forEach { assertTrue(shouldRevealTwoChoiceLabels(generic, it), "generic labels visible in $it") }
    }

    @Test
    fun a_null_prompt_is_treated_as_visible() {
        assertTrue(shouldRevealTwoChoiceLabels(null, SpeedPhase.Foreperiod))
    }
}
