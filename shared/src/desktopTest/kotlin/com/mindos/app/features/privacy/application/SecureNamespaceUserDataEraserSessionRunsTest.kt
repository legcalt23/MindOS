package com.mindos.app.features.privacy.application

import com.mindos.app.features.mindboost.core.session.testCognitiveRunStore

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRunFixtures
import com.mindos.app.features.mindboost.core.session.cognitive.SecureCognitiveSessionRunStore
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * SYSTEM-1 slice 6-R3 (P0 fix, 6R3-P0C-01) · [SecureNamespaceUserDataEraser]'s SessionRuns exception: unlike
 * every other account-bound namespace (still blanket-cleared, [SecureNamespaceUserDataEraserTest]), the
 * `CognitiveSessionRunStore` namespace is subject-scoped and must be erased WITHOUT collaterally destroying a
 * DIFFERENT account's runs sharing the same physical [InMemorySecureKeyValueStore] — the account-deletion
 * collateral-wipe half of the P0 finding (`AccountDeletionCoordinator.kt` KDoc, pre-fix).
 *
 * Lives in desktopTest because it needs [CognitiveSessionRunFixtures] (desktopTest-only, per its own KDoc).
 */
class SecureNamespaceUserDataEraserSessionRunsTest {

    @Test
    fun `deleting account A erases only A's runs, never B's, sharing the same SessionRuns namespace`() = runTest {
        val runsStore = InMemorySecureKeyValueStore()
        val otherStore = InMemorySecureKeyValueStore()
        val storeA = testCognitiveRunStore(runsStore, subject = "user-a")
        val storeB = testCognitiveRunStore(runsStore, subject = "user-b")
        storeA.save(CognitiveSessionRunFixtures.validRun(sessionId = "a1", blockId = ScoreModuleId.SPEED))
        storeA.save(CognitiveSessionRunFixtures.validRun(sessionId = "a2", blockId = ScoreModuleId.MEMORY))
        storeB.save(CognitiveSessionRunFixtures.validRun(sessionId = "b1", blockId = ScoreModuleId.SPEED))
        otherStore.put("span:user-a", "some-other-namespace-data")

        val eraser = SecureNamespaceUserDataEraser(
            accountBoundStores = listOf(otherStore, runsStore),
            subjectScopedRunsStore = runsStore,
            revocationStore = InMemorySecureKeyValueStore(),
        )
        eraser.eraseUserData("user-a")

        assertTrue(storeA.listRecent(10).isEmpty(), "A's own runs must be fully erased")
        assertEquals(
            listOf("b1"),
            storeB.listRecent(10).map { it.sessionId },
            "B's runs must survive A's account deletion — no collateral wipe",
        )
        assertEquals(listOf("b1"), storeB.listByBlock(ScoreModuleId.SPEED, 10).map { it.sessionId })
        assertTrue(otherStore.keys("").isEmpty(), "non-session-runs namespaces are still blanket-cleared as before")
    }

    @Test
    fun `deleting account B afterward also fully erases B's own runs`() = runTest {
        val runsStore = InMemorySecureKeyValueStore()
        val storeA = testCognitiveRunStore(runsStore, subject = "user-a")
        val storeB = testCognitiveRunStore(runsStore, subject = "user-b")
        storeA.save(CognitiveSessionRunFixtures.validRun(sessionId = "a1"))
        storeB.save(CognitiveSessionRunFixtures.validRun(sessionId = "b1"))
        val eraser = SecureNamespaceUserDataEraser(
            accountBoundStores = emptyList(),
            subjectScopedRunsStore = runsStore,
            revocationStore = InMemorySecureKeyValueStore(),
        )

        eraser.eraseUserData("user-a")
        eraser.eraseUserData("user-b")

        assertTrue(storeA.listRecent(10).isEmpty())
        assertTrue(storeB.listRecent(10).isEmpty(), "erasure must still be COMPLETE for the deleting user's own data")
    }

    @Test
    fun `without a subjectScopedRunsStore the legacy blanket-clear-everything behavior is unchanged`() = runTest {
        val runsStore = InMemorySecureKeyValueStore()
        runsStore.put("crun:user-a:a1", "irrelevant-raw-value")
        val eraser = SecureNamespaceUserDataEraser(accountBoundStores = listOf(runsStore))

        eraser.eraseUserData("user-a")

        assertTrue(runsStore.keys("").isEmpty(), "a caller that omits the param keeps the pre-fix blanket clear")
    }
}
