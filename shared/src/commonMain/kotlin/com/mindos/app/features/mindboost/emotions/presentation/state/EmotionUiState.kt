package com.mindos.app.features.mindboost.emotions.presentation.state

import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.feedback.FourPartFeedback
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory

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

    /**
     * EM3 Separador: classify [phrase] into one of the four categories, offered in [options].
     *
     * Ola 3 · R3 · P1 — the order is drawn per trial and is part of the field, not a constant of the renderer.
     * Rendered in a fixed 2×2 grid, the map from category to quadrant was the same for every user and every trial,
     * which made "always tap the same quadrant" score whatever "always answer the same category" scores. Measured
     * over the real bank that is 0.34 of trials at the bottom of the ambiguity axis and 0.45 at the top, against a
     * chance of 0.25 — the cheapest possible strategy paying best exactly where the engine sends a good user.
     *
     * Shuffling here fixes the POSITION half of that and only that: it makes tapping a place worth chance no matter
     * how the bank is written, which rebalancing today's items would not (the next authored item can undo it). The
     * remaining category skew is a fact about content selection and is recorded as its own finding.
     */
    data class FourCategory(val phrase: String, val options: List<EmotionCategory>) : EmotionFieldState

    /**
     * Ola 3 · R2 — the brief neutral marker shown after a wrong answer, and nothing else.
     *
     * It says a mistake happened. It does NOT say which side was right: the exercise holds the error rate at a
     * target, so revealing the answer would turn the next trial into a memory test of the last one. It carries no
     * text and no colour of its own for the same reason the cost is a streak reset rather than a penalty — the
     * mistake has to be visible and survivable, not punished.
     */
    data object ErrorMarker : EmotionFieldState
}

/**
 * Live, lightweight model the running screen renders.
 *
 * [streak] is the count of consecutive correct answers, or null for an exercise that has no streak. Ola 3 · R2
 * introduced it as the VISIBLE, RECOVERABLE cost of an error: it drops to zero and can be rebuilt in the very
 * next trial. Nothing else about a mistake is surfaced, and no metric is ever shown in red.
 */
data class EmotionRunningModel(
    val exerciseName: String,
    val field: EmotionFieldState,
    val trialIndex: Int,
    val trialTotal: Int,
    val intensity: Int,
    val streak: Int? = null,
)

/** Top-level state of a session run. The screen renders the always-visible safety bar over Running. */
sealed interface EmotionSessionUiState {
    data class SelfReportBefore(val exerciseName: String, val instructions: String) : EmotionSessionUiState
    data class Running(val model: EmotionRunningModel) : EmotionSessionUiState

    /**
     * Ola 1 · N1 — the confidence strip, shown for at most [windowMs] after an answered trial in a paradigm that
     * can carry a judgment. It is NON-BLOCKING by design: the session moves on when the window elapses, and the
     * trial simply does not enter calibration. Making it blocking would let a metacognitive question stall a task
     * whose whole measurement is how fast the primary decision is made.
     */
    data class ConfidencePrompt(val windowMs: Long) : EmotionSessionUiState

    /**
     * Ola 1 · N1 — the per-block forecast, asked BEFORE the block starts: "of the next [blockSize], how many will
     * you get right?". The user is never shown their trial-by-trial outcome, so this cannot be assembled from
     * feedback fed to them along the way.
     */
    data class BlockForecastPrompt(val blockSize: Int, val blockIndex: Int) : EmotionSessionUiState

    /** Proxy-armed mid-session check-in: a single 1..5 intensity question (never names an emotion). */
    data object MidCheckIn : EmotionSessionUiState

    /** Neutral, low-motion micro-pause overlay; exit visible the whole time. */
    data object MicroPause : EmotionSessionUiState

    data class SelfReportAfter(val exerciseName: String) : EmotionSessionUiState

    /**
     * Sober post-session result; [stoppedForSafety] swaps the copy to the neutral safe-close message.
     * [validSessions] is the count of PRD-valid sessions (progress toward the 5 needed for the official range).
     * [counted] (MC8u-B §3) is whether THIS session is a PRD-valid session that counts toward calibration — false for
     * a safety stop or a too-short session, which the result then explains as "practice, not counted". [intensityReduced]
     * (MC8u-B §3) is whether induction intensity was lowered mid-session, surfaced as a reassuring, non-punitive note.
     */
    data class Result(
        val exerciseName: String,
        val feedback: FourPartFeedback,
        val stoppedForSafety: Boolean,
        val validSessions: Int = 0,
        val counted: Boolean = true,
        val intensityReduced: Boolean = false,
    ) : EmotionSessionUiState

    data object Exited : EmotionSessionUiState

    /** MC8h: the terminal could NOT be made durable — NEITHER the canonical summary NOR its retry marker persisted.
     * Shown INSTEAD of the neutral [Exited] so a total storage failure is never presented as a recoverable success. */
    data object SaveFailed : EmotionSessionUiState
}
