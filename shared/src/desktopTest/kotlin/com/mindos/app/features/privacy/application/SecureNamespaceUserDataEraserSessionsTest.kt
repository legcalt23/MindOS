package com.mindos.app.features.privacy.application

import com.mindos.app.features.mindboost.core.session.testSessionRecordStore

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.SecureSessionRecordStore
import com.mindos.app.features.mindboost.core.session.SessionRecord
import com.mindos.app.features.mindboost.core.session.SessionStatus
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * SYSTEM-1 slice 6-R3 (§14 fold-in, the P1 sibling of 6R3-P0C-01) · [SecureNamespaceUserDataEraser]'s Sessions
 * exception: unlike every other account-bound namespace (still blanket-cleared,
 * [SecureNamespaceUserDataEraserTest]), the `SessionRecordStore` namespace is subject-scoped and must be erased
 * WITHOUT collaterally destroying a DIFFERENT account's session lifecycle rows sharing the same physical
 * [InMemorySecureKeyValueStore] — mirrors [SecureNamespaceUserDataEraserSessionRunsTest] exactly.
 */
class SecureNamespaceUserDataEraserSessionsTest {

    private fun record(id: String, block: ScoreModuleId = ScoreModuleId.SPEED) = SessionRecord(
        sessionId = id,
        block = block,
        status = SessionStatus.FINALIZED,
        startedAtMs = 1_000L,
        updatedAtMs = 2_000L,
        completedTrials = 8,
        plannedTrials = 8,
        activeDurationMs = 60_000L,
    )

    @Test
    fun `deleting account A erases only A's session records, never B's, sharing the same Sessions namespace`() =
        runTest {
            val sessionsStore = InMemorySecureKeyValueStore()
            val otherStore = InMemorySecureKeyValueStore()
            val storeA = testSessionRecordStore(sessionsStore, subject = "user-a")
            val storeB = testSessionRecordStore(sessionsStore, subject = "user-b")
            storeA.upsert(record("a1", ScoreModuleId.SPEED))
            storeA.upsert(record("a2", ScoreModuleId.MEMORY))
            storeB.upsert(record("b1", ScoreModuleId.SPEED))
            otherStore.put("span:user-a", "some-other-namespace-data")

            val eraser = SecureNamespaceUserDataEraser(
                accountBoundStores = listOf(otherStore, sessionsStore),
                subjectScopedSessionsStore = sessionsStore,
                revocationStore = InMemorySecureKeyValueStore(),
            )
            eraser.eraseUserData("user-a")

            assertTrue(storeA.all().isEmpty(), "A's own session records must be fully erased")
            assertEquals(
                listOf("b1"),
                storeB.all().map { it.sessionId },
                "B's session records must survive A's account deletion — no collateral wipe",
            )
            assertTrue(otherStore.keys("").isEmpty(), "non-sessions namespaces are still blanket-cleared as before")
        }

    @Test
    fun `deleting account B afterward also fully erases B's own session records`() = runTest {
        val sessionsStore = InMemorySecureKeyValueStore()
        val storeA = testSessionRecordStore(sessionsStore, subject = "user-a")
        val storeB = testSessionRecordStore(sessionsStore, subject = "user-b")
        storeA.upsert(record("a1"))
        storeB.upsert(record("b1"))
        val eraser = SecureNamespaceUserDataEraser(
            accountBoundStores = emptyList(),
            subjectScopedSessionsStore = sessionsStore,
            revocationStore = InMemorySecureKeyValueStore(),
        )

        eraser.eraseUserData("user-a")
        eraser.eraseUserData("user-b")

        assertTrue(storeA.all().isEmpty())
        assertTrue(storeB.all().isEmpty(), "erasure must still be COMPLETE for the deleting user's own data")
    }

    @Test
    fun `without a subjectScopedSessionsStore the legacy blanket-clear-everything behavior is unchanged`() = runTest {
        val sessionsStore = InMemorySecureKeyValueStore()
        sessionsStore.put("session_record:user-a:a1", "irrelevant-raw-value")
        val eraser = SecureNamespaceUserDataEraser(accountBoundStores = listOf(sessionsStore))

        eraser.eraseUserData("user-a")

        assertTrue(sessionsStore.keys("").isEmpty(), "a caller that omits the param keeps the pre-fix blanket clear")
    }

    @Test
    fun `both subject-scoped exceptions coexist without interfering with each other`() = runTest {
        // AppGraph wires BOTH subjectScopedRunsStore and subjectScopedSessionsStore simultaneously — prove
        // neither exclusion accidentally skips (or double-excludes) the other's store.
        val sessionsStore = InMemorySecureKeyValueStore()
        val runsStore = InMemorySecureKeyValueStore()
        val storeA = testSessionRecordStore(sessionsStore, subject = "user-a")
        val storeB = testSessionRecordStore(sessionsStore, subject = "user-b")
        storeA.upsert(record("a1"))
        storeB.upsert(record("b1"))
        runsStore.put("crun:user-a:r1", "irrelevant-raw-value")

        val eraser = SecureNamespaceUserDataEraser(
            accountBoundStores = listOf(sessionsStore, runsStore),
            subjectScopedRunsStore = runsStore,
            subjectScopedSessionsStore = sessionsStore,
            revocationStore = InMemorySecureKeyValueStore(),
        )
        eraser.eraseUserData("user-a")

        assertTrue(storeA.all().isEmpty(), "A's session records erased via the subject-scoped path")
        assertEquals(listOf("b1"), storeB.all().map { it.sessionId }, "B's session records survive")
        assertTrue(runsStore.keys("crun:user-a:").isEmpty(), "A's run row erased via ITS OWN subject-scoped path too")
    }
}
