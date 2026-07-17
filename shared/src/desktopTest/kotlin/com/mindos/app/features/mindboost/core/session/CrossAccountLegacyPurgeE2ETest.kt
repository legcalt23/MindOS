package com.mindos.app.features.mindboost.core.session

import com.mindos.app.features.mindboost.core.session.testSessionRecordStore
import com.mindos.app.features.mindboost.core.session.testCognitiveRunStore

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRun
import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRunFixtures
import com.mindos.app.features.mindboost.core.session.cognitive.ReadResult
import com.mindos.app.features.mindboost.core.session.cognitive.SaveResult
import com.mindos.app.features.mindboost.core.session.cognitive.SecureCognitiveSessionRunStore
import com.mindos.app.features.mindboost.core.session.cognitive.StoreFailureReason
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureReadResult
import com.mindos.app.features.mindboost.focus.di.focusSessionRecovery
import com.mindos.app.features.mindboost.speed.di.speedSessionRecovery
import com.mindos.app.features.privacy.application.SecureNamespaceUserDataEraser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * SYSTEM-1 slice 6-R3 (CEO closure contract, §18 C5) · single-process, cross-account isolation over a SHARED
 * physical backing store per namespace, exercised through the REAL production factories: pre-migration legacy
 * rows seeded on an "un-upgraded device", subject A's full lifecycle (write, recovery/reconciliation — which
 * are the FIRST readers and therefore the ones that mechanically trigger the legacy purge), an in-process
 * account switch to subject B (brand-new factories, the SAME backing stores, no process restart), B's total
 * blindness to A across find/list/delete, account-deletion erasure, and the migration marker surviving BOTH
 * subjects' erasure — every assertion pinned at the PHYSICAL KEY level of the underlying
 * [InMemorySecureKeyValueStore]s, never through a derived/aggregated view.
 *
 * Honest scope (declared, not silently assumed): the auth -> composable binding (`HomeScreen` ownerId -> the
 * MindBoost hub -> each root screen's `remember(userId)`) is covered by the `privacy-security` file:line trace
 * recorded in the reopen doc (§17), not re-derived here — mounting the full Compose tree is not possible in
 * desktopTest without Firebase (learning 2026-06-19). This test starts from the SAME real, `internal` factory
 * functions those composables call (`focusSessionRecovery`, `speedSessionRecovery`,
 * `SecureSessionRecordStore`/`SecureCognitiveSessionRunStore` construction), with `subject`/`userId` supplied
 * directly — exactly the value those composables thread from the authenticated Firebase uid.
 */
class CrossAccountLegacyPurgeE2ETest {

    private companion object {
        const val NOW = 1_000_000L
        const val OLD = 0L
        const val MARKER_KEY = "legacy_purge_marker:v1"
    }

    private fun orphan(id: String) = SessionRecord(
        sessionId = id,
        block = ScoreModuleId.FOCUS,
        status = SessionStatus.STARTED,
        startedAtMs = OLD,
        updatedAtMs = OLD,
        completedTrials = 0,
        plannedTrials = 10,
        activeDurationMs = 0L,
    )

    @Test
    fun crossAccountIsolation_survivesAnInProcessAccountSwitch_withLegacyPurgeAndErasure() = runTest {
        val sessionsBacking = InMemorySecureKeyValueStore()
        val runsBacking = InMemorySecureKeyValueStore()
        val clock = EpochClock { NOW }

        // ---- pre-migration legacy rows, seeded directly (as an un-upgraded device would already carry them) ----
        sessionsBacking.put("session_record:legacy_orphan", "irrelevant-raw-value")
        runsBacking.put("crun:legacy_run", "irrelevant-raw-value")
        runsBacking.put("crun_idx_recent", "irrelevant-raw-value")
        runsBacking.put("crun_idx_block:SPEED", "irrelevant-raw-value")

        // ---- subject A: real production factories ----
        val sessionStoreA = testSessionRecordStore(sessionsBacking, subject = "A")
        sessionStoreA.upsert(orphan("a_session"))
        val runStoreA = testCognitiveRunStore(runsBacking, subject = "A")
        runStoreA.save(CognitiveSessionRunFixtures.validRun(sessionId = "a_run"))

        // Recovery (Focus's real factory) is the FIRST reader over the sessions namespace: it both recovers A's
        // own stale orphan AND mechanically purges the legacy row before anything else can ever observe it.
        focusSessionRecovery(sessionsBacking, InMemorySecureKeyValueStore(), clock, "A").recover()
        runStoreA.reconcileIndices() // reconciliation is the FIRST reader over the runs namespace

        assertTrue(sessionsBacking.keys("session_record:legacy_orphan").isEmpty(), "sessions legacy row purged")
        assertTrue(runsBacking.keys("crun:legacy_run").isEmpty(), "runs legacy primary purged")
        assertEquals(
            setOf("crun_idx_recent:A"),
            runsBacking.keys("crun_idx_recent"),
            "legacy GLOBAL recent index purged, A's subject-scoped one intact",
        )
        assertEquals(
            setOf("crun_idx_block:A:SPEED"),
            runsBacking.keys("crun_idx_block:"),
            "legacy block index purged, A's subject-scoped one intact",
        )
        assertEquals(SessionStatus.ABANDONED, sessionStoreA.find("a_session")?.status, "A's own orphan recovered")
        assertIs<ReadResult.Found<CognitiveSessionRun>>(runStoreA.get("a_run"), "A's own run intact after reconcile")

        // ---- switch to subject B: SAME process, SAME backing stores, brand-new factories ----
        val sessionStoreB = testSessionRecordStore(sessionsBacking, subject = "B")
        val runStoreB = testCognitiveRunStore(runsBacking, subject = "B")

        assertNull(sessionStoreB.find("a_session"), "B cannot read A's session by id")
        assertTrue(sessionStoreB.all().isEmpty(), "B's session listing is empty -- A's rows are invisible")
        assertEquals(ReadResult.Missing, runStoreB.get("a_run"), "B cannot read A's run by id")
        assertTrue(runStoreB.listRecent(10).isEmpty(), "B's run listing is empty -- A's rows are invisible")
        assertTrue(runStoreB.listByBlock(ScoreModuleId.SPEED, 10).isEmpty(), "B's block listing is empty too")

        sessionStoreB.upsert(orphan("b_session"))
        runStoreB.save(CognitiveSessionRunFixtures.validRun(sessionId = "b_run"))
        val recoveredForB = speedSessionRecovery(sessionsBacking, InMemorySecureKeyValueStore(), clock, "B").recover()
        assertEquals(1, recoveredForB, "B's recovery counts only B's own orphan, never A's")
        assertEquals(
            SessionStatus.ABANDONED,
            sessionStoreA.find("a_session")?.status,
            "B's recovery pass never re-touches A's already-recovered row",
        )

        sessionStoreB.delete("a_session") // targets "session_record:B:a_session" -- physically not A's key
        assertEquals(
            SessionStatus.ABANDONED,
            sessionStoreA.find("a_session")?.status,
            "A's row survives B's delete -- B cannot physically reach A's key",
        )
        runStoreB.deleteAll() // wipes B's OWN "b_run" -- proves A's run survives, never that B has nothing left
        assertIs<ReadResult.Found<CognitiveSessionRun>>(runStoreA.get("a_run"), "A's run survives B's deleteAll")

        // ---- the legacy-purge marker, present in BOTH namespaces after the first gated call in each ----
        assertEquals(SecureReadResult.Value("1"), sessionsBacking.read(MARKER_KEY))
        assertEquals(SecureReadResult.Value("1"), runsBacking.read(MARKER_KEY))

        // SYSTEM-1 slice 6-R3 (P2-01): runStoreB.deleteAll() above permanently tombstones subject B on this
        // physical backend, so a LATER save attempt for B is refused, never resurrecting a run after B's own
        // erasure -- through the SAME real production factory used throughout this test.
        assertEquals(
            SaveResult.Failed(StoreFailureReason.SUBJECT_TOMBSTONED),
            runStoreB.save(CognitiveSessionRunFixtures.validRun(sessionId = "b_run_2")),
            "a subject already erased via its own deleteAll can never resurrect a run through a later save",
        )

        // ---- account-level erasure, the real production eraser ----
        val eraser = SecureNamespaceUserDataEraser(
            accountBoundStores = listOf(sessionsBacking, runsBacking),
            subjectScopedRunsStore = runsBacking,
            subjectScopedSessionsStore = sessionsBacking,
            revocationStore = InMemorySecureKeyValueStore(),
        )
        eraser.eraseUserData("B")

        assertTrue(sessionsBacking.keys("session_record:B:").isEmpty(), "B's session rows erased")
        assertTrue(runsBacking.keys("crun:B:").isEmpty(), "B's run rows erased")
        assertTrue(sessionsBacking.keys("session_record:A:").isNotEmpty(), "A's session rows survive B's erasure")
        assertTrue(runsBacking.keys("crun:A:").isNotEmpty(), "A's run rows survive B's erasure")
        assertEquals(SecureReadResult.Value("1"), sessionsBacking.read(MARKER_KEY), "marker survives B's erasure")
        assertEquals(SecureReadResult.Value("1"), runsBacking.read(MARKER_KEY), "marker survives B's erasure")

        eraser.eraseUserData("A")

        assertTrue(sessionsBacking.keys("session_record:A:").isEmpty(), "A's session rows now erased too")
        assertTrue(runsBacking.keys("crun:A:").isEmpty(), "A's run rows now erased too")
        assertEquals(SecureReadResult.Value("1"), sessionsBacking.read(MARKER_KEY), "marker survives A's erasure")
        assertEquals(SecureReadResult.Value("1"), runsBacking.read(MARKER_KEY), "marker survives A's erasure")
    }
}
