package com.mindos.app.features.mindboost.focus.data.local

import com.mindos.app.features.mindboost.focus.domain.evaluation.ProbeBank
import com.mindos.app.features.mindboost.focus.domain.model.BaselineSnapshot
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.SessionId
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.model.UserArchetype
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalFocusRepositoriesTest {

    private fun session(userId: String, endedAtMs: Long) = SessionResult(
        sessionId = SessionId("$userId:$endedAtMs"),
        mode = Mode.TRAINING,
        exerciseFocus = ExerciseId.E1_VIGIA,
        blocks = emptyList(),
        startedAtMs = 0,
        endedAtMs = endedAtMs,
        sessionMetrics = emptyMap(),
    )

    @Test
    fun corruptBlobs_areQuarantined_loadNullAndListsEmpty_neverCrash() = runTest {
        // Malformed profile/session blobs must load as null / be skipped — never crash, never fabricate.
        val store = InMemorySecureKeyValueStore()
        store.put("profile:user", "{ not valid focus json") // keys mirror FocusKeys prefixes
        store.put("session:user:5", "{ garbage session")
        assertNull(LocalFocusProfileRepository(store).load("user"))
        assertTrue(LocalSessionHistoryRepository(store).recent("user", 10).isEmpty())
    }

    @Test
    fun profile_savesAndReloads() = runTest {
        val repo = LocalFocusProfileRepository(InMemorySecureKeyValueStore())
        val profile = UserCognitiveProfile(
            userId = "user",
            archetype = UserArchetype.ADVANCED,
            totalSessions = 4,
            cycleWeek = 2,
            updatedAtMs = 1_000,
        )
        repo.save(profile)
        assertEquals(profile, repo.load("user"))
    }

    @Test
    fun profile_returnsNull_whenAbsent() = runTest {
        val repo = LocalFocusProfileRepository(InMemorySecureKeyValueStore())
        assertNull(repo.load("nobody"))
    }

    @Test
    fun baseline_savesAndReloads() = runTest {
        val repo = LocalBaselineRepository(InMemorySecureKeyValueStore())
        val snapshot = BaselineSnapshot(userId = "user", perMetric = emptyMap(), capturedAtMs = 7)
        repo.save(snapshot)
        assertEquals(snapshot, repo.load("user"))
    }

    @Test
    fun sessions_returnNewestFirst_andRespectLimit() = runTest {
        val repo = LocalSessionHistoryRepository(InMemorySecureKeyValueStore())
        repo.append(session("user", 100))
        repo.append(session("user", 300))
        repo.append(session("user", 200))

        val recent = repo.recent("user", limit = 2)
        assertEquals(listOf(300L, 200L), recent.map { it.endedAtMs })
    }

    @Test
    fun recent_rejectsNonPositiveLimit() = runTest {
        val repo = LocalSessionHistoryRepository(InMemorySecureKeyValueStore())
        assertFailsWith<IllegalArgumentException> { repo.recent("user", 0) }
    }

    @Test
    fun probeResults_areStoredSeparatelyFromTrainingHistory() = runTest {
        val store = InMemorySecureKeyValueStore() // shared store, separated by key prefix
        val sessions = LocalSessionHistoryRepository(store)
        val probes = LocalProbeBankRepository(store)

        sessions.append(session("user", 100))
        probes.recordProbe("user", session("user", 150))

        // Training history sees only the training session; probe history only the probe.
        assertEquals(1, sessions.recent("user", 10).size)
        assertEquals(1, probes.probeHistory("user", 10).size)
        assertTrue(sessions.recent("user", 10).none { it.endedAtMs == 150L })
    }

    @Test
    fun probeScheduler_persistsRotationCursor() = runTest {
        val repo = LocalProbeBankRepository(InMemorySecureKeyValueStore(), probes = listOf(ProbeBank.VIGILANCE_PROBE))
        val first = repo.nextProbe("user", week = 1)
        val second = repo.nextProbe("user", week = 1)
        assertNotEquals(first.formSeed, second.formSeed)
    }
}
