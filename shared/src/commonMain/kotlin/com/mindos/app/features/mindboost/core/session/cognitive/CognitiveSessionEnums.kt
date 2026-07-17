package com.mindos.app.features.mindboost.core.session.cognitive

/**
 * SYSTEM-1 · Why a cognitive session ran. This gates whether the sealed [CognitiveSessionRun] may EVER count
 * toward the profile, calibration, or ranking — a NECESSARY (not sufficient) condition: duration/protocol,
 * timing integrity and data quality still apply on top (see [SessionEligibilityPolicy]).
 *
 * - [INITIAL_ASSESSMENT]   baseline: may feed a PROVISIONAL profile, never the official rank.
 * - [TRAINING_RECOMMENDED] the daily-plan pick; counts when it meets the quality gates.
 * - [TRAINING_FREE]        user-chosen OFFICIAL training (not practice); counts when it meets the quality gates.
 * - [CALIBRATION]          an explicit calibration run; counts when it meets the quality gates.
 * - [PRACTICE]             low-stakes warm-up; NEVER counts (profile/calibration/ranking).
 * - [DEBUG]                developer/testing; NEVER counts.
 */
enum class SessionPurpose {
    INITIAL_ASSESSMENT,
    TRAINING_RECOMMENDED,
    TRAINING_FREE,
    CALIBRATION,
    PRACTICE,
    DEBUG,
    ;

    /** True when this purpose may contribute a (provisional or official) profile signal at all. */
    fun mayContributeToProfile(): Boolean = this != PRACTICE && this != DEBUG

    /** True when this purpose may count toward calibration's valid sessions (still gated by duration/quality). */
    fun mayContributeToCalibration(): Boolean =
        this == TRAINING_RECOMMENDED || this == TRAINING_FREE || this == CALIBRATION

    /** True when this purpose may count toward the OFFICIAL ranking. INITIAL_ASSESSMENT is provisional-only. */
    fun mayContributeToRanking(): Boolean =
        this == TRAINING_RECOMMENDED || this == TRAINING_FREE || this == CALIBRATION
}

/**
 * The outcome of ONE trial, general across paradigms. Critically, [CORRECT_INHIBITION] lets a NO-GO / stop
 * paradigm record a correct "did not respond" WITHOUT a fabricated response timestamp — the exact gap that
 * `MeasurementRun`/`TrialRecord` (which require a response uptime + isCorrect) could not express. [LOST_INPUT]
 * records a measured-but-un-timestampable tap: it is COUNTED, never turned into a fabricated reaction time.
 */
enum class TrialOutcomeKind {
    ACTIVE_CORRECT,
    ACTIVE_INCORRECT,
    OMISSION,
    CORRECT_INHIBITION,
    COMMISSION,
    ANTICIPATION,
    LATE_RESPONSE,
    TIMEOUT,
    SKIPPED,
    PARTIAL_CORRECT,
    INVALID_RESPONSE,
    INVALID_LIFECYCLE,
    LOST_INPUT,
    QUALITY_REJECTED,
}

/**
 * How a session ENDED, as a snapshot on the sealed run. The LIVE recovery authority remains the lifecycle
 * `SessionRecord` (a STARTED/CHECKPOINT orphan is marked ABANDONED by `SessionRecoveryScanner`, not here); this
 * enum only records the terminal state observed at seal time.
 */
enum class SessionCompletionStatus {
    COMPLETED,
    ABANDONED,
    INVALID_LIFECYCLE,
    FINALIZATION_FAILED,
}

/** Why a finalization failed, present iff [SessionCompletionStatus.FINALIZATION_FAILED]. */
enum class FinalizationFailureReason {
    SAVE_FAILED,
    BUILD_FAILED,
    INTERRUPTED,
}
