package com.mindos.app.features.mindboost.memory.data.mapper

import com.mindos.app.features.mindboost.memory.data.dto.PairsRecordLogDto
import com.mindos.app.features.mindboost.memory.data.dto.PairsTrialDto
import com.mindos.app.features.mindboost.memory.domain.scoring.PairsRecordLog
import com.mindos.app.features.mindboost.memory.domain.scoring.PairsTrial

/**
 * DTO ↔ domain mappers for the EX4 paired-associates log (P2b). Kept in their own file (cohesion + the
 * persistence-mapper file's function budget). Fields are 1:1; the bounded retention lives in
 * [PairsRecordLog.append], not here.
 */

fun PairsTrial.toDto() = PairsTrialDto(
    targetCount = targetCount,
    hits = hits,
    distractorCount = distractorCount,
    falseAlarms = falseAlarms,
)

fun PairsTrialDto.toDomain() = PairsTrial(
    targetCount = targetCount,
    hits = hits,
    distractorCount = distractorCount,
    falseAlarms = falseAlarms,
)

fun PairsRecordLog.toDto() = PairsRecordLogDto(trials = trials.map { it.toDto() })

fun PairsRecordLogDto.toDomain() = PairsRecordLog(trials = trials.map { it.toDomain() })
