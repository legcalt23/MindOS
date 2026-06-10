package com.mindos.app.features.mindboost.memory.domain.exercise

import com.mindos.app.features.mindboost.memory.domain.content.MemoryContent
import com.mindos.app.features.mindboost.memory.domain.content.model.PairId
import com.mindos.app.features.mindboost.memory.domain.content.model.WordCategory
import com.mindos.app.features.mindboost.memory.domain.content.model.WordId
import com.mindos.app.features.mindboost.memory.domain.content.model.WordItem
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyVector
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import kotlin.math.roundToLong

/** Concrete EX3 parameters decoded from the relevant difficulty axes. */
internal data class Ex3Params(
    val listLength: Int,
    val studyMsPerItem: Long,
    val delayMs: Long,
    val freeRecall: Boolean,
)

/**
 * Maps the difficulty vector to EX3 (Lista con Estrategia) parameters.
 *
 * V1 maps LOAD, DEPTH, DELAY and RETRIEVAL. SIMILARITY and INTERFERENCE are
 * declared relevant for EX3 but their content-control (single-category lists,
 * multi-list interference) is DEFERRED until the ES pool grows / Phase 7 wires
 * session-level interference — sampling stays mixed here to avoid exhausting the
 * small V1 pool. Documented gap, not an oversight.
 */
@Suppress("MagicNumber")
internal object Ex3DifficultyMapping {
    private const val THRESHOLD = 0.5

    fun map(d: MemoryDifficultyVector): Ex3Params = Ex3Params(
        // List length grows with load: 8 → 20 words.
        listLength = DifficultyDecode.decodeCount(d.load, 8, 20),
        // Deeper encoding demand ⇒ LESS study time (forces elaboration): 4000 → 2000 ms.
        studyMsPerItem = DifficultyDecode.lerp(4000.0, 2000.0, d.depth.value).roundToLong(),
        // Delay before recall grows with the delay axis: 0 → 5 min.
        delayMs = DifficultyDecode.decodeMillis(d.delay, 0L, 300_000L),
        // High retrieval demand ⇒ free recall (harder); low ⇒ category-cued recall.
        freeRecall = d.retrieval.value >= THRESHOLD,
    )
}

/**
 * EX3 Lista con Estrategia generator. Produces a study list recalled after a delay,
 * either freely or with a category cue. Deterministic.
 */
class Ex3ListGenerator : MemoryTrialGenerator {
    override val exerciseId: MemoryExerciseId = MemoryExerciseId.EX3_LISTA_ESTRATEGIA

    override fun generate(
        difficulty: MemoryDifficultyVector,
        content: MemoryContent,
        seed: Long,
        excludeRecentWordIds: Set<WordId>,
        excludeRecentPairIds: Set<PairId>,
    ): MemoryTrialPlan {
        val params = Ex3DifficultyMapping.map(difficulty)
        val sampler = content.samplerFor(exerciseId)

        val studyList = sampler.sampleWords(
            count = params.listLength,
            seed = seed,
            excludeRecentIds = excludeRecentWordIds,
        )

        val recallType = if (params.freeRecall) RecallType.FREE else RecallType.CUED
        val cueCategory = if (params.freeRecall) null else dominantCategory(studyList)

        return RecallListTrial(
            studyList = studyList,
            recallType = recallType,
            studyMsPerItem = params.studyMsPerItem,
            delayMs = params.delayMs,
            cueCategory = cueCategory,
        )
    }

    /** The most frequent category in [list], deterministic by encounter order on ties. */
    private fun dominantCategory(list: List<WordItem>): WordCategory =
        requireNotNull(list.groupingBy { it.category }.eachCount().maxByOrNull { it.value }) {
            "dominantCategory requires a non-empty list."
        }.key
}
