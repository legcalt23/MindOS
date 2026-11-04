package com.mindos.app.features.mindboost.creativity.data.export

import com.mindos.app.features.mindboost.creativity.data.persistence.CreativityJson
import com.mindos.app.features.mindboost.creativity.data.persistence.CreativityResponseRecord
import com.mindos.app.features.mindboost.creativity.data.persistence.CreativityRubricRecord
import com.mindos.app.features.mindboost.creativity.data.persistence.CreativitySessionRecord
import com.mindos.app.features.mindboost.creativity.data.persistence.PersistedCreativitySessionDto
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityExerciseId
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityPracticeLabel
import com.mindos.app.features.mindboost.core.copy.ForbiddenLexicon
import com.mindos.app.features.mindboost.core.copy.MindBoostCopySafetyScanner
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
        // `MB-EMO-0029`: these two lines used to assert that the disclaimer CONTAINED the denial of
        // a health claim, so the shipped copy could not be fixed without changing the test pinning
        // it. What the disclaimer exists to do is bound the reading without naming a claim at all —
        // that property is asserted now, not the literal.
        assertTrue(
            MindBoostCopySafetyScanner.scan(decoded.disclaimer, ForbiddenLexicon.ENGLISH_CLAIMS).isEmpty(),
            "exported disclaimer names a forbidden claim: ${decoded.disclaimer}",
        )
        assertTrue(decoded.disclaimer.contains("Indicative only"), "disclaimer must bound the reading")
    }

    @Test
    fun csvHasDisclaimerHeaderAndOneRowPerSession() {
        val csv = CreativityExporter.toCsv(listOf(sample("s1"), sample("s2")))
        val lines = csv.split("\n")
        assertEquals("# ${CreativityExport.DISCLAIMER}", lines[0])
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
