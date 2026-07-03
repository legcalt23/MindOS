package com.mindos.app.features.mindboost.memory.data.mapper

import com.mindos.app.features.mindboost.memory.data.dto.DeferredRecallOutcomeDto
import com.mindos.app.features.mindboost.memory.data.dto.DeferredRecordLogDto
import com.mindos.app.features.mindboost.memory.domain.scoring.DeferredRecallOutcome
import com.mindos.app.features.mindboost.memory.domain.scoring.DeferredRecordLog

/**
 * DTO ↔ domain mappers for the EX3 delayed-recall log (P3). Own file (cohesion + the persistence-mapper
 * file's function budget). Fields 1:1; bounded retention lives in [DeferredRecordLog.append], not here.
 */

fun DeferredRecallOutcome.toDto() = DeferredRecallOutcomeDto(
    studiedCount = studiedCount,
    hits = hits,
    distractorCount = distractorCount,
    falseAlarms = falseAlarms,
)

fun DeferredRecallOutcomeDto.toDomain() = DeferredRecallOutcome(
    studiedCount = studiedCount,
    hits = hits,
    distractorCount = distractorCount,
    falseAlarms = falseAlarms,
)

fun DeferredRecordLog.toDto() = DeferredRecordLogDto(outcomes = outcomes.map { it.toDto() })

fun DeferredRecordLogDto.toDomain() = DeferredRecordLog(outcomes = outcomes.map { it.toDomain() })
