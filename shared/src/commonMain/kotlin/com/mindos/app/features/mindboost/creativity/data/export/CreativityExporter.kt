package com.mindos.app.features.mindboost.creativity.data.export

import com.mindos.app.features.mindboost.creativity.data.persistence.CreativityJson
import com.mindos.app.features.mindboost.creativity.data.persistence.PersistedCreativitySessionDto
import com.mindos.app.features.mindboost.creativity.domain.scoring.CreativityRubricScorer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/**
 * Envelope for a user-initiated, LOCAL export of creativity practice history. Carries an explicit
 * non-clinical disclaimer and an evidence-gates-pending note, and never claims an AI-derived score.
 */
@Serializable
data class CreativityExport(
    val format: String = FORMAT,
    val schemaVersion: Int = SCHEMA_VERSION,
    val disclaimer: String = DISCLAIMER,
    val aiDerived: Boolean = false,
    val sessions: List<PersistedCreativitySessionDto>,
) {
    companion object {
        const val FORMAT = "creativity-practice-export"
        const val SCHEMA_VERSION = 1
        const val DISCLAIMER =
            "Local practice export. Not a diagnosis. Functional proxy, not a graded test. Evidence gates pending."
    }
}

/**
 * Turns local practice history into a JSON or CSV string. Pure and deterministic — it only
 * formats already-stored data and performs NO network, NO upload, NO AI. The caller (UI) decides
 * when to invoke it and where the resulting string goes (local share/save).
 */
object CreativityExporter {

    private const val MISSING_SIGNAL = 0.0

    fun toJson(sessions: List<PersistedCreativitySessionDto>): String =
        CreativityJson.instance.encodeToString(CreativityExport(sessions = sessions))

    fun toCsv(sessions: List<PersistedCreativitySessionDto>): String {
        val rows = sessions.map { dto ->
            val session = dto.session
            val ideaCount = dto.responses.sumOf { it.ideas.size }
            val fluency = dto.rubric?.signals?.get(CreativityRubricScorer.SIGNAL_FLUENCY) ?: MISSING_SIGNAL
            val elaboration = dto.rubric?.signals?.get(CreativityRubricScorer.SIGNAL_ELABORATION) ?: MISSING_SIGNAL
            val label = dto.rubric?.summary?.name ?: ""
            listOf(
                session.sessionId,
                session.exerciseId.name,
                session.createdAtMs,
                ideaCount,
                fluency,
                elaboration,
                label,
            ).joinToString(",")
        }
        return (listOf("# ${CreativityExport.DISCLAIMER}", CSV_HEADER) + rows).joinToString("\n")
    }

    private const val CSV_HEADER =
        "sessionId,exerciseId,createdAtMs,ideaCount,fluency,elaboration,label"
}
