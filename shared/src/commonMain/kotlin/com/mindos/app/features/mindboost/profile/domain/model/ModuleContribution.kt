package com.mindos.app.features.mindboost.profile.domain.model

/**
 * A coarse, non-PII signal one module exposes to the global profile. Mirrors the one-way pattern of
 * `EmotionProfileReadOnly`: modules expose this; the profile consumes it. It NEVER carries raw trials,
 * timings, or anything uploadable.
 *
 * [normalizedScore] is `0.0..1.0` or `null` ("not computable yet" — never a silent 0). [dataSufficiency]
 * and [confidence] are `0.0..1.0` gating signals the aggregator uses to decide whether the contribution
 * counts. [preliminary] marks an uncalibrated signal (e.g. before a module's physical-validation gate).
 * [sessionEvidence] is the coarse cross-session evidence a SESSION-BASED source exposes so the aggregator
 * can cap the dimension's maturity (DC-3, via `CalibrationPolicy`); `null` for trial-accumulation modules,
 * which are not gated by the cross-session clause (their sufficiency is trial-based).
 *
 * [updatedAtMs] is the epoch-millis of the REAL event the evidence describes (session end / capture time),
 * or `null` when that time is unknown/unreliable — NEVER a fabricated `0L` (which would masquerade as a
 * real 1970 timestamp; "MindBoost must not fabricate recency", Fase 1.7). It is metadata only: the
 * aggregator stamps the profile with its own injected `EpochClock` and does not read this field.
 */
@Suppress("LongParameterList") // A coarse module→profile boundary record; its fields are cohesive.
data class ModuleContribution(
    val moduleId: CognitiveModuleId,
    val normalizedScore: Double?,
    val dataSufficiency: Double,
    val confidence: Double,
    val trend: ProfileTrend = ProfileTrend.UNKNOWN,
    val preliminary: Boolean = true,
    val updatedAtMs: Long? = null,
    val sessionEvidence: SessionEvidence? = null,
) {
    /** The dimension this module primarily informs. */
    val dimension: CognitiveDimension get() = moduleId.primaryDimension

    init {
        require(normalizedScore == null || normalizedScore in 0.0..1.0) {
            "normalizedScore must be null or in 0.0..1.0 (was $normalizedScore)."
        }
        require(dataSufficiency in 0.0..1.0) { "dataSufficiency must be in 0.0..1.0 (was $dataSufficiency)." }
        require(confidence in 0.0..1.0) { "confidence must be in 0.0..1.0 (was $confidence)." }
    }
}
