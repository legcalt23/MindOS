package com.mindos.app.features.mindboost.storage

import kotlin.test.Test
import kotlin.test.assertEquals
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
            ),
            MindBoostStorageNamespaces.AccountBound.toSet(),
            "If this fails, also update the SecureNamespaceUserDataEraser store list in AppGraph.",
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
}
