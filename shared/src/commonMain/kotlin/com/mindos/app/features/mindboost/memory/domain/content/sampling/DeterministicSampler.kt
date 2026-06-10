package com.mindos.app.features.mindboost.memory.domain.content.sampling

import com.mindos.app.features.mindboost.memory.domain.content.model.PairId
import com.mindos.app.features.mindboost.memory.domain.content.model.PairItem
import com.mindos.app.features.mindboost.memory.domain.content.model.WordId
import com.mindos.app.features.mindboost.memory.domain.content.model.WordItem
import com.mindos.app.features.mindboost.memory.domain.content.pool.PairPool
import com.mindos.app.features.mindboost.memory.domain.content.pool.WordPool
import kotlin.random.Random

/**
 * Deterministic content sampler — the mechanism, NOT the trial generator.
 *
 * Same (count, seed, filters, exclude) → same subset, replayable and unit-testable,
 * the same contract as focus's `TrialGenerator` (seeded `Random`, `shuffled`).
 *
 * Anti-engaño (doc §6/§13.4): [excludeRecentIds] removes already-seen items so the
 * caller (Phase 7/10) can guarantee no repetition across sessions.
 *
 * Fails loudly, never silently: if filtering + exclusion leaves fewer than [count]
 * items it throws with explicit counts instead of returning a short list or
 * duplicating. The result never contains duplicates.
 *
 * RNG note: [Random] (seeded, non-cryptographic) is the correct and deliberate
 * choice here — pools are authored, public content (not secrets), and reproducible
 * seeding is required for tests/replay; a CSPRNG cannot be reproducibly seeded.
 */
interface DeterministicSampler {
    fun sampleWords(
        count: Int,
        seed: Long,
        filters: SamplingFilters = SamplingFilters.None,
        excludeRecentIds: Set<WordId> = emptySet(),
    ): List<WordItem>

    fun samplePairs(
        count: Int,
        seed: Long,
        filters: SamplingFilters = SamplingFilters.None,
        excludeRecentIds: Set<PairId> = emptySet(),
    ): List<PairItem>
}

/** Production sampler over a [WordPool] + [PairPool]. */
class DefaultDeterministicSampler(
    private val wordPool: WordPool,
    private val pairPool: PairPool,
) : DeterministicSampler {

    override fun sampleWords(
        count: Int,
        seed: Long,
        filters: SamplingFilters,
        excludeRecentIds: Set<WordId>,
    ): List<WordItem> {
        require(count > 0) { "sampleWords count must be > 0 (was $count)." }
        val filtered = wordPool.items.filter { word ->
            (filters.category == null || word.category == filters.category) &&
                (filters.concreteness == null || word.concreteness == filters.concreteness) &&
                (filters.frequency == null || word.frequency == filters.frequency)
        }
        val available = filtered.filterNot { it.id in excludeRecentIds }
        checkEnough(
            requested = count,
            total = wordPool.size,
            afterFilter = filtered.size,
            afterExclude = available.size,
            kind = "words",
        )
        return available.shuffled(Random(seed)).take(count)
    }

    override fun samplePairs(
        count: Int,
        seed: Long,
        filters: SamplingFilters,
        excludeRecentIds: Set<PairId>,
    ): List<PairItem> {
        require(count > 0) { "samplePairs count must be > 0 (was $count)." }
        val filtered = pairPool.items.filter { pair ->
            filters.associationStrength == null || pair.associationStrength == filters.associationStrength
        }
        val available = filtered.filterNot { it.id in excludeRecentIds }
        checkEnough(
            requested = count,
            total = pairPool.size,
            afterFilter = filtered.size,
            afterExclude = available.size,
            kind = "pairs",
        )
        return available.shuffled(Random(seed)).take(count)
    }

    private fun checkEnough(requested: Int, total: Int, afterFilter: Int, afterExclude: Int, kind: String) {
        check(afterExclude >= requested) {
            "Not enough $kind to sample: requested $requested but only $afterExclude available " +
                "(pool=$total, afterFilter=$afterFilter, afterExclude=$afterExclude). " +
                "Expand the pool or relax filters/exclusions — no silent truncation is applied."
        }
    }
}

/** Factory mirroring the project's `rememberX`/`defaultX` wiring style. */
fun deterministicSampler(wordPool: WordPool, pairPool: PairPool): DeterministicSampler =
    DefaultDeterministicSampler(wordPool, pairPool)
