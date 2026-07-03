package com.mindos.app.features.mindboost.profile.domain.model

/** Maturity of a dimension estimate. Descriptive, never a clinical label. */
enum class DimensionStatus {
    /** Not enough confident data to estimate a value yet. */
    CALIBRATING,

    /** A value exists but confidence is still low. */
    EMERGING,

    /** A confidently estimated value. */
    ESTABLISHED,
}

/**
 * Aggregated state of ONE cognitive dimension.
 *
 * [value] is `null` when not computable yet (`CALIBRATING`) — the UI must render this as "calibrating",
 * NEVER as 0. [preliminary] is true when any contributor is uncalibrated.
 */
data class DimensionState(
    val dimension: CognitiveDimension,
    val value: Double?,
    val confidence: Double,
    val dataSufficiency: Double,
    val trend: ProfileTrend,
    val status: DimensionStatus,
    val preliminary: Boolean,
    val contributingModules: List<CognitiveModuleId>,
) {
    init {
        require(value == null || value in 0.0..1.0) { "value must be null or in 0.0..1.0 (was $value)." }
        require(confidence in 0.0..1.0) { "confidence must be in 0.0..1.0 (was $confidence)." }
        require(dataSufficiency in 0.0..1.0) { "dataSufficiency must be in 0.0..1.0." }
        require((status == DimensionStatus.CALIBRATING) == (value == null)) {
            "value and CALIBRATING status must agree (value=$value, status=$status)."
        }
    }

    companion object {
        /** The default state of a dimension with no usable signal yet. */
        fun calibrating(dimension: CognitiveDimension): DimensionState = DimensionState(
            dimension = dimension,
            value = null,
            confidence = 0.0,
            dataSufficiency = 0.0,
            trend = ProfileTrend.UNKNOWN,
            status = DimensionStatus.CALIBRATING,
            preliminary = true,
            contributingModules = emptyList(),
        )
    }
}

/**
 * Coarse, descriptive cross-dimension summary, computed ONLY over `ESTABLISHED` dimensions.
 * [compositeIndex] is null until enough dimensions are established. [developing] is the lowest
 * established dimension — framed as "an area to develop", never "weak"/clinical.
 */
data class OverallSummary(
    val compositeIndex: Double?,
    val establishedDimensionCount: Int,
    val strongest: CognitiveDimension?,
    val developing: CognitiveDimension?,
)

/**
 * The living, local-first functional cognitive profile. Sensitive data: local-only, never uploaded,
 * never presented as a diagnosis. Rebuilt from module contributions after sessions.
 */
data class MindBoostProfile(
    val userId: String,
    val dimensions: Map<CognitiveDimension, DimensionState>,
    val overall: OverallSummary?,
    val updatedAtMs: Long,
) {
    /** The state of [dimension], or a `CALIBRATING` default when absent. */
    fun dimension(dimension: CognitiveDimension): DimensionState =
        dimensions[dimension] ?: DimensionState.calibrating(dimension)
}
