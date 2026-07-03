// Flat persistence DTOs; their many-field constructors are the data shape, not a code
// smell (same rationale as SpeedRecords). LongParameterList is suppressed file-wide.
@file:Suppress("LongParameterList")

package com.mindos.app.features.mindboost.creativity.data.persistence

import com.mindos.app.features.mindboost.creativity.domain.model.CreativityExerciseId
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityPracticeLabel
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityPromptSource
import kotlinx.serialization.Serializable

/** Provenance marker: every persisted creativity record is produced locally / offline. */
@Serializable
enum class CreativityRecordSource { LOCAL }

@Serializable
data class CreativitySessionRecord(
    val sessionId: String,
    val userId: String,
    val exerciseId: CreativityExerciseId,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val source: CreativityRecordSource = CreativityRecordSource.LOCAL,
)

@Serializable
data class CreativityPromptRecord(
    val promptId: String,
    val sessionId: String,
    val exerciseId: CreativityExerciseId,
    val text: String,
    val origin: CreativityPromptSource = CreativityPromptSource.LOCAL_STATIC,
)

@Serializable
data class CreativityResponseRecord(
    val responseId: String,
    val sessionId: String,
    val promptId: String,
    val ideas: List<String>,
    val elapsedMonotonicMs: Long,
)

@Serializable
data class CreativityRubricRecord(
    val sessionId: String,
    val rubricVersion: Int,
    val signals: Map<String, Double>,
    val summary: CreativityPracticeLabel,
    /** Always false: no AI-derived field is ever persisted in the MVP. */
    val aiDerived: Boolean = false,
)

/**
 * The stored blob shape: one persisted creativity practice session (mirror of `PersistedRunDto`).
 * Logical FKs are by id (sessionId / promptId). `schemaVersion` lets the format migrate without
 * losing old data.
 */
@Serializable
data class PersistedCreativitySessionDto(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val session: CreativitySessionRecord,
    val prompts: List<CreativityPromptRecord> = emptyList(),
    val responses: List<CreativityResponseRecord> = emptyList(),
    val rubric: CreativityRubricRecord? = null,
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}
