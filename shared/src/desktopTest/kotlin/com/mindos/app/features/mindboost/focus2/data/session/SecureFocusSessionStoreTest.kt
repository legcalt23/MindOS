package com.mindos.app.features.mindboost.focus2.data.session

import com.mindos.app.features.mindboost.core.runtime.RngVersion
import com.mindos.app.features.mindboost.core.runtime.SeedSource
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.core.session.purpose.SessionMeasurementPurpose
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureReadResult
import com.mindos.app.features.mindboost.focus2.domain.generator.VectorFilterPlanner
import com.mindos.app.features.mindboost.focus2.domain.generator.VectorFilterSpec
import com.mindos.app.features.mindboost.focus2.domain.runtime.FocusRuntimeSnapshot
import com.mindos.app.features.mindboost.focus2.domain.runtime.FocusSessionDescriptor
import com.mindos.app.features.mindboost.focus2.domain.runtime.InputModality
import com.mindos.app.features.mindboost.focus2.domain.runtime.RestoreOutcome
import com.mindos.app.features.mindboost.focus2.domain.runtime.RestoreRejection
import com.mindos.app.features.mindboost.focus2.domain.runtime.TrialPhase
import com.mindos.app.features.mindboost.focus2.domain.scoring.DescriptiveScorer
import com.mindos.app.features.mindboost.focus2.domain.session.FocusSessionState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Focus V5 · S3 · tarea (4) — write-ahead persistence and recovery. */
class SecureFocusSessionStoreTest {

    private fun descriptor(sessionId: String = SESSION) = FocusSessionDescriptor.seal(
        sessionId = sessionId,
        exerciseId = VectorFilterPlanner.EXERCISE_ID,
        purpose = SessionMeasurementPurpose.MEASUREMENT,
        seedSource = SeedSource { SessionSeed(3L, RngVersion.V1) },
        generatorVersion = VectorFilterPlanner.GENERATOR_VERSION,
        scorerVersion = DescriptiveScorer.SCORER_VERSION,
        constructModelVersion = 1,
        spec = VectorFilterSpec(),
        inputModality = InputModality.TOUCH_DIRECT,
        screenReaderActive = false,
    )

    private fun snapshot(sessionId: String = SESSION) = FocusRuntimeSnapshot(
        sessionId = sessionId,
        stateName = FocusSessionState.RUNNING.name,
        phaseName = TrialPhase.INTER_STIMULUS.name,
        trialIndex = 3,
    )

    /** A backend whose writes fail — the case a `Unit`-returning write would report as success. */
    private class UnwritableStore : SecureKeyValueStore by InMemorySecureKeyValueStore() {
        override suspend fun put(key: String, value: String): Unit = throw IllegalStateException("keystore gone")
    }

    @Test
    fun a_sealed_session_with_a_checkpoint_restores() = runTest {
        val store = SecureFocusSessionStore(InMemorySecureKeyValueStore())
        assertEquals(FocusWriteOutcome.Stored, store.sealDescriptor(USER, descriptor()))
        assertEquals(FocusWriteOutcome.Stored, store.checkpoint(USER, snapshot()))

        val restored = assertIs<RestoreOutcome.Restored>(store.restore(USER, SESSION))
        assertEquals(3, restored.snapshot.trialIndex)
        assertEquals(descriptor().seedHex, restored.descriptor.seedHex)
    }

    @Test
    fun a_write_that_did_not_happen_is_reported_as_a_failure_not_as_success() = runTest {
        val store = SecureFocusSessionStore(UnwritableStore())
        val outcome = assertIs<FocusWriteOutcome.Failed>(store.sealDescriptor(USER, descriptor()))
        assertEquals("IllegalStateException", outcome.reason, "the TYPE only — a message can carry user content")
    }

    @Test
    fun a_seal_with_no_checkpoint_is_absent_progress_rather_than_a_broken_record() = runTest {
        val store = SecureFocusSessionStore(InMemorySecureKeyValueStore())
        store.sealDescriptor(USER, descriptor())
        assertEquals(
            RestoreRejection.SNAPSHOT_ABSENT,
            assertIs<RestoreOutcome.Rejected>(store.restore(USER, SESSION)).reason,
        )
    }

    /**
     * The distinction that matters most in this file: a decryption failure must not read as "nothing was ever
     * here". The discriminating half is the test above — with the same code path, a genuinely missing record
     * still reports ABSENT.
     */
    @Test
    fun a_damaged_record_is_unreadable_never_absent_and_is_never_quarantined() = runTest {
        listOf(":descriptor" to RestoreRejection.DESCRIPTOR_UNREADABLE, ":snapshot" to RestoreRejection.SNAPSHOT_UNREADABLE)
            .forEach { (suffix, expected) ->
                val backing = InMemorySecureKeyValueStore()
                val store = SecureFocusSessionStore(backing)
                store.sealDescriptor(USER, descriptor())
                store.checkpoint(USER, snapshot())

                val key = "focus2_session:$USER:$SESSION$suffix"
                backing.simulateReadFailure(key, SecureReadResult.Corrupt)

                assertEquals(expected, assertIs<RestoreOutcome.Rejected>(store.restore(USER, SESSION)).reason)

                // Durability: an in-flight session is not regenerable, so a transient fault must not delete it.
                assertNull(backing.quarantinedValue(key), "restore must never quarantine a live session record")
                assertTrue(backing.keys("focus2_session:").contains(key), "the record must still be on disk")
            }
    }

    @Test
    fun the_recovery_scan_lists_sealed_sessions_and_discard_removes_them() = runTest {
        val store = SecureFocusSessionStore(InMemorySecureKeyValueStore())
        store.sealDescriptor(USER, descriptor("s-a"))
        store.sealDescriptor(USER, descriptor("s-b"))
        store.checkpoint(USER, snapshot("s-a"))

        assertEquals(setOf("s-a", "s-b"), assertIs<SessionScan.Scanned>(store.scanSessionIds(USER)).sessionIds)

        assertEquals(FocusWriteOutcome.Stored, store.discard(USER, "s-a"))
        assertEquals(setOf("s-b"), assertIs<SessionScan.Scanned>(store.scanSessionIds(USER)).sessionIds)
    }

    @Test
    fun another_users_sessions_are_not_visible_in_the_scan() = runTest {
        val store = SecureFocusSessionStore(InMemorySecureKeyValueStore())
        store.sealDescriptor(USER, descriptor("s-mine"))
        store.sealDescriptor("other-user", descriptor("s-theirs"))
        assertEquals(setOf("s-mine"), assertIs<SessionScan.Scanned>(store.scanSessionIds(USER)).sessionIds)
    }

    @Test
    fun an_identifier_that_could_corrupt_the_key_namespace_is_refused() = runTest {
        val store = SecureFocusSessionStore(InMemorySecureKeyValueStore())
        listOf("bad:id", "", "a".repeat(129)).forEach { hostile ->
            assertTrue(
                runCatching { store.sealDescriptor(USER, descriptor().copy(sessionId = hostile)) }.isFailure ||
                    runCatching { store.scanSessionIds(hostile) }.isFailure,
                "'$hostile' must not be accepted into a key",
            )
        }
    }

    private companion object {
        const val USER = "user-1"
        const val SESSION = "s-1"
    }
}
