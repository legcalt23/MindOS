package com.mindos.app.features.mindboost.focus.domain.model

import com.mindos.app.features.mindboost.core.lifecycle.LifecycleObservation
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricSet
import com.mindos.app.features.mindboost.focus.domain.quality.SessionAdmission

/**
 * Result of a single trial. The atomic unit of measurement. [reactionTime] is
 * null for outcomes with no response (MISS / TIMEOUT / CORRECT_REJECTION).
 *
 * @param difficulty the difficulty vector in force during this trial (so a block
 *   that adapted mid-stream still attributes each trial to its real difficulty).
 * @param timestampMs wall-clock epoch millis (from an injected clock) for ordering
 *   and persistence — NEVER used to compute RT (that is monotonic, see runtime).
 */
data class TrialResult(
    val trialId: TrialId,
    val exerciseId: ExerciseId,
    val mode: Mode,
    val stimulusIsTarget: Boolean,
    val requiredStop: Boolean,
    val outcome: TrialOutcome,
    val reactionTime: ReactionTimeMs?,
    val difficulty: DifficultyVector,
    val stopSignalDelayMs: Long? = null,
    val timestampMs: Long,
    /** Paradigm-specific per-trial markers (congruency, switch, set-size, …). */
    val markers: TrialMarkers = TrialMarkers.NONE,
) {
    // Convenience accessors so metric code reads `trial.congruent` etc. directly.
    val congruent: Boolean? get() = markers.congruent
    val isSwitch: Boolean? get() = markers.isSwitch
    val setSize: Int? get() = markers.setSize
    val dualTask: Boolean? get() = markers.dualTask
    val underPressure: Boolean? get() = markers.underPressure
    val duringIntrusion: Boolean? get() = markers.duringIntrusion
}

/** Aggregated result of one block (one exercise at a roughly stable difficulty). */
data class BlockResult(
    val blockId: BlockId,
    val exerciseId: ExerciseId,
    val mode: Mode,
    val trials: List<TrialResult>,
    val difficultyStart: DifficultyVector,
    val difficultyEnd: DifficultyVector,
    val metrics: MetricSet,
)

/**
 * Aggregated result of a full session (warmup → core → … → reflection).
 * The [subjectiveState] questionnaire (sleep, caffeine, mood, hour) contextualizes
 * the variance — it is covariate data, never a diagnosis.
 */
data class SessionResult(
    val sessionId: SessionId,
    val mode: Mode,
    val exerciseFocus: ExerciseId,
    val blocks: List<BlockResult>,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val subjectiveState: SubjectiveState? = null,
    val sessionMetrics: MetricSet,
    /**
     * FOC-P0-03 · What the lifecycle sensor saw over the session window. `null` = NOT RECORDED (a legacy row or
     * an assembler path that never attached it) — it is never "clean"; "watched and nothing happened" is
     * `Observed(emptyList())`, and "could not watch" is `NotObservable(reason)`.
     */
    val lifecycle: LifecycleObservation? = null,
    /**
     * FOC-P0-05 · The quality gate's verdict on this session. `null` = NOT GATED (a legacy row, or an assembler
     * path that never went through `FocusSessionAssembler.admit`) — never read as `Admitted`.
     */
    val admission: SessionAdmission? = null,
) {
    val durationMs: Long get() = (endedAtMs - startedAtMs).coerceAtLeast(0)
    val totalTrials: Int get() = blocks.sumOf { it.trials.size }
}

/** Brief self-report captured before a session. All fields optional. */
@Suppress("MagicNumber") // 1..5 and 0..23 are the domain ranges named by the field names.
data class SubjectiveState(
    val sleepHours: Double? = null,
    val caffeine: Boolean? = null,
    val mood1to5: Int? = null,
    /** Local hour 0..23, for circadian context. */
    val localHour: Int? = null,
) {
    init {
        mood1to5?.let { require(it in 1..5) { "mood1to5 must be in 1..5." } }
        localHour?.let { require(it in 0..23) { "localHour must be in 0..23." } }
    }
}
