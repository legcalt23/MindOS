package com.mindos.app.features.mindboost.core.session.cognitive

import com.mindos.app.features.mindboost.core.session.purpose.SessionMeasurementPurpose

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
 *
 * **This enum does NOT answer profile eligibility.** Focus V5 · S1 · ADR-FOCUS-PURPOSE retired the
 * `mayContributeToProfile()` derivation that used to live here: it disagreed with the real authority
 * (it allowed TRAINING_FREE, which the authority denies), so one of the two lied on every call. The single
 * authority is now `core.session.purpose.ProfileMeasurementEligibility`, which reads a
 * `SessionMeasurementPurpose` — a sealed value carried by the run, not derived from this enum.
 *
 * The retired predicate was additionally fail-OPEN by construction (`!= PRACTICE && != DEBUG`): every member
 * added here in the future would have defaulted to profile-eligible. Do not reintroduce that shape. The two
 * predicates that remain are allowlists, so a new member defaults to INELIGIBLE for both.
 */
enum class SessionPurpose {
    INITIAL_ASSESSMENT,
    TRAINING_RECOMMENDED,
    TRAINING_FREE,
    CALIBRATION,
    PRACTICE,
    DEBUG,
}

/**
 * Widens a persisted [SessionPurpose] into the single authority's vocabulary
 * (Focus V5 · S1 · ADR-FOCUS-PURPOSE).
 *
 * This is a total, name-preserving INJECTION, not the lossy many-to-one projection the ADR rejected:
 * [SessionMeasurementPurpose] is a strict superset of these six names, so every value maps to its exact
 * counterpart and nothing collapses. The `when` is exhaustive, so a member added to [SessionPurpose] fails the
 * build here rather than silently landing on a default.
 *
 * It exists because [SessionPurpose] is still the shape `CognitiveSessionRun` PERSISTS. Migrating that record to
 * the 11-value vocabulary is a session-model change with a DTO migration, sequenced separately; until then this
 * one boundary function is where the old shape meets the single authority, and it is the only such place.
 */
fun SessionPurpose.asMeasurementPurpose(): SessionMeasurementPurpose = when (this) {
    SessionPurpose.INITIAL_ASSESSMENT -> SessionMeasurementPurpose.INITIAL_ASSESSMENT
    SessionPurpose.TRAINING_RECOMMENDED -> SessionMeasurementPurpose.TRAINING_RECOMMENDED
    SessionPurpose.TRAINING_FREE -> SessionMeasurementPurpose.TRAINING_FREE
    SessionPurpose.CALIBRATION -> SessionMeasurementPurpose.CALIBRATION
    SessionPurpose.PRACTICE -> SessionMeasurementPurpose.PRACTICE
    SessionPurpose.DEBUG -> SessionMeasurementPurpose.DEBUG
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
