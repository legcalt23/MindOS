package com.mindos.app.features.mindboost.memory.domain.repository

import com.mindos.app.features.mindboost.memory.domain.content.sampling.RecentItemRegistry
import com.mindos.app.features.mindboost.memory.domain.deferred.DeferredRecallItem
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.domain.model.MemoryProfileState
import com.mindos.app.features.mindboost.memory.domain.model.MemorySessionSummary

/** Persists the evolving [MemoryProfileState] (encrypted in the local implementation). */
interface MemoryProfileRepository {
    suspend fun load(userId: String): MemoryProfileState?
    suspend fun save(state: MemoryProfileState)
}

/** Persists session SUMMARIES (never raw trials), listable newest-first per user. */
interface MemorySessionHistoryRepository {
    suspend fun append(summary: MemorySessionSummary)
    suspend fun recent(userId: String, limit: Int): List<MemorySessionSummary>
}

/** Persists the user's pending between-session delayed-recall items (doc §9). */
interface DeferredRecallRepository {
    suspend fun load(userId: String): List<DeferredRecallItem>
    suspend fun save(userId: String, items: List<DeferredRecallItem>)
}

/** Persists the per-exercise recent-item registry that enforces no-repetition (doc §13.4). */
interface RecentItemRegistryRepository {
    suspend fun load(userId: String, exerciseId: MemoryExerciseId): RecentItemRegistry?
    suspend fun save(userId: String, registry: RecentItemRegistry)
}
