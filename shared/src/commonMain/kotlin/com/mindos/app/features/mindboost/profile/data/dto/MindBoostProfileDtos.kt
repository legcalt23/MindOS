package com.mindos.app.features.mindboost.profile.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Schema version of the persisted profile snapshot. Bump on incompatible changes. */
object MindBoostProfileSchema {
    const val CURRENT: Int = 1
}

/** Tolerant JSON used for the local-first profile snapshot. */
object MindBoostProfileJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}

/**
 * Persisted snapshot of the global cognitive profile. COARSE, non-PII, LOCAL-ONLY (encrypted at rest via
 * `SecureKeyValueStore`). It is a derived cache of the latest aggregation — never raw trials, never uploaded.
 */
@Serializable
data class MindBoostProfileDto(
    val schemaVersion: Int = MindBoostProfileSchema.CURRENT,
    val userId: String,
    val dimensions: List<DimensionStateDto> = emptyList(),
    val overall: OverallSummaryDto? = null,
    val updatedAtMs: Long = 0L,
)

@Serializable
data class DimensionStateDto(
    val dimension: String,
    val value: Double? = null,
    val confidence: Double = 0.0,
    val dataSufficiency: Double = 0.0,
    val trend: String = "UNKNOWN",
    val status: String = "CALIBRATING",
    val preliminary: Boolean = true,
    val contributingModules: List<String> = emptyList(),
)

@Serializable
data class OverallSummaryDto(
    val compositeIndex: Double? = null,
    val establishedDimensionCount: Int = 0,
    val strongest: String? = null,
    val developing: String? = null,
)
