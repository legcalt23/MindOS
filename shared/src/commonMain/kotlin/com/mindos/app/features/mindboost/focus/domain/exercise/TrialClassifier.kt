package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.focus.domain.model.ReactionTimeMs
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome

/**
 * Pure classification of a single trial's outcome from its timing. Kept separate
 * from any runtime/IO so it is trivially unit-testable and shared across exercises.
 */
object TrialClassifier {

    data class Classification(val outcome: TrialOutcome, val reactionTime: ReactionTimeMs?)

    /**
     * Classifies a go/no-go style trial.
     *
     * @param isTarget whether the stimulus required a response.
     * @param responseLatencyMs latency from stimulus onset to the response, or null
     *   if the user did not respond within the window.
     * @param anticipationFloorMs responses faster than this are anticipations.
     * @param responseWindowMs responses after this are treated as no-response.
     */
    fun classify(
        isTarget: Boolean,
        responseLatencyMs: Long?,
        anticipationFloorMs: Long,
        responseWindowMs: Long,
    ): Classification {
        val responded = responseLatencyMs != null && responseLatencyMs in 0..responseWindowMs
        return when {
            // Premature response (whether or not a target) is an anticipation.
            responseLatencyMs != null && responseLatencyMs < anticipationFloorMs ->
                Classification(TrialOutcome.ANTICIPATION, null)

            isTarget && responded ->
                Classification(TrialOutcome.HIT, ReactionTimeMs(responseLatencyMs))

            isTarget && !responded ->
                Classification(TrialOutcome.MISS, null)

            // Keep the latency of the erroneous response: the adaptive engine needs
            // it to detect fast commissions (impulsivity). Metrics ignore non-HIT
            // RTs, so this never pollutes RT-median / RT-CV.
            !isTarget && responded ->
                Classification(TrialOutcome.FALSE_ALARM, ReactionTimeMs(requireNotNull(responseLatencyMs)))

            else ->
                Classification(TrialOutcome.CORRECT_REJECTION, null)
        }
    }

    /**
     * Classifies a two-choice (choice-RT) trial, e.g. flanker (E2) or Stroop (E5):
     * a response always required, scored by whether the chosen [responseKey]
     * matches [correctKey].
     *
     * - correct key in window  → HIT (with RT)
     * - wrong key in window    → FALSE_ALARM (a commission; RT kept for impulsivity)
     * - response below floor   → ANTICIPATION
     * - no response in window  → MISS
     */
    fun classifyChoice(
        correctKey: ResponseKey,
        responseKey: ResponseKey?,
        responseLatencyMs: Long?,
        anticipationFloorMs: Long,
        responseWindowMs: Long,
    ): Classification {
        val inWindow = responseLatencyMs != null && responseLatencyMs in 0..responseWindowMs
        return when {
            responseLatencyMs != null && responseLatencyMs < anticipationFloorMs ->
                Classification(TrialOutcome.ANTICIPATION, null)

            inWindow && responseKey == correctKey ->
                Classification(TrialOutcome.HIT, ReactionTimeMs(requireNotNull(responseLatencyMs)))

            inWindow ->
                Classification(TrialOutcome.FALSE_ALARM, ReactionTimeMs(requireNotNull(responseLatencyMs)))

            else ->
                Classification(TrialOutcome.MISS, null)
        }
    }
}
