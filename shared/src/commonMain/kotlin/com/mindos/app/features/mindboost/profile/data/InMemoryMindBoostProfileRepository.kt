package com.mindos.app.features.mindboost.profile.data

import com.mindos.app.features.mindboost.profile.domain.model.MindBoostProfile
import com.mindos.app.features.mindboost.profile.domain.repository.MindBoostProfileRepository

/** Non-durable in-memory repository for tests and desktop/iOS previews. */
class InMemoryMindBoostProfileRepository : MindBoostProfileRepository {
    private val saved = mutableMapOf<String, MindBoostProfile>()

    override suspend fun load(userId: String): MindBoostProfile? = saved[userId]

    override suspend fun save(profile: MindBoostProfile) {
        saved[profile.userId] = profile
    }

    override suspend fun clear(userId: String) {
        saved.remove(userId)
    }
}
