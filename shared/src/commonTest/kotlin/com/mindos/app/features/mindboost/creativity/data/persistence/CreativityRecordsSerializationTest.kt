package com.mindos.app.features.mindboost.creativity.data.persistence

import com.mindos.app.features.mindboost.creativity.domain.model.CreativityExerciseId
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityPracticeLabel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreativityRecordsSerializationTest {

    private val json = CreativityJson.instance

    private fun sampleSession() = PersistedCreativitySessionDto(
        session = CreativitySessionRecord(
            sessionId = "s1",
            userId = "u1",
            exerciseId = CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA,
            createdAtMs = 1_000,
            updatedAtMs = 2_000,
        ),
        prompts = listOf(
            CreativityPromptRecord("p1", "s1", CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA, "A brick"),
        ),
        responses = listOf(
            CreativityResponseRecord("r1", "s1", "p1", listOf("doorstop", "paperweight"), 4_200),
        ),
        rubric = CreativityRubricRecord("s1", 1, mapOf("fluency" to 2.0), CreativityPracticeLabel.STEADY_PRACTICE),
    )

    @Test
    fun aggregateRoundTripsLosslessly() {
        val original = sampleSession()
        val decoded = json.decodeFromString<PersistedCreativitySessionDto>(json.encodeToString(original))
        assertEquals(original, decoded)
    }

    @Test
    fun schemaVersionIsAlwaysEncoded() {
        val encoded = json.encodeToString(sampleSession())
        assertTrue(encoded.contains("\"schemaVersion\""))
    }

    @Test
    fun sourceDefaultsToLocal() {
        assertEquals(CreativityRecordSource.LOCAL, sampleSession().session.source)
    }

    @Test
    fun unknownKeysAreIgnoredOnRead() {
        val withExtra = """
            {"schemaVersion":1,"unknownFutureField":true,
             "session":{"sessionId":"s1","userId":"u1","exerciseId":"EX01_SUPPRESS_FIRST_IDEA",
             "createdAtMs":1,"updatedAtMs":2,"source":"LOCAL"}}
        """.trimIndent()
        val decoded = json.decodeFromString<PersistedCreativitySessionDto>(withExtra)
        assertEquals("s1", decoded.session.sessionId)
    }
}
