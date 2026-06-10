package com.mindos.app.features.mindboost.memory.data.mapper

import com.mindos.app.features.mindboost.memory.data.dto.CmeSensitivityDto
import com.mindos.app.features.mindboost.memory.data.dto.DifficultyVectorDto
import com.mindos.app.features.mindboost.memory.data.dto.MemoryProfileStateDto
import com.mindos.app.features.mindboost.memory.data.dto.MemorySessionSummaryDto
import com.mindos.app.features.mindboost.memory.domain.model.AxisLevel
import com.mindos.app.features.mindboost.memory.domain.model.CmeSensitivity
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyVector
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.domain.model.MemoryProfileState
import com.mindos.app.features.mindboost.memory.domain.model.MemorySessionSummary
import com.mindos.app.features.mindboost.memory.domain.personalization.MemoryArchetype

/**
 * DTO ↔ domain mappers for profile/session state. `toDomain` clamps numeric ranges
 * so a corrupt or out-of-range persisted value is tolerated, and resolves enums by
 * name (an unknown name throws — the repository wraps decode in `runCatching` and
 * yields null). Content/registry mappers live in MemoryContentMappers.kt.
 */

fun CmeSensitivity.toDto() = CmeSensitivityDto(alpha, beta, gamma, delta, epsilon)

fun CmeSensitivityDto.toDomain() = CmeSensitivity(alpha, beta, gamma, delta, epsilon)

fun MemoryDifficultyVector.toDto() = DifficultyVectorDto(
    load = load.value,
    interference = interference.value,
    delay = delay.value,
    similarity = similarity.value,
    depth = depth.value,
    retrieval = retrieval.value,
    context = context.value,
    concurrency = concurrency.value,
)

fun DifficultyVectorDto.toDomain() = MemoryDifficultyVector(
    load = AxisLevel.clamped(load),
    interference = AxisLevel.clamped(interference),
    delay = AxisLevel.clamped(delay),
    similarity = AxisLevel.clamped(similarity),
    depth = AxisLevel.clamped(depth),
    retrieval = AxisLevel.clamped(retrieval),
    context = AxisLevel.clamped(context),
    concurrency = AxisLevel.clamped(concurrency),
)

fun MemoryProfileState.toDto() = MemoryProfileStateDto(
    userId = userId,
    archetype = archetype.name,
    sensitivity = sensitivity.toDto(),
    difficultyByExercise = difficultyByExercise.entries.associate { it.key.name to it.value.toDto() },
    currentCycleWeek = currentCycleWeek,
)

fun MemoryProfileStateDto.toDomain() = MemoryProfileState(
    userId = userId,
    archetype = MemoryArchetype.valueOf(archetype),
    sensitivity = sensitivity.toDomain(),
    difficultyByExercise = difficultyByExercise.entries
        .associate { MemoryExerciseId.valueOf(it.key) to it.value.toDomain() },
    currentCycleWeek = currentCycleWeek,
)

fun MemorySessionSummary.toDto() = MemorySessionSummaryDto(
    sessionId = sessionId,
    userId = userId,
    focusExerciseId = focusExerciseId.name,
    endedAtEpoch = endedAtEpoch,
    immediateAccuracy = immediateAccuracy,
    delayedAccuracy = delayedAccuracy,
    performance = performance,
)

fun MemorySessionSummaryDto.toDomain() = MemorySessionSummary(
    sessionId = sessionId,
    userId = userId,
    focusExerciseId = MemoryExerciseId.valueOf(focusExerciseId),
    endedAtEpoch = endedAtEpoch,
    immediateAccuracy = immediateAccuracy,
    delayedAccuracy = delayedAccuracy,
    performance = performance,
)
