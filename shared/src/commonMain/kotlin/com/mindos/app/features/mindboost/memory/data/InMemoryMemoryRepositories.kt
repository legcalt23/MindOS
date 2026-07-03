package com.mindos.app.features.mindboost.memory.data

import com.mindos.app.features.mindboost.memory.domain.content.sampling.RecentItemRegistry
import com.mindos.app.features.mindboost.memory.domain.deferred.DeferredRecallItem
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.domain.model.MemoryProfileState
import com.mindos.app.features.mindboost.memory.domain.model.MemorySessionSummary
import com.mindos.app.features.mindboost.memory.domain.repository.DeferredRecallRepository
import com.mindos.app.features.mindboost.memory.domain.repository.MemoryProfileRepository
import com.mindos.app.features.mindboost.memory.domain.repository.MemorySessionHistoryRepository
import com.mindos.app.features.mindboost.memory.domain.repository.RecentItemRegistryRepository
import com.mindos.app.features.mindboost.memory.domain.repository.DeferredResultStore
import com.mindos.app.features.mindboost.memory.domain.repository.PairsResultStore
import com.mindos.app.features.mindboost.memory.domain.repository.SpanResultStore
import com.mindos.app.features.mindboost.memory.domain.scoring.DeferredRecallOutcome
import com.mindos.app.features.mindboost.memory.domain.scoring.DeferredRecordLog
import com.mindos.app.features.mindboost.memory.domain.scoring.PairsRecordLog
import com.mindos.app.features.mindboost.memory.domain.scoring.PairsTrial
import com.mindos.app.features.mindboost.memory.domain.scoring.SpanRecordLog
import com.mindos.app.features.mindboost.memory.domain.scoring.SpanTrial

/**
 * In-memory repository fakes (no serialization). For unit-testing higher layers
 * (state holders, runtime) without the encrypted store. Not durable across runs.
 */
class InMemoryMemoryProfileRepository : MemoryProfileRepository {
    private val byUser = mutableMapOf<String, MemoryProfileState>()
    override suspend fun load(userId: String): MemoryProfileState? = byUser[userId]
    override suspend fun save(state: MemoryProfileState) {
        byUser[state.userId] = state
    }
}

class InMemoryMemorySessionHistoryRepository : MemorySessionHistoryRepository {
    private val summaries = mutableListOf<MemorySessionSummary>()

    override suspend fun append(summary: MemorySessionSummary) {
        summaries.removeAll { it.sessionId == summary.sessionId }
        summaries.add(summary)
    }

    override suspend fun recent(userId: String, limit: Int): List<MemorySessionSummary> {
        require(limit > 0) { "limit must be > 0." }
        return summaries.filter { it.userId == userId }
            .sortedByDescending { it.endedAtEpoch }
            .take(limit)
    }
}

class InMemoryDeferredRecallRepository : DeferredRecallRepository {
    private val byUser = mutableMapOf<String, List<DeferredRecallItem>>()
    override suspend fun load(userId: String): List<DeferredRecallItem> = byUser[userId].orEmpty()
    override suspend fun save(userId: String, items: List<DeferredRecallItem>) {
        byUser[userId] = items
    }
}

class InMemoryRecentItemRegistryRepository : RecentItemRegistryRepository {
    private val byKey = mutableMapOf<String, RecentItemRegistry>()
    override suspend fun load(userId: String, exerciseId: MemoryExerciseId): RecentItemRegistry? =
        byKey["$userId:${exerciseId.name}"]

    override suspend fun save(userId: String, registry: RecentItemRegistry) {
        byKey["$userId:${registry.exerciseId.name}"] = registry
    }
}

class InMemorySpanResultStore : SpanResultStore {
    private val byUser = mutableMapOf<String, SpanRecordLog>()
    override suspend fun load(userId: String): SpanRecordLog = byUser[userId] ?: SpanRecordLog()

    override suspend fun record(
        userId: String,
        trial: SpanTrial,
        processingResponses: List<Int>,
    ): SpanRecordLog {
        val updated = load(userId).append(trial, processingResponses)
        byUser[userId] = updated
        return updated
    }
}

class InMemoryPairsResultStore : PairsResultStore {
    private val byUser = mutableMapOf<String, PairsRecordLog>()
    override suspend fun load(userId: String): PairsRecordLog = byUser[userId] ?: PairsRecordLog()

    override suspend fun record(userId: String, trial: PairsTrial): PairsRecordLog {
        val updated = load(userId).append(trial)
        byUser[userId] = updated
        return updated
    }
}

class InMemoryDeferredResultStore : DeferredResultStore {
    private val byUser = mutableMapOf<String, DeferredRecordLog>()
    override suspend fun load(userId: String): DeferredRecordLog = byUser[userId] ?: DeferredRecordLog()

    override suspend fun record(userId: String, outcome: DeferredRecallOutcome): DeferredRecordLog {
        val updated = load(userId).append(outcome)
        byUser[userId] = updated
        return updated
    }
}
