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
import kotlin.test.assertFalse
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

    @Test
    fun probeRelease_givesBackExactlyTheLastBurn_andNeverMore() = runTest {
        val probeId = ProbeBank.VIGILANCE_PROBE.probeId
        val repo = LocalProbeBankRepository(InMemorySecureKeyValueStore(), probes = listOf(ProbeBank.VIGILANCE_PROBE))

        // Released before the user saw it: the SAME form comes back, so an abort costs no supply.
        val burned = repo.nextProbe("user", week = 1)
        repo.releaseProbe("user", probeId)
        assertEquals(burned.formSeed, repo.nextProbe("user", week = 1).formSeed)

        // ...but exactly ONE burn: a release must not rewind into a form already administered.
        val second = repo.nextProbe("user", week = 1)
        assertNotEquals(burned.formSeed, second.formSeed)
        repo.releaseProbe("user", probeId)
        assertEquals(second.formSeed, repo.nextProbe("user", week = 1).formSeed)

        // A release with no burn behind it is a floor, not a reset: the cursor cannot go negative and
        // hand back a form the user has already seen on a later wrap.
        val fresh = LocalProbeBankRepository(InMemorySecureKeyValueStore(), probes = listOf(ProbeBank.VIGILANCE_PROBE))
        fresh.releaseProbe("user", probeId)
        assertEquals(ProbeBank.VIGILANCE_PROBE.reservedForms.first(), fresh.nextProbe("user", week = 1).formSeed)
    }

    @Test
    fun scheduler_declaresTheWrap_whenTheReservedSupplyComesRoundAgain() = runTest {
        val probe = ProbeBank.VIGILANCE_PROBE
        val repo = LocalProbeBankRepository(InMemorySecureKeyValueStore(), probes = listOf(probe))

        // Every reserved form gets a first showing, and none of them may be reported as a repeat.
        val served = List(probe.reservedForms.size) { repo.nextProbe("user", week = 1) }
        assertTrue(served.none { it.repeatsEarlierForm }, "a first showing must never be reported as a repeat")
        assertEquals(probe.reservedForms.size, served.map { it.formSeed }.toSet().size)

        // The supply is finite, so the next administration comes round to the FIRST form again.
        val wrapped = repo.nextProbe("user", week = 1)
        assertTrue(wrapped.repeatsEarlierForm, "the wrap must be declared, not silent")
        // Discriminating half: the flag is not decorative — it names a form the user has really been given.
        assertEquals(served.first().formSeed, wrapped.formSeed)
    }

    @Test
    fun aReleasedFormWasNeverAdministered_soItDoesNotCountTowardTheWrap() = runTest {
        val probe = ProbeBank.VIGILANCE_PROBE
        val repo = LocalProbeBankRepository(InMemorySecureKeyValueStore(), probes = listOf(probe))

        // Burned and given back: the user never saw it, so it must not push them toward a wrap.
        repo.nextProbe("user", week = 1)
        repo.releaseProbe("user", probe.probeId)

        val served = List(probe.reservedForms.size) { repo.nextProbe("user", week = 1) }
        assertTrue(served.none { it.repeatsEarlierForm }, "a released burn is not a form the user was given")
        assertTrue(repo.nextProbe("user", week = 1).repeatsEarlierForm)
    }
    /**
     * P1 found by the R13 exit gate. The cursor is the single owner of "which reserved forms this user has
     * already been served" — it cannot be re-derived from anything — so an unreadable one must not collapse
     * into 0. Collapsing produces the MOST favourable value there is ("nothing served"), which re-serves the
     * baseline's own form stamped as fresh: the disclosure clause is dropped from the copy and the R13
     * freshness gate passes on a fact nobody measured.
     */
    @Test
    fun anUnreadableCursorIsNeverReportedAsFreshMaterial() = runTest {
        val probe = ProbeBank.VIGILANCE_PROBE
        val store = InMemorySecureKeyValueStore()
        val repo = LocalProbeBankRepository(store, probes = listOf(probe))

        // One real burn, so the key under test is the one PRODUCTION writes, not one this test invented.
        repo.nextProbe("user", week = 1)
        val cursorKey = store.keys("").single()

        // Discriminating half: while the cursor reads, the second form really is fresh and says so.
        assertFalse(repo.nextProbe("user", week = 1).repeatsEarlierForm)

        store.simulateReadFailure(cursorKey, SecureReadResult.KeyInvalidated)
        assertTrue(
            repo.nextProbe("user", week = 1).repeatsEarlierForm,
            "a cursor that cannot be read cannot claim the form is new",
        )
        // ...and the entry SURVIVES: quarantine would delete the only record of what was served.
        assertNull(store.quarantinedValue(cursorKey), "the cursor must not be quarantined away")
        assertTrue(store.read(cursorKey) is SecureReadResult.Failure, "the entry must still be there")

        // A stored value that does not parse is the same fact: unreadable, not absent.
        store.put(cursorKey, "not-a-number")
        assertTrue(repo.nextProbe("user", week = 1).repeatsEarlierForm, "a corrupt cursor fails closed too")
        assertNull(store.quarantinedValue(cursorKey))
    }

    @Test
    fun aReleaseCannotRewindACursorItCouldNotRead() = runTest {
        val probe = ProbeBank.VIGILANCE_PROBE
        val store = InMemorySecureKeyValueStore()
        val repo = LocalProbeBankRepository(store, probes = listOf(probe))
        repo.nextProbe("user", week = 1)
        val cursorKey = store.keys("").single()

        store.simulateReadFailure(cursorKey, SecureReadResult.KeyInvalidated)
        repo.releaseProbe("user", probe.probeId)

        // Writing a fabricated position here would hand back a form the user has already been shown.
        // A put() would clear the simulated failure, so a surviving Failure proves nothing was written.
        assertTrue(store.read(cursorKey) is SecureReadResult.Failure, "release must not overwrite it")
        assertNull(store.quarantinedValue(cursorKey))
    }
}
