package com.mindos.app.features.mindboost.logic.data

import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureReadResult
import com.mindos.app.features.mindboost.logic.LogicTestFixtures
import com.mindos.app.features.mindboost.logic.data.local.LocalLogicTrainingRepository
import com.mindos.app.features.mindboost.logic.domain.model.LogicScoringVersion
import com.mindos.app.features.mindboost.logic.domain.model.LogicSession
import com.mindos.app.features.mindboost.logic.domain.model.LogicSessionResult
import com.mindos.app.features.mindboost.logic.domain.model.SkillState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LocalLogicTrainingRepositoryTest {

    @Test
    fun corruptBlob_isQuarantined_readsFallBackToEmpty_neverCrash() = runTest {
        // A malformed persisted blob must fall back to the empty default — never crash, never fabricate.
        val store = InMemorySecureKeyValueStore()
        val repo = LocalLogicTrainingRepository(store)
        store.put("logic_user:u1", "{ not valid logic json") // key mirrors the repo's prefix
        assertTrue(repo.recentSessions("u1", 10).isEmpty())
        assertTrue(repo.loadSkillStates("u1").isEmpty())
    }

    @Test
    fun sessionAndSkillProgressRoundTripWithVersionedPayload() = runTest {
        val store = InMemorySecureKeyValueStore()
        val repo = LocalLogicTrainingRepository(store)
        val attempt = LogicTestFixtures.attempt()
        val session = LogicSession(
            id = "u1:s1",
            userId = "u1",
            startedAt = 10,
            endedAt = 20,
            attempts = listOf(attempt),
            result = result(),
        )
        val skill = SkillState(attempt.targetSkills.first(), attemptsCount = 1, recentIcr = 90.0)

        repo.saveSession(session)
        repo.saveSkillStates("u1", listOf(skill))

        assertEquals(session, repo.recentSessions("u1", 1).single())
        assertEquals(listOf(skill), repo.loadSkillStates("u1"))
        val raw = (
            store.read("logic_user:u1") as?
                com.mindos.app.features.mindboost.focus.data.local.SecureReadResult.Value
            )
            ?.value.orEmpty()
        assertTrue(raw.contains("schemaVersion"))
        assertTrue(raw.contains("scoreBreakdown"))
    }

    @Test
    fun deleteUserDataRemovesOnlyRequestedAggregate() = runTest {
        val store = InMemorySecureKeyValueStore()
        val repo = LocalLogicTrainingRepository(store)
        repo.saveSession(session("u1"))
        repo.saveSession(session("u2"))

        repo.deleteUserData("u1")

        assertTrue(repo.recentSessions("u1", 1).isEmpty())
        assertEquals(1, repo.recentSessions("u2", 1).size)
    }

    @Test
    fun futureSchema_isQuarantinedAndNeverOverwrittenDuringRead() = runTest {
        val store = InMemorySecureKeyValueStore()
        val raw = """{"schemaVersion":99}"""
        store.put("logic_user:u1", raw)
        val repo = LocalLogicTrainingRepository(store)

        assertTrue(repo.recentSessions("u1", 1).isEmpty())

        assertEquals(SecureReadResult.Missing, store.read("logic_user:u1"))
        assertEquals(raw, store.quarantinedValue("logic_user:u1"))
        assertEquals(
            SecureReadResult.UnsupportedVersion(99),
            store.quarantineFailure("logic_user:u1"),
        )
    }

    @Test
    fun aSessionStoredWithoutAScoringVersionReadsBackAsLegacy() = runTest {
        // Simulates a blob written before the version marker existed. Two things must hold at once: it
        // stays READABLE — rejecting it would quarantine the key and destroy the user's history — and it
        // does NOT get to claim the current instrument measured it.
        val store = InMemorySecureKeyValueStore()
        val repo = LocalLogicTrainingRepository(store)
        repo.saveSession(session("u1"))

        val written = assertIs<SecureReadResult.Value>(store.read("logic_user:u1")).value
        val field = "\"scoringVersion\":${LogicScoringVersion.CURRENT}"
        // Anti-vacuity: if the encoded form never carried the field, stripping it would prove nothing.
        assertTrue(written.contains(field), "expected the encoded session to carry $field")
        val legacyBlob = written.replace(",$field", "").replace("$field,", "")
        assertTrue(!legacyBlob.contains("scoringVersion"), "the simulated legacy blob must not carry the field")
        store.put("logic_user:u1", legacyBlob)

        val reread = repo.recentSessions("u1", 1).single()
        assertEquals(LogicScoringVersion.LEGACY_PRECEDENCE_BUG, reread.result.scoringVersion)
        // The rest of the payload survived: this is a version-marker default, not a fallback to empty.
        assertEquals(90.0, reread.result.averageIcr)
    }

    private fun session(userId: String) = LogicSession(
        id = "$userId:s1",
        userId = userId,
        startedAt = 10,
        endedAt = 20,
        attempts = listOf(LogicTestFixtures.attempt()),
        result = result(),
    )

    private fun result() = LogicSessionResult(
        averageIcr = 90.0,
        inferentialAccuracy = 1.0,
        averageJustification = 1.0,
        averageConfidenceCalibration = 0.9,
        transferConsistency = null,
        topErrorTags = emptyList(),
        recommendation = "Mantener.",
        scoringVersion = LogicScoringVersion.CURRENT,
    )
}
