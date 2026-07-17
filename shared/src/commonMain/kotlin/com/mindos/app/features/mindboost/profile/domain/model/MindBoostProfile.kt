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
 * A cross-dimension COVERAGE view — explicitly NOT a composite number and NOT an official/validated Performance
 * Score. SYSTEM-1 Slice 6-R3 R3-FINAL (§33/§34 D4): an unweighted mean of heterogeneous, non-equated, differently
 * reliable dimensions is psychometrically indefensible (implied g-factor, non-comparable summands, partial-as-whole
 * coverage, discarded uncertainty), so this type carries NO scalar `value` — only which dimensions are covered vs
 * missing. Because it structurally cannot hold a number, no flag flip can surface one. Only ever produced while the
 * `mindboostGlobalScoreEnabled` kill switch is ON (default OFF everywhere — C2.5). A defensible provisional NUMBER
 * may be reintroduced ONLY under `MindBoostProfileAggregator.PROVISIONAL_GLOBAL_PROXY_APPROVED` (a code constant,
 * not a flag) plus an approved ADR-SCI-006 weighting model and ADR-SCI-008 comparability bridge. NEVER feeds ranking.
 */
data class ProvisionalGlobalIndex(
    val coveredDimensions: List<CognitiveDimension>,
    val missingDimensions: List<CognitiveDimension>,
)

/**
 * Coarse, descriptive cross-dimension summary, computed ONLY over `ESTABLISHED` dimensions.
 * [compositeIndex] is the OFFICIAL composite — structurally null today (SYSTEM-1 6R3 R3-FINAL §34 D5: the former
 * unweighted-mean-over-9-dimensions formula was psychometrically indefensible AND a category error vs the charter's
 * 8 calibrated+comparable motors; a real official composite needs an approved ADR-SCI-006/008 model, not just a
 * const flip). [provisionalIndex] is the flag-gated COVERAGE view (SCO-014, no scalar) — never confusable with an
 * official score. [developing] is the lowest established dimension — framed as "an area to develop", never
 * "weak"/clinical.
 */
data class OverallSummary(
    val compositeIndex: Double?,
    val provisionalIndex: ProvisionalGlobalIndex?,
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
