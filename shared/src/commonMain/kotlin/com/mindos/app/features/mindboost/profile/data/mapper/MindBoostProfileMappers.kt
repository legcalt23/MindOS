package com.mindos.app.features.mindboost.profile.data.mapper

import com.mindos.app.features.mindboost.profile.data.dto.DimensionStateDto
import com.mindos.app.features.mindboost.profile.data.dto.MindBoostProfileDto
import com.mindos.app.features.mindboost.profile.data.dto.MindBoostProfileSchema
import com.mindos.app.features.mindboost.profile.data.dto.OverallSummaryDto
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.DimensionState
import com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
import com.mindos.app.features.mindboost.profile.domain.model.MindBoostProfile
import com.mindos.app.features.mindboost.profile.domain.model.OverallSummary
import com.mindos.app.features.mindboost.profile.domain.model.ProfileTrend

fun MindBoostProfile.toDto(): MindBoostProfileDto = MindBoostProfileDto(
    schemaVersion = MindBoostProfileSchema.CURRENT,
    userId = userId,
    dimensions = dimensions.values.map { it.toDto() },
    overall = overall?.toDto(),
    updatedAtMs = updatedAtMs,
)

/**
 * Lenient reverse mapping. Unknown dimensions are dropped, unknown enum names fall back, and the
 * value/status invariant is reconciled so a slightly-drifted snapshot decodes instead of throwing (a
 * genuinely corrupt blob still throws upstream and is quarantined → the profile is recomputed).
 */
fun MindBoostProfileDto.toDomain(): MindBoostProfile {
    val states = dimensions.mapNotNull { it.toDomainOrNull() }.associateBy { it.dimension }
    return MindBoostProfile(
        userId = userId,
        dimensions = states,
        overall = overall?.toDomain(),
        updatedAtMs = updatedAtMs,
    )
}

private fun DimensionState.toDto(): DimensionStateDto = DimensionStateDto(
    dimension = dimension.name,
    value = value,
    confidence = confidence,
    dataSufficiency = dataSufficiency,
    trend = trend.name,
    status = status.name,
    preliminary = preliminary,
    contributingModules = contributingModules.map { it.name },
)

private fun DimensionStateDto.toDomainOrNull(): DimensionState? {
    val parsedDimension = enumOrNull<CognitiveDimension>(dimension) ?: return null
    val trend = enumOrNull<ProfileTrend>(trend) ?: ProfileTrend.UNKNOWN
    val modules = contributingModules.mapNotNull { enumOrNull<CognitiveModuleId>(it) }
    val rawStatus = enumOrNull<DimensionStatus>(status)
        ?: if (value == null) DimensionStatus.CALIBRATING else DimensionStatus.EMERGING
    // Reconcile the value/status invariant rather than throwing on a drifted snapshot.
    val status = when {
        value == null -> DimensionStatus.CALIBRATING
        rawStatus == DimensionStatus.CALIBRATING -> DimensionStatus.EMERGING
        else -> rawStatus
    }
    return DimensionState(
        dimension = parsedDimension,
        value = value?.coerceIn(0.0, 1.0),
        confidence = confidence.coerceIn(0.0, 1.0),
        dataSufficiency = dataSufficiency.coerceIn(0.0, 1.0),
        trend = trend,
        status = status,
        preliminary = preliminary,
        contributingModules = modules,
    )
}

private fun OverallSummary.toDto(): OverallSummaryDto = OverallSummaryDto(
    compositeIndex = compositeIndex,
    establishedDimensionCount = establishedDimensionCount,
    strongest = strongest?.name,
    developing = developing?.name,
)

private fun OverallSummaryDto.toDomain(): OverallSummary = OverallSummary(
    compositeIndex = compositeIndex,
    establishedDimensionCount = establishedDimensionCount,
    strongest = strongest?.let { enumOrNull<CognitiveDimension>(it) },
    developing = developing?.let { enumOrNull<CognitiveDimension>(it) },
)

private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
    name?.let { value -> enumValues<T>().firstOrNull { it.name == value } }
