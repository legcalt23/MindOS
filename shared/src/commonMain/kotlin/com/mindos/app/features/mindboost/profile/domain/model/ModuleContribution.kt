package com.mindos.app.features.mindboost.profile.domain.model

/**
 * A coarse, non-PII signal one module exposes to the global profile. Mirrors the one-way pattern of
 * `EmotionProfileReadOnly`: modules expose this; the profile consumes it. It NEVER carries raw trials,
 * timings, or anything uploadable.
 *
 * [normalizedScore] is `0.0..1.0` or `null` ("not computable yet" — never a silent 0). [dataSufficiency]
 * and [confidence] are `0.0..1.0` gating signals the aggregator uses to decide whether the contribution
 * counts. [preliminary] marks an uncalibrated signal (e.g. before a module's physical-validation gate).
 */
data class ModuleContribution(
    val moduleId: CognitiveModuleId,
    val normalizedScore: Double?,
    val dataSufficiency: Double,
    val confidence: Double,
    val trend: ProfileTrend = ProfileTrend.UNKNOWN,
    val preliminary: Boolean = true,
    val updatedAtMs: Long = 0L,
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
