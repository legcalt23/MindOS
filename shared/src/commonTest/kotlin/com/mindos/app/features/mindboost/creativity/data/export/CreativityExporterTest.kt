package com.mindos.app.features.mindboost.creativity.data.export

import com.mindos.app.features.mindboost.creativity.data.persistence.CreativityJson
import com.mindos.app.features.mindboost.creativity.data.persistence.CreativityResponseRecord
import com.mindos.app.features.mindboost.creativity.data.persistence.CreativityRubricRecord
import com.mindos.app.features.mindboost.creativity.data.persistence.CreativitySessionRecord
import com.mindos.app.features.mindboost.creativity.data.persistence.PersistedCreativitySessionDto
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityExerciseId
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityPracticeLabel
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CreativityExporterTest {

    private fun sample(sessionId: String) = PersistedCreativitySessionDto(
        session = CreativitySessionRecord(
            sessionId = sessionId,
            userId = "u1",
            exerciseId = CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA,
            createdAtMs = 1_000,
            updatedAtMs = 1_000,
        ),
        responses = listOf(
            CreativityResponseRecord("r1", sessionId, "p1", listOf("a", "b", "c"), 4_000),
        ),
        rubric = CreativityRubricRecord(
            sessionId = sessionId,
            rubricVersion = 1,
            signals = mapOf("fluency" to 3.0, "elaboration" to 1.0),
            summary = CreativityPracticeLabel.STEADY_PRACTICE,
        ),
    )

    @Test
    fun jsonRoundTripsAndCarriesSafeMetadata() {
        val json = CreativityExporter.toJson(listOf(sample("s1")))
        val decoded = CreativityJson.instance.decodeFromString<CreativityExport>(json)
        assertEquals(1, decoded.sessions.size)
        assertFalse(decoded.aiDerived)
        assertTrue(decoded.disclaimer.contains("Not a diagnosis"))
        assertTrue(decoded.disclaimer.contains("Functional proxy"))
    }

    @Test
    fun csvHasDisclaimerHeaderAndOneRowPerSession() {
        val csv = CreativityExporter.toCsv(listOf(sample("s1"), sample("s2")))
        val lines = csv.split("\n")
        assertTrue(lines[0].startsWith("# "))
        assertTrue(lines[0].contains("Not a diagnosis"))
        assertEquals("sessionId,exerciseId,createdAtMs,ideaCount,fluency,elaboration,label", lines[1])
        assertEquals(4, lines.size) // disclaimer + header + 2 rows
        assertTrue(lines[2].startsWith("s1,EX01_SUPPRESS_FIRST_IDEA,1000,3,"))
    }

    @Test
    fun emptyHistoryProducesHeaderOnlyCsvAndEmptyJsonSessions() {
        val csv = CreativityExporter.toCsv(emptyList())
        assertEquals(2, csv.split("\n").size) // disclaimer + header only
        val decoded = CreativityJson.instance.decodeFromString<CreativityExport>(
            CreativityExporter.toJson(emptyList()),
        )
        assertTrue(decoded.sessions.isEmpty())
    }
}
