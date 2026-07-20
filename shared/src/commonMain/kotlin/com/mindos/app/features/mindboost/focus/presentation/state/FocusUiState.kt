package com.mindos.app.features.mindboost.focus.presentation.state

import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.domain.session.FocusReport
import com.mindos.app.features.mindboost.focus.domain.session.SessionTemplate

/** A selectable exercise in the focus home menu. */
data class FocusExerciseCardUi(
    val id: ExerciseId,
    val name: String,
    val description: String,
)

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
        /** Every selectable exercise (mirrors `ExerciseCatalog.implemented`); never empty. */
        val exercises: List<FocusExerciseCardUi> = emptyList(),
        /** Overtraining guidance to show (null = none); [forceRest] makes it a hard recommendation. */
        val overtrainingNote: String? = null,
        val forceRest: Boolean = false,
        /** Recovery/rest guidance from today's daily plan (null = normal training day). */
        val recoveryNote: String? = null,
        /**
         * Whether the CONTAINED v1 score surfaces may render (`FeatureFlags.focusScoreSurfacesEnabled`).
         * Defaults to `false` so a caller that forgets to set it hides the numbers rather than exposing
         * them — the flag is a containment gate, so its failure mode must be "hide", never "show".
         */
        val scoreSurfacesVisible: Boolean = false,
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

    /** Stop-signal (E4): the go target turned into a STOP cue — withhold / abort the response. */
    data object Stop : FocusFieldState

    /**
     * A two-choice stimulus (flanker E2 / Stroop-Simon E5): the user answers LEFT or RIGHT to the
     * central [targetDirection]. [congruent] flags whether the surrounding/spatial dimension agrees;
     * [distractorCount] is the number of flankers (0 for the Simon-style conflict).
     */
    data class Choice(
        val targetDirection: ResponseKey,
        val congruent: Boolean,
        val distractorCount: Int,
        val paradigm: ChoiceParadigm,
        /** E13 only: the trial is under a tight deadline (urgency cue + tighter window). */
        val underPressure: Boolean = false,
    ) : FocusFieldState

    /**
     * Visual search (E7): a display of [setSize] items with the target present or not. Single-tap —
     * respond if the target is present, withhold if absent. [layoutSeed] keeps the per-trial layout
     * stable across recompositions while varying between trials.
     */
    data class Search(
        val setSize: Int,
        val targetPresent: Boolean,
        val layoutSeed: Int,
    ) : FocusFieldState

    /**
     * Task switching (E6): a colored shape; the active rule decides which dimension maps to the
     * answer. [ruleIsColor] true → answer by colour (red=LEFT), false → answer by shape
     * (circle=LEFT). Two-choice LEFT/RIGHT response.
     */
    data class RuleSwitch(
        val colorIsRed: Boolean,
        val shapeIsCircle: Boolean,
        val ruleIsColor: Boolean,
    ) : FocusFieldState

    /**
     * Digital distraction (E11): a simple LEFT/RIGHT main task ([targetDirection]); on some trials a
     * notification-like intruder appears ([intruderPresent]) that must be ignored. The challenge is
     * resisting the distractor, not the main discrimination.
     */
    data class Anchor(
        val targetDirection: ResponseKey,
        val intruderPresent: Boolean,
    ) : FocusFieldState

    /**
     * Multiple-object tracking (E9): [objectCount] dots; [targetIndices] are highlighted, then all
     * move identically; at the end one dot is probed. Single-tap — respond if the probed dot was a
     * tracked target ([probedIsTarget]). [layoutSeed] fixes the per-trial motion/probe.
     */
    data class Tracking(
        val objectCount: Int,
        val targetIndices: Set<Int>,
        val speed: Double,
        val probedIsTarget: Boolean,
        val layoutSeed: Int,
    ) : FocusFieldState

    /**
     * RSVP (E10): a rapid stream of [length] items with a target at [t1Index] (and maybe [t2Index]).
     * Single-tap — respond when a target flashes. [layoutSeed] fixes the per-trial letters.
     */
    data class Stream(
        val length: Int,
        val t1Index: Int,
        val t2Index: Int?,
        val layoutSeed: Int,
    ) : FocusFieldState

    /**
     * Dual task (E8): a go/no-go primary; on [dual] trials a concurrent monitor task adds attentional
     * load. Single-tap — respond on [go]. The cost is the accuracy drop from single to dual trials.
     */
    data class Dual(
        val go: Boolean,
        val dual: Boolean,
    ) : FocusFieldState

    /** Brief post-response feedback flash. */
    data class Feedback(val kind: FeedbackKind) : FocusFieldState
}

/** Which two-choice paradigm a [FocusFieldState.Choice] renders. */
enum class ChoiceParadigm { FLANKER, CONFLICT, DEADLINE }

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
        /** Short, paradigm-appropriate how-to lines shown before the run. */
        val instructions: List<String> = emptyList(),
    ) : FocusSessionUiState

    data class Running(val model: FocusRunningModel) : FocusSessionUiState

    data class Finished(val report: FocusReport, val template: SessionTemplate) : FocusSessionUiState

    data class Aborted(val reason: String) : FocusSessionUiState
}
