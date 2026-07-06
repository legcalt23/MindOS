package com.mindos.app.features.mindboost.emotions.presentation.state

import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.feedback.FourPartFeedback

/** A selectable exercise in the emotions home menu (mirrors `EmotionExerciseCatalog.implemented`). */
data class EmotionExerciseCardUi(
    val id: EmotionExerciseId,
    val name: String,
    val description: String,
)

/** What the live exercise field is showing right now. Drives the renderer; honest (tone is not the answer). */
sealed interface EmotionFieldState {
    /** EM1 Pausa: respond (single tap) only when [frameOpen]; [word] is a mild affective distractor. */
    data class GoNoGo(val frameOpen: Boolean, val word: String) : EmotionFieldState

    /** EM2 Recuperación: two perceptual bars; tap the side with the LARGER bar. The bigger side is correct. */
    data class TwoAlt(val leftMagnitude: Float, val rightMagnitude: Float) : EmotionFieldState

    /** EM3 Separador: classify [phrase] into one of the four fixed categories. */
    data class FourCategory(val phrase: String) : EmotionFieldState
}

/** Live, lightweight model the running screen renders. */
data class EmotionRunningModel(
    val exerciseName: String,
    val field: EmotionFieldState,
    val trialIndex: Int,
    val trialTotal: Int,
    val intensity: Int,
)

/** Top-level state of a session run. The screen renders the always-visible safety bar over Running. */
sealed interface EmotionSessionUiState {
    data class SelfReportBefore(val exerciseName: String, val instructions: String) : EmotionSessionUiState
    data class Running(val model: EmotionRunningModel) : EmotionSessionUiState

    /** Proxy-armed mid-session check-in: a single 1..5 intensity question (never names an emotion). */
    data object MidCheckIn : EmotionSessionUiState

    /** Neutral, low-motion micro-pause overlay; exit visible the whole time. */
    data object MicroPause : EmotionSessionUiState

    data class SelfReportAfter(val exerciseName: String) : EmotionSessionUiState

    /** Sober four-part result; [stoppedForSafety] swaps the copy to the neutral safe-close message. */
    data class Result(
        val exerciseName: String,
        val feedback: FourPartFeedback,
        val stoppedForSafety: Boolean,
    ) : EmotionSessionUiState

    data object Exited : EmotionSessionUiState

    /** MC8h: the terminal could NOT be made durable — NEITHER the canonical summary NOR its retry marker persisted.
     * Shown INSTEAD of the neutral [Exited] so a total storage failure is never presented as a recoverable success. */
    data object SaveFailed : EmotionSessionUiState
}
