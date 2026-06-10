package com.mindos.app.features.mindboost.memory.domain.content.partition

import com.mindos.app.features.mindboost.memory.domain.content.model.Language
import com.mindos.app.features.mindboost.memory.domain.content.model.PairItem
import com.mindos.app.features.mindboost.memory.domain.content.model.WordItem
import com.mindos.app.features.mindboost.memory.domain.content.pool.PairPool
import com.mindos.app.features.mindboost.memory.domain.content.pool.WordPool
import kotlin.random.Random

/**
 * Disjoint split of the content into a TRAINING view and a RESERVED transfer-probe
 * view that training NEVER touches — the structural core of the anti-engaño
 * principle (doc §6/§9/§13.4). The reserved set is consumed only by Phase-8
 * evaluation. Disjointness is validated in `init` (fail at construction, not late).
 *
 * [partition] is deterministic (same seed → same reserve) so the reserved probes
 * are STABLE across app runs — a word reserved once is never later sampled into
 * training. Callers must therefore use a FIXED seed for a given language/version.
 */
data class TransferProbePartition(
    val language: Language,
    val trainingWords: List<WordItem>,
    val reservedWords: List<WordItem>,
    val trainingPairs: List<PairItem>,
    val reservedPairs: List<PairItem>,
) {
    init {
        val trainingWordIds = trainingWords.map { it.id }.toSet()
        val reservedWordIds = reservedWords.map { it.id }.toSet()
        require(trainingWordIds.intersect(reservedWordIds).isEmpty()) {
            "Training and reserved words must be disjoint."
        }
        val trainingPairIds = trainingPairs.map { it.id }.toSet()
        val reservedPairIds = reservedPairs.map { it.id }.toSet()
        require(trainingPairIds.intersect(reservedPairIds).isEmpty()) {
            "Training and reserved pairs must be disjoint."
        }
    }

    /** The training-only word pool (reserve already excluded). */
    fun trainingWordPool(version: Int): WordPool =
        WordPool(version = version, language = language, items = trainingWords)

    /** The training-only pair pool (reserve already excluded). */
    fun trainingPairPool(version: Int): PairPool =
        PairPool(version = version, language = language, items = trainingPairs)

    val reservedWordCount: Int get() = reservedWords.size
    val reservedPairCount: Int get() = reservedPairs.size

    companion object {
        const val DEFAULT_WORD_RESERVE_RATIO = 0.20
        const val DEFAULT_PAIR_RESERVE_RATIO = 0.20

        /**
         * Deterministically reserves [wordReserveRatio]/[pairReserveRatio] of each
         * pool as transfer probes. Exhaustive: training ∪ reserved == original pool.
         */
        fun partition(
            wordPool: WordPool,
            pairPool: PairPool,
            seed: Long,
            wordReserveRatio: Double = DEFAULT_WORD_RESERVE_RATIO,
            pairReserveRatio: Double = DEFAULT_PAIR_RESERVE_RATIO,
        ): TransferProbePartition {
            require(wordReserveRatio in 0.0..1.0) { "wordReserveRatio must be in 0.0..1.0." }
            require(pairReserveRatio in 0.0..1.0) { "pairReserveRatio must be in 0.0..1.0." }

            val shuffledWords = wordPool.items.shuffled(Random(seed))
            val reservedWordCount = (wordPool.size * wordReserveRatio).toInt()

            val shuffledPairs = pairPool.items.shuffled(Random(seed))
            val reservedPairCount = (pairPool.size * pairReserveRatio).toInt()

            return TransferProbePartition(
                language = wordPool.language,
                reservedWords = shuffledWords.take(reservedWordCount),
                trainingWords = shuffledWords.drop(reservedWordCount),
                reservedPairs = shuffledPairs.take(reservedPairCount),
                trainingPairs = shuffledPairs.drop(reservedPairCount),
            )
        }
    }
}
