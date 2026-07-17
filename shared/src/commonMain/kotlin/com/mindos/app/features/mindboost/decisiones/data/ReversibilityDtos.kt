package com.mindos.app.features.mindboost.decisiones.data

import com.mindos.app.features.mindboost.decisiones.domain.scoring.ReversibilityRecordLog
import com.mindos.app.features.mindboost.decisiones.domain.scoring.ReversibilityTrial
import kotlinx.serialization.Serializable

/**
 * E2 Reversibilidad (Decisiones · P2) · Versioned on-disk shapes for the per-user reversibility-card log,
 * plus 1:1 DTO ↔ domain mappers. Only the two facts the honest signal needs are persisted
 * (actuallyReversible/selectedReversible) — never RT, lock, or any richer per-card behavior. Bounded
 * retention lives in [ReversibilityRecordLog.append], not here.
 */
@Serializable
data class ReversibilityTrialDto(
    val actuallyReversible: Boolean,
    val selectedReversible: Boolean,
)

@Serializable
data class ReversibilityRecordLogDto(
    val schemaVersion: Int = SCHEMA_VERSION,
    val trials: List<ReversibilityTrialDto> = emptyList(),
    // S3: recently-merged session ids for write-ahead replay dedup. Additive + defaulted, so a v1 blob (which lacks
    // it) decodes to an empty list under `ignoreUnknownKeys`, and `readVersioned` accepts v1 <= v2 (no data loss on
    // upgrade). Bumped to v2 to keep the schema change explicit (repo convention).
    val mergedSessionIds: List<String> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 2
    }
}

fun ReversibilityTrial.toDto() = ReversibilityTrialDto(
    actuallyReversible = actuallyReversible,
    selectedReversible = selectedReversible,
)

fun ReversibilityTrialDto.toDomain() = ReversibilityTrial(
    actuallyReversible = actuallyReversible,
    selectedReversible = selectedReversible,
)

fun ReversibilityRecordLog.toDto() =
    ReversibilityRecordLogDto(trials = trials.map { it.toDto() }, mergedSessionIds = mergedSessionIds)

fun ReversibilityRecordLogDto.toDomain() =
    ReversibilityRecordLog(trials = trials.map { it.toDomain() }, mergedSessionIds = mergedSessionIds)
