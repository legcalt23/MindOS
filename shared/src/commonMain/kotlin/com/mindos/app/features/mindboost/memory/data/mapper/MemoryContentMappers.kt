package com.mindos.app.features.mindboost.memory.data.mapper

import com.mindos.app.features.mindboost.memory.data.dto.DeferredRecallItemDto
import com.mindos.app.features.mindboost.memory.data.dto.DeferredRecallItemsDto
import com.mindos.app.features.mindboost.memory.data.dto.RecentItemRegistryDto
import com.mindos.app.features.mindboost.memory.domain.content.model.Language
import com.mindos.app.features.mindboost.memory.domain.content.model.PairId
import com.mindos.app.features.mindboost.memory.domain.content.model.WordId
import com.mindos.app.features.mindboost.memory.domain.content.sampling.RecentItemRegistry
import com.mindos.app.features.mindboost.memory.domain.deferred.DeferredRecallConfig
import com.mindos.app.features.mindboost.memory.domain.deferred.DeferredRecallItem
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId

/**
 * DTO ↔ domain mappers for content-side persistence: deferred-recall items and the
 * recent-item registry. Same conventions as MemoryPersistenceMappers.kt (enums by
 * name; decode failures are caught by the repository).
 */

fun DeferredRecallItem.toDto() = DeferredRecallItemDto(
    id = id,
    exerciseId = exerciseId.name,
    language = language.name,
    studiedWordIds = studiedWordIds.map { it.value },
    studiedAtEpoch = studiedAtEpoch,
    minDelayMillis = config.minDelayMillis,
    idealDelayMillis = config.idealDelayMillis,
    maxWindowMillis = config.maxWindowMillis,
)

fun DeferredRecallItemDto.toDomain() = DeferredRecallItem(
    id = id,
    exerciseId = MemoryExerciseId.valueOf(exerciseId),
    language = Language.valueOf(language),
    studiedWordIds = studiedWordIds.map { WordId(it) },
    studiedAtEpoch = studiedAtEpoch,
    config = DeferredRecallConfig(minDelayMillis, idealDelayMillis, maxWindowMillis),
)

fun List<DeferredRecallItem>.toDto() = DeferredRecallItemsDto(items = map { it.toDto() })

fun DeferredRecallItemsDto.toDomain(): List<DeferredRecallItem> = items.map { it.toDomain() }

fun RecentItemRegistry.toDto() = RecentItemRegistryDto(
    exerciseId = exerciseId.name,
    wordUsage = wordUsage.entries.associate { it.key.value to it.value },
    pairUsage = pairUsage.entries.associate { it.key.value to it.value },
    ttlMillis = ttlMillis,
)

fun RecentItemRegistryDto.toDomain() = RecentItemRegistry(
    exerciseId = MemoryExerciseId.valueOf(exerciseId),
    wordUsage = wordUsage.entries.associate { WordId(it.key) to it.value },
    pairUsage = pairUsage.entries.associate { PairId(it.key) to it.value },
    ttlMillis = ttlMillis,
)
