package com.mindos.app.features.mindboost.memory.data.dto

import kotlinx.serialization.Serializable

/**
 * `@Serializable` persistence DTOs for MindBoost · Memoria. Every aggregate carries
 * a [schemaVersion] (project learning 2026-06-05) so old payloads can be migrated;
 * combined with `ignoreUnknownKeys`/`encodeDefaults` (MemoryJson) reads stay
 * forward-compatible. Enums are stored by NAME for stability.
 *
 * Only summaries/state are persisted — never raw trials.
 */

@Serializable
data class CmeSensitivityDto(
    val alpha: Double = 1.0,
    val beta: Double = 1.0,
    val gamma: Double = 1.0,
    val delta: Double = 1.0,
    val epsilon: Double = 1.0,
)

@Serializable
data class DifficultyVectorDto(
    val load: Double = 0.0,
    val interference: Double = 0.0,
    val delay: Double = 0.0,
    val similarity: Double = 0.0,
    val depth: Double = 0.0,
    val retrieval: Double = 0.0,
    val context: Double = 0.0,
    val concurrency: Double = 0.0,
)

@Serializable
data class MemoryProfileStateDto(
    val schemaVersion: Int = SCHEMA_VERSION,
    val userId: String,
    val archetype: String,
    val sensitivity: CmeSensitivityDto = CmeSensitivityDto(),
    val difficultyByExercise: Map<String, DifficultyVectorDto> = emptyMap(),
    val currentCycleWeek: Int = 1,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

@Serializable
data class MemorySessionSummaryDto(
    val schemaVersion: Int = SCHEMA_VERSION,
    val sessionId: String,
    val userId: String,
    val focusExerciseId: String,
    val endedAtEpoch: Long,
    val immediateAccuracy: Double? = null,
    val delayedAccuracy: Double? = null,
    val performance: Double,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

@Serializable
data class DeferredRecallItemDto(
    val schemaVersion: Int = SCHEMA_VERSION,
    val id: String,
    val exerciseId: String,
    val language: String,
    val studiedWordIds: List<String>,
    val studiedAtEpoch: Long,
    val minDelayMillis: Long,
    val idealDelayMillis: Long,
    val maxWindowMillis: Long,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

@Serializable
data class DeferredRecallItemsDto(
    val schemaVersion: Int = SCHEMA_VERSION,
    val items: List<DeferredRecallItemDto> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

@Serializable
data class RecentItemRegistryDto(
    val schemaVersion: Int = SCHEMA_VERSION,
    val exerciseId: String,
    val wordUsage: Map<String, Long> = emptyMap(),
    val pairUsage: Map<String, Long> = emptyMap(),
    val ttlMillis: Long,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/** One persisted complex-span trial summary (EX1, P4). Compact — never raw trial data. */
@Serializable
data class SpanTrialDto(
    val sequenceLength: Int,
    val processingCorrect: Boolean,
    val serialCorrectItems: Int,
    val intrusions: Int = 0,
)

/** The accumulated, bounded EX1 span log for one user (trials + processing-response window). */
@Serializable
data class SpanRecordLogDto(
    val schemaVersion: Int = SCHEMA_VERSION,
    val trials: List<SpanTrialDto> = emptyList(),
    val processingResponses: List<Int> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/** One persisted paired-associates recognition trial summary (EX4, P2b). Compact — never raw trial data. */
@Serializable
data class PairsTrialDto(
    val targetCount: Int,
    val hits: Int,
    val distractorCount: Int,
    val falseAlarms: Int,
)

/** The accumulated, bounded EX4 paired-associates log for one user. */
@Serializable
data class PairsRecordLogDto(
    val schemaVersion: Int = SCHEMA_VERSION,
    val trials: List<PairsTrialDto> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/** One persisted 24h delayed-recall outcome (EX3, P3). Compact — never raw trial data. */
@Serializable
data class DeferredRecallOutcomeDto(
    val studiedCount: Int,
    val hits: Int,
    val distractorCount: Int,
    val falseAlarms: Int,
)

/** The accumulated, bounded EX3 delayed-recall log for one user (scored test outcomes). */
@Serializable
data class DeferredRecordLogDto(
    val schemaVersion: Int = SCHEMA_VERSION,
    val outcomes: List<DeferredRecallOutcomeDto> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}
