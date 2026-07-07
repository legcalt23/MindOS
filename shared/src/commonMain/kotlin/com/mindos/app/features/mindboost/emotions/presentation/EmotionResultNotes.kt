package com.mindos.app.features.mindboost.emotions.presentation

import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionSessionUiState

/**
 * The single, PURE authority for the ordered set of non-punitive status notes shown under a result (MC8u-B §3/§5).
 * Kept out of the Compose screen so it is unit-testable and the exact wording/order is pinned by tests. Every note is
 * reassuring and session-scoped — it explains what happened (session not counted, limited sample, still calibrating,
 * intensity reduced), never a judgment of the person.
 *
 * Order: session-counting status → this-session confidence → calibration progress → intensity adjustment.
 * LOW_CONFIDENCE (this session's sample) and CALIBRATING (progress toward the 5 valid sessions an official reading
 * needs) are DISTINCT notes with distinct copy — one can hold without the other.
 */
object EmotionResultNotes {

    fun notesFor(state: EmotionSessionUiState.Result): List<String> = buildList {
        if (state.stoppedForSafety) {
            // A safety stop is protective, never a completed calibration — always explain it does not count.
            add(EmotionStrings.RESULT_SESSION_NOT_COUNTED)
        } else {
            if (!state.counted) add(EmotionStrings.RESULT_SESSION_NOT_COUNTED)
            if (!state.feedback.confident) add(EmotionStrings.RESULT_LOW_CONFIDENCE)
            if (state.validSessions < EmotionStrings.VALID_SESSION_TARGET) add(EmotionStrings.RESULT_CALIBRATING)
        }
        if (state.intensityReduced) add(EmotionStrings.RESULT_INTENSITY_REDUCED)
    }
}
