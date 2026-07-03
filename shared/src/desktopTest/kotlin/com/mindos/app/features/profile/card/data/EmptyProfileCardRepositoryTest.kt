package com.mindos.app.features.profile.card.data

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNull

/**
 * Pins the no-op contract of [EmptyProfileCardRepository] — the stable default
 * `AppGraph` uses. Its loads MUST resolve to `null`, so when no real repository
 * is injected the current user's favorite accent is unknown and every migrated
 * loader falls back to its own brand color (no regression, no fabricated accent).
 */
class EmptyProfileCardRepositoryTest {

    @Test
    fun loads_resolve_to_null() = runTest {
        assertNull(EmptyProfileCardRepository.load("user-1"))
        assertNull(EmptyProfileCardRepository.loadPublic("user-1"))
    }
}
