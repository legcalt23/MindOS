package com.mindos.app.features.mindboost.core.scoring

import kotlin.jvm.JvmInline

/**
 * INFRA-01 · Supporting value types for [ScoreResult] and [ScoreStateMachine]. All pure data; no
 * platform deps. These describe a scoring verdict's provenance, timing/quality criticality, and the
 * honesty-claim ceiling — they do not measure anything (INFRA-02 populates timing fields, INFRA-04 the
 * bank integrity, INFRA-03 the anti-gaming coverage).
 */

/** Which MindBoost module produced the score. PII-free identifier. */
enum class ScoreModuleId { FOCUS, SPEED, MEMORY, EPISODIC_MEMORY, LOGIC, LANGUAGE, DECISIONS, EMOTIONS, CREATIVITY }

/** How time-dependent the primary signal is — drives the timebase/jank gates. */
enum class TimingCriticality { RT_CRITICAL, RT_SECONDARY, RT_IRRELEVANT }

/** Trust level of the device timebase for RT capture (INFRA-02 classifies this). */
enum class TimebaseQuality { AUTHORITATIVE, MONOTONIC_NONFRAME, WALLCLOCK_DERIVED, UNTRUSTED, NOT_TIME_CRITICAL }

/** Integrity of the item bank backing the exercise (INFRA-04 validates this). */
enum class BankIntegrity {
    OK, SMALL_UNVALIDATED, HOLDOUT_EXHAUSTED, PROCEDURAL, CONTAMINATED, NOT_APPLICABLE,
    ;

    /**
     * Integrity states that must NEVER back an official profile signal — the single source of truth for
     * the charter invariant "contaminated / exhausted / small-unvalidated bank blocks the profile". The
     * score state machine and every module→profile read gate delegate here so the blocking set never forks.
     */
    fun blocksProfileEligibility(): Boolean =
        this == CONTAMINATED || this == HOLDOUT_EXHAUSTED || this == SMALL_UNVALIDATED
}

/** Stability of the metric across the session. */
enum class MetricStability { STABLE, UNSTABLE, UNKNOWN }

/** Why a session was invalidated (when [DataQuality.invalidatedBy] is non-null). */
enum class InvalidationReason { ANTI_GAMING, QUALITY_LOW, AGRS_HIGH, CLOCK_DISCONTINUITY, OTHER }

/** Stable, PII-free key identifying a signal (e.g. "accuracy", "congruency_effect"). */
@JvmInline
value class SignalKey(val id: String)

/** Unit of a signal value. */
enum class SignalUnit { MILLISECONDS, RATIO, COUNT, DPRIME, INDEX, Z_SCORE }

/** Qualitative uncertainty attached to a signal. */
enum class UncertaintyLabel { LOW, MODERATE, HIGH, UNKNOWN }

/**
 * What the UI is permitted to AFFIRM, weakest → strongest. Never names a far-transfer/clinical
 * construct; the copy scanner (INFRA-05) enforces the wording, this enum gates the strength.
 */
enum class ClaimLevel { NONE, PRACTICE_SIGNAL, TASK_PERFORMANCE, WITHIN_TASK_TREND }

/** Copy-safety hints carried with a result (e.g. an absolute construct was suppressed in UI). */
enum class CopySafetyFlag { ABSOLUTE_CONSTRUCT_SUPPRESSED, FAR_TRANSFER_BLOCKED, PRELIMINARY_LABEL_REQUIRED }

/**
 * How much real, tested anti-gaming detection covers this exercise's criticality. A "designed but not
 * tested" detector counts as absent (invariant: anti-gaming only counts if tested) — model it as
 * [NONE]/[PARTIAL], never as full coverage.
 */
enum class DetectorCoverage {
    NONE,
    PARTIAL,
    ADEQUATE,
    ;

    /** Adequate coverage for the given criticality. RT-critical demands full coverage. */
    fun adequateFor(criticality: TimingCriticality): Boolean = when (this) {
        ADEQUATE -> true
        PARTIAL -> criticality != TimingCriticality.RT_CRITICAL
        NONE -> false
    }
}
