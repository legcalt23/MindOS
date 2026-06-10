package com.mindos.app.features.mindboost.memory.domain.content.sampling

import com.mindos.app.features.mindboost.memory.domain.content.model.PairId
import com.mindos.app.features.mindboost.memory.domain.content.model.WordId
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId

/**
 * Pure, copy-on-write registry of which content items were shown to a user and
 * when, per exercise — the data the sampler's `excludeRecentIds` consumes to
 * enforce cross-session no-repetition (anti-engaño, doc §6/§13.4).
 *
 * Time is injected: every method takes `nowEpoch`/`usedAtEpoch` (epoch millis)
 * from the caller's `EpochClock`. This class NEVER calls `Clock.System` (project
 * learning 2026-05-29). Persistence of this registry is Phase 10.
 */
data class RecentItemRegistry(
    val exerciseId: MemoryExerciseId,
    val wordUsage: Map<WordId, Long> = emptyMap(),
    val pairUsage: Map<PairId, Long> = emptyMap(),
    val ttlMillis: Long = DEFAULT_TTL_MILLIS,
) {
    init {
        require(ttlMillis > 0) { "ttlMillis must be > 0 (was $ttlMillis)." }
    }

    /** Word ids used within [ttlMillis] before [nowEpoch] — pass to `excludeRecentIds`. */
    fun recentWordIds(nowEpoch: Long): Set<WordId> =
        wordUsage.filterValues { nowEpoch - it < ttlMillis }.keys

    /** Pair ids used within [ttlMillis] before [nowEpoch]. */
    fun recentPairIds(nowEpoch: Long): Set<PairId> =
        pairUsage.filterValues { nowEpoch - it < ttlMillis }.keys

    fun withWordsUsed(ids: Collection<WordId>, usedAtEpoch: Long): RecentItemRegistry =
        copy(wordUsage = wordUsage + ids.associateWith { usedAtEpoch })

    fun withPairsUsed(ids: Collection<PairId>, usedAtEpoch: Long): RecentItemRegistry =
        copy(pairUsage = pairUsage + ids.associateWith { usedAtEpoch })

    /** Drops entries older than [ttlMillis] relative to [nowEpoch] (keeps the map bounded). */
    fun pruned(nowEpoch: Long): RecentItemRegistry = copy(
        wordUsage = wordUsage.filterValues { nowEpoch - it < ttlMillis },
        pairUsage = pairUsage.filterValues { nowEpoch - it < ttlMillis },
    )

    companion object {
        private const val DAYS_RETAINED = 14L
        private const val MILLIS_PER_DAY = 86_400_000L

        /** Default no-repetition window: 14 days. */
        const val DEFAULT_TTL_MILLIS: Long = DAYS_RETAINED * MILLIS_PER_DAY
    }
}
