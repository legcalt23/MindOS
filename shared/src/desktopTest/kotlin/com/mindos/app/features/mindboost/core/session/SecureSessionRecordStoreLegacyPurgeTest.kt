package com.mindos.app.features.mindboost.core.session

import com.mindos.app.features.mindboost.core.session.testSessionRecordStore

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureReadResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * SYSTEM-1 slice 6-R3 (CEO closure contract, §18 C1) · the legacy-key purge integrated with the REAL
 * [SecureSessionRecordStore] over `mindboost_sessions`: a genuinely pre-6R3, subject-less row (one colon) is
 * swept the moment ANY read/list/delete method runs; a subject-scoped row (two colons) is never touched; and
 * the reserved marker survives a subject's own [SecureSessionRecordStore.deleteAll] (account erasure).
 */
class SecureSessionRecordStoreLegacyPurgeTest {

    private companion object {
        const val MARKER_KEY = "legacy_purge_marker:v1"
    }

    private fun record(id: String) = SessionRecord(
        sessionId = id,
        block = ScoreModuleId.FOCUS,
        status = SessionStatus.FINALIZED,
        startedAtMs = 1_000L,
        updatedAtMs = 1_000L,
        completedTrials = 1,
        plannedTrials = 1,
        activeDurationMs = 1_000L,
    )

    @Test
    fun aGenuineLegacyRow_isPurgedTheFirstTimeAnyReadMethodRuns() = runTest {
        val backing = InMemorySecureKeyValueStore()
        backing.put("session_record:pre_6r3_orphan", "irrelevant-raw-value") // ONE colon: the pre-fix shape
        val store = testSessionRecordStore(backing, subject = "u1")
        store.upsert(record("current")) // a write never triggers the purge on its own

        assertTrue(backing.keys("session_record:pre_6r3_orphan").isNotEmpty(), "not purged before a read runs")

        store.all() // the FIRST read/list/delete method call on this instance

        assertTrue(backing.keys("session_record:pre_6r3_orphan").isEmpty(), "the legacy row is now gone")
        assertEquals(listOf("current"), store.all().map { it.sessionId }, "the current subject's row is untouched")
    }

    @Test
    fun aSubjectScopedRow_isNeverMistakenForLegacy() = runTest {
        val backing = InMemorySecureKeyValueStore()
        val store = testSessionRecordStore(backing, subject = "u1")
        store.upsert(record("s1")) // writes "session_record:u1:s1" — TWO colons, today's real shape, never legacy

        store.find("s1") // triggers the purge scan

        assertEquals(SessionStatus.FINALIZED, store.find("s1")?.status, "the genuine subject-scoped row survives")
    }

    @Test
    fun find_list_delete_andFindEvidence_allTriggerThePurge() = runTest {
        for (probe in listOf<suspend (SecureSessionRecordStore) -> Unit>(
            { it.find("missing") },
            { it.all() },
            { it.delete("missing") },
            { it.findEvidence("missing") },
        )) {
            val backing = InMemorySecureKeyValueStore()
            backing.put("session_record:orphan", "x")
            probe(testSessionRecordStore(backing, subject = "u1"))
            assertTrue(backing.keys("session_record:orphan").isEmpty(), "every read/list/delete method must gate")
        }
    }

    @Test
    fun deleteAll_forOneSubject_neverRemovesTheLegacyPurgeMarker() = runTest {
        val backing = InMemorySecureKeyValueStore()
        val store = testSessionRecordStore(backing, subject = "u1")
        store.all() // triggers the purge, writes the marker
        assertEquals(SecureReadResult.Value("1"), backing.read(MARKER_KEY))

        store.upsert(record("s1"))
        store.deleteAll()

        assertEquals(
            SecureReadResult.Value("1"),
            backing.read(MARKER_KEY),
            "account-deletion erases only this subject's own rows, never the device-wide purge marker",
        )
    }

    @Test
    fun signingInAsADifferentSubject_overTheSamePurgedBacking_doesNotDisturbTheMarker() = runTest {
        val backing = InMemorySecureKeyValueStore()
        testSessionRecordStore(backing, subject = "u1").all() // first sign-in
        val markerAfterFirstSubject = backing.read(MARKER_KEY)

        testSessionRecordStore(backing, subject = "u2").all() // a DIFFERENT account signs in — a fresh instance

        assertEquals(markerAfterFirstSubject, backing.read(MARKER_KEY), "a second subject's sign-in is a no-op here")
    }
}
