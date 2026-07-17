package com.mindos.app.features.mindboost.profile.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Schema version of the persisted profile snapshot. Bump on incompatible changes.
 *
 * v2 (SYSTEM-1 6R3 R3-FINAL D1/D4): the estimator changed incompatibly — Focus/Logic are now fail-closed (their
 * dimensions can no longer be ESTABLISHED off training data) and the global composite is coverage-only (no scalar).
 * Any v1 snapshot was produced by the removed unsound estimator, so it is DISCARDED on load (see
 * `LocalMindBoostProfileRepository.load`) and recomputed from the current sources; raw session history is untouched.
 */
object MindBoostProfileSchema {
    const val CURRENT: Int = 2
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
    val provisionalIndex: ProvisionalGlobalIndexDto? = null,
    val establishedDimensionCount: Int = 0,
    val strongest: String? = null,
    val developing: String? = null,
)

/**
 * Persisted mirror of `ProvisionalGlobalIndex` — a SCO-014 COVERAGE view (no scalar), never the official score.
 * SYSTEM-1 6R3 R3-FINAL §33/§34 D4: the legacy `value: Double` field was dropped; old blobs that still carry it
 * are read leniently (unknown key ignored by `ignoreUnknownKeys`), the number is simply discarded.
 */
@Serializable
data class ProvisionalGlobalIndexDto(
    val coveredDimensions: List<String> = emptyList(),
    val missingDimensions: List<String> = emptyList(),
)
