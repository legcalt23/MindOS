package com.mindos.app.features.mindboost.memory.data.mapper

import com.mindos.app.features.mindboost.memory.data.dto.SpanRecordLogDto
import com.mindos.app.features.mindboost.memory.data.dto.SpanTrialDto
import com.mindos.app.features.mindboost.memory.domain.scoring.SpanRecordLog
import com.mindos.app.features.mindboost.memory.domain.scoring.SpanTrial

/**
 * DTO ↔ domain mappers for the EX1 complex-span log (P4). Kept in their own file (cohesion + the
 * persistence-mapper file's function budget). Fields are 1:1; the bounded retention lives in
 * [SpanRecordLog.append], not here.
 */

fun SpanTrial.toDto() = SpanTrialDto(
    sequenceLength = sequenceLength,
    processingCorrect = processingCorrect,
    serialCorrectItems = serialCorrectItems,
    intrusions = intrusions,
)

fun SpanTrialDto.toDomain() = SpanTrial(
    sequenceLength = sequenceLength,
    processingCorrect = processingCorrect,
    serialCorrectItems = serialCorrectItems,
    intrusions = intrusions,
)

fun SpanRecordLog.toDto() = SpanRecordLogDto(
    trials = trials.map { it.toDto() },
    processingResponses = processingResponses,
)

fun SpanRecordLogDto.toDomain() = SpanRecordLog(
    trials = trials.map { it.toDomain() },
    processingResponses = processingResponses,
)
