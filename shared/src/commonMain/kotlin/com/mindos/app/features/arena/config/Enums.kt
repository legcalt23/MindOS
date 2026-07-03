package com.mindos.app.features.arena.config

/**
 * Phase 01: Enum Registry
 * Contains all enums used across arena modules.
 */

enum class ActivityStateEnum {
    ACTIVE,
    INACTIVE
}

enum class VerificationStateEnum {
    UNVERIFIED,
    VERIFIED
}

enum class IntegrityStateEnum {
    OPEN,
    REVIEW_REQUIRED,
    HOLD,
    INVALIDATED,

    /**
     * Phase 03: the only state under which the Bayesian rating engine is allowed to
     * apply a rating delta (source of truth: 17_metric_integrity_hardening.md AC #1).
     * This supersedes the earlier "Integrity == OPEN" gate sketched in the detailed
     * 03_rating_core_engine plan, per the revised source-of-truth priority order.
     */
    VERIFIED
}

enum class MinorStateEnum {
    ADULT,
    MINOR
}

enum class ComparabilityStateEnum {
    COMPARABLE,
    PARTIALLY_COMPARABLE,
    NOT_COMPARABLE
}

enum class ConfidenceStateEnum {
    SUFFICIENT,
    INSUFFICIENT
}

enum class SessionValidityStatusEnum {
    VALID,
    INVALID
}

enum class IsolationState {
    OPEN,
    ISOLATED
}

enum class ContaminationRiskState {
    LOW,
    HIGH
}

enum class MetricEligibilityStateEnum {
    ELIGIBLE,
    INELIGIBLE
}

enum class DriftStateEnum {
    STABLE,
    DRIFTING
}

/**
 * Phase 03/05: The three MVP cognitive motors supported by the offline arena.
 * No other motors are introduced. Mental Speed is hardware-class sensitive.
 */
enum class TaskMotorType {
    FOCUS_INTERFERENCE_CONTROL,
    WORKING_MEMORY_SEQUENCE_UPDATING,
    MENTAL_SPEED_CHOICE_REACTION
}

/**
 * Phase 03: Smurf-suspicion level used by the sigma update. When HIGH, sigma is kept
 * high or inflated to allow rapid recalibration and is never collapsed prematurely.
 */
enum class SmurfSuspicionLevel {
    NONE,
    LOW,
    HIGH
}

/**
 * Phase 05/Task 5: Operating-system family used by HardwareClass segmentation.
 */
enum class OsFamily {
    ANDROID,
    IOS,
    OTHER
}

/**
 * Phase 05/Task 5: Refresh-rate tier used by HardwareClass segmentation.
 */
enum class RefreshRateTier {
    HZ_60,
    HZ_90,
    HZ_120,
    UNKNOWN
}

/**
 * Phase 03: Typed reason codes for a rejected (zero-delta) rating update. Reason codes
 * are static enum values and carry no identifiers or rating values, so they are safe to
 * surface across the sanitization boundary.
 */
enum class RatingUpdateRejectionReason {
    INTEGRITY_NOT_VERIFIED,
    LEDGER_NOT_APPROVED_CURRENT,
    NOT_COMPARABLE,
    INSUFFICIENT_MEASUREMENT_CONFIDENCE,
    SESSION_INVALID
}
