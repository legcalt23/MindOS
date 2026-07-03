package com.mindos.app.features.profile.card.data

import com.mindos.app.features.profile.card.domain.ProfileCardDoc
import com.mindos.app.features.profile.card.domain.repository.ProfileCardRepository

/**
 * Non-persistent profile-card repository for tests, desktop and previews. Mirrors the
 * Firebase auto-publish semantics: [save] writes both the private draft and the public
 * snapshot. [failSaveWith] lets tests simulate a Firestore failure.
 */
class InMemoryProfileCardRepository : ProfileCardRepository {
    private val privateStore = mutableMapOf<String, ProfileCardDoc>()
    private val publicStore = mutableMapOf<String, ProfileCardDoc>()

    /** When non-null, the next [save] throws this (then it is cleared). */
    var failSaveWith: Throwable? = null

    override suspend fun load(userId: String): ProfileCardDoc? = privateStore[userId]

    override suspend fun loadPublic(userId: String): ProfileCardDoc? = publicStore[userId]

    override suspend fun save(userId: String, doc: ProfileCardDoc) {
        failSaveWith?.let { error ->
            failSaveWith = null
            throw error
        }
        privateStore[userId] = doc
        publicStore[userId] = doc
    }
}
