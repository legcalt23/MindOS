package com.mindos.app.features.mindboost.storage

import com.mindos.app.core.di.accountBoundEraserStores
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Guards the account-deletion coverage invariant. The actual eraser store list is built inline in
 * `AppGraph.rememberAppGraph()` (Compose `remember` rules forbid mapping the list there), so it is kept
 * in sync with [MindBoostStorageNamespaces.AccountBound] by hand. This suite pins the canonical
 * namespace set so adding a new account-bound namespace fails here until the dev ALSO updates the
 * AppGraph `SecureNamespaceUserDataEraser` store list — closing the "new sensitive data never erased"
 * gap for the emotions module (decision J).
 */
class AccountBoundErasureCoverageTest {

    @Test
    fun account_bound_includes_emotions() {
        assertTrue(
            MindBoostStorageNamespaces.Emotions in MindBoostStorageNamespaces.AccountBound,
            "Emotions is sensitive cognitive data and MUST be erased on account deletion.",
        )
    }

    @Test
    fun account_bound_includes_session_runs() {
        assertTrue(
            MindBoostStorageNamespaces.SessionRuns in MindBoostStorageNamespaces.AccountBound,
            "The SYSTEM-1 cognitive-history store holds sensitive per-session outcomes and MUST be erased " +
                "on account deletion (covered BEFORE any exercise is wired to seal runs into it).",
        )
    }

    @Test
    fun account_bound_is_the_canonical_namespace_set() {
        assertEquals(
            setOf(
                MindBoostStorageNamespaces.Focus,
                MindBoostStorageNamespaces.Memory,
                MindBoostStorageNamespaces.Speed,
                MindBoostStorageNamespaces.Logic,
                MindBoostStorageNamespaces.Creativity,
                MindBoostStorageNamespaces.Emotions,
                MindBoostStorageNamespaces.Decisiones,
                MindBoostStorageNamespaces.Language,
                MindBoostStorageNamespaces.Profile,
                MindBoostStorageNamespaces.Sessions,
                MindBoostStorageNamespaces.SessionRuns,
                MindBoostStorageNamespaces.SyncQueue,
                MindBoostStorageNamespaces.Coach,
                MindBoostStorageNamespaces.PayloadJournal,
            ),
            MindBoostStorageNamespaces.AccountBound.toSet(),
            "If this fails, also update the SecureNamespaceUserDataEraser store list in AppGraph.",
        )
    }

    @Test
    fun revocation_namespace_must_never_be_account_bound() {
        // SYSTEM-1 slice 6-R3 (P2-01) · the permanent per-subject revocation tombstone (SubjectRevocationAuthority)
        // is an ANTI-RESURRECTION marker that MUST SURVIVE account deletion. Adding `mindboost_revocation` to
        // AccountBound would blanket-clear it on the very deletion it is meant to outlive, letting a stale writer
        // resurrect a deleted subject's data. This negative barrier fails if a future change wires it into the
        // account-erasure path (the §6 automated guard for the revocation namespace).
        assertTrue(
            MindBoostStorageNamespaces.Revocation !in MindBoostStorageNamespaces.AccountBound,
            "mindboost_revocation is a PERMANENT anti-resurrection marker and must NEVER be account-bound/erased.",
        )
    }

    @Test
    fun account_bound_has_no_duplicates() {
        assertEquals(
            MindBoostStorageNamespaces.AccountBound.size,
            MindBoostStorageNamespaces.AccountBound.toSet().size,
            "Duplicate namespace in AccountBound.",
        )
    }

    @Test
    fun appGraphEraserNamespaces_matchAccountBoundNamespaces() {
        // AppGraph builds the eraser list via accountBoundEraserStores, derived from AccountBound: it must
        // resolve one store for EVERY account-bound namespace (protecting the wiring, not just the set).
        val stores = MindBoostStorageNamespaces.AccountBound.associateWith { InMemorySecureKeyValueStore() }
        assertEquals(
            MindBoostStorageNamespaces.AccountBound.size,
            accountBoundEraserStores(stores).size,
            "Every account-bound namespace must resolve to an eraser store.",
        )
    }

    @Test
    fun accountBoundEraserStores_failsWhenANamespaceIsMissing() {
        // If AppGraph forgot to wire a namespace's store (e.g. syncQueueStore), construction must fail fast,
        // NOT silently drop it from account deletion.
        val incomplete = MindBoostStorageNamespaces.AccountBound
            .filterNot { it == MindBoostStorageNamespaces.SyncQueue }
            .associateWith { InMemorySecureKeyValueStore() }
        assertFailsWith<IllegalArgumentException> { accountBoundEraserStores(incomplete) }
    }
}
