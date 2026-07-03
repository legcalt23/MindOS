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
) {
    companion object {
        const val SCHEMA_VERSION = 1
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

fun ReversibilityRecordLog.toDto() = ReversibilityRecordLogDto(trials = trials.map { it.toDto() })

fun ReversibilityRecordLogDto.toDomain() = ReversibilityRecordLog(trials = trials.map { it.toDomain() })
