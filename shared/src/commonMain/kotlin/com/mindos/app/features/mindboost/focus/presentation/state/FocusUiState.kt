package com.mindos.app.features.mindboost.focus.presentation.state

import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.session.FocusReport
import com.mindos.app.features.mindboost.focus.domain.session.SessionTemplate

/** Top-level state of the MindBoost home screen. */
sealed interface FocusHomeUiState {
    data object Loading : FocusHomeUiState

    data class Ready(
        val displayName: String,
        val cycleWeek: Int,
        val streakDays: Int,
        val totalSessions: Int,
        val lastSustainedFocusIndex: Double?,
        val needsBaseline: Boolean,
        val recommendedExercise: ExerciseId,
        val recommendedExerciseName: String,
        val recommendedDescription: String,
    ) : FocusHomeUiState
}

/** What the live exercise field is doing right now. Drives the canvas render. */
sealed interface FocusFieldState {
    /** Calm gap between events. */
    data object Idle : FocusFieldState

    /** The target signal is visible; the user should respond. */
    data class Signal(val contrast: Float) : FocusFieldState

    /** A lure / non-target is visible; the user should NOT respond. */
    data class Lure(val contrast: Float) : FocusFieldState

    /** Brief post-response feedback flash. */
    data class Feedback(val kind: FeedbackKind) : FocusFieldState
}

enum class FeedbackKind { HIT, MISS, FALSE_ALARM, CORRECT_REJECTION, TOO_EARLY }

/** Phase of a running session. */
enum class FocusPhase { WARMUP, CORE, COOLDOWN }

/** Live, lightweight model the session screen renders. */
data class FocusRunningModel(
    val exerciseName: String,
    val phase: FocusPhase,
    val field: FocusFieldState,
    /** 0.0..1.0 progress through the planned trials. */
    val progress: Float,
    val trialsDone: Int,
    val trialsTotal: Int,
    val hits: Int,
    val lapses: Int,
    val falseAlarms: Int,
    /** Last reaction time in ms, for a subtle live readout. Null until first hit. */
    val lastReactionMs: Long?,
)

/** Top-level state of a session run. */
sealed interface FocusSessionUiState {
    data class Intro(
        val exerciseName: String,
        val exerciseDescription: String,
        val template: SessionTemplate,
    ) : FocusSessionUiState

    data class Running(val model: FocusRunningModel) : FocusSessionUiState

    data class Finished(val report: FocusReport, val template: SessionTemplate) : FocusSessionUiState

    data class Aborted(val reason: String) : FocusSessionUiState
}
