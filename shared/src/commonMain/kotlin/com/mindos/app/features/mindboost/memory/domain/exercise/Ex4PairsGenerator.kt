package com.mindos.app.features.mindboost.memory.domain.exercise

import com.mindos.app.features.mindboost.memory.domain.content.MemoryContent
import com.mindos.app.features.mindboost.memory.domain.content.model.AssociationStrength
import com.mindos.app.features.mindboost.memory.domain.content.model.PairId
import com.mindos.app.features.mindboost.memory.domain.content.model.PairItem
import com.mindos.app.features.mindboost.memory.domain.content.model.WordId
import com.mindos.app.features.mindboost.memory.domain.content.sampling.SamplingFilters
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyVector
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import kotlin.math.roundToLong
import kotlin.random.Random

/** Concrete EX4 parameters decoded from the relevant difficulty axes. */
internal data class Ex4Params(
    val pairCount: Int,
    val studyMsPerPair: Long,
    val delayMs: Long,
    val bidirectional: Boolean,
    val weakOnly: Boolean,
)

/**
 * Maps the difficulty vector to EX4 (Pares Asociados) parameters.
 *
 * V1 maps LOAD, DEPTH, DELAY, RETRIEVAL and SIMILARITY. The fan-effect
 * (INTERFERENCE via words shared across pairs) is DEFERRED — it is a set-level
 * concern requiring curated overlapping pairs the small V1 pool does not yet
 * support. [pairCount] is capped at 10 so a single-strength filter never exhausts
 * the V1 training pool (documented V1 limit, not a silent cap).
 */
@Suppress("MagicNumber")
internal object Ex4DifficultyMapping {
    private const val THRESHOLD = 0.5

    fun map(d: MemoryDifficultyVector): Ex4Params = Ex4Params(
        // Number of pairs grows with load: 4 → 10 (V1 pool-bound cap).
        pairCount = DifficultyDecode.decodeCount(d.load, 4, 10),
        // Deeper encoding demand ⇒ less study time per pair: 5000 → 3000 ms.
        studyMsPerPair = DifficultyDecode.lerp(5000.0, 3000.0, d.depth.value).roundToLong(),
        // Delay before test grows with the delay axis: 0 → 5 min.
        delayMs = DifficultyDecode.decodeMillis(d.delay, 0L, 300_000L),
        // High retrieval demand ⇒ bidirectional cued recall (A→B and B→A).
        bidirectional = d.retrieval.value >= THRESHOLD,
        // High similarity ⇒ weakly-associated pairs only (harder to bind).
        weakOnly = d.similarity.value >= THRESHOLD,
    )
}

/**
 * EX4 Pares Asociados generator. Produces study pairs plus cued-recall probes,
 * unidirectional or bidirectional, after a delay. Deterministic.
 */
class Ex4PairsGenerator : MemoryTrialGenerator {
    override val exerciseId: MemoryExerciseId = MemoryExerciseId.EX4_PARES_ASOCIADOS

    override fun generate(
        difficulty: MemoryDifficultyVector,
        content: MemoryContent,
        seed: Long,
        excludeRecentWordIds: Set<WordId>,
        excludeRecentPairIds: Set<PairId>,
    ): MemoryTrialPlan {
        val params = Ex4DifficultyMapping.map(difficulty)
        val sampler = content.samplerFor(exerciseId)
        val filters = if (params.weakOnly) {
            SamplingFilters(associationStrength = AssociationStrength.WEAK)
        } else {
            SamplingFilters.None
        }

        val studyPairs = sampler.samplePairs(
            count = params.pairCount,
            seed = seed,
            filters = filters,
            excludeRecentIds = excludeRecentPairIds,
        )

        val probes = buildProbes(studyPairs, params.bidirectional).shuffled(Random(seed))

        return PairedAssociatesTrial(
            studyPairs = studyPairs,
            probes = probes,
            studyMsPerPair = params.studyMsPerPair,
            delayMs = params.delayMs,
        )
    }

    private fun buildProbes(pairs: List<PairItem>, bidirectional: Boolean): List<CuedRecallProbe> =
        buildList {
            pairs.forEach { pair ->
                add(CuedRecallProbe(pair.cue, pair.target, RecallDirection.FORWARD))
                if (bidirectional) {
                    add(CuedRecallProbe(pair.target, pair.cue, RecallDirection.BACKWARD))
                }
            }
        }
}
