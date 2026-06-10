package com.mindos.app.features.mindboost.memory.domain.content

import com.mindos.app.features.mindboost.memory.domain.catalog.MemoryExerciseCatalog
import com.mindos.app.features.mindboost.memory.domain.content.data.SpanishMemoryContentData
import com.mindos.app.features.mindboost.memory.domain.content.model.Language
import com.mindos.app.features.mindboost.memory.domain.content.partition.TransferProbePartition
import com.mindos.app.features.mindboost.memory.domain.content.pool.PairPool
import com.mindos.app.features.mindboost.memory.domain.content.pool.WordPool
import com.mindos.app.features.mindboost.memory.domain.content.sampling.DeterministicSampler
import com.mindos.app.features.mindboost.memory.domain.content.sampling.deterministicSampler
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId

/**
 * Single facade the Phase-4 generators query for all content, without coupling to
 * pools/sampler/partition (mirrors the project's AppGraph wiring style).
 *
 * It exposes ONLY TRAINING pools (the transfer-probe reserve is already excluded),
 * so no training sample can ever touch a reserved probe — the anti-engaño guarantee
 * holds by construction (doc §6/§9/§13.4). V1 scope is enforced here:
 * [supportedExercises] mirrors [MemoryExerciseCatalog.implemented] and [samplerFor]
 * rejects any out-of-scope exercise.
 */
interface MemoryContent {
    fun supportedLanguages(): Set<Language>
    fun supportedExercises(): Set<MemoryExerciseId>
    fun trainingWordPool(language: Language = Language.SPANISH_ES): WordPool
    fun trainingPairPool(language: Language = Language.SPANISH_ES): PairPool
    fun transferProbePartition(language: Language = Language.SPANISH_ES): TransferProbePartition
    fun samplerFor(exerciseId: MemoryExerciseId, language: Language = Language.SPANISH_ES): DeterministicSampler
}

/**
 * Production content (Spanish). The transfer-probe partition uses a FIXED seed so
 * the reserved set is STABLE across app runs — a word reserved once is never later
 * sampled into training.
 */
class DefaultMemoryContent : MemoryContent {

    private val partition: TransferProbePartition = TransferProbePartition.partition(
        wordPool = SpanishMemoryContentData.wordPool,
        pairPool = SpanishMemoryContentData.pairPool,
        seed = PROBE_PARTITION_SEED,
    )

    private val trainingWords: WordPool = partition.trainingWordPool(SpanishMemoryContentData.WORD_POOL_VERSION)
    private val trainingPairs: PairPool = partition.trainingPairPool(SpanishMemoryContentData.PAIR_POOL_VERSION)
    private val sampler: DeterministicSampler = deterministicSampler(trainingWords, trainingPairs)

    override fun supportedLanguages(): Set<Language> = setOf(Language.SPANISH_ES)

    override fun supportedExercises(): Set<MemoryExerciseId> = MemoryExerciseCatalog.implemented.toSet()

    override fun trainingWordPool(language: Language): WordPool {
        requireSupported(language)
        return trainingWords
    }

    override fun trainingPairPool(language: Language): PairPool {
        requireSupported(language)
        return trainingPairs
    }

    override fun transferProbePartition(language: Language): TransferProbePartition {
        requireSupported(language)
        return partition
    }

    override fun samplerFor(exerciseId: MemoryExerciseId, language: Language): DeterministicSampler {
        requireSupported(language)
        require(exerciseId in supportedExercises()) {
            "Exercise $exerciseId is not in the V1 implemented set ${supportedExercises()}."
        }
        return sampler
    }

    private fun requireSupported(language: Language) {
        require(language in supportedLanguages()) {
            "Language $language is not supported; available: ${supportedLanguages()}."
        }
    }

    private companion object {
        /** Fixed seed → stable transfer-probe reserve across runs. */
        const val PROBE_PARTITION_SEED = 0x4D454D31L // "MEM1"
    }
}

/** Production factory (Spanish content). */
fun defaultMemoryContent(): MemoryContent = DefaultMemoryContent()
