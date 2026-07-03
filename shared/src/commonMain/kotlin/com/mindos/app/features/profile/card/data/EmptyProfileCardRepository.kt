package com.mindos.app.features.profile.card.data

import com.mindos.app.features.profile.card.domain.ProfileCardDoc
import com.mindos.app.features.profile.card.domain.repository.ProfileCardRepository

/**
 * Stable no-op [ProfileCardRepository] used as the default in `AppGraph`.
 *
 * It is an `object` (not a fresh `InMemoryProfileCardRepository()`), so the graph's
 * default keeps a stable identity — a per-instance default would break `AppGraph`
 * structural equality (see AppGraphTest). Loads resolve to `null`; saving is a
 * no-op. Production wires `FirebaseProfileCardRepository`; tests inject a fake.
 */
object EmptyProfileCardRepository : ProfileCardRepository {
    override suspend fun load(userId: String): ProfileCardDoc? = null
    override suspend fun loadPublic(userId: String): ProfileCardDoc? = null
    override suspend fun save(userId: String, doc: ProfileCardDoc) = Unit
}
