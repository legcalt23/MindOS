package com.mindos.app.features.mindboost.core.session.cognitive

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A lightweight projection of a sealed run, kept in the recent/block indices so listing can SORT without
 * decoding every primary (CEO slice-2 requirement). Carries exactly the fields needed to order and preview.
 */
internal data class RunIndexEntry(
    val runId: String,
    val blockId: ScoreModuleId,
    val completedAtEpochMs: Long?,
    val startedAtEpochMs: Long,
    val exerciseId: String,
    val scoreNormalized: Double?,
) {
    /** Recency key: completion time when known, else the start time. */
    fun recencyKey(): Long = completedAtEpochMs ?: startedAtEpochMs
}

/** Most-recent first by [RunIndexEntry.recencyKey], with a stable final tiebreak by runId. */
internal val RUN_RECENCY_COMPARATOR: Comparator<RunIndexEntry> =
    compareByDescending<RunIndexEntry> { it.recencyKey() }.thenBy { it.runId }

internal fun CognitiveSessionRun.toIndexEntry(): RunIndexEntry = RunIndexEntry(
    runId = sessionId,
    blockId = blockId,
    completedAtEpochMs = completedAtEpochMs,
    startedAtEpochMs = startedAtEpochMs,
    exerciseId = exerciseId,
    scoreNormalized = score.normalized,
)

@Serializable
internal data class RunIndexEntryDto(
    val runId: String,
    val blockId: String,
    val completedAtEpochMs: Long?,
    val startedAtEpochMs: Long,
    val exerciseId: String,
    val scoreNormalized: Double?,
)

/** Versioned envelope for an index's entry list. */
@Serializable
internal data class RunIndexDto(
    val schemaVersion: Int = SCHEMA_VERSION,
    val entries: List<RunIndexEntryDto>,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

private fun RunIndexEntry.toDto(): RunIndexEntryDto =
    RunIndexEntryDto(runId, blockId.name, completedAtEpochMs, startedAtEpochMs, exerciseId, scoreNormalized)

private fun RunIndexEntryDto.toDomain(): RunIndexEntry = RunIndexEntry(
    runId = runId,
    blockId = ScoreModuleId.valueOf(blockId),
    completedAtEpochMs = completedAtEpochMs,
    startedAtEpochMs = startedAtEpochMs,
    exerciseId = exerciseId,
    scoreNormalized = scoreNormalized,
)

internal fun encodeIndex(json: Json, entries: List<RunIndexEntry>): String =
    json.encodeToString(RunIndexDto.serializer(), RunIndexDto(entries = entries.map { it.toDto() }))

/**
 * Decodes an index, dropping only entries that individually fail to parse (an unknown blockId enum) — a corrupt
 * index never crashes a list. Returns an empty list if the whole envelope is unreadable; the caller's reconcile
 * can rebuild it from the primary rows.
 */
internal fun decodeIndex(json: Json, raw: String): List<RunIndexEntry> {
    val dto = json.decodeFromString(RunIndexDto.serializer(), raw)
    if (dto.schemaVersion > RunIndexDto.SCHEMA_VERSION) return emptyList()
    return dto.entries.mapNotNull { entry -> runCatching { entry.toDomain() }.getOrNull() }
}
