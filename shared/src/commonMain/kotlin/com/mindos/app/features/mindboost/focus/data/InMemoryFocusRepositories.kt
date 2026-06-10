package com.mindos.app.features.mindboost.focus.data

import com.mindos.app.features.mindboost.focus.domain.model.BaselineSnapshot
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile
import com.mindos.app.features.mindboost.focus.domain.repository.BaselineRepository
import com.mindos.app.features.mindboost.focus.domain.repository.FocusProfileRepository
import com.mindos.app.features.mindboost.focus.domain.repository.SessionHistoryRepository

/**
 * In-memory implementations of the focus repositories.
 *
 * Used today for the vertical slice, previews and tests. Real local-first
 * encrypted persistence (mirroring `PublisherLocalDraftStore`) replaces these in
 * the persistence phase (10_data_persistence_plan.md); the contracts stay
 * identical, so call sites do not change.
 */
class InMemoryFocusProfileRepository : FocusProfileRepository {
    private val profiles = mutableMapOf<String, UserCognitiveProfile>()

    override suspend fun load(userId: String): UserCognitiveProfile? = profiles[userId]

    override suspend fun save(profile: UserCognitiveProfile) {
        profiles[profile.userId] = profile
    }
}

class InMemorySessionHistoryRepository : SessionHistoryRepository {
    private val byUser = mutableMapOf<String, MutableList<SessionResult>>()

    override suspend fun append(result: SessionResult) {
        // The session id is not user-scoped here; the holder passes user-scoped data.
        val bucket = byUser.getOrPut(result.sessionId.value.substringBefore(':')) { mutableListOf() }
        bucket.add(result)
    }

    override suspend fun recent(userId: String, limit: Int): List<SessionResult> {
        require(limit > 0) { "limit must be > 0." }
        return byUser[userId].orEmpty().sortedByDescending { it.endedAtMs }.take(limit)
    }

    /** Test/preview helper. */
    fun all(userId: String): List<SessionResult> = byUser[userId].orEmpty().toList()
}

class InMemoryBaselineRepository : BaselineRepository {
    private val baselines = mutableMapOf<String, BaselineSnapshot>()

    override suspend fun load(userId: String): BaselineSnapshot? = baselines[userId]

    override suspend fun save(snapshot: BaselineSnapshot) {
        baselines[snapshot.userId] = snapshot
    }
}
