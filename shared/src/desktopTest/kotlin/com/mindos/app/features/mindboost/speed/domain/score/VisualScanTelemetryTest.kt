package com.mindos.app.features.mindboost.speed.domain.score

import com.mindos.app.features.mindboost.speed.domain.SpeedChoice
import com.mindos.app.features.mindboost.speed.domain.exercise.SpeedTrialPrompt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** MC9-C-R · E5 — reconstruction of per-trial outcomes and the run summary from captured responses. */
class VisualScanTelemetryTest {

    private fun prompt(items: Int, correct: SpeedChoice): SpeedTrialPrompt {
        val panel = (0 until items).joinToString(" ") { "●" }
        return SpeedTrialPrompt(
            correctChoice = correct,
            cue = "Busca ★",
            leftLabel = panel,
            rightLabel = panel,
            revealLabelsOnlyAtStimulus = true,
        )
    }

    @Test
    fun outcomes_are_reconstructed_from_captured_responses() {
        val responses = listOf(
            RecordedResponse(prompt(4, SpeedChoice.LEFT), SpeedChoice.LEFT, 400_000_000L, correct = true, anticipation = false),
            RecordedResponse(prompt(6, SpeedChoice.RIGHT), SpeedChoice.LEFT, 500_000_000L, correct = false, anticipation = false),
        )
        val outcomes = VisualScanTrialOutcome.from(responses)
        assertEquals(2, outcomes.size)
        assertEquals(4, outcomes[0].itemsPerSide, "itemsPerSide parsed from the panel symbol count")
        assertEquals(SpeedChoice.LEFT, outcomes[0].targetSide, "target side = prompt.correctChoice")
        assertEquals(6, outcomes[1].itemsPerSide)
        assertEquals(SpeedChoice.RIGHT, outcomes[1].targetSide)
        assertEquals(SpeedChoice.LEFT, outcomes[1].responseSide, "response side = chosen side")
    }

    @Test
    fun a_response_without_a_prompt_is_dropped() {
        val responses = listOf(RecordedResponse(null, null, 300_000_000L, correct = true, anticipation = false))
        assertTrue(VisualScanTrialOutcome.from(responses).isEmpty(), "a promptless response (e.g. E1) is not an E5 trial")
    }

    @Test
    fun summary_aggregates_accuracy_and_rt_by_load() {
        val outcomes = listOf(
            VisualScanTrialOutcome(3, SpeedChoice.LEFT, SpeedChoice.LEFT, 400_000_000L, correct = true, anticipation = false),
            VisualScanTrialOutcome(3, SpeedChoice.LEFT, SpeedChoice.LEFT, 400_000_000L, correct = true, anticipation = false),
            VisualScanTrialOutcome(6, SpeedChoice.LEFT, SpeedChoice.RIGHT, 500_000_000L, correct = false, anticipation = false),
        )
        val s = VisualScanRunSummary.from(outcomes)
        assertEquals(3, s.totalTrials)
        assertEquals(2, s.correctTrials)
        assertEquals(1.0, s.byLoad.first { it.itemsPerSide == 3 }.accuracy, 1e-9)
        assertEquals(0.0, s.byLoad.first { it.itemsPerSide == 6 }.accuracy, 1e-9)
        assertEquals(400.0, s.byLoad.first { it.itemsPerSide == 3 }.medianRtMs)
    }
}
