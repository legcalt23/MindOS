package com.mindos.app.features.mindboost.core.session.cognitive

import com.mindos.app.features.mindboost.core.session.testCognitiveRunStore

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureReadResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * SYSTEM-1 slice 6-R3 (CEO closure contract, §18 C1) · the legacy-key purge integrated with the REAL
 * [SecureCognitiveSessionRunStore] over `mindboost_session_runs`: pre-6R3 subject-less primary/index rows are
 * swept the moment ANY read/list/delete/reconcile method runs, a subject-scoped row is never touched, and the
 * reserved marker survives a subject's own [SecureCognitiveSessionRunStore.deleteAll] (account erasure).
 */
class SecureCognitiveSessionRunStoreLegacyPurgeTest {

    private companion object {
        const val MARKER_KEY = "legacy_purge_marker:v1"
    }

    private fun run(id: String) = CognitiveSessionRunFixtures.validRun(sessionId = id)

    @Test
    fun genuineLegacyRows_areSweptTheFirstTimeAnyReadMethodRuns() = runTest {
        val backing = InMemorySecureKeyValueStore()
        backing.put("crun:pre_6r3_orphan", "irrelevant-raw-value") // ONE colon: the pre-fix primary shape
        backing.put("crun_idx_recent", "irrelevant-raw-value") // the pre-fix GLOBAL recent index (no colon at all)
        backing.put("crun_idx_block:SPEED", "irrelevant-raw-value") // the pre-fix block-index shape (one colon)
        val store = testCognitiveRunStore(backing, subject = "u1")
        store.save(run("current")) // a write never triggers the purge; also seeds the NEW-format subject indices

        assertTrue(backing.keys("crun:pre_6r3_orphan").isNotEmpty(), "not purged before a read runs")

        store.listRecent(10) // the FIRST read/list/delete/reconcile method call on this instance

        assertTrue(backing.keys("crun:pre_6r3_orphan").isEmpty(), "the legacy primary row is gone")
        assertEquals(
            setOf("crun_idx_recent:u1"),
            backing.keys("crun_idx_recent"),
            "the legacy GLOBAL recent index is gone, the new subject-scoped one is intact",
        )
        assertEquals(
            setOf("crun_idx_block:u1:SPEED"),
            backing.keys("crun_idx_block:"),
            "the legacy block index is gone, the new subject-scoped one is intact",
        )
        assertEquals(listOf("current"), store.listRecent(10).map { it.sessionId }, "current subject's run intact")
    }

    @Test
    fun aSubjectScopedRun_isNeverMistakenForLegacy() = runTest {
        val backing = InMemorySecureKeyValueStore()
        val store = testCognitiveRunStore(backing, subject = "u1")
        store.save(run("r1")) // writes "crun:u1:r1" — TWO colons, today's real shape, never legacy

        store.get("r1") // triggers the purge scan

        assertIs<ReadResult.Found<CognitiveSessionRun>>(store.get("r1"))
    }

    @Test
    fun get_listRecent_listByBlock_deleteAll_andReconcile_allTriggerThePurge() = runTest {
        val probes: List<suspend (SecureCognitiveSessionRunStore) -> Unit> = listOf(
            { it.get("missing") },
            { it.listRecent(10) },
            { it.listByBlock(ScoreModuleId.SPEED, 10) },
            { it.reconcileIndices() },
            { it.deleteAll() },
        )
        for (probe in probes) {
            val backing = InMemorySecureKeyValueStore()
            backing.put("crun:orphan", "x")
            probe(testCognitiveRunStore(backing, subject = "u1"))
            assertTrue(backing.keys("crun:orphan").isEmpty(), "every gated method must trigger the purge")
        }
    }

    @Test
    fun deleteAll_forOneSubject_neverRemovesTheLegacyPurgeMarker() = runTest {
        val backing = InMemorySecureKeyValueStore()
        val store = testCognitiveRunStore(backing, subject = "u1")
        store.listRecent(10) // triggers the purge, writes the marker
        assertEquals(SecureReadResult.Value("1"), backing.read(MARKER_KEY))

        store.save(run("r1"))
        store.deleteAll()

        assertEquals(
            SecureReadResult.Value("1"),
            backing.read(MARKER_KEY),
            "account-deletion erases only this subject's own rows, never the device-wide purge marker",
        )
    }

    @Test
    fun reconcileIndices_isAlsoAGatedEntryPoint() = runTest {
        val backing = InMemorySecureKeyValueStore()
        backing.put("crun:pre_6r3_orphan", "irrelevant-raw-value")
        val store = testCognitiveRunStore(backing, subject = "u1")

        store.reconcileIndices()

        assertTrue(backing.keys("crun:pre_6r3_orphan").isEmpty(), "reconcile is also a gated entry point")
    }
}
